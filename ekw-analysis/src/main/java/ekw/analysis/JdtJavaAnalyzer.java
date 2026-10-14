package ekw.analysis;

import ekw.core.Todo;

import java.nio.file.Path;
import java.util.List;

/**
 * Java facts with Eclipse JDT (design: stage 2, section 3.2.3): one batch parse per module with bindings resolved.
 * Emits types, members and call sites to a {@link Facts.FactSink}. Reads source and class files only; runs nothing.
 *
 * <p>Chapter 4 of the development guide.
 */
public final class JdtJavaAnalyzer implements Facts.LanguageAnalyzer {

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
        // TODO (chapter 4): keep both arguments in fields.
    }

    @Override
    public String language() {
        return "java";
    }

    @Override
    public boolean accepts(Facts.ModuleSpec module) {
        throw Todo.chapter(4, "JdtJavaAnalyzer.accepts");
    }

    @Override
    public Facts.AnalysisReport analyze(Facts.ModuleSpec module, Facts.Classpath classpath, Facts.FactSink sink) {
        throw Todo.chapter(4, "JdtJavaAnalyzer.analyze");
    }
}
