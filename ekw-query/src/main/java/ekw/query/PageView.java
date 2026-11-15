package ekw.query;

import ekw.core.Bundle;
import ekw.core.Item;

import java.util.List;
import java.util.Objects;

public record PageView(String path, String type, Bundle.Status status, Bundle.TrustTier tier, boolean stale,
                        String text, List<Item> citedEvidence) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String path;
        private String type;
        private Bundle.Status status;
        private Bundle.TrustTier tier;
        private boolean stale;
        private String text;
        private List<Item> citedEvidence = List.of();

        private Builder() {
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder status(Bundle.Status status) {
            this.status = status;
            return this;
        }

        public Builder tier(Bundle.TrustTier tier) {
            this.tier = tier;
            return this;
        }

        public Builder stale(boolean stale) {
            this.stale = stale;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder citedEvidence(List<Item> citedEvidence) {
            this.citedEvidence = citedEvidence;
            return this;
        }

        public PageView build() {
            return new PageView(Objects.requireNonNull(path, "path"), Objects.requireNonNull(type, "type"),
                    Objects.requireNonNull(status, "status"), Objects.requireNonNull(tier, "tier"), stale,
                    Objects.requireNonNull(text, "text"), citedEvidence);
        }
    }
}
