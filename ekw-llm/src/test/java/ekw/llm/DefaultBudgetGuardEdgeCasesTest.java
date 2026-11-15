package ekw.llm;

import ekw.llm.Models.BudgetExceeded;
import ekw.llm.Models.Reservation;
import ekw.llm.Models.TokenEstimate;
import ekw.llm.Models.Usage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Edge cases beyond the chapter's five tests: surcharge thresholds, cache tokens, rounding, exactly-once
 * settle/release, error messages, and thread safety.
 */
@Tag("ch06")
class DefaultBudgetGuardEdgeCasesTest {
    private static final PriceTable PRICES = TestModels.PRICES;
    private static final ModelInfo SONNET = TestModels.info("claude-sonnet-5-5", "anthropic");

    private static DefaultBudgetGuard guard(String limit) {
        return new DefaultBudgetGuard(new BigDecimal(limit), PRICES, Map.of());
    }

    // ---- PriceTable ---------------------------------------------------------------------------------------------

    @Test
    void cacheWritesAboveTheThresholdGetTheInputSurcharge() {
        // 300K total input > 272K: 200K x $2 x 2 + 100K x $2.50 x 2 = $0.80 + $0.50
        assertThat(PRICES.actual("gpt-6.1-sol", new Usage(200_000, 0, 0, 100_000)))
                .isEqualByComparingTo("1.30");
    }

    @Test
    void cacheReadsCountTowardsTheThreshold() {
        // 200K + 100K read = 300K > 272K: 200K x $2 x 2 + 100K x $0.10 x 2 = $0.80 + $0.02 (not 0.41)
        assertThat(PRICES.actual("gpt-6.1-sol", new Usage(200_000, 0, 100_000, 0)))
                .isEqualByComparingTo("0.82");
    }

    @Test
    void theSurchargeStartsStrictlyAboveTheThreshold() {
        // exactly 272K: 272K x $2.50 + 10K x $10 = $0.68 + $0.10, no surcharge
        assertThat(PRICES.worstCase("gpt-6.1-sol", 272_000, 10_000)).isEqualByComparingTo("0.78");
        // one token more: 272,001 x $5 + 10K x $15
        assertThat(PRICES.worstCase("gpt-6.1-sol", 272_001, 10_000)).isEqualByComparingTo("1.510005");
        // the same rule for actual(): exactly 272K plain input at $2
        assertThat(PRICES.actual("gpt-6.1-sol", new Usage(272_000, 0, 0, 0))).isEqualByComparingTo("0.544");
    }

    @Test
    void roundsUpSoACostIsNeverZero() {
        // 1 token x $0.01 / 1M = 0.00000001 exactly; anything smaller must round up, not to 0
        assertThat(PRICES.actual("claude-haiku-5-5", new Usage(0, 0, 1, 0))).isEqualByComparingTo("0.00000001");
        assertThat(PRICES.actual("claude-haiku-5-5", new Usage(1, 0, 0, 0)).signum()).isPositive();
    }

    @Test
    void anUnknownModelIsAnError() {
        assertThatThrownBy(() -> PRICES.worstCase("nope", 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nope");
        assertThatThrownBy(() -> PRICES.actual("nope", new Usage(1, 1, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- DefaultBudgetGuard -------------------------------------------------------------------------------------

    @Test
    void anEstimatedInputGetsA25PercentMargin() {
        // $0.21 x 1.25
        Reservation r = guard("15").reserve(SONNET, new TokenEstimate(20_000, false), 16_000);
        assertThat(r.worstCaseUsd()).isEqualByComparingTo("0.2625");
    }

    @Test
    void aWorstCaseEqualToTheLimitIsAllowed() {
        DefaultBudgetGuard g = guard("0.21");
        Reservation r = g.reserve(SONNET, new TokenEstimate(20_000, true), 16_000);
        assertThat(r.worstCaseUsd()).isEqualByComparingTo("0.21");
        assertThat(g.reservedUsd()).isEqualByComparingTo("0.21");
    }

    @Test
    void anUnknownModelReservesNothing() {
        DefaultBudgetGuard g = guard("15");
        ModelInfo unknown = ModelInfo.builder().profile("x").provider("x").modelId("nope")
                .prices(SONNET.prices()).build();
        assertThatThrownBy(() -> g.reserve(unknown, new TokenEstimate(1, true), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(g.reservedUsd()).isEqualByComparingTo("0");
    }

    @Test
    void aReservationIsSettledOrReleasedExactlyOnce() {
        DefaultBudgetGuard g = guard("15");
        Reservation settled = g.reserve(SONNET, new TokenEstimate(20_000, true), 16_000);
        g.settle(settled, new Usage(20_000, 5_000, 0, 0), SONNET);                     // $0.09

        assertThatThrownBy(() -> g.settle(settled, new Usage(20_000, 5_000, 0, 0), SONNET))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> g.release(settled)).isInstanceOf(IllegalStateException.class);

        Reservation released = g.reserve(SONNET, new TokenEstimate(20_000, true), 16_000);
        g.release(released);
        assertThatThrownBy(() -> g.release(released)).isInstanceOf(IllegalStateException.class);

        // the rejected calls changed nothing
        assertThat(g.spentUsd()).isEqualByComparingTo("0.09");
        assertThat(g.reservedUsd()).isEqualByComparingTo("0");
    }

    @Test
    void aRefusalGivesTheNumbers() {
        // 10K x $2.50 + 16K x $10 = $0.185 > $0.10
        assertThatThrownBy(() -> guard("0.10").reserve(SONNET, new TokenEstimate(10_000, true), 16_000))
                .isInstanceOf(BudgetExceeded.class)
                .hasMessageContaining("0.185")
                .hasMessageContaining("0.10");
    }

    @Test
    void aPriceTableErrorStillBooksTheCost() {
        DefaultBudgetGuard g = guard("15");
        Reservation r = g.reserve(SONNET, new TokenEstimate(1_000, true), 100);       // $0.0035
        // 1K x $2 + 50K x $10 = $0.502: the money is gone, so it must be counted
        assertThatThrownBy(() -> g.settle(r, new Usage(1_000, 50_000, 0, 0), SONNET))
                .hasMessageContaining("PRICE_TABLE_ERROR")
                .hasMessageContaining("0.502")
                .hasMessageContaining("0.0035");
        assertThat(g.spentUsd()).isEqualByComparingTo("0.502");
        assertThat(g.reservedUsd()).isEqualByComparingTo("0");
    }

    /**
     * Many threads reserve at once. If the check-and-book in reserve() is not atomic, bookings are lost and
     * reservedUsd() no longer equals the sum of the reservations handed out.
     */
    @Test
    void concurrentReservationsAreNeverLost() throws Exception {
        int threads = 16;
        int perThread = 500;
        for (int round = 0; round < 20; round++) {
            DefaultBudgetGuard g = guard("1000");
            CountDownLatch start = new CountDownLatch(1);
            List<Future<BigDecimal>> results = new ArrayList<>();
            try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
                for (int t = 0; t < threads; t++) {
                    results.add(pool.submit(() -> {
                        start.await();                         // all threads start together, so they really race
                        BigDecimal sum = BigDecimal.ZERO;
                        for (int i = 0; i < perThread; i++) {
                            sum = sum.add(g.reserve(SONNET, new TokenEstimate(1_000, true), 100).worstCaseUsd());
                        }
                        return sum;
                    }));
                }
                start.countDown();
                BigDecimal handedOut = BigDecimal.ZERO;
                for (Future<BigDecimal> f : results) {
                    handedOut = handedOut.add(f.get());
                }
                assertThat(g.reservedUsd()).as("round %d", round).isEqualByComparingTo(handedOut);
            }
        }
    }
}
