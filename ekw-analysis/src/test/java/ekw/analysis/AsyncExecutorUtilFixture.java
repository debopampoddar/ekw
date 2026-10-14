package ekw.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Test fixture: a clone of debopampoddar/asyncexecutorutil at the milestone 1 commit, under target/fixtures.
 * Uses the git command line so the analysis module does not depend on JGit. Tests using it are tagged "network".
 */
public final class AsyncExecutorUtilFixture {
    public static final String URL = "https://github.com/debopampoddar/asyncexecutorutil.git";
    public static final String COMMIT = "ef0d2c95941b822766c392fdc04640c8cc92d053";

    private AsyncExecutorUtilFixture() {
    }

    public static synchronized Path checkout()
            throws IOException, InterruptedException {
        Path dir = Path.of("target", "fixtures", "asyncexecutorutil")
                       .toAbsolutePath();
        if (!Files.isDirectory(dir.resolve(".git"))) {
            Files.createDirectories(dir.getParent());
            run(dir.getParent(), "git", "clone", "--quiet", URL, dir.toString());
        }
        run(dir, "git", "checkout", "--quiet", COMMIT);
        return dir;
    }

    private static void run(Path cwd, String... cmd)
            throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).directory(cwd.toFile())
                                           .inheritIO()
                                           .start();
        if (!p.waitFor(5, TimeUnit.MINUTES) || p.exitValue() != 0)
            throw new IOException("failed: " + String.join(" ", cmd));
    }
}
