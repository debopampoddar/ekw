package ekw.pipeline;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ch03")
@Tag("network")
class GitCheckoutTest {
    private static final String URL = "https://github.com/debopampoddar/asyncexecutorutil.git";
    private static final String M1 = "ef0d2c95941b822766c392fdc04640c8cc92d053";

    @Test
    void clonesChecksOutAndDiffs() {
        Path dir = Path.of("target", "fixtures", "repos", "github.com-debopampoddar-asyncexecutorutil");
        try (GitCheckout repo = GitCheckout.open(URL, dir)) {
            assertThat(repo.head("main")).hasSize(40);
            repo.checkout(M1);
            assertThat(Files.exists(dir.resolve("src/main/java/com/debopam/asynexecutor/util/RetryPolicy.java"))).isTrue();
            assertThat(repo.commitTime(M1).toString()).startsWith("2025-02-23");
            assertThat(repo.changedPaths("93e346dab4846bbf6acf8a4f2631aa3859c18668", M1)).containsExactly(
                    "README.md", "src/main/java/com/debopam/asynexecutor/util/ExecutionResult.java");
        }
        try (GitCheckout again = GitCheckout.open(URL, dir)) {                  // second open fetches
            assertThat(again.head("main")).hasSize(40);
        }
    }
}
