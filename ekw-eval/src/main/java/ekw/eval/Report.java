package ekw.eval;

import ekw.eval.Eval.Agreement;
import ekw.eval.Eval.Setup;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Report(String suite, Map<Setup, Map<String, Double>> metrics, Map<Setup, BigDecimal> costUsd,
                      List<Agreement> calibration, List<String> notes) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String suite;
        private Map<Setup, Map<String, Double>> metrics = Map.of();
        private Map<Setup, BigDecimal> costUsd = Map.of();
        private List<Agreement> calibration = List.of();
        private List<String> notes = List.of();

        private Builder() {
        }

        public Builder suite(String suite) {
            this.suite = suite;
            return this;
        }

        public Builder metrics(Map<Setup, Map<String, Double>> metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder costUsd(Map<Setup, BigDecimal> costUsd) {
            this.costUsd = costUsd;
            return this;
        }

        public Builder calibration(List<Agreement> calibration) {
            this.calibration = calibration;
            return this;
        }

        public Builder notes(List<String> notes) {
            this.notes = notes;
            return this;
        }

        public Report build() {
            return new Report(Objects.requireNonNull(suite, "suite"), metrics, costUsd, calibration, notes);
        }
    }
}
