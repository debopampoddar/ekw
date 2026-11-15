package ekw.pipeline;

import ekw.core.Evidence;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record SourceConfig(String id, String kind, String url, Optional<String> branch, String owner,
                            Evidence.ModelPolicy policy, BigDecimal budgetUsdPerRun, Map<String, Object> options) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String kind;
        private String url;
        private Optional<String> branch = Optional.empty();
        private String owner;
        private Evidence.ModelPolicy policy;
        private BigDecimal budgetUsdPerRun;
        private Map<String, Object> options = Map.of();

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder kind(String kind) {
            this.kind = kind;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder branch(Optional<String> branch) {
            this.branch = branch;
            return this;
        }

        public Builder branch(String branch) {
            this.branch = Optional.ofNullable(branch);
            return this;
        }

        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder policy(Evidence.ModelPolicy policy) {
            this.policy = policy;
            return this;
        }

        public Builder budgetUsdPerRun(BigDecimal budgetUsdPerRun) {
            this.budgetUsdPerRun = budgetUsdPerRun;
            return this;
        }

        public Builder options(Map<String, Object> options) {
            this.options = options;
            return this;
        }

        public SourceConfig build() {
            return new SourceConfig(Objects.requireNonNull(id, "id"), Objects.requireNonNull(kind, "kind"),
                    Objects.requireNonNull(url, "url"), branch, Objects.requireNonNull(owner, "owner"),
                    Objects.requireNonNull(policy, "policy"), Objects.requireNonNull(budgetUsdPerRun, "budgetUsdPerRun"),
                    options);
        }
    }
}
