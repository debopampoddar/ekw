package ekw.eval;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Per setup and model: mean score, answers with score >= 0.75 ("correct"), cost, cost per correct answer.
 */
public record Summary(String setup, String model, int answers, double meanScore, int correct,
                       BigDecimal totalCostUsd, Optional<BigDecimal> costPerCorrectUsd) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String setup;
        private String model;
        private int answers;
        private double meanScore;
        private int correct;
        private BigDecimal totalCostUsd = BigDecimal.ZERO;
        private Optional<BigDecimal> costPerCorrectUsd = Optional.empty();

        private Builder() {
        }

        public Builder setup(String setup) {
            this.setup = setup;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder answers(int answers) {
            this.answers = answers;
            return this;
        }

        public Builder meanScore(double meanScore) {
            this.meanScore = meanScore;
            return this;
        }

        public Builder correct(int correct) {
            this.correct = correct;
            return this;
        }

        public Builder totalCostUsd(BigDecimal totalCostUsd) {
            this.totalCostUsd = totalCostUsd;
            return this;
        }

        public Builder costPerCorrectUsd(Optional<BigDecimal> costPerCorrectUsd) {
            this.costPerCorrectUsd = costPerCorrectUsd;
            return this;
        }

        public Builder costPerCorrectUsd(BigDecimal costPerCorrectUsd) {
            this.costPerCorrectUsd = Optional.ofNullable(costPerCorrectUsd);
            return this;
        }

        public Summary build() {
            return new Summary(Objects.requireNonNull(setup, "setup"), Objects.requireNonNull(model, "model"),
                    answers, meanScore, correct, totalCostUsd, costPerCorrectUsd);
        }
    }
}
