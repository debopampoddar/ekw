package ekw.llm;

import ekw.core.Evidence;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Provider-neutral model layer. Adapters: Anthropic SDK, OpenAI Responses SDK, Ollama /api/chat, OpenAI-compatible. */
public final class Models {
    private Models() {}

    public enum Role { WRITER, ANSWERER, CHECKER, ROUTER, TRIAGE, JUDGE }

    public enum Effort { LOW, MEDIUM, HIGH, XHIGH, MAX }

    public enum StopReason { END_TURN, TOOL_USE, MAX_TOKENS, REFUSAL, ERROR }

    /** Prices in USD per million tokens. Long-context surcharges are applied by the adapter's cost function. */
    public record Prices(BigDecimal input, BigDecimal output, BigDecimal cacheRead, BigDecimal cacheWrite) {}

    public record ModelInfo(String profile, String provider, String modelId, long contextTokens, long maxOutputTokens,
                            Prices prices, boolean local, List<Role> qualifiedRoles) {}

    /** Versioned prompt reference, e.g. ("writer.component", 1). Recorded in every ledger line and page. */
    public record PromptRef(String id, int version) {}

    public sealed interface Part permits Text, ToolCall, ToolResult, Native {}
    public record Text(String text) implements Part {}
    public record ToolCall(String callId, String name, Map<String, Object> arguments) implements Part {}
    public record ToolResult(String callId, String content, boolean isError) implements Part {}

    /**
     * The provider's own assistant turn (Claude: the Message with its thinking blocks; OpenAI: the output items with
     * reasoning items). Adapters add it to every assistant message they return and send it back unchanged on the
     * next call to the same provider, because tool use with thinking requires the turn verbatim.
     */
    public record Native(String provider, Object turn) implements Part {}

    public record Message(String role, List<Part> parts) {}

    /** JSON Schema as a plain map; adapters translate it (Claude output_config.format, OpenAI text.format, Ollama format). */
    public record ToolSpec(String name, String description, Map<String, Object> inputSchema) {}

    /** cacheablePrefixParts: how many leading system/tool parts form the stable, cached prefix. */
    public record ChatRequest(Role role, PromptRef prompt, String system, List<Message> messages,
                              List<ToolSpec> tools, Optional<Map<String, Object>> outputSchema,
                              int maxOutputTokens, Optional<Effort> effort, Evidence.Packet evidence,
                              int cacheablePrefixParts) {}

    public record Usage(long inputTokens, long outputTokens, long cacheReadTokens, long cacheWriteTokens) {}

    public record ChatResult(List<Part> content, StopReason stop, Usage usage, String servedByModelId,
                             Duration latency) {}

    public record TokenEstimate(long inputTokens, boolean exact) {}

    public interface ChatModel {
        ModelInfo info();
        ChatResult chat(ChatRequest request);
        TokenEstimate estimate(ChatRequest request);
        default Optional<String> submitBatch(List<ChatRequest> requests) { return Optional.empty(); }
    }

    /** Chooses the model for a role and enforces model_policy (local-only evidence never goes to a hosted model). */
    public interface ModelRouter {
        ChatModel forRole(Role role, Evidence.Packet evidence);
    }

    public record Reservation(String id, BigDecimal worstCaseUsd) {}

    public static final class BudgetExceeded extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public BudgetExceeded(String message) { super(message); }
    }

    /**
     * Hard ceiling. reserve() charges the worst case (input tokens x input price + maxOutput x output price,
     * at the dearer of the model and its refusal fallback) before any call; settle() books the actual usage.
     */
    public interface BudgetGuard {
        Reservation reserve(ModelInfo model, TokenEstimate input, int maxOutputTokens) throws BudgetExceeded;
        void settle(Reservation reservation, Usage actual, ModelInfo servedBy);
        void release(Reservation reservation);
        BigDecimal limitUsd();
        BigDecimal spentUsd();
        BigDecimal reservedUsd();
    }
}
