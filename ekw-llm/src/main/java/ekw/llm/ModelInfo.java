package ekw.llm;

import ekw.llm.Models.Prices;
import ekw.llm.Models.Role;

import java.util.List;
import java.util.Objects;

public record ModelInfo(String profile, String provider, String modelId, long contextTokens, long maxOutputTokens,
                         Prices prices, boolean local, List<Role> qualifiedRoles) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String profile;
        private String provider;
        private String modelId;
        private long contextTokens;
        private long maxOutputTokens;
        private Prices prices;
        private boolean local;
        private List<Role> qualifiedRoles = List.of();

        private Builder() {
        }

        public Builder profile(String profile) {
            this.profile = profile;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder contextTokens(long contextTokens) {
            this.contextTokens = contextTokens;
            return this;
        }

        public Builder maxOutputTokens(long maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
            return this;
        }

        public Builder prices(Prices prices) {
            this.prices = prices;
            return this;
        }

        public Builder local(boolean local) {
            this.local = local;
            return this;
        }

        public Builder qualifiedRoles(List<Role> qualifiedRoles) {
            this.qualifiedRoles = qualifiedRoles;
            return this;
        }

        public ModelInfo build() {
            return new ModelInfo(Objects.requireNonNull(profile, "profile"),
                    Objects.requireNonNull(provider, "provider"), Objects.requireNonNull(modelId, "modelId"),
                    contextTokens, maxOutputTokens, Objects.requireNonNull(prices, "prices"), local, qualifiedRoles);
        }
    }
}
