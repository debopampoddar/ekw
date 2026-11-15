package ekw.query;

import ekw.query.Query.QuestionType;

import java.util.List;
import java.util.Objects;

public record Route(QuestionType type, List<String> entities, List<String> systems, List<String> startPages,
                     int maxToolCalls, long inputTokenBudget, boolean supportCheck) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private QuestionType type;
        private List<String> entities = List.of();
        private List<String> systems = List.of();
        private List<String> startPages = List.of();
        private int maxToolCalls;
        private long inputTokenBudget;
        private boolean supportCheck;

        private Builder() {
        }

        public Builder type(QuestionType type) {
            this.type = type;
            return this;
        }

        public Builder entities(List<String> entities) {
            this.entities = entities;
            return this;
        }

        public Builder systems(List<String> systems) {
            this.systems = systems;
            return this;
        }

        public Builder startPages(List<String> startPages) {
            this.startPages = startPages;
            return this;
        }

        public Builder maxToolCalls(int maxToolCalls) {
            this.maxToolCalls = maxToolCalls;
            return this;
        }

        public Builder inputTokenBudget(long inputTokenBudget) {
            this.inputTokenBudget = inputTokenBudget;
            return this;
        }

        public Builder supportCheck(boolean supportCheck) {
            this.supportCheck = supportCheck;
            return this;
        }

        public Route build() {
            return new Route(Objects.requireNonNull(type, "type"), entities, systems, startPages, maxToolCalls,
                    inputTokenBudget, supportCheck);
        }
    }
}
