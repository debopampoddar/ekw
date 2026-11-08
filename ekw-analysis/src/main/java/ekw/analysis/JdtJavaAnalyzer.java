package ekw.analysis;

import ekw.analysis.Facts.CallFact;
import ekw.analysis.Facts.CallFlag;
import ekw.analysis.Facts.FactSink;
import ekw.analysis.Facts.MemberFact;
import ekw.analysis.Facts.MemberKind;
import ekw.analysis.Facts.ModuleSpec;
import ekw.analysis.Facts.SymbolId;
import ekw.analysis.Facts.TypeFact;
import ekw.analysis.Facts.TypeKind;
import ekw.core.Evidence.CodeLocation;
import ekw.core.Fingerprint;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java facts with Eclipse JDT (design: stage 2, section 3.2.3): one batch parse per module with bindings resolved.
 * Emits types, members and call sites to a {@link Facts.FactSink}. Reads source and class files only; runs nothing.
 *
 * <p>Chapter 4 of the development guide.
 */
public final class JdtJavaAnalyzer implements Facts.LanguageAnalyzer {
    private final Path repositoryRoot;
    private final List<Path> siblingSourceRoots;

    /**
     * Paths in facts are relative to {@code repositoryRoot}, so citations work for every module.
     */
    public JdtJavaAnalyzer(Path repositoryRoot) {
        this(repositoryRoot, List.of());
    }

    /**
     * @param siblingSourceRoots other modules' src/main/java folders: on the source path, so calls into sibling modules
     *                           resolve; their files are not analyzed in this pass
     */
    public JdtJavaAnalyzer(Path repositoryRoot, List<Path> siblingSourceRoots) {
        this.repositoryRoot = repositoryRoot;
        this.siblingSourceRoots =
                null ==  siblingSourceRoots ? List.of() : List.copyOf(siblingSourceRoots);
    }

    @Override
    public String language() {
        return "java";
    }

    @Override
    public boolean accepts(Facts.ModuleSpec module) {
        return module.sourceRoots().stream().anyMatch(Files::isDirectory);
    }

    @Override
    public Facts.AnalysisReport analyze(Facts.ModuleSpec module, Facts.Classpath classpath, Facts.FactSink sink) {
        long t0 = System.nanoTime();
        Set<String> testFiles = new HashSet<>(javaFiles(module.testRoots()));
        List<String> files = new ArrayList<>(javaFiles(module.sourceRoots()));
        files.addAll(testFiles);

        List<String> sourcepathEntries = Stream.of(module.sourceRoots(), module.testRoots(), siblingSourceRoots)
                .flatMap(List::stream)
                .filter(Files::isDirectory)
                .distinct()
                .map(path -> path.toAbsolutePath().normalize().toString())
                .collect(Collectors.toList());

        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());  // the newest Java language level JDT knows
        parser.setKind(ASTParser.K_COMPILATION_UNIT);                // we give it whole files
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(module.javaRelease(), options);  // the module's Java level
        parser.setCompilerOptions(options);
        parser.setEnvironment(
                classpath.jars()
                         .stream()
                         .map(Path::toString)
                         .toArray(String[]::new), // String[]: the classpath
                sourcepathEntries.toArray(String[]::new),// String []: folders with .java files
                utf8(sourcepathEntries.size()),  // String[]: "UTF-8" per source root
                true);                                               // also use the running JDK's classes
        parser.setResolveBindings(true);                             // we want bindings
        parser.setBindingsRecovery(true);                            // keep going when something is missing
        parser.setStatementsRecovery(true);                          // and when a statement does not parse
        int[] counts = new int[2];       // [call sites, resolved]
        parser.createASTs(files.toArray(String[]::new),
                utf8(files.size()),
                new String[0],
                new FileASTRequestor() {
                    @Override
                    public void acceptAST(String sourceFilePath, CompilationUnit cu) {
                        try {
                            String source = Files.readString(Path.of(sourceFilePath));
                            String rel = repositoryRoot.relativize(Path.of(sourceFilePath)
                                                                       .toAbsolutePath()
                                                                       .normalize())
                                                       .toString()
                                                       .replace('\\', '/');
                            cu.accept(new Visitor(module, rel, cu, source,
                                    testFiles.contains(sourceFilePath), sink, counts));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, null);
        return new Facts.AnalysisReport(module.name(), files.size(), counts[0], counts[1], counts[0] - counts[1],
                Duration.ofNanos(System.nanoTime() - t0), new ArrayList<>());
    }

    private static List<String> javaFiles(List<Path> pathList) {
        List<String> javaFilesLocation = new ArrayList<>();
        for (Path folder : pathList) {
            if (!Files.isDirectory(folder))
                continue;

            try (Stream<Path> s = Files.walk(folder)) {
                s.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .map(Path::toString)
                 .sorted()
                 .forEach(javaFilesLocation::add);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return javaFilesLocation;
    }

    private static String[] utf8(int n) {
        String[] a = new String[n];
        Arrays.fill(a, "UTF-8");
        return a;
    }

    static SymbolId typeId(ITypeBinding t) {
        return new SymbolId("java:" + t.getErasure().getQualifiedName());
    }

    static SymbolId methodId(IMethodBinding b) {
        IMethodBinding md = b.getMethodDeclaration();
        String params = Arrays.stream(md.getParameterTypes()).map(p -> p.getErasure().getQualifiedName())
                              .collect(Collectors.joining(","));
        String owner = md.getDeclaringClass().getErasure().getQualifiedName();
        return new SymbolId("java:" + owner + "#" + (md.isConstructor() ? "<init>" : md.getName()) + "(" + params + ")");
    }

    // ---- visitor --------------------------------------------------------------------------------------------------

    private static final class Visitor extends ASTVisitor {
        private final ModuleSpec module;
        private final String path;
        private final CompilationUnit cu;
        private final String source;
        private final boolean test;
        private final FactSink sink;
        private final int[] counts;

        Visitor(ModuleSpec module, String path, CompilationUnit cu, String source, boolean test, FactSink sink,
                int[] counts) {
            super(true);    // visit Javadoc tags too (none needed today; cheap)
            this.module = module;
            this.path = path;
            this.cu = cu;
            this.source = source;
            this.test = test;
            this.sink = sink;
            this.counts = counts;
        }

        /**
         * Declaration span without its Javadoc, so a citation starts at the declaration line.
         */
        private CodeLocation span(ASTNode node, Javadoc doc) {
            int start = node.getStartPosition();
            if (doc != null) {
                start = doc.getStartPosition() + doc.getLength();
                while (start < source.length() && Character.isWhitespace(source.charAt(start)))
                    start++;
            }
            int end = node.getStartPosition() + node.getLength() - 1;
            return new CodeLocation(module.system(), module.commit(), path, cu.getLineNumber(start),
                    cu.getLineNumber(end), Optional.empty());
        }

        private CodeLocation at(int pos) {
            int line = cu.getLineNumber(pos);
            return new CodeLocation(module.system(), module.commit(), path, line, line, Optional.empty());
        }

        private static Optional<String> firstSentence(Javadoc doc) {
            if (doc == null) return Optional.empty();
            StringBuilder sb = new StringBuilder();
            for (Object o : doc.tags()) {
                TagElement tag = (TagElement) o;
                if (tag.getTagName() != null)
                    break;     // only the main description
                for (Object f : tag.fragments())
                    if (f instanceof TextElement te)
                        sb.append(te.getText()).append(' ');
            }
            String text = sb.toString().trim().replaceAll("\\s+", " ");
            int dot = text.indexOf(". ");
            text = dot >= 0 ? text.substring(0, dot + 1) : text;
            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        }

        /**
         * JDT modifier flags use the JVM bit values for these keywords; "default" is JDT-only.
         */
        private static Set<String> modifiers(int flags) {
            Set<String> out = Arrays.stream(
                    java.lang.reflect.Modifier.toString(flags).split(" "))
                                    .filter(s -> !s.isEmpty())
                                    .collect(Collectors.toCollection(java.util.TreeSet::new));
            if (Modifier.isDefault(flags)) out.add("default");
            return out;
        }

        /**
         * Hash of the declaration text without Javadoc, spaces collapsed: recognizes code that moved unchanged.
         */
        private String bodyHash(ASTNode node, Javadoc doc) {
            String text = source.substring(node.getStartPosition(), node.getStartPosition() + node.getLength());
            if (doc != null) text = text.substring(doc.getLength());
            return Fingerprint.sha256Hex(text.strip()
                                             .replaceAll("[ \\t]+", " "))
                              .substring(0, 16);
        }

        private boolean type(AbstractTypeDeclaration node, TypeKind kind) {
            ITypeBinding b = node.resolveBinding();
            if (b == null) return true;
            List<SymbolId> supers = new ArrayList<>();
            if (b.getSuperclass() != null
                    && !"java.lang.Object".equals(b.getSuperclass().getQualifiedName())) {
                supers.add(typeId(b.getSuperclass()));
            }
            for (ITypeBinding i : b.getInterfaces())
                supers.add(typeId(i));
            sink.type(new TypeFact(typeId(b), module.name(), kind, modifiers(b.getModifiers()),
                    span(node, node.getJavadoc()), firstSentence(node.getJavadoc()), supers, test));
            return true;
        }

        @Override
        public boolean visit(TypeDeclaration n) {
            return type(n,
                    n.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS);
        }

        @Override
        public boolean visit(EnumDeclaration n) {
            return type(n, TypeKind.ENUM);
        }

        @Override
        public boolean visit(RecordDeclaration n) {
            return type(n, TypeKind.RECORD);
        }

        @Override
        public boolean visit(AnnotationTypeDeclaration n) {
            return type(n, TypeKind.ANNOTATION);
        }

        @Override
        public boolean visit(MethodDeclaration n) {
            IMethodBinding b = n.resolveBinding();
            if (b == null || b.getDeclaringClass() == null) return true;
            sink.member(new MemberFact(methodId(b), typeId(b.getDeclaringClass()),
                    n.isConstructor() ? MemberKind.CONSTRUCTOR
                                      : MemberKind.METHOD, b.toString(),
                    modifiers(n.getModifiers()), span(n, n.getJavadoc()), bodyHash(n, n.getJavadoc()),
                    firstSentence(n.getJavadoc())));
            return true;
        }

        @Override
        public boolean visit(FieldDeclaration n) {
            for (Object o : n.fragments()) {
                IVariableBinding v = ((VariableDeclarationFragment) o).resolveBinding();
                if (v == null || v.getDeclaringClass() == null) continue;
                String owner = v.getDeclaringClass()
                                .getErasure()
                                .getQualifiedName();
                sink.member(new MemberFact(new SymbolId("java:" + owner + "#" + v.getName()), typeId(v.getDeclaringClass()),
                        MemberKind.FIELD, v.getType()
                                           .getErasure()
                                           .getQualifiedName() + " " + v.getName(),
                        modifiers(n.getModifiers()), span(n, n.getJavadoc()), bodyHash(n, n.getJavadoc()),
                        firstSentence(n.getJavadoc())));
            }
            return true;
        }

        // ---- call sites (as in the spike: every invocation form, with flags) --------------------------------------

        @Override
        public boolean visit(MethodInvocation n) {
            call(n, n.getName()
                     .getStartPosition(), n.resolveMethodBinding(), n.arguments(),
                    n.getExpression() instanceof MethodInvocation, Set.of());
            return true;
        }

        @Override
        public boolean visit(SuperMethodInvocation n) {
            call(n, n.getName()
                     .getStartPosition(), n.resolveMethodBinding(), n.arguments(), false, Set.of(CallFlag.SUPER_CALL));
            return true;
        }

        @Override
        public boolean visit(ClassInstanceCreation n) {
            call(n, n.getType()
                     .getStartPosition(), n.resolveConstructorBinding(), n.arguments(), false, Set.of(CallFlag.CONSTRUCTOR_CALL));
            return true;
        }

        @Override
        public boolean visit(ConstructorInvocation n) {
            call(n, n.getStartPosition(), n.resolveConstructorBinding(), n.arguments(), false, Set.of(CallFlag.CONSTRUCTOR_CALL));
            return true;
        }

        @Override
        public boolean visit(SuperConstructorInvocation n) {
            call(n, n.getStartPosition(), n.resolveConstructorBinding(), n.arguments(), false, Set.of(CallFlag.CONSTRUCTOR_CALL, CallFlag.SUPER_CALL));
            return true;
        }

        @Override
        public boolean visit(ExpressionMethodReference n) {
            ref(n, n.resolveMethodBinding());
            return true;
        }

        @Override
        public boolean visit(TypeMethodReference n) {
            ref(n, n.resolveMethodBinding());
            return true;
        }

        @Override
        public boolean visit(SuperMethodReference n) {
            ref(n, n.resolveMethodBinding());
            return true;
        }

        @Override
        public boolean visit(CreationReference n) {
            ref(n, n.resolveMethodBinding());
            return true;
        }

        private void ref(ASTNode n, IMethodBinding b) {
            call(n, n.getStartPosition(), b, List.of(), false, Set.of(CallFlag.METHOD_REFERENCE));
        }

        private void call(ASTNode n, int pos, IMethodBinding b, List<?> args, boolean chained, Set<CallFlag> base) {
            EnumSet<CallFlag> flags = EnumSet.noneOf(CallFlag.class);
            flags.addAll(base);
            if (chained) flags.add(CallFlag.CHAINED);
            for (Object a : args)
                if (a instanceof LambdaExpression || a instanceof MethodReference)
                    flags.add(CallFlag.PASSES_LAMBDA);
            ASTNode enclosing = null;
            for (ASTNode p = n.getParent(); p != null; p = p.getParent()) {
                if (p instanceof LambdaExpression)
                    flags.add(CallFlag.IN_LAMBDA);
                if (p instanceof AnonymousClassDeclaration)
                    flags.add(CallFlag.IN_ANONYMOUS);
                if (p instanceof ForStatement || p instanceof EnhancedForStatement || p instanceof WhileStatement
                        || p instanceof DoStatement)
                    flags.add(CallFlag.IN_LOOP);
                if (p instanceof IfStatement || p instanceof ConditionalExpression || p instanceof SwitchStatement
                        || p instanceof SwitchExpression)
                    flags.add(CallFlag.CONDITIONAL);
                if (p instanceof CatchClause) flags.add(CallFlag.IN_CATCH);
                if (p.getLocationInParent() == TryStatement.FINALLY_PROPERTY)
                    flags.add(CallFlag.IN_FINALLY);
                if (enclosing == null && (p instanceof MethodDeclaration || p instanceof Initializer
                        || p instanceof FieldDeclaration)) enclosing = p;
                if (p instanceof AbstractTypeDeclaration) break;
            }
            boolean resolved = b != null && !b.isRecovered() && !b.getMethodDeclaration()
                                                                  .getDeclaringClass()
                                                                  .isRecovered();
            counts[0]++;
            if (resolved) counts[1]++;
            sink.call(new CallFact(callerId(enclosing),
                    resolved ? Optional.of(methodId(b))
                             : Optional.empty(), at(pos),
                    flags, resolved ? Optional.empty() : Optional.of(
                    b == null ? "no binding" : "recovered binding")));
        }

        private SymbolId callerId(ASTNode enclosing) {
            if (enclosing instanceof MethodDeclaration md && md.resolveBinding() != null)
                return methodId(md.resolveBinding());
            for (ASTNode p = enclosing; p != null; p = p.getParent()) {
                if (p instanceof AbstractTypeDeclaration t && t.resolveBinding() != null) {
                    return new SymbolId(typeId(t.resolveBinding()).value() + "#<clinit>");
                }
            }
            return new SymbolId("java:?");
        }
    }
}
