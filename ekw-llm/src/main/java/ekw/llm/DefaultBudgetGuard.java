package ekw.llm;

import ekw.llm.Models.Reservation;
import ekw.llm.Models.TokenEstimate;
import ekw.llm.Models.Usage;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The hard ceiling (design: stage 2, section 3.8). Every call reserves its worst case before it is sent; a reservation
 * that would pass the limit is refused with {@link Models.BudgetExceeded}. Must be thread-safe.
 *
 * <p>Chapter 6 of the development guide.
 */
public final class DefaultBudgetGuard implements Models.BudgetGuard {

    private final BigDecimal limit;
    private final PriceTable prices;
    private final Map<String, String> fallbackModelId;
    private final Map<String, BigDecimal> open = new HashMap<>();     // reservation id -> amount
    private BigDecimal spent = BigDecimal.ZERO;
    private BigDecimal reserved = BigDecimal.ZERO;


    /**
     * @param fallbackModelId model id -> the model that serves the call if the first one refuses
     */
    public DefaultBudgetGuard(BigDecimal limitUsd, PriceTable prices, Map<String, String> fallbackModelId) {
        this.limit = limitUsd;
        this.prices = prices;
        this.fallbackModelId = (null == fallbackModelId) ? Map.of() : Map.copyOf(fallbackModelId);
    }

    @Override
    public synchronized Reservation reserve(ModelInfo model, TokenEstimate input, int maxOutputTokens) {
        BigDecimal worst = prices.worstCase(model.modelId(), input.inputTokens(), maxOutputTokens);
        String fallback = fallbackModelId.get(model.modelId());
        if(null != fallback) {
            worst = worst.max(prices.worstCase(fallback, input.inputTokens(), maxOutputTokens));
        }

        if(!input.exact()) {
            worst = worst.multiply(new BigDecimal("1.25"));
        }

        if(spentUsd().add(reservedUsd()).add(worst).compareTo(limitUsd()) > 0) {
            throw new Models.BudgetExceeded("call needs up to $" + worst
                    + ", but spent $" + spent
                    + " + reserved $" + reserved
                    + " leaves too little of $" + limit);
        }
        Reservation reservation = new Reservation(UUID.randomUUID().toString(), worst);
        open.put(reservation.id(), worst);
        reserved = reserved.add(worst);
        return reservation;
    }

    @Override
    public synchronized void settle(Reservation reservation, Usage actual, ModelInfo servedBy) {
        BigDecimal worst = take(reservation);
        BigDecimal cost = prices.actual(servedBy.modelId(), actual);
        spent = spentUsd().add(cost);
        if(cost.compareTo(worst) > 0) {
            throw new IllegalStateException("PRICE_TABLE_ERROR: actual $" + cost + " > reserved $" + worst
                    + " for " + servedBy.modelId());
        }
    }

    /** Removes an open reservation and frees its amount. */
    private BigDecimal take(Reservation r) {
        BigDecimal worst = open.remove(r.id());
        if (worst == null) throw new IllegalStateException("unknown or already settled reservation " + r.id());
        reserved = reserved.subtract(worst);
        return worst;
    }

    @Override
    public synchronized void release(Reservation reservation) {
        take(reservation);
    }

    @Override
    public BigDecimal limitUsd() {
        return limit;
    }

    @Override
    public synchronized BigDecimal spentUsd() {
        return spent;
    }

    @Override
    public synchronized BigDecimal reservedUsd() {
        return reserved;
    }
}
