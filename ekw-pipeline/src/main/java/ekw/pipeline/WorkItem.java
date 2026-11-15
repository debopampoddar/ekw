package ekw.pipeline;

import ekw.core.Bundle;
import ekw.core.Evidence;
import ekw.llm.Models;

import java.util.Objects;
import java.util.Set;

public record WorkItem(String id, Pipeline.WorkKind kind, Bundle.PageType type, String targetPath,
                        Set<String> sectionsToWrite, Set<String> readOnlySections, Evidence.Packet packet,
                        double rank, Models.PromptRef prompt, String inputHash) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private Pipeline.WorkKind kind;
        private Bundle.PageType type;
        private String targetPath;
        private Set<String> sectionsToWrite = Set.of();
        private Set<String> readOnlySections = Set.of();
        private Evidence.Packet packet = new Evidence.Packet(java.util.List.of());
        private double rank;
        private Models.PromptRef prompt;
        private String inputHash;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder kind(Pipeline.WorkKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder type(Bundle.PageType type) {
            this.type = type;
            return this;
        }

        public Builder targetPath(String targetPath) {
            this.targetPath = targetPath;
            return this;
        }

        public Builder sectionsToWrite(Set<String> sectionsToWrite) {
            this.sectionsToWrite = sectionsToWrite;
            return this;
        }

        public Builder readOnlySections(Set<String> readOnlySections) {
            this.readOnlySections = readOnlySections;
            return this;
        }

        public Builder packet(Evidence.Packet packet) {
            this.packet = packet;
            return this;
        }

        public Builder rank(double rank) {
            this.rank = rank;
            return this;
        }

        public Builder prompt(Models.PromptRef prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder inputHash(String inputHash) {
            this.inputHash = inputHash;
            return this;
        }

        public WorkItem build() {
            return new WorkItem(Objects.requireNonNull(id, "id"), Objects.requireNonNull(kind, "kind"),
                    Objects.requireNonNull(type, "type"), Objects.requireNonNull(targetPath, "targetPath"),
                    sectionsToWrite, readOnlySections, packet, rank, Objects.requireNonNull(prompt, "prompt"),
                    Objects.requireNonNull(inputHash, "inputHash"));
        }
    }
}
