package ekw.analysis;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ModuleSpec(String system, String commit, String name, Path root, List<Path> sourceRoots,
                          List<Path> testRoots, String javaRelease) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String system;
        private String commit;
        private String name;
        private Path root;
        private List<Path> sourceRoots = List.of();
        private List<Path> testRoots = List.of();
        private String javaRelease;

        private Builder() {
        }

        public Builder system(String system) {
            this.system = system;
            return this;
        }

        public Builder commit(String commit) {
            this.commit = commit;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder root(Path root) {
            this.root = root;
            return this;
        }

        public Builder sourceRoots(List<Path> sourceRoots) {
            this.sourceRoots = sourceRoots;
            return this;
        }

        public Builder testRoots(List<Path> testRoots) {
            this.testRoots = testRoots;
            return this;
        }

        public Builder javaRelease(String javaRelease) {
            this.javaRelease = javaRelease;
            return this;
        }

        public ModuleSpec build() {
            return new ModuleSpec(Objects.requireNonNull(system, "system"), Objects.requireNonNull(commit, "commit"),
                    Objects.requireNonNull(name, "name"), Objects.requireNonNull(root, "root"), sourceRoots,
                    testRoots, Objects.requireNonNull(javaRelease, "javaRelease"));
        }
    }
}
