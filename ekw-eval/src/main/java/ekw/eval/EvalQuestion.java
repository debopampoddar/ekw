package ekw.eval;

import ekw.query.Query;

import java.util.List;
import java.util.Objects;

public record EvalQuestion(String id, String suite, Query.QuestionType type, String question,
                            List<String> keyPoints, List<Eval.RequiredEvidence> requiredEvidence,
                            boolean expectNotFound, List<String> mustNotSay, String difficulty) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String suite;
        private Query.QuestionType type;
        private String question;
        private List<String> keyPoints = List.of();
        private List<Eval.RequiredEvidence> requiredEvidence = List.of();
        private boolean expectNotFound;
        private List<String> mustNotSay = List.of();
        private String difficulty;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder suite(String suite) {
            this.suite = suite;
            return this;
        }

        public Builder type(Query.QuestionType type) {
            this.type = type;
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

        public Builder requiredEvidence(List<Eval.RequiredEvidence> requiredEvidence) {
            this.requiredEvidence = requiredEvidence;
            return this;
        }

        public Builder expectNotFound(boolean expectNotFound) {
            this.expectNotFound = expectNotFound;
            return this;
        }

        public Builder mustNotSay(List<String> mustNotSay) {
            this.mustNotSay = mustNotSay;
            return this;
        }

        public Builder difficulty(String difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public EvalQuestion build() {
            return new EvalQuestion(Objects.requireNonNull(id, "id"), Objects.requireNonNull(suite, "suite"),
                    Objects.requireNonNull(type, "type"), Objects.requireNonNull(question, "question"), keyPoints,
                    requiredEvidence, expectNotFound, mustNotSay, Objects.requireNonNull(difficulty, "difficulty"));
        }
    }
}
