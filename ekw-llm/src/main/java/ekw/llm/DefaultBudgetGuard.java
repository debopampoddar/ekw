package ekw.llm;

import ekw.core.Todo;
import ekw.llm.Models.ModelInfo;
import ekw.llm.Models.Reservation;
import ekw.llm.Models.TokenEstimate;
import ekw.llm.Models.Usage;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The hard ceiling (design: stage 2, section 3.8). Every call reserves its worst case before it is sent; a reservation
 * that would pass the limit is refused with {@link Models.BudgetExceeded}. Must be thread-safe.
 *
 * <p>Chapter 6 of the development guide.
 */
public final class DefaultBudgetGuard implements Models.BudgetGuard {

    /**
     * @param fallbackModelId model id -> the model that serves the call if the first one refuses
     */
    public DefaultBudgetGuard(BigDecimal limitUsd, PriceTable prices, Map<String, String> fallbackModelId) {
        // TODO (chapter 6): keep the arguments.
    }

    @Override
    public Reservation reserve(ModelInfo model, TokenEstimate input, int maxOutputTokens) {
        throw Todo.chapter(6, "DefaultBudgetGuard.reserve");
    }

    @Override
    public void settle(Reservation reservation, Usage actual, ModelInfo servedBy) {
        throw Todo.chapter(6, "DefaultBudgetGuard.settle");
    }

    @Override
    public void release(Reservation reservation) {
        throw Todo.chapter(6, "DefaultBudgetGuard.release");
    }

    @Override
    public BigDecimal limitUsd() {
        throw Todo.chapter(6, "DefaultBudgetGuard.limitUsd");
    }

    @Override
    public BigDecimal spentUsd() {
        throw Todo.chapter(6, "DefaultBudgetGuard.spentUsd");
    }

    @Override
    public BigDecimal reservedUsd() {
        throw Todo.chapter(6, "DefaultBudgetGuard.reservedUsd");
    }
}
