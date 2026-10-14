package ekw.store;

import ekw.analysis.Facts;
import ekw.analysis.Facts.CallFact;
import ekw.analysis.Facts.ConfigKeyFact;
import ekw.analysis.Facts.DependencyFact;
import ekw.analysis.Facts.ErrorCodeFact;
import ekw.analysis.Facts.MemberFact;
import ekw.analysis.Facts.SymbolId;
import ekw.analysis.Facts.TypeFact;
import ekw.core.Todo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Facts for one snapshot (system, commit): the analyzer writes through {@link #sink}, everything else reads through
 * the {@link Facts.FactsStore} methods. Reads always use the snapshot marked current.
 *
 * <p>Chapter 5 of the development guide.
 */
public final class SqliteFacts implements Facts.FactsStore {

    public SqliteFacts(FactsDatabase db) {
        // TODO (chapter 5): keep the database.
    }

    /** Opens a snapshot (replacing facts left by an earlier attempt at the same commit). Commit it to make it current. */
    public Sink sink(String system, String commit, String commitTime, String analyzer) {
        throw Todo.chapter(5, "SqliteFacts.sink");
    }

    /** Writes facts in one transaction. {@link #commit()} makes the snapshot current; {@link #close()} without it rolls back. */
    public static final class Sink implements Facts.FactSink, AutoCloseable {
        private Sink() {}

        @Override public void type(TypeFact t) { throw Todo.chapter(5, "Sink.type"); }
        @Override public void member(MemberFact m) { throw Todo.chapter(5, "Sink.member"); }
        @Override public void call(CallFact c) { throw Todo.chapter(5, "Sink.call"); }

        // Later chapters (part C) store these fact kinds; ignore them for now.
        @Override public void annotation(Facts.AnnotationFact a) {}
        @Override public void configKey(ConfigKeyFact k) {}
        @Override public void errorCode(ErrorCodeFact e) {}
        @Override public void spi(Facts.SpiFact s) {}
        @Override public void module(Facts.ModuleFact m) {}
        @Override public void dependency(DependencyFact d) {}

        public void commit() { throw Todo.chapter(5, "Sink.commit"); }
        @Override public void close() { throw Todo.chapter(5, "Sink.close"); }
    }

    @Override public Optional<TypeFact> type(String system, SymbolId id) { throw Todo.chapter(5, "type"); }
    @Override public List<TypeFact> findTypes(String system, String name, int limit) { throw Todo.chapter(5, "findTypes"); }
    @Override public List<MemberFact> members(String system, SymbolId type) { throw Todo.chapter(5, "members"); }
    @Override public List<CallFact> callers(String system, SymbolId callee, int limit) { throw Todo.chapter(5, "callers"); }
    @Override public List<CallFact> callees(String system, SymbolId caller, int limit) { throw Todo.chapter(5, "callees"); }
    @Override public List<TypeFact> implementations(String system, SymbolId type, int limit) { throw Todo.chapter(5, "implementations"); }
    @Override public Optional<MemberFact> memberByBodyHash(String system, String bodyHash) { throw Todo.chapter(5, "memberByBodyHash"); }
    @Override public Map<SymbolId, Double> rank(String system) { throw Todo.chapter(5, "rank"); }

    // Part C fills these; they return nothing until then.
    @Override public List<ConfigKeyFact> configKeys(String system, String keyFragment, int limit) { return List.of(); }
    @Override public List<ErrorCodeFact> errorCodes(String system, String code) { return List.of(); }
    @Override public List<DependencyFact> dependencies(String system, String fragment, int limit) { return List.of(); }
}
