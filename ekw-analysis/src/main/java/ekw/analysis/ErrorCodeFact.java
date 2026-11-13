package ekw.analysis;

import ekw.analysis.Facts.SymbolId;
import ekw.core.Evidence.CodeLocation;

import java.util.Objects;
import java.util.Optional;

/**
 * A log call whose first argument is a constant code, e.g. Dubbo's "0-1".
 */
public record ErrorCodeFact(String code, Optional<SymbolId> constant, CodeLocation logSite, String level) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String code;
        private Optional<SymbolId> constant = Optional.empty();
        private CodeLocation logSite;
        private String level;

        private Builder() {
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder constant(Optional<SymbolId> constant) {
            this.constant = constant;
            return this;
        }

        public Builder constant(SymbolId constant) {
            this.constant = Optional.ofNullable(constant);
            return this;
        }

        public Builder logSite(CodeLocation logSite) {
            this.logSite = logSite;
            return this;
        }

        public Builder level(String level) {
            this.level = level;
            return this;
        }

        public ErrorCodeFact build() {
            return new ErrorCodeFact(Objects.requireNonNull(code, "code"), constant,
                    Objects.requireNonNull(logSite, "logSite"), Objects.requireNonNull(level, "level"));
        }
    }
}
