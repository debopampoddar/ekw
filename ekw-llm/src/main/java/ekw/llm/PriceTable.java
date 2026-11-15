package ekw.llm;

import ekw.llm.Models.Prices;
import ekw.llm.Models.Usage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * Prices per model id, in USD per million tokens. Long-context surcharges multiply the whole call's prices once the
 * input passes the threshold.
 *
 * <p>Chapter 6 of the development guide.
 */
public record PriceTable(Map<String, Entry> byModelId) {

    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    public record Surcharge(long aboveInputTokens
            , BigDecimal inputMultiplier
            , BigDecimal outputMultiplier) {
    }

    public record Entry (Prices prices, Optional < Surcharge > longContext){
    }

    /** The entry for a model id; an unknown id is an error (never price a call at zero by accident). */
    public Entry entry (String modelId){
        Entry entry = byModelId.get(modelId);
        if (null == entry) {
            throw new IllegalArgumentException("no price for model " + modelId);
        }
        return entry;
    }

    /**
     * The most a call can cost: every input token at the dearer of the input and cache-write price, every allowed
     * output token at the output price, surcharges applied when the input passes the threshold.
     */
    public BigDecimal worstCase (String modelId
            ,long inputTokens
            , long maxOutputTokens) {
        Entry e = entry(modelId);
        BigDecimal inputCost = e.prices().input().max(e.prices().cacheWrite());
        BigDecimal outputCost = e.prices().output();
        if (e.longContext().isPresent()
                && inputTokens > e.longContext().get().aboveInputTokens()) {
            inputCost = inputCost.multiply(e.longContext().get().inputMultiplier());
            outputCost = outputCost.multiply(e.longContext().get().outputMultiplier());
        }
        return perMillion(inputCost, inputTokens).add(perMillion(outputCost, maxOutputTokens));
    }

    /** What a call cost, from its reported usage (input, cache reads, cache writes and output priced separately). */
    public BigDecimal actual (String modelId
            , Usage usage){
        Entry e = entry(modelId);
        long totalInput =
                usage.inputTokens() + usage.cacheReadTokens() + usage.cacheWriteTokens();
        BigDecimal inputMultiplier = BigDecimal.ONE;
        BigDecimal outputMultiplier = BigDecimal.ONE;
        if (e.longContext().isPresent() && totalInput > e.longContext().get().aboveInputTokens()) {
            inputMultiplier = e.longContext().get().inputMultiplier();
            outputMultiplier = e.longContext().get().outputMultiplier();
        }
        Prices prices = e.prices();
        BigDecimal actualCost = perMillion(prices.input().multiply(inputMultiplier), usage.inputTokens())
                .add(perMillion(prices.cacheRead().multiply(inputMultiplier), usage.cacheReadTokens()))
                .add(perMillion(prices.cacheWrite().multiply(inputMultiplier), usage.cacheWriteTokens()))
                .add(perMillion(prices.output().multiply(outputMultiplier), usage.outputTokens()));
        return actualCost;
    }

    private static BigDecimal perMillion(BigDecimal price, long tokens) {
        return price.multiply(BigDecimal.valueOf(tokens)).divide(MILLION, 8, RoundingMode.UP);
    }
}
