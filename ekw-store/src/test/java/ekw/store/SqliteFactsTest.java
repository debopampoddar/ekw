package ekw.store;

import ekw.analysis.Facts;
import ekw.analysis.Facts.SymbolId;
import ekw.core.Evidence.CodeLocation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ch05")
class SqliteFactsTest {

    private static CodeLocation at(String path, int start, int end) {
        return new CodeLocation("demo", "c1", path, start, end, Optional.empty());
    }

    @Test
    void writesASnapshotAndReadsItBack(@TempDir Path dir) {
        FactsDatabase db = FactsDatabase.open(dir.resolve(".index/ekw.db"));     // runs the Flyway migration
        SqliteFacts facts = new SqliteFacts(db);
        SymbolId policy = new SymbolId("java:demo.RetryPolicy");
        SymbolId linear = new SymbolId("java:demo.RetryPolicy.Linear");
        SymbolId execute = new SymbolId("java:demo.RetryPolicy#execute(java.util.concurrent.Callable)");
        SymbolId caller = new SymbolId("java:demo.TaskExecutor#run()");

        try (SqliteFacts.Sink sink = facts.sink("demo", "c1", "2025-02-23T17:40:45Z", "ekw-analyzer/0.1+jdt-3.47.0")) {
            sink.type(new Facts.TypeFact(policy, "demo", Facts.TypeKind.CLASS, Set.of("public", "abstract"),
                    at("RetryPolicy.java", 14, 135), Optional.of("Retry policy."), List.of(), false));
            sink.type(new Facts.TypeFact(linear, "demo", Facts.TypeKind.CLASS, Set.of("public", "static"),
                    at("RetryPolicy.java", 71, 96), Optional.empty(), List.of(policy), false));
            sink.member(new Facts.MemberFact(execute, policy, Facts.MemberKind.METHOD, "execute(Callable)",
                    Set.of("public", "abstract"), at("RetryPolicy.java", 39, 39), "abcd", Optional.empty()));
            sink.call(new Facts.CallFact(caller, Optional.of(execute), at("TaskExecutor.java", 220, 220),
                    Set.of(Facts.CallFlag.IN_LAMBDA), Optional.empty()));
            sink.call(new Facts.CallFact(caller, Optional.empty(), at("TaskExecutor.java", 220, 220),
                    Set.of(), Optional.of("no binding")));          // second call on the same line
            sink.commit();
        }

        assertThat(facts.findTypes("demo", "Linear", 5)).singleElement()
                .satisfies(t -> assertThat(t.span().startLine()).isEqualTo(71));
        assertThat(facts.findTypes("demo", "demo.RetryPolicy", 5)).extracting(Facts.TypeFact::id).containsExactly(policy);
        assertThat(facts.implementations("demo", policy, 10)).extracting(Facts.TypeFact::id).containsExactly(linear);
        assertThat(facts.members("demo", policy)).extracting(Facts.MemberFact::id).containsExactly(execute);
        assertThat(facts.callers("demo", execute, 10)).singleElement()
                .satisfies(c -> assertThat(c.flags()).containsExactly(Facts.CallFlag.IN_LAMBDA));
        assertThat(facts.callees("demo", caller, 10)).hasSize(2);
        assertThat(facts.memberByBodyHash("demo", "abcd")).map(Facts.MemberFact::id).contains(execute);
    }

    @Test
    void reopeningDoesNotReapplyMigrations(@TempDir Path dir) {
        FactsDatabase.open(dir.resolve("ekw.db"));
        FactsDatabase again = FactsDatabase.open(dir.resolve("ekw.db"));
        Integer tables = again.jdbi().withHandle(h -> h.createQuery(
                "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'flyway%'")
                .mapTo(Integer.class).one());
        assertThat(tables).isEqualTo(24);
    }
}
