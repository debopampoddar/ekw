package ekw.core;

import ekw.core.Evidence.Id;
import ekw.core.Evidence.Location;
import ekw.core.Evidence.ModelPolicy;

import java.util.Objects;

/**
 * One citable item.
 * @param fingerprint first 16 hex chars of SHA-256 over the cited lines joined with LF plus a trailing LF
 */
public record Item(Id id, Location location, String title, String content, String fingerprint,
                    ModelPolicy policy, String sourceId) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Id id;
        private Location location;
        private String title;
        private String content;
        private String fingerprint;
        private ModelPolicy policy;
        private String sourceId;

        private Builder() {
        }

        public Builder id(Id id) {
            this.id = id;
            return this;
        }

        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder fingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        public Builder policy(ModelPolicy policy) {
            this.policy = policy;
            return this;
        }

        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Item build() {
            return new Item(Objects.requireNonNull(id, "id"), Objects.requireNonNull(location, "location"),
                    Objects.requireNonNull(title, "title"), Objects.requireNonNull(content, "content"),
                    Objects.requireNonNull(fingerprint, "fingerprint"), Objects.requireNonNull(policy, "policy"),
                    Objects.requireNonNull(sourceId, "sourceId"));
        }
    }
}
