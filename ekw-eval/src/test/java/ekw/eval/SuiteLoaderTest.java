package ekw.eval;

import ekw.query.Query.QuestionType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads the four suites in eval/suites (directory given by the ekw.suites system property).
 */
@Tag("ch11")
class SuiteLoaderTest {
    private static final Path DIR = Path.of(System.getProperty("ekw.suites", "eval/suites"));

    @Test
    void loadsEverySuiteWithExpandedPaths() throws Exception {
        SuiteLoader.Suite aeu = SuiteLoader.load(DIR.resolve("asyncexecutorutil.yaml"));
        assertThat(aeu.questions()).hasSize(30);
        assertThat(aeu.commit()).isEqualTo("ef0d2c95941b822766c392fdc04640c8cc92d053");
        Eval.EvalQuestion a01 = aeu.questions().get(0);
        assertThat(a01.type()).isEqualTo(QuestionType.T1_PURPOSE);
        assertThat(a01.requiredEvidence()).extracting(Eval.RequiredEvidence::location)
                                          .contains("src/main/java/com/debopam/asynexecutor/util/TaskExecutor.java#L24-L27");
        assertThat(aeu.questions()).filteredOn(Eval.EvalQuestion::expectNotFound)
                                   .extracting(Eval.EvalQuestion::id)
                                   .containsExactly("A13", "A20", "A21", "A22");          // the negatives of stage 2, section 11.5

        assertThat(SuiteLoader.load(DIR.resolve("mybatis-3.yaml"))
                              .questions()).hasSize(15);
        assertThat(SuiteLoader.load(DIR.resolve("dubbo.yaml"))
                              .questions()).hasSize(15);
        assertThat(SuiteLoader.load(DIR.resolve("dubbo-docs.yaml"))
                              .questions()).hasSize(12);
    }
}
