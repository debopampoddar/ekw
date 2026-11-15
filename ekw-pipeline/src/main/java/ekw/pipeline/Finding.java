package ekw.pipeline;

import java.util.Objects;

public record Finding(String ruleId, Pipeline.Severity severity, String pagePath, String message,
                       boolean needsPerson) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String ruleId;
        private Pipeline.Severity severity;
        private String pagePath;
        private String message;
        private boolean needsPerson;

        private Builder() {
        }

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder severity(Pipeline.Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder pagePath(String pagePath) {
            this.pagePath = pagePath;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder needsPerson(boolean needsPerson) {
            this.needsPerson = needsPerson;
            return this;
        }

        public Finding build() {
            return new Finding(Objects.requireNonNull(ruleId, "ruleId"), Objects.requireNonNull(severity, "severity"),
                    Objects.requireNonNull(pagePath, "pagePath"), Objects.requireNonNull(message, "message"),
                    needsPerson);
        }
    }
}
