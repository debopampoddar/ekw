package ekw.core;

import ekw.core.Evidence.Location;

import java.util.Objects;
import java.util.Optional;

/** Lines are 1-based and inclusive. symbol is the analyzer's stable id when the range is a declaration. */
public record CodeLocation(String system, String commit, String path, int startLine, int endLine,
                            Optional<String> symbol) implements Location {
    public CodeLocation {
        if (startLine < 1 || endLine < startLine) throw new IllegalArgumentException("bad range");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String system;
        private String commit;
        private String path;
        private int startLine;
        private int endLine;
        private Optional<String> symbol = Optional.empty();

        private Builder() {
        }

        public Builder system(String system) {
            this.system = system;
            return this;
        }

        public Builder commit(String commit) {
            this.commit = commit;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder startLine(int startLine) {
            this.startLine = startLine;
            return this;
        }

        public Builder endLine(int endLine) {
            this.endLine = endLine;
            return this;
        }

        public Builder symbol(Optional<String> symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder symbol(String symbol) {
            this.symbol = Optional.ofNullable(symbol);
            return this;
        }

        public CodeLocation build() {
            return new CodeLocation(Objects.requireNonNull(system, "system"), Objects.requireNonNull(commit, "commit"),
                    Objects.requireNonNull(path, "path"), startLine, endLine, symbol);
        }
    }
}
