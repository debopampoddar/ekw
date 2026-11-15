package ekw.eval;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * One answer to grade.
 */
public record Answer(String questionId, String setup, String model, String question, List<String> keyPoints,
                      String answerMarkdown, BigDecimal costUsd, long inputTokens, long outputTokens,
                      int toolCalls) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String questionId;
        private String setup;
        private String model;
        private String question;
        private List<String> keyPoints = List.of();
        private String answerMarkdown;
        private BigDecimal costUsd = BigDecimal.ZERO;
        private long inputTokens;
        private long outputTokens;
        private int toolCalls;

        private Builder() {
        }

        public Builder questionId(String questionId) {
            this.questionId = questionId;
            return this;
        }

        public Builder setup(String setup) {
            this.setup = setup;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder keyPoints(List<String> keyPoints) {
            this.keyPoints = keyPoints;
            return this;
        }

        public Builder answerMarkdown(String answerMarkdown) {
            this.answerMarkdown = answerMarkdown;
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

        public Builder toolCalls(int toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }

        public Answer build() {
            return new Answer(Objects.requireNonNull(questionId, "questionId"), Objects.requireNonNull(setup, "setup"),
                    Objects.requireNonNull(model, "model"), Objects.requireNonNull(question, "question"), keyPoints,
                    Objects.requireNonNull(answerMarkdown, "answerMarkdown"), costUsd, inputTokens, outputTokens,
                    toolCalls);
        }
    }
}
