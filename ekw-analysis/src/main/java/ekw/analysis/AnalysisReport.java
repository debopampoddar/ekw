package ekw.analysis;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record AnalysisReport(String module, int files, int callSites, int resolvedCallSites,
                              int unresolvedCallSites, Duration time, List<String> warnings) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String module;
        private int files;
        private int callSites;
        private int resolvedCallSites;
        private int unresolvedCallSites;
        private Duration time;
        private List<String> warnings = List.of();

        private Builder() {
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder files(int files) {
            this.files = files;
            return this;
        }

        public Builder callSites(int callSites) {
            this.callSites = callSites;
            return this;
        }

        public Builder resolvedCallSites(int resolvedCallSites) {
            this.resolvedCallSites = resolvedCallSites;
            return this;
        }

        public Builder unresolvedCallSites(int unresolvedCallSites) {
            this.unresolvedCallSites = unresolvedCallSites;
            return this;
        }

        public Builder time(Duration time) {
            this.time = time;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        public AnalysisReport build() {
            return new AnalysisReport(Objects.requireNonNull(module, "module"), files, callSites, resolvedCallSites,
                    unresolvedCallSites, Objects.requireNonNull(time, "time"), warnings);
        }
    }
}
