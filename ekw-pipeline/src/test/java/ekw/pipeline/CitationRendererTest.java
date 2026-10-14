package ekw.pipeline;

import ekw.core.Evidence;
import ekw.pipeline.Pipeline.Paragraph;
import ekw.pipeline.Pipeline.Section;
import ekw.pipeline.Pipeline.Sentence;
import ekw.pipeline.Pipeline.WriterOutput;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ch09")
class CitationRendererTest {
    private static final String COMMIT = "ef0d2c95941b822766c392fdc04640c8cc92d053";
    private static final String PATH = "src/main/java/com/debopam/asynexecutor/util/RetryPolicy.java";

    private static Evidence.Item code(String id, String title, int a, int b, String fp) {
        return new Evidence.Item(new Evidence.Id(id),
                new Evidence.CodeLocation("asyncexecutorutil", COMMIT, PATH, a, b, Optional.empty()),
                title, "...", fp, Evidence.ModelPolicy.HOSTED_ALLOWED, "asyncexecutorutil");
    }

    private static final Evidence.Packet PACKET = new Evidence.Packet(List.of(
            code("E1", "RetryPolicy.Linear", 71, 96, "358dcfc3d6460042"),
            code("E2", "RetryPolicy.None", 57, 66, "7a23d9a3cf48bc0e")));

    private static WriterOutput output(String... ids) {
        List<Sentence> sentences = java.util.Arrays.stream(ids)
                                                   .map(i -> new Sentence("Claim citing " + i + ".", List.of(new Evidence.Id(i))))
                                                   .toList();
        return new WriterOutput("Retry policies", "How a task is retried.",
                List.of(new Section("How it works", List.of(new Paragraph(sentences)), Optional.empty())),
                List.of("Fixed delays suit transient failures."), List.of("retry"), List.of(), List.of(), List.of());
    }

    @Test
    void rendersFootnotesAndSourcesInFirstCitationOrder() {
        CitationRenderer.Rendered r = new CitationRenderer(s -> "https://github.com/debopampoddar/" + s)
                .render(output("E2", "E1", "E2"), PACKET);

        assertThat(r.body()).contains("Claim citing E2.[^retrypolicy-none] Claim citing E1.[^retrypolicy-linear]");
        assertThat(r.body()).contains("[^retrypolicy-linear]: RetryPolicy.java L71-L96\n");
        assertThat(r.body()).contains("> Synthesis: Fixed delays suit transient failures.");
        assertThat(r.sources()).extracting(s -> s.get("id"))
                               .containsExactly("retrypolicy-none", "retrypolicy-linear");
        Map<String, Object> linear = r.sources().get(1);
        assertThat(linear.get("resource")).isEqualTo("https://github.com/debopampoddar/asyncexecutorutil/blob/"
                + COMMIT + "/" + PATH + "#L71-L96");
        assertThat(((Map<?, ?>) linear.get("x-ekw")).get("sha256")).isEqualTo("358dcfc3d6460042");
    }

    @Test
    void rejectsIdsThatWereNotInThePacket() {
        assertThatThrownBy(() -> new CitationRenderer(s -> "").render(output("E1", "E9"), PACKET))
                .isInstanceOf(CitationRenderer.UnknownEvidence.class)
                .hasMessageContaining("E9");
    }
}
