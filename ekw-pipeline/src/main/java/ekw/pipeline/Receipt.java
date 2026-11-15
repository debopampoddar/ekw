package ekw.pipeline;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One URI in, one raw artifact plus this receipt out (the workshop's adapter contract). Stored as JSON in raw/
 * (snake_case field names). It records the read and never contains code.
 *
 * @param version         commit SHA for repositories (JSON "commit"); content hash for documents and tickets
 * @param previousVersion the version read by the previous run (JSON "previous_commit"); empty on the first run
 * @param checkout        repositories only: the git-ignored checkout under .cache/repos/ at {@code version}
 * @param versionTime     commit time for repositories; last-modified time for documents, when known
 * @param fileCounts      e.g. java_main, java_test, build, docs, other, skipped
 */
public record Receipt(String sourceId, String origin, String originalUri, String rawPath, Optional<String> checkout,
                       Optional<String> branch, String version, Optional<String> previousVersion,
                       Optional<Instant> versionTime, Instant fetchedAt, List<String> changedPaths,
                       Map<String, Integer> fileCounts, List<Pipeline.TriageEntry> triage,
                       List<Pipeline.Redaction> redactions, int injectionFindings) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String sourceId;
        private String origin;
        private String originalUri;
        private String rawPath;
        private Optional<String> checkout = Optional.empty();
        private Optional<String> branch = Optional.empty();
        private String version;
        private Optional<String> previousVersion = Optional.empty();
        private Optional<Instant> versionTime = Optional.empty();
        private Instant fetchedAt;
        private List<String> changedPaths = List.of();
        private Map<String, Integer> fileCounts = Map.of();
        private List<Pipeline.TriageEntry> triage = List.of();
        private List<Pipeline.Redaction> redactions = List.of();
        private int injectionFindings;

        private Builder() {
        }

        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder originalUri(String originalUri) {
            this.originalUri = originalUri;
            return this;
        }

        public Builder rawPath(String rawPath) {
            this.rawPath = rawPath;
            return this;
        }

        public Builder checkout(Optional<String> checkout) {
            this.checkout = checkout;
            return this;
        }

        public Builder checkout(String checkout) {
            this.checkout = Optional.ofNullable(checkout);
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

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder previousVersion(Optional<String> previousVersion) {
            this.previousVersion = previousVersion;
            return this;
        }

        public Builder previousVersion(String previousVersion) {
            this.previousVersion = Optional.ofNullable(previousVersion);
            return this;
        }

        public Builder versionTime(Optional<Instant> versionTime) {
            this.versionTime = versionTime;
            return this;
        }

        public Builder versionTime(Instant versionTime) {
            this.versionTime = Optional.ofNullable(versionTime);
            return this;
        }

        public Builder fetchedAt(Instant fetchedAt) {
            this.fetchedAt = fetchedAt;
            return this;
        }

        public Builder changedPaths(List<String> changedPaths) {
            this.changedPaths = changedPaths;
            return this;
        }

        public Builder fileCounts(Map<String, Integer> fileCounts) {
            this.fileCounts = fileCounts;
            return this;
        }

        public Builder triage(List<Pipeline.TriageEntry> triage) {
            this.triage = triage;
            return this;
        }

        public Builder redactions(List<Pipeline.Redaction> redactions) {
            this.redactions = redactions;
            return this;
        }

        public Builder injectionFindings(int injectionFindings) {
            this.injectionFindings = injectionFindings;
            return this;
        }

        public Receipt build() {
            return new Receipt(Objects.requireNonNull(sourceId, "sourceId"), Objects.requireNonNull(origin, "origin"),
                    Objects.requireNonNull(originalUri, "originalUri"), Objects.requireNonNull(rawPath, "rawPath"),
                    checkout, branch, Objects.requireNonNull(version, "version"), previousVersion, versionTime,
                    Objects.requireNonNull(fetchedAt, "fetchedAt"), changedPaths, fileCounts, triage, redactions,
                    injectionFindings);
        }
    }
}
