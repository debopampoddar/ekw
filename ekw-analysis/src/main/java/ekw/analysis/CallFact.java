package ekw.analysis;

import ekw.analysis.Facts.CallFlag;
import ekw.analysis.Facts.SymbolId;
import ekw.core.Evidence.CodeLocation;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record CallFact(SymbolId caller, Optional<SymbolId> callee, CodeLocation site, Set<CallFlag> flags,
                        Optional<String> unresolvedReason) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private SymbolId caller;
        private Optional<SymbolId> callee = Optional.empty();
        private CodeLocation site;
        private Set<CallFlag> flags = Set.of();
        private Optional<String> unresolvedReason = Optional.empty();

        private Builder() {
        }

        public Builder caller(SymbolId caller) {
            this.caller = caller;
            return this;
        }

        public Builder callee(Optional<SymbolId> callee) {
            this.callee = callee;
            return this;
        }

        public Builder callee(SymbolId callee) {
            this.callee = Optional.ofNullable(callee);
            return this;
        }

        public Builder site(CodeLocation site) {
            this.site = site;
            return this;
        }

        public Builder flags(Set<CallFlag> flags) {
            this.flags = flags;
            return this;
        }

        public Builder flags(CallFlag... flags) {
            this.flags = flags.length == 0 ? Set.of() : EnumSet.copyOf(Arrays.asList(flags));
            return this;
        }

        public Builder unresolvedReason(Optional<String> unresolvedReason) {
            this.unresolvedReason = unresolvedReason;
            return this;
        }

        public Builder unresolvedReason(String unresolvedReason) {
            this.unresolvedReason = Optional.ofNullable(unresolvedReason);
            return this;
        }

        public CallFact build() {
            return new CallFact(Objects.requireNonNull(caller, "caller"), callee,
                    Objects.requireNonNull(site, "site"), flags, unresolvedReason);
        }
    }
}
