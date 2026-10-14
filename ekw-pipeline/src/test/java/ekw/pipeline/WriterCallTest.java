package ekw.pipeline;

import ekw.core.Evidence;
import ekw.llm.DefaultBudgetGuard;
import ekw.llm.Models;
import ekw.llm.PriceTable;
import ekw.llm.PromptRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ch10")
class WriterCallTest {
    private static final String COMMIT = "ef0d2c95941b822766c392fdc04640c8cc92d053";
    private static final Models.Prices PRICES = new Models.Prices(new BigDecimal("2"), new BigDecimal("10"),
            new BigDecimal("0.2"), new BigDecimal("2.5"));
    private static final PriceTable TABLE = new PriceTable(Map.of("fake", new PriceTable.Entry(PRICES, Optional.empty())));

    private static String json() throws IOException {
        try (InputStream in = WriterCallTest.class.getResourceAsStream("/writer-output.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static final Evidence.Item CODE = new Evidence.Item(new Evidence.Id("E2"),
            new Evidence.CodeLocation("asyncexecutorutil", COMMIT,
                    "src/main/java/com/debopam/asynexecutor/util/RetryPolicy.java", 71, 96, Optional.empty()),
            "RetryPolicy.Linear", "public static class Linear extends RetryPolicy {", "358dcfc3d6460042",
            Evidence.ModelPolicy.HOSTED_ALLOWED, "asyncexecutorutil");

    private static final Evidence.Item ONE_LINE = new Evidence.Item(new Evidence.Id("E1"),
            new Evidence.CodeLocation("asyncexecutorutil", COMMIT,
                    "src/main/java/com/debopam/asynexecutor/util/Task.java", 44, 44, Optional.empty()),
            "Task.Builder default policy", "private RetryPolicy retryPolicy = RetryPolicy.none();", "96287f82fc11f10c",
            Evidence.ModelPolicy.LOCAL_ONLY, "private-repo");

    @Test
    void rendersEvidenceBlocks() {
        assertThat(WriterCall.evidenceBlock(CODE)).isEqualTo("""
                <evidence id="E2" kind="code" location="RetryPolicy.java L71-L96" source-policy="hosted-allowed">
                public static class Linear extends RetryPolicy {
                </evidence>""");
        assertThat(WriterCall.evidenceBlock(ONE_LINE))
                .startsWith("<evidence id=\"E1\" kind=\"code\" location=\"Task.java L44\" source-policy=\"local-only\">\n");
    }

    @Test
    void parsesTheWritersJson() throws IOException {
        Pipeline.WriterOutput out = WriterCall.parse(json());
        assertThat(out.title()).isEqualTo("Retry policies");
        assertThat(out.sections()).hasSize(2);
        Pipeline.Sentence second = out.sections().get(0).paragraphs().get(0).sentences().get(1);
        assertThat(second.evidence()).containsExactly(new Evidence.Id("E2"), new Evidence.Id("E1"));
        assertThat(out.sections().get(0).diagramEvidenceId()).isEmpty();
        assertThat(out.sections().get(1).diagramEvidenceId()).contains("E3");
        assertThat(out.synthesis()).containsExactly("Fixed delays suit transient failures.");
        assertThat(out.conceptSlugs()).containsExactly("retry-policy");
        assertThat(out.readCoverage()).containsExactly("RetryPolicy.java", "Task.java");
        assertThat(out.notRead()).containsExactly("tests");
        assertThat(out.evidenceRequests()).isEmpty();
    }

    /** Returns the recorded JSON and remembers the request. */
    static final class ScriptedWriter implements Models.ChatModel {
        final List<Models.ChatRequest> requests = new ArrayList<>();
        final String reply;
        ScriptedWriter(String reply) { this.reply = reply; }
        @Override public Models.ModelInfo info() {
            return new Models.ModelInfo("fake", "test", "fake", 200_000, 16_000, PRICES, false, List.of());
        }
        @Override public Models.ChatResult chat(Models.ChatRequest r) {
            requests.add(r);
            return new Models.ChatResult(List.of(new Models.Text(reply)), Models.StopReason.END_TURN,
                    new Models.Usage(5_000, 1_000, 0, 0), "fake", Duration.ZERO);
        }
        @Override public Models.TokenEstimate estimate(Models.ChatRequest r) { return new Models.TokenEstimate(5_000, true); }
    }

    @Test
    void buildsTheRequestAndWritesThroughTheGuard() throws IOException {
        ScriptedWriter model = new ScriptedWriter(json());
        DefaultBudgetGuard guard = new DefaultBudgetGuard(new BigDecimal("1"), TABLE, Map.of());
        WriterCall writer = new WriterCall(model, guard, PromptRegistry.fromClasspath(), "CONVENTIONS EXCERPT",
                Models.Effort.HIGH, 16_000);
        WriterCall.PageRequest page = new WriterCall.PageRequest("Component", "Retry policies", "asyncexecutorutil",
                "ef0d2c9", "## What it does *", "Explain the component.", "(none)",
                new Evidence.Packet(List.of(ONE_LINE, CODE)));

        Models.ChatRequest request = writer.request(page);
        assertThat(request.role()).isEqualTo(Models.Role.WRITER);
        assertThat(request.prompt()).isEqualTo(new Models.PromptRef("writer.page", 1));
        assertThat(request.system()).contains("CONVENTIONS EXCERPT").doesNotContain("{{");
        assertThat(request.tools()).isEmpty();
        assertThat(request.outputSchema()).isPresent();
        assertThat(request.effort()).contains(Models.Effort.HIGH);
        assertThat(request.maxOutputTokens()).isEqualTo(16_000);
        assertThat(request.messages()).hasSize(1);
        String user = ((Models.Text) request.messages().get(0).parts().get(0)).text();
        assertThat(user).contains("Write the \"Component\" page \"Retry policies\" for system \"asyncexecutorutil\" at commit ef0d2c9.")
                .contains(WriterCall.evidenceBlock(ONE_LINE)).contains(WriterCall.evidenceBlock(CODE)).doesNotContain("{{");
        assertThat(request.evidence().items()).hasSize(2);

        Pipeline.WriterOutput out = writer.write(page);
        assertThat(out.title()).isEqualTo("Retry policies");
        assertThat(model.requests).hasSize(1);
        assertThat(guard.spentUsd()).isEqualByComparingTo("0.02");     // 5K x $2 + 1K x $10, per million
        assertThat(guard.reservedUsd()).isEqualByComparingTo("0");
    }
}
