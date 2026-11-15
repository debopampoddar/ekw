package ekw.eval;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * @param questionIds  the 12 questions: A01, A02, A04, A06, A09, A13, A15, A18, A20, A21, A26, A28
 * @param writerModels model ids that write pages, e.g. claude-sonnet-5-5 (effort high), claude-opus-5-5 (effort low)
 * @param answerModels model ids that answer in WIKI_TOOLS; the first one also answers RAW_AGENT and FACTS_ONLY
 */
public record Config(Path work, Path suite, BigDecimal budgetUsd, List<String> questionIds,
                      List<String> writerModels, List<String> answerModels) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Path work;
        private Path suite;
        private BigDecimal budgetUsd;
        private List<String> questionIds = List.of();
        private List<String> writerModels = List.of();
        private List<String> answerModels = List.of();

        private Builder() {
        }

        public Builder work(Path work) {
            this.work = work;
            return this;
        }

        public Builder suite(Path suite) {
            this.suite = suite;
            return this;
        }

        public Builder budgetUsd(BigDecimal budgetUsd) {
            this.budgetUsd = budgetUsd;
            return this;
        }

        public Builder questionIds(List<String> questionIds) {
            this.questionIds = questionIds;
            return this;
        }

        public Builder writerModels(List<String> writerModels) {
            this.writerModels = writerModels;
            return this;
        }

        public Builder answerModels(List<String> answerModels) {
            this.answerModels = answerModels;
            return this;
        }

        public Config build() {
            return new Config(Objects.requireNonNull(work, "work"), Objects.requireNonNull(suite, "suite"),
                    Objects.requireNonNull(budgetUsd, "budgetUsd"), questionIds, writerModels, answerModels);
        }
    }
}
