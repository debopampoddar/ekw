package ekw.store;

import ekw.analysis.Facts;
import ekw.analysis.JdtJavaAnalyzer;
import ekw.analysis.MavenClasspathResolver;
import ekw.analysis.ModuleSpec;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Tag("try")
@Disabled
class TryRealFactsTest {
    @Test
    void analyzeAsyncExecutorUtilIntoADatabase() throws Exception {
        Path repo = Path.of("target/aeu").toAbsolutePath();
        if (!Files.isDirectory(repo.resolve(".git"))) {
            new ProcessBuilder("git", "clone", "--quiet",
                    "https://github.com/debopampoddar/asyncexecutorutil.git", repo.toString()).inheritIO().start().waitFor();
        }
        new ProcessBuilder("git", "checkout", "--quiet", "ef0d2c95941b822766c392fdc04640c8cc92d053")
                .directory(repo.toFile()).inheritIO().start().waitFor();
        ModuleSpec module = ModuleSpec.builder()
                .system("asyncexecutorutil")
                .commit("ef0d2c95941b822766c392fdc04640c8cc92d053")
                .name("asyncexecutorutil")
                .root(repo)
                .sourceRoots(List.of(repo.resolve("src/main/java")))
                .testRoots(List.of(repo.resolve("src/test/java")))
                .javaRelease("21")
                .build();
        Facts.Classpath classpath;
        try (MavenClasspathResolver resolver = new MavenClasspathResolver(Path.of("target/m2"), repo)) {
            classpath = resolver.resolve(module);
        }
        SqliteFacts facts = new SqliteFacts(FactsDatabase.open(Path.of("target/aeu.db")));
        try (SqliteFacts.Sink sink = facts.sink("asyncexecutorutil", module.commit()
                , "2025-02-23T17:40:45Z"
                , "try")) {
            new JdtJavaAnalyzer(repo).analyze(module, classpath, sink);
            sink.commit();
        }
    }
}

