package ekw.query;

import ekw.llm.DefaultBudgetGuard;
import ekw.llm.Models;
import ekw.llm.Models.ChatRequest;
import ekw.llm.Models.ChatResult;
import ekw.llm.Models.Part;
import ekw.llm.Models.Text;
import ekw.llm.Models.ToolCall;
import ekw.llm.Models.ToolResult;
import ekw.llm.PriceTable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ch08")
class AnswerLoopTest {
    private static final Models.Prices PRICES = new Models.Prices(new BigDecimal("2"), new BigDecimal("10"),
            new BigDecimal("0.2"), new BigDecimal("2.5"));
    private static final PriceTable TABLE = new PriceTable(Map.of("fake", new PriceTable.Entry(PRICES, Optional.empty())));
    private static final Map<String, Object> SCHEMA = Map.of("type", "object");

    /** Replays scripted replies and records every request. */
    static final class ScriptedModel implements Models.ChatModel {
        final Deque<List<Part>> replies = new ArrayDeque<>();
        final List<ChatRequest> requests = new ArrayList<>();

        ScriptedModel reply(Part... parts) { replies.add(List.of(parts)); return this; }

        @Override public Models.ModelInfo info() {
            return new Models.ModelInfo("fake", "test", "fake", 200_000, 8_000, PRICES, false, List.of());
        }
        @Override public ChatResult chat(ChatRequest r) {
            requests.add(r);
            List<Part> parts = replies.removeFirst();
            boolean tool = parts.stream().anyMatch(p -> p instanceof ToolCall);
            return new ChatResult(parts, tool ? Models.StopReason.TOOL_USE : Models.StopReason.END_TURN,
                    new Models.Usage(1_000, 100, 0, 0), "fake", Duration.ZERO);
        }
        @Override public Models.TokenEstimate estimate(ChatRequest r) { return new Models.TokenEstimate(1_000, true); }
    }

    private static AnswerLoop.Tool readPage(List<String> log) {
        return new AnswerLoop.Tool(new Models.ToolSpec("read_page", "Read a page.", Map.of("type", "object")),
                args -> { log.add((String) args.get("path")); return new AnswerLoop.ToolOutput("[E1] page text", false); });
    }

    @Test
    void readsThenSubmits() {
        List<String> log = new ArrayList<>();
        ScriptedModel model = new ScriptedModel()
                .reply(new ToolCall("c1", "read_page", Map.of("path", "a.md")))
                .reply(new ToolCall("c2", "submit_answer", Map.of("outcome", "answered", "answer_markdown", "A [E1].")));
        DefaultBudgetGuard guard = new DefaultBudgetGuard(new BigDecimal("2"), TABLE, Map.of());

        AnswerLoop.Result r = new AnswerLoop(model, guard, SCHEMA)
                .run("system", "What is A?", List.of(readPage(log)), 8, 2_000, Optional.empty());

        assertThat(r.end()).isEqualTo(AnswerLoop.End.SUBMITTED);
        assertThat(r.answer()).containsEntry("answer_markdown", "A [E1].");
        assertThat(log).containsExactly("a.md");
        assertThat(model.requests.get(0).tools()).extracting(Models.ToolSpec::name).containsExactly("read_page", "submit_answer");
        assertThat(model.requests.get(1).messages().get(2).parts()).first().isInstanceOf(ToolResult.class);
        assertThat(guard.spentUsd()).isEqualByComparingTo("0.006");          // 2 calls x (1K x $2 + 100 x $10) / 1M
    }

    @Test
    void plainTextEndingGetsOneConversionCall() {
        ScriptedModel model = new ScriptedModel()
                .reply(new Text("It does X."))
                .reply(new Text("{\"outcome\": \"answered\", \"answer_markdown\": \"It does X.\"}"));
        AnswerLoop.Result r = new AnswerLoop(model, new DefaultBudgetGuard(new BigDecimal("2"), TABLE, Map.of()), SCHEMA)
                .run("system", "q", List.of(), 8, 2_000, Optional.empty());
        assertThat(r.end()).isEqualTo(AnswerLoop.End.CONVERTED);
        assertThat(r.answer()).containsEntry("outcome", "answered");
        assertThat(model.requests.get(1).tools()).isEmpty();
        assertThat(model.requests.get(1).outputSchema()).contains(SCHEMA);
    }

    @Test
    void stopsAtTheBudgetWithAnInconclusiveAnswer() {
        ScriptedModel model = new ScriptedModel();
        DefaultBudgetGuard tiny = new DefaultBudgetGuard(new BigDecimal("0.001"), TABLE, Map.of());
        AnswerLoop.Result r = new AnswerLoop(model, tiny, SCHEMA).run("system", "q", List.of(), 8, 2_000, Optional.empty());
        assertThat(r.end()).isEqualTo(AnswerLoop.End.BUDGET);
        assertThat(r.answer()).containsEntry("outcome", "inconclusive");
        assertThat(model.requests).isEmpty();                           // nothing was sent
    }
}
