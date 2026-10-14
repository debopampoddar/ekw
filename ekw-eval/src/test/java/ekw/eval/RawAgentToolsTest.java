package ekw.eval;

import ekw.query.AnswerLoop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ch11")
class RawAgentToolsTest {
    @TempDir
    Path dir;
    private Map<String, AnswerLoop.Tool> tools;

    @BeforeEach
    void setUp() throws IOException {
        Path checkout = dir.resolve("checkout");
        Files.createDirectories(checkout.resolve("src/sub"));
        Files.createDirectories(checkout.resolve(".git"));
        Files.writeString(checkout.resolve("src/A.java"), "class A {\n  void foo() {}\n}\n");
        Files.writeString(checkout.resolve("src/sub/B.java"), "class B { int foo; }\n");
        Files.writeString(checkout.resolve(".git/config"), "foo = git internals\n");
        Files.writeString(checkout.resolve("README.md"), "Use foo to start.\n");
        Files.writeString(checkout.resolve("src/Many.txt"),
                IntStream.rangeClosed(1, 300)
                         .mapToObj(i -> "foo " + i)
                         .collect(Collectors.joining("\n", "", "\n")));
        Files.writeString(dir.resolve("outside.txt"), "secret foo\n");
        tools = new RawAgentTools(checkout).tools()
                                           .stream()
                                           .collect(Collectors.toMap(t -> t.spec()
                                                                           .name(), t -> t));
    }

    private AnswerLoop.ToolOutput call(String tool, Map<String, Object> args) {
        return tools.get(tool).run().apply(args);
    }

    @Test
    void grepFindsLinesButNotGitInternals() {
        String hits = call("grep", Map.of("pattern", "void foo", "glob", "**/*.java")).text();
        assertThat(hits).contains("src/A.java:2:   void foo() {}")
                        .doesNotContain("B.java");
        String all = call("grep", Map.of("pattern", "foo")).text();
        assertThat(all).contains("README.md:1: Use foo to start.")
                       .doesNotContain(".git");      // files in path order
        assertThat(call("grep", Map.of("pattern", "int foo")).text()).isEqualTo("src/sub/B.java:1: class B { int foo; }");
        assertThat(all.lines()
                      .filter(l -> l.matches("^[^ ]+:\\d+: .*"))
                      .count()).isEqualTo(50);
        assertThat(all).contains("limited to 50");
        assertThat(call("grep", Map.of("pattern", "foo(")).isError()).isTrue();          // invalid regex
    }

    @Test
    void globListsSortedPaths() {
        assertThat(call("glob", Map.of("pattern", "**/*.java")).text()).isEqualTo("src/A.java\nsrc/sub/B.java");
        assertThat(call("glob", Map.of("pattern", "**/config")).text()).doesNotContain(".git");
    }

    @Test
    void readFileReturnsNumberedLinesWithinTheCheckoutOnly() {
        assertThat(call("read_file", Map.of("path", "src/A.java")).text()).contains("1: class A {\n2:   void foo() {}\n3: }");
        assertThat(call("read_file", Map.of("path", "src/Many.txt", "start", 10, "end", 11)).text())
                .contains("10: foo 10\n11: foo 11").doesNotContain("12: ");
        String capped = call("read_file", Map.of("path", "src/Many.txt")).text();
        assertThat(capped).contains("200: foo 200")
                          .doesNotContain("201: ")
                          .contains("truncated at 200 lines");
        assertThat(call("read_file", Map.of("path", "../outside.txt")).isError()).isTrue();
        assertThat(call("read_file", Map.of("path", dir.resolve("outside.txt")
                                                       .toString())).isError()).isTrue();
        assertThat(call("read_file", Map.of("path", "src/Nope.java")).isError()).isTrue();
    }
}
