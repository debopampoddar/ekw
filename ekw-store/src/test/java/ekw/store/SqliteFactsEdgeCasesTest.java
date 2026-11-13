package ekw.store;

import ekw.analysis.CallFact;
import ekw.analysis.Facts;
import ekw.analysis.Facts.SymbolId;
import ekw.analysis.MemberFact;
import ekw.analysis.TypeFact;
import ekw.core.Evidence.CodeLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge cases the chapter's own test does not cover: a write that never reaches commit(), ambiguous body hashes,
 * the stored text of sets, and the col counter.
 */
@Tag("ch05")
class SqliteFactsEdgeCasesTest {

    private static final SymbolId POLICY = new SymbolId("java:demo.RetryPolicy");
    private static final SymbolId LINEAR = new SymbolId("java:demo.RetryPolicy.Linear");
    private static final SymbolId RUN = new SymbolId("java:demo.TaskExecutor#run()");

    private FactsDatabase db;
    private SqliteFacts facts;

    @BeforeEach
    void open(@TempDir Path dir) {
        db = FactsDatabase.open(dir.resolve("ekw.db"));
        facts = new SqliteFacts(db);
    }

    private static CodeLocation at(String path, int start, int end) {
        return new CodeLocation("demo", "c1", path, start, end, Optional.empty());
    }

    private static TypeFact type(SymbolId id) {
        return TypeFact.builder().id(id).module("demo").kind(Facts.TypeKind.CLASS)
                .span(at("RetryPolicy.java", 1, 10)).build();
    }

    private void writeAndCommit(String commit, TypeFact type) {
        try (SqliteFacts.Sink sink = facts.sink("demo", commit, "2025-02-23T17:40:45Z", "test")) {
            sink.type(type);
            sink.commit();
        }
    }

    private List<String> snapshots() {
        return db.jdbi().withHandle(h -> h.createQuery(
                        "SELECT commit_sha || ':' || current FROM snapshot ORDER BY commit_sha")
                .mapTo(String.class).list());
    }

    @Test
    void rewritingTheCurrentCommitWithoutCommitKeepsTheOldFacts() {
        writeAndCommit("c1", type(POLICY));

        try (SqliteFacts.Sink sink = facts.sink("demo", "c1", "2025-02-23T17:40:45Z", "test")) {
            sink.type(type(LINEAR));
            // no commit(): as if the analyzer had thrown
        }

        assertThat(facts.type("demo", POLICY)).isPresent();
        assertThat(facts.type("demo", LINEAR)).isEmpty();
        assertThat(snapshots()).containsExactly("c1:1");
    }

    @Test
    void aNewCommitWithoutCommitLeavesThePreviousOneCurrent() {
        writeAndCommit("c1", type(POLICY));

        try (SqliteFacts.Sink sink = facts.sink("demo", "c2", "2025-02-24T09:00:00Z", "test")) {
            sink.type(type(LINEAR));
        }

        assertThat(facts.type("demo", POLICY)).isPresent();
        assertThat(facts.type("demo", LINEAR)).isEmpty();
        assertThat(snapshots()).containsExactly("c1:1");
    }

    @Test
    void twoMembersWithTheSameBodyHashAreNotFound() {
        try (SqliteFacts.Sink sink = facts.sink("demo", "c1", "2025-02-23T17:40:45Z", "test")) {
            sink.type(type(POLICY));
            for (String name : List.of("a", "b")) {
                sink.member(MemberFact.builder().id(new SymbolId("java:demo.RetryPolicy#" + name + "()"))
                        .owner(POLICY).kind(Facts.MemberKind.METHOD).signature(name + "()")
                        .span(at("RetryPolicy.java", 2, 3)).bodyHash("same").build());
            }
            sink.commit();
        }

        assertThat(facts.memberByBodyHash("demo", "same")).isEmpty();
    }

    @Test
    void setsAreStoredAsSortedCommaSeparatedText() {
        try (SqliteFacts.Sink sink = facts.sink("demo", "c1", "2025-02-23T17:40:45Z", "test")) {
            sink.type(TypeFact.builder().id(POLICY).module("demo").kind(Facts.TypeKind.CLASS)
                    .modifiers(new LinkedHashSet<>(List.of("static", "public", "abstract")))
                    .span(at("RetryPolicy.java", 1, 10)).build());
            sink.call(CallFact.builder().caller(RUN).site(at("TaskExecutor.java", 5, 5))
                    .flags(Facts.CallFlag.IN_LAMBDA, Facts.CallFlag.CHAINED).build());
            sink.commit();
        }

        String modifiers = db.jdbi().withHandle(h -> h.createQuery("SELECT modifiers FROM symbol")
                .mapTo(String.class).one());
        String flags = db.jdbi().withHandle(h -> h.createQuery("SELECT flags FROM call_site")
                .mapTo(String.class).one());
        assertThat(modifiers).isEqualTo("abstract,public,static");
        assertThat(flags).isEqualTo("CHAINED,IN_LAMBDA");
        assertThat(facts.type("demo", POLICY)).get()
                .satisfies(t -> assertThat(t.modifiers()).containsExactly("abstract", "public", "static"));
    }

    @Test
    void threeCallsOnOneLineGetColumnsOneTwoThree() {
        try (SqliteFacts.Sink sink = facts.sink("demo", "c1", "2025-02-23T17:40:45Z", "test")) {
            for (int i = 0; i < 3; i++) {
                sink.call(CallFact.builder().caller(RUN).site(at("TaskExecutor.java", 221, 221)).build());
            }
            sink.commit();
        }

        List<Integer> cols = db.jdbi().withHandle(h -> h.createQuery(
                        "SELECT col FROM call_site WHERE line = 221 ORDER BY col")
                .mapTo(Integer.class).list());
        assertThat(cols).containsExactly(1, 2, 3);
        assertThat(facts.callees("demo", RUN, 10)).hasSize(3);
    }
}
