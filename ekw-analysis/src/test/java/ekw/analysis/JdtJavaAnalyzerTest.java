package ekw.analysis;

import ekw.core.Fingerprint;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Analyzes the milestone 1 repository at its pinned commit and checks facts known from the stage 1 example.
 */
@Tag("ch04")
@Tag("network")
class JdtJavaAnalyzerTest {

    @Test
    void analyzesAsyncExecutorUtil() throws Exception {
        Path repo = AsyncExecutorUtilFixture.checkout();
        Facts.ModuleSpec module = new Facts.ModuleSpec("asyncexecutorutil", AsyncExecutorUtilFixture.COMMIT,
                "asyncexecutorutil", repo, List.of(repo.resolve("src/main/java")),
                List.of(repo.resolve("src/test/java")), "21");
        Facts.Classpath classpath;
        try (MavenClasspathResolver resolver = new MavenClasspathResolver(Path.of("target", "m2"), repo)) {
            classpath = resolver.resolve(module);
        }
        assertThat(classpath.unresolvedCoordinates()).isEmpty();
        assertThat(classpath.jars()).anyMatch(j -> j.getFileName()
                                                    .toString()
                                                    .equals("junit-jupiter-api-5.9.2.jar"));

        CollectingSink sink = new CollectingSink();
        Facts.AnalysisReport report = new JdtJavaAnalyzer(repo).analyze(module, classpath, sink);

        // The JDT spike measured 757 call sites in main + test code, all resolved.
        assertThat(report.callSites()).isEqualTo(757);
        assertThat(report.resolvedCallSites()).isEqualTo(757);

        // RetryPolicy.Linear: span L71-L96 and the fingerprint cited by the stage 1 example (rp-linear).
        Facts.TypeFact linear = sink.types.stream()
                                          .filter(t -> t.id()
                                                        .value()
                                                        .equals("java:com.debopam.asynexecutor.util.RetryPolicy.Linear"))
                                          .findFirst().orElseThrow();
        assertThat(linear.span().startLine()).isEqualTo(71);
        assertThat(linear.span().endLine()).isEqualTo(96);
        assertThat(linear.span()
                         .path()).isEqualTo("src/main/java/com/debopam/asynexecutor/util/RetryPolicy.java");
        String text = Files.readString(repo.resolve(linear.span().path()));
        assertThat(Fingerprint.ofRange(text, 71, 96)).isEqualTo("358dcfc3d6460042");
        assertThat(linear.supertypes()).extracting(Facts.SymbolId::value)
                                       .containsExactly("java:com.debopam.asynexecutor.util.RetryPolicy");

        // Task is a record; tests are marked as tests.
        assertThat(sink.types).anySatisfy(t -> {
            assertThat(t.id()
                        .value()).isEqualTo("java:com.debopam.asynexecutor.util.Task");
            assertThat(t.kind()).isEqualTo(Facts.TypeKind.RECORD);
        });
        assertThat(sink.types).filteredOn(Facts.TypeFact::test).isNotEmpty();

        // A call inside a lambda is resolved and flagged.
        assertThat(sink.calls).anySatisfy(c -> {
            assertThat(c.flags()).contains(Facts.CallFlag.IN_LAMBDA);
            assertThat(c.callee()).isPresent();
        });
    }
}
