package ekw.core;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record Frontmatter(Bundle.PageType type, String title, String description, Optional<String> resource,
                           List<Bundle.SourceEntry> sources, Optional<Bundle.Stamp> generated,
                           List<Bundle.Stamp> verified, Bundle.Status status, Optional<Instant> staleAfter,
                           Map<String, Object> ekw, Map<String, Object> unknownKeys) {
    public Bundle.TrustTier tier() {
        if (verified.isEmpty()) return Bundle.TrustTier.UNVERIFIED;
        return verified.stream().anyMatch(s -> s.by().isHuman())
                ? Bundle.TrustTier.HUMAN_REVIEWED : Bundle.TrustTier.MACHINE_CONFIRMED;
    }

    public boolean isStale(Instant now) {
        return staleAfter.map(t -> !now.isBefore(t)).orElse(false)
                || "stale".equals(ekw.get("evidence_state"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Bundle.PageType type;
        private String title;
        private String description;
        private Optional<String> resource = Optional.empty();
        private List<Bundle.SourceEntry> sources = List.of();
        private Optional<Bundle.Stamp> generated = Optional.empty();
        private List<Bundle.Stamp> verified = List.of();
        private Bundle.Status status;
        private Optional<Instant> staleAfter = Optional.empty();
        private Map<String, Object> ekw = Map.of();
        private Map<String, Object> unknownKeys = Map.of();

        private Builder() {
        }

        public Builder type(Bundle.PageType type) {
            this.type = type;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder resource(Optional<String> resource) {
            this.resource = resource;
            return this;
        }

        public Builder resource(String resource) {
            this.resource = Optional.ofNullable(resource);
            return this;
        }

        public Builder sources(List<Bundle.SourceEntry> sources) {
            this.sources = sources;
            return this;
        }

        public Builder generated(Optional<Bundle.Stamp> generated) {
            this.generated = generated;
            return this;
        }

        public Builder generated(Bundle.Stamp generated) {
            this.generated = Optional.ofNullable(generated);
            return this;
        }

        public Builder verified(List<Bundle.Stamp> verified) {
            this.verified = verified;
            return this;
        }

        public Builder status(Bundle.Status status) {
            this.status = status;
            return this;
        }

        public Builder staleAfter(Optional<Instant> staleAfter) {
            this.staleAfter = staleAfter;
            return this;
        }

        public Builder staleAfter(Instant staleAfter) {
            this.staleAfter = Optional.ofNullable(staleAfter);
            return this;
        }

        public Builder ekw(Map<String, Object> ekw) {
            this.ekw = ekw;
            return this;
        }

        public Builder unknownKeys(Map<String, Object> unknownKeys) {
            this.unknownKeys = unknownKeys;
            return this;
        }

        public Frontmatter build() {
            return new Frontmatter(Objects.requireNonNull(type, "type"), Objects.requireNonNull(title, "title"),
                    Objects.requireNonNull(description, "description"), resource, sources, generated, verified,
                    Objects.requireNonNull(status, "status"), staleAfter, ekw, unknownKeys);
        }
    }
}
