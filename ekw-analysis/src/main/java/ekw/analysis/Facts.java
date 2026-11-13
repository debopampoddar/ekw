package ekw.analysis;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Facts produced by static analysis. Never produced by a model.
 *
 * <p>The fact records themselves ({@link TypeFact}, {@link MemberFact}, {@link CallFact}, and so on) live in their
 * own top-level classes, each with a builder; this class holds the small shared types and the interfaces that tie
 * them together.
 */
public final class Facts {
    private Facts() {
    }

    /**
     * e.g. "java:org.apache.ibatis.binding.MapperProxy#invoke(java.lang.Object,java.lang.reflect.Method,java.lang.Object[])"
     */
    public record SymbolId(String value) {
    }

    public enum TypeKind {CLASS, INTERFACE, ENUM, RECORD, ANNOTATION}

    public enum MemberKind {METHOD, CONSTRUCTOR, FIELD, RECORD_COMPONENT, ENUM_CONSTANT}

    public enum CallFlag {
        IN_LAMBDA, PASSES_LAMBDA, CHAINED, METHOD_REFERENCE, IN_ANONYMOUS,
        IN_LOOP, CONDITIONAL, IN_CATCH, IN_FINALLY, CONSTRUCTOR_CALL, SUPER_CALL, VIRTUAL
    }

    public record Classpath(List<Path> jars, List<String> unresolvedCoordinates) {
    }

    /**
     * Receives facts as they are produced; the SQLite store implements it.
     */
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

    /**
     * Extension point for languages: Java (Eclipse JDT) now; Python and others later, same fact records.
     */
    public interface LanguageAnalyzer {
        String language();

        boolean accepts(ModuleSpec module);

        AnalysisReport analyze(ModuleSpec module, Classpath classpath, FactSink sink);
    }

    /**
     * Resolves dependency jars from Maven Central without running any build or plugin.
     */
    public interface ClasspathResolver {
        Classpath resolve(ModuleSpec module);
    }

    /**
     * Read side used by the planner, renderer, checks and the code_facts tool.
     */
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
