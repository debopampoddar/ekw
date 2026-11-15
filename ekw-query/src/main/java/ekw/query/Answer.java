package ekw.query;

import ekw.core.Item;
import ekw.query.Query.Confidence;
import ekw.query.Query.ClaimView;
import ekw.query.Query.Outcome;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record Answer(Outcome outcome, String markdown, List<ClaimView> claims, List<String> synthesis,
                      List<String> checked, List<Item> sources, Confidence confidence, BigDecimal costUsd,
                      long inputTokens, long outputTokens, Optional<String> savedDraft) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Outcome outcome;
        private String markdown;
        private List<ClaimView> claims = List.of();
        private List<String> synthesis = List.of();
        private List<String> checked = List.of();
        private List<Item> sources = List.of();
        private Confidence confidence;
        private BigDecimal costUsd = BigDecimal.ZERO;
        private long inputTokens;
        private long outputTokens;
        private Optional<String> savedDraft = Optional.empty();

        private Builder() {
        }

        public Builder outcome(Outcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Builder markdown(String markdown) {
            this.markdown = markdown;
            return this;
        }

        public Builder claims(List<ClaimView> claims) {
            this.claims = claims;
            return this;
        }

        public Builder synthesis(List<String> synthesis) {
            this.synthesis = synthesis;
            return this;
        }

        public Builder checked(List<String> checked) {
            this.checked = checked;
            return this;
        }

        public Builder sources(List<Item> sources) {
            this.sources = sources;
            return this;
        }

        public Builder confidence(Confidence confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder costUsd(BigDecimal costUsd) {
            this.costUsd = costUsd;
            return this;
        }

        public Builder inputTokens(long inputTokens) {
            this.inputTokens = inputTokens;
            return this;
        }

        public Builder outputTokens(long outputTokens) {
            this.outputTokens = outputTokens;
            return this;
        }

        public Builder savedDraft(Optional<String> savedDraft) {
            this.savedDraft = savedDraft;
            return this;
        }

        public Builder savedDraft(String savedDraft) {
            this.savedDraft = Optional.ofNullable(savedDraft);
            return this;
        }

        public Answer build() {
            return new Answer(Objects.requireNonNull(outcome, "outcome"), Objects.requireNonNull(markdown, "markdown"),
                    claims, synthesis, checked, sources, Objects.requireNonNull(confidence, "confidence"), costUsd,
                    inputTokens, outputTokens, savedDraft);
        }
    }
}
