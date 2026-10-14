package ekw.pipeline;

import ekw.analysis.Facts;
import ekw.analysis.Facts.SymbolId;
import ekw.core.Evidence.CodeLocation;
import ekw.core.MarkdownPage;
import ekw.store.FactsDatabase;
import ekw.store.SqliteFacts;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ch11")
class SimpleFactPagesTest {
    static final SymbolId POLICY = new SymbolId("java:demo.RetryPolicy");
    static final SymbolId LINEAR = new SymbolId("java:demo.RetryPolicy.Linear");
    static final SymbolId EXECUTE = new SymbolId("java:demo.RetryPolicy#execute(java.util.concurrent.Callable)");
    static final SymbolId RUN = new SymbolId("java:demo.TaskExecutor#run()");
    static final SymbolId CALL = new SymbolId("java:java.util.concurrent.Callable#call()");

    static CodeLocation at(String path, int start, int end) {
        return new CodeLocation("demo", "c1", path, start, end, Optional.empty());
    }

    /**
     * A small facts snapshot shared by the chapter 11 tests.
     */
    static SqliteFacts sampleFacts(Path dir) {
        SqliteFacts facts = new SqliteFacts(FactsDatabase.open(dir.resolve("ekw.db")));
        try (SqliteFacts.Sink sink = facts.sink("demo", "c1", "2025-02-23T17:40:45Z", "test")) {
            sink.type(new Facts.TypeFact(POLICY, "demo", Facts.TypeKind.CLASS, Set.of("public", "abstract"),
                    at("RetryPolicy.java", 14, 135), Optional.of("Retry policy."), List.of(), false));
            sink.type(new Facts.TypeFact(LINEAR, "demo", Facts.TypeKind.CLASS, Set.of("public", "static"),
                    at("RetryPolicy.java", 71, 96), Optional.empty(), List.of(POLICY), false));
            sink.member(new Facts.MemberFact(EXECUTE, POLICY, Facts.MemberKind.METHOD, "execute(Callable)",
                    Set.of("public", "abstract"), at("RetryPolicy.java", 39, 39), "abcd", Optional.empty()));
            sink.call(new Facts.CallFact(RUN, Optional.of(EXECUTE), at("TaskExecutor.java", 220, 220),
                    Set.of(), Optional.empty()));
            sink.call(new Facts.CallFact(EXECUTE, Optional.of(CALL), at("RetryPolicy.java", 39, 39),
                    Set.of(), Optional.empty()));
            sink.commit();
        }
        return facts;
    }

    @Test
    void rendersATypePage(@TempDir Path dir) {
        SimpleFactPages pages = new SimpleFactPages(sampleFacts(dir), "demo", "c1", "2025-02-23T17:40:45Z");
        MarkdownPage page = pages.typePage(POLICY);

        assertThat(page.frontmatter().keySet())
                .containsExactly("type", "title", "description", "generated", "status", "x-ekw");
        assertThat(page.type()).isEqualTo("Java Type");
        assertThat(page.frontmatter()).containsEntry("title", "RetryPolicy")
                                      .containsEntry("description", "Retry policy.")
                                      .containsEntry("status", "stable")
                                      .containsEntry("generated", Map.of("by", "ekw-analyzer/0.1", "at", "2025-02-23T17:40:45Z"));
        @SuppressWarnings("unchecked")
        Map<String, Object> ekw = (Map<String, Object>) page.frontmatter()
                                                            .get("x-ekw");
        assertThat(ekw).containsEntry("layer", "facts")
                       .containsEntry("system", "demo")
                       .containsEntry("commit", "c1")
                       .containsEntry("symbol", "java:demo.RetryPolicy")
                       .containsEntry("span", Map.of("path", "RetryPolicy.java", "start", 14, "end", 135));

        assertThat(page.body()).startsWith("# RetryPolicy\n")
                               .contains("## Members\n\n- `execute(Callable)` (L39)\n")
                               .contains("## Supertypes\n\n- (none)\n")
                               .contains("## Implementations\n\n- `demo.RetryPolicy.Linear`\n")
                               .contains("## Used by\n\n- `demo.TaskExecutor#run()` at TaskExecutor.java L220\n")
                               .contains("## Calls\n\n- `java.util.concurrent.Callable#call()`\n");
        assertThat(pages.typePage(POLICY)
                        .render()).isEqualTo(page.render());          // same bytes every time
    }

    @Test
    void describesTypesWithoutJavadocAndNamesFiles(@TempDir Path dir) {
        SimpleFactPages pages = new SimpleFactPages(sampleFacts(dir), "demo", "c1", "2025-02-23T17:40:45Z");
        MarkdownPage linear = pages.typePage(LINEAR);
        assertThat(linear.frontmatter()).containsEntry("title", "Linear")
                                        .containsEntry("description", "Java type demo.RetryPolicy.Linear.");
        assertThat(linear.body()).contains("## Supertypes\n\n- `demo.RetryPolicy`\n")
                                 .contains("## Members\n\n- (none)\n");
        assertThat(SimpleFactPages.fileName(LINEAR)).isEqualTo("demo.RetryPolicy.Linear.md");
    }
}
