package ekw.llm;

import ekw.llm.Models.BudgetExceeded;
import ekw.llm.Models.Reservation;
import ekw.llm.Models.TokenEstimate;
import ekw.llm.Models.Usage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ch06")
class DefaultBudgetGuardTest {
    private static final Models.ModelInfo SONNET = TestModels.info("claude-sonnet-5-5", "anthropic");

    @Test
    void reservesTheWorstCaseIncludingTheDearerFallback() {
        DefaultBudgetGuard guard = new DefaultBudgetGuard(new BigDecimal("15"), TestModels.PRICES,
                Map.of("claude-sonnet-5-5", "claude-opus-5-5"));
        Reservation r = guard.reserve(SONNET, new TokenEstimate(100_000, true), 16_000);
        // Opus prices: 100K x $5 (cache write) + 16K x $20 = $0.50 + $0.32
        assertThat(r.worstCaseUsd()).isEqualByComparingTo("0.82");
        guard.settle(r, new Usage(90_000, 3_000, 0, 0), SONNET);   // 0.18 + 0.03
        assertThat(guard.spentUsd()).isEqualByComparingTo("0.21");
        assertThat(guard.reservedUsd()).isEqualByComparingTo("0");
    }

    @Test
    void refusesACallThatCouldPassTheLimit() {
        DefaultBudgetGuard guard = new DefaultBudgetGuard(new BigDecimal("0.10"), TestModels.PRICES, Map.of());
        assertThatThrownBy(() -> guard.reserve(SONNET, new TokenEstimate(10_000, true), 16_000))
                .isInstanceOf(BudgetExceeded.class);        // 0.02 + 0.16 > 0.10
        assertThat(guard.reservedUsd()).isEqualByComparingTo("0");
    }

    @Test
    void appliesLongContextSurcharges() {
        assertThat(TestModels.PRICES.worstCase("gpt-6.1-sol", 300_000, 10_000))
                .isEqualByComparingTo("1.65");              // 300K x 2.50 x 2 + 10K x 10 x 1.5
    }

    /** Property: with any mix of calls, spending never passes the limit when usage stays within the reservation. */
    @Test
    void neverPassesTheLimit() {
        Random random = new Random(42);
        for (int run = 0; run < 200; run++) {
            BigDecimal limit = BigDecimal.valueOf(random.nextInt(500) + 1, 2);
            DefaultBudgetGuard guard = new DefaultBudgetGuard(limit, TestModels.PRICES, Map.of());
            for (int call = 0; call < 100; call++) {
                long in = random.nextInt(50_000);
                int maxOut = random.nextInt(8_000) + 1;
                Reservation r;
                try {
                    r = guard.reserve(SONNET, new TokenEstimate(in, true), maxOut);
                } catch (BudgetExceeded e) {
                    continue;
                }
                if (random.nextInt(10) == 0) { guard.release(r); continue; }
                long read = random.nextLong(in + 1);
                guard.settle(r, new Usage(in - read, random.nextInt(maxOut + 1), read, 0), SONNET);
            }
            assertThat(guard.spentUsd()).isLessThanOrEqualTo(limit);
        }
    }

    @Test
    void stopsOnPriceTableErrors() {
        DefaultBudgetGuard guard = new DefaultBudgetGuard(new BigDecimal("15"), TestModels.PRICES, Map.of());
        Reservation r = guard.reserve(SONNET, new TokenEstimate(1_000, true), 100);
        assertThatThrownBy(() -> guard.settle(r, new Usage(1_000, 50_000, 0, 0), SONNET))
                .hasMessageContaining("PRICE_TABLE_ERROR");
    }
}
