package ekw.query;

import ekw.analysis.Facts;
import ekw.analysis.Facts.SymbolId;
import ekw.core.Evidence.CodeLocation;
import ekw.store.FactsDatabase;
import ekw.store.SqliteFacts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ch11")
class SimpleKnowledgeToolsTest {
    @TempDir
    Path dir;
    private Map<String, AnswerLoop.Tool> tools;

    private static CodeLocation at(String path, int start, int end) {
        return new CodeLocation("demo", "c1", path, start, end, Optional.empty());
    }

    @BeforeEach
    void setUp() throws IOException {
        Path wiki = dir.resolve("wiki");
        Files.createDirectories(wiki.resolve("systems/demo"));
        Files.writeString(wiki.resolve("index.md"), "* [demo](systems/demo/index.md) - A demo system.\n");
        Files.writeString(wiki.resolve("systems/demo/index.md"), "* [demo](overview.md) - What demo does.\n");
        Files.writeString(wiki.resolve("systems/demo/overview.md"), "---\ntype: System Overview\n---\n# demo\n");
        Files.writeString(dir.resolve("secret.txt"), "do not read");
        Path checkout = dir.resolve("checkout");
        Files.createDirectories(checkout.resolve("src"));
        Files.writeString(checkout.resolve("src/RetryPolicy.java"),
                IntStream.rangeClosed(1, 200)
                         .mapToObj(i -> "line " + i)
                         .collect(Collectors.joining("\n", "", "\n")));

        SymbolId policy = new SymbolId("java:demo.RetryPolicy");
        SymbolId execute = new SymbolId("java:demo.RetryPolicy#execute(java.util.concurrent.Callable)");
        SqliteFacts facts = new SqliteFacts(FactsDatabase.open(dir.resolve("ekw.db")));
        try (SqliteFacts.Sink sink = facts.sink("demo", "c1", "2025-02-23T17:40:45Z", "test")) {
            sink.type(new Facts.TypeFact(policy, "demo", Facts.TypeKind.CLASS, Set.of("public"),
                    at("src/RetryPolicy.java", 14, 135), Optional.empty(), List.of(), false));
            sink.type(new Facts.TypeFact(new SymbolId("java:demo.RetryPolicy.Linear"), "demo", Facts.TypeKind.CLASS,
                    Set.of("public"), at("src/RetryPolicy.java", 71, 96), Optional.empty(), List.of(policy), false));
            sink.member(new Facts.MemberFact(execute, policy, Facts.MemberKind.METHOD, "execute(Callable)",
                    Set.of("public"), at("src/RetryPolicy.java", 39, 39), "abcd", Optional.empty()));
            sink.call(new Facts.CallFact(new SymbolId("java:demo.TaskExecutor#run()"), Optional.of(execute),
                    at("src/TaskExecutor.java", 220, 220), Set.of(), Optional.empty()));
            sink.commit();
        }
        tools = new SimpleKnowledgeTools(wiki, facts, checkout, "demo").tools()
                                                                       .stream()
                                                                       .collect(Collectors.toMap(t -> t.spec()
                                                                                                       .name(), t -> t));
    }

    private AnswerLoop.ToolOutput call(String tool, Map<String, Object> args) {
        return tools.get(tool).run().apply(args);
    }

    @Test
    void offersFourToolsWithStrictSchemas() throws IOException {
        List<String> names = new SimpleKnowledgeTools(dir.resolve("wiki"), null, dir, "demo").tools()
                                                                                             .stream()
                                                                                             .map(t -> t.spec()
                                                                                                        .name())
                                                                                             .toList();
        assertThat(names).containsExactly("read_index", "read_page", "code_facts", "read_code_lines");
        assertThat(tools.values()).allSatisfy(t -> {
            assertThat(t.spec().description()).isNotBlank();
            assertThat(t.spec().inputSchema()).containsEntry("type", "object")
                                              .containsEntry("additionalProperties", false);
        });
    }

    @Test
    void readsIndexesAndPagesInsideTheWikiOnly() {
        assertThat(call("read_index", Map.of()).text()).contains("[demo](systems/demo/index.md)");
        assertThat(call("read_index", Map.of("path", "systems/demo")).text()).contains("What demo does.");
        assertThat(call("read_page", Map.of("path", "systems/demo/overview.md")).text()).contains("# demo");
        assertThat(call("read_page", Map.of("path", "../secret.txt")).isError()).isTrue();
        assertThat(call("read_page", Map.of("path", "systems/demo/missing.md")).isError()).isTrue();
    }

    @Test
    void answersCodeFactsWithLocations() {
        assertThat(call("code_facts", Map.of("op", "symbol", "name", "Linear")).text())
                .contains("java:demo.RetryPolicy.Linear")
                .contains("src/RetryPolicy.java L71-L96");
        assertThat(call("code_facts", Map.of("op", "implementations", "name", "RetryPolicy")).text())
                .contains("java:demo.RetryPolicy.Linear");
        assertThat(call("code_facts", Map.of("op", "members", "name", "RetryPolicy")).text())
                .contains("execute(Callable)")
                .contains("src/RetryPolicy.java L39");
        assertThat(call("code_facts", Map.of("op", "callers", "name", "RetryPolicy#execute")).text())
                .contains("java:demo.TaskExecutor#run()")
                .contains("src/TaskExecutor.java L220");
        AnswerLoop.ToolOutput none = call("code_facts", Map.of("op", "symbol", "name", "Missing"));
        assertThat(none.isError()).isFalse();                          // "nothing found" is an answer, not an error
        assertThat(none.text()).contains("No facts");
        assertThat(call("code_facts", Map.of("op", "teleport", "name", "X")).isError()).isTrue();
    }

    @Test
    void readsCodeLinesWithinLimits() {
        AnswerLoop.ToolOutput lines = call("read_code_lines", Map.of("path", "src/RetryPolicy.java", "start", 71, "end", 73));
        assertThat(lines.isError()).isFalse();
        assertThat(lines.text()).contains("71: line 71\n72: line 72\n73: line 73");
        assertThat(lines.text()).doesNotContain("74: ");
        // JSON numbers may arrive as Long or Double
        assertThat(call("read_code_lines", Map.of("path", "src/RetryPolicy.java", "start", 1L, "end", 2.0)).isError()).isFalse();
        assertThat(call("read_code_lines", Map.of("path", "src/RetryPolicy.java", "start", 1, "end", 121)).isError()).isTrue();
        assertThat(call("read_code_lines", Map.of("path", "src/RetryPolicy.java", "start", 199, "end", 201)).isError()).isTrue();
        assertThat(call("read_code_lines", Map.of("path", "../secret.txt", "start", 1, "end", 1)).isError()).isTrue();
    }
}
