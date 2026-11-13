package ekw.analysis;

import ekw.core.Evidence.CodeLocation;

import java.util.Objects;

public record DependencyFact(String module, String groupId, String artifactId, String version, String scope,
                              boolean optional, boolean resolved, CodeLocation declaredAt) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String module;
        private String groupId;
        private String artifactId;
        private String version;
        private String scope = "compile";
        private boolean optional;
        private boolean resolved;
        private CodeLocation declaredAt;

        private Builder() {
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public Builder resolved(boolean resolved) {
            this.resolved = resolved;
            return this;
        }

        public Builder declaredAt(CodeLocation declaredAt) {
            this.declaredAt = declaredAt;
            return this;
        }

        public DependencyFact build() {
            return new DependencyFact(Objects.requireNonNull(module, "module"),
                    Objects.requireNonNull(groupId, "groupId"), Objects.requireNonNull(artifactId, "artifactId"),
                    Objects.requireNonNull(version, "version"), scope, optional, resolved,
                    Objects.requireNonNull(declaredAt, "declaredAt"));
        }
    }
}
