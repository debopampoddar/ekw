package ekw.query;

import ekw.core.Bundle;

import java.util.Objects;

public record IndexEntry(String path, String title, String type, String description, Bundle.Status status,
                          Bundle.TrustTier tier) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String path;
        private String title;
        private String type;
        private String description;
        private Bundle.Status status;
        private Bundle.TrustTier tier;

        private Builder() {
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
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

        public IndexEntry build() {
            return new IndexEntry(Objects.requireNonNull(path, "path"), Objects.requireNonNull(title, "title"),
                    Objects.requireNonNull(type, "type"), Objects.requireNonNull(description, "description"),
                    Objects.requireNonNull(status, "status"), Objects.requireNonNull(tier, "tier"));
        }
    }
}
