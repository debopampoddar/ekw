package ekw.store;

import ekw.analysis.CallFact;
import ekw.analysis.Facts;
import ekw.analysis.Facts.SymbolId;
import ekw.analysis.MemberFact;
import ekw.analysis.TypeFact;
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
            sink.type(TypeFact.builder().id(policy).module("demo").kind(Facts.TypeKind.CLASS)
                    .modifiers(Set.of("public", "abstract")).span(at("RetryPolicy.java", 14, 135))
                    .javadocFirstSentence("Retry policy.").build());
            sink.type(TypeFact.builder().id(linear).module("demo").kind(Facts.TypeKind.CLASS)
                    .modifiers(Set.of("public", "static")).span(at("RetryPolicy.java", 71, 96))
                    .supertypes(List.of(policy)).build());
            sink.member(MemberFact.builder().id(execute).owner(policy).kind(Facts.MemberKind.METHOD)
                    .signature("execute(Callable)").modifiers(Set.of("public", "abstract"))
                    .span(at("RetryPolicy.java", 39, 39)).bodyHash("abcd").build());
            sink.call(CallFact.builder().caller(caller).callee(execute).site(at("TaskExecutor.java", 220, 220))
                    .flags(Facts.CallFlag.IN_LAMBDA).build());
            sink.call(CallFact.builder().caller(caller).site(at("TaskExecutor.java", 220, 220))
                    .unresolvedReason("no binding").build());          // second call on the same line
            sink.commit();
        }

        assertThat(facts.findTypes("demo", "Linear", 5)).singleElement()
                                                        .satisfies(t -> assertThat(t.span()
                                                                                    .startLine()).isEqualTo(71));
        assertThat(facts.findTypes("demo", "demo.RetryPolicy", 5)).extracting(TypeFact::id)
                                                                  .containsExactly(policy);
        assertThat(facts.implementations("demo", policy, 10)).extracting(TypeFact::id)
                                                             .containsExactly(linear);
        assertThat(facts.members("demo", policy)).extracting(MemberFact::id)
                                                 .containsExactly(execute);
        assertThat(facts.callers("demo", execute, 10)).singleElement()
                                                      .satisfies(c -> assertThat(c.flags()).containsExactly(Facts.CallFlag.IN_LAMBDA));
        assertThat(facts.callees("demo", caller, 10)).hasSize(2);
        assertThat(facts.memberByBodyHash("demo", "abcd")).map(MemberFact::id)
                                                          .contains(execute);
    }

    @Test
    void reopeningDoesNotReapplyMigrations(@TempDir Path dir) {
        FactsDatabase.open(dir.resolve("ekw.db"));
        FactsDatabase again = FactsDatabase.open(dir.resolve("ekw.db"));
        Integer tables = again.jdbi().withHandle(h -> h.createQuery(
                                                               "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'flyway%'")
                                                       .mapTo(Integer.class)
                                                       .one());
        assertThat(tables).isEqualTo(24);
    }
}
