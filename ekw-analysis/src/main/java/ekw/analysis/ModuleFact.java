package ekw.analysis;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ModuleFact(String name, String groupId, String artifactId, Path root, String javaRelease,
                          List<String> dependsOnModules) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String groupId;
        private String artifactId;
        private Path root;
        private String javaRelease;
        private List<String> dependsOnModules = List.of();

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
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

        public Builder root(Path root) {
            this.root = root;
            return this;
        }

        public Builder javaRelease(String javaRelease) {
            this.javaRelease = javaRelease;
            return this;
        }

        public Builder dependsOnModules(List<String> dependsOnModules) {
            this.dependsOnModules = dependsOnModules;
            return this;
        }

        public ModuleFact build() {
            return new ModuleFact(Objects.requireNonNull(name, "name"), Objects.requireNonNull(groupId, "groupId"),
                    Objects.requireNonNull(artifactId, "artifactId"), Objects.requireNonNull(root, "root"),
                    Objects.requireNonNull(javaRelease, "javaRelease"), dependsOnModules);
        }
    }
}
