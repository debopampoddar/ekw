package ekw.analysis;

import ekw.core.Evidence.CodeLocation;

import java.util.Objects;
import java.util.Optional;

/**
 * A configuration key read in code or declared in a properties/yaml file; values are never stored.
 */
public record ConfigKeyFact(String key, Optional<String> defaultValue, CodeLocation site, String how) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String key;
        private Optional<String> defaultValue = Optional.empty();
        private CodeLocation site;
        private String how;

        private Builder() {
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder defaultValue(Optional<String> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = Optional.ofNullable(defaultValue);
            return this;
        }

        public Builder site(CodeLocation site) {
            this.site = site;
            return this;
        }

        public Builder how(String how) {
            this.how = how;
            return this;
        }

        public ConfigKeyFact build() {
            return new ConfigKeyFact(Objects.requireNonNull(key, "key"), defaultValue,
                    Objects.requireNonNull(site, "site"), Objects.requireNonNull(how, "how"));
        }
    }
}
