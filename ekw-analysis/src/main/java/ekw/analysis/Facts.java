package ekw.analysis;

import ekw.core.Evidence.CodeLocation;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Facts produced by static analysis. Never produced by a model. */
public final class Facts {
    private Facts() {}

    /** e.g. "java:org.apache.ibatis.binding.MapperProxy#invoke(java.lang.Object,java.lang.reflect.Method,java.lang.Object[])" */
    public record SymbolId(String value) {}

    public enum TypeKind { CLASS, INTERFACE, ENUM, RECORD, ANNOTATION }

    public enum MemberKind { METHOD, CONSTRUCTOR, FIELD, RECORD_COMPONENT, ENUM_CONSTANT }

    public enum CallFlag {
        IN_LAMBDA, PASSES_LAMBDA, CHAINED, METHOD_REFERENCE, IN_ANONYMOUS,
        IN_LOOP, CONDITIONAL, IN_CATCH, IN_FINALLY, CONSTRUCTOR_CALL, SUPER_CALL, VIRTUAL
    }

    public record TypeFact(SymbolId id, String module, TypeKind kind, Set<String> modifiers, CodeLocation span,
                           Optional<String> javadocFirstSentence, List<SymbolId> supertypes, boolean test) {}

    /** bodyHash lets relocated code be recognized (see evidence relocation). */
    public record MemberFact(SymbolId id, SymbolId owner, MemberKind kind, String signature, Set<String> modifiers,
                             CodeLocation span, String bodyHash, Optional<String> javadocFirstSentence) {}

    public record CallFact(SymbolId caller, Optional<SymbolId> callee, CodeLocation site, Set<CallFlag> flags,
                           Optional<String> unresolvedReason) {}

    public record AnnotationFact(SymbolId target, String annotationType, Map<String, String> values,
                                 CodeLocation site) {}

    /** A configuration key read in code or declared in a properties/yaml file; values are never stored. */
    public record ConfigKeyFact(String key, Optional<String> defaultValue, CodeLocation site, String how) {}

    /** A log call whose first argument is a constant code, e.g. Dubbo's "0-1". */
    public record ErrorCodeFact(String code, Optional<SymbolId> constant, CodeLocation logSite, String level) {}

    /** SPI registration: interface -> name -> implementation (META-INF/services, META-INF/dubbo/**). */
    public record SpiFact(SymbolId extensionInterface, String name, SymbolId implementation, CodeLocation file) {}

    public record ModuleFact(String name, String groupId, String artifactId, Path root, String javaRelease,
                             List<String> dependsOnModules) {}

    public record DependencyFact(String module, String groupId, String artifactId, String version, String scope,
                                 boolean optional, boolean resolved, CodeLocation declaredAt) {}

    public record ModuleSpec(String system, String commit, String name, Path root, List<Path> sourceRoots,
                             List<Path> testRoots, String javaRelease) {}

    public record Classpath(List<Path> jars, List<String> unresolvedCoordinates) {}

    public record AnalysisReport(String module, int files, int callSites, int resolvedCallSites,
                                 int unresolvedCallSites, Duration time, List<String> warnings) {}

    /** Receives facts as they are produced; the SQLite store implements it. */
    public interface FactSink {
        void type(TypeFact t);
        void member(MemberFact m);
        void call(CallFact c);
        void annotation(AnnotationFact a);
        void configKey(ConfigKeyFact k);
        void errorCode(ErrorCodeFact e);
        void spi(SpiFact s);
        void module(ModuleFact m);
        void dependency(DependencyFact d);
    }

    /** Extension point for languages: Java (Eclipse JDT) now; Python and others later, same fact records. */
    public interface LanguageAnalyzer {
        String language();
        boolean accepts(ModuleSpec module);
        AnalysisReport analyze(ModuleSpec module, Classpath classpath, FactSink sink);
    }

    /** Resolves dependency jars from Maven Central without running any build or plugin. */
    public interface ClasspathResolver {
        Classpath resolve(ModuleSpec module);
    }

    /** Read side used by the planner, renderer, checks and the code_facts tool. */
    public interface FactsStore {
        Optional<TypeFact> type(String system, SymbolId id);
        List<TypeFact> findTypes(String system, String simpleOrQualifiedName, int limit);
        List<MemberFact> members(String system, SymbolId type);
        List<CallFact> callers(String system, SymbolId callee, int limit);
        List<CallFact> callees(String system, SymbolId caller, int limit);
        List<TypeFact> implementations(String system, SymbolId type, int limit);
        List<ConfigKeyFact> configKeys(String system, String keyFragment, int limit);
        List<ErrorCodeFact> errorCodes(String system, String code);
        List<DependencyFact> dependencies(String system, String coordinateFragment, int limit);
        Optional<MemberFact> memberByBodyHash(String system, String bodyHash);
        Map<SymbolId, Double> rank(String system);
    }
}
