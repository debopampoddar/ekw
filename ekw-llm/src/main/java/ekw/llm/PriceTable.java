package ekw.llm;

import ekw.core.Todo;
import ekw.llm.Models.Prices;
import ekw.llm.Models.Usage;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Prices per model id, in USD per million tokens. Long-context surcharges multiply the whole call's prices once the
 * input passes the threshold.
 *
 * <p>Chapter 6 of the development guide.
 */
public record PriceTable(Map<String, Entry> byModelId) {

    public record Surcharge(long aboveInputTokens, BigDecimal inputMultiplier, BigDecimal outputMultiplier) {}

    public record Entry(Prices prices, Optional<Surcharge> longContext) {}

    /** The entry for a model id; an unknown id is an error (never price a call at zero by accident). */
    public Entry entry(String modelId) {
        throw Todo.chapter(6, "PriceTable.entry");
    }

    /**
     * The most a call can cost: every input token at the dearer of the input and cache-write price, every allowed
     * output token at the output price, surcharges applied when the input passes the threshold.
     */
    public BigDecimal worstCase(String modelId, long inputTokens, long maxOutputTokens) {
        throw Todo.chapter(6, "PriceTable.worstCase");
    }

    /** What a call cost, from its reported usage (input, cache reads, cache writes and output priced separately). */
    public BigDecimal actual(String modelId, Usage usage) {
        throw Todo.chapter(6, "PriceTable.actual");
    }
}
