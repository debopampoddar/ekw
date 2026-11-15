package ekw.query;

import ekw.core.Evidence.Id;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DraftAnswer(String question, String title, String answerMarkdown, List<Id> cited,
                           Optional<String> promoteTo) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String question;
        private String title;
        private String answerMarkdown;
        private List<Id> cited = List.of();
        private Optional<String> promoteTo = Optional.empty();

        private Builder() {
        }

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder answerMarkdown(String answerMarkdown) {
            this.answerMarkdown = answerMarkdown;
            return this;
        }

        public Builder cited(List<Id> cited) {
            this.cited = cited;
            return this;
        }

        public Builder promoteTo(Optional<String> promoteTo) {
            this.promoteTo = promoteTo;
            return this;
        }

        public Builder promoteTo(String promoteTo) {
            this.promoteTo = Optional.ofNullable(promoteTo);
            return this;
        }

        public DraftAnswer build() {
            return new DraftAnswer(Objects.requireNonNull(question, "question"),
                    Objects.requireNonNull(title, "title"),
                    Objects.requireNonNull(answerMarkdown, "answerMarkdown"), cited, promoteTo);
        }
    }
}
