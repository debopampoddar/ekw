package ekw.pipeline;

import java.util.List;
import java.util.Objects;

public record WriterOutput(String title, String description, List<Pipeline.Section> sections,
                            List<String> synthesis, List<String> conceptSlugs, List<String> readCoverage,
                            List<String> notRead, List<String> evidenceRequests) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title;
        private String description;
        private List<Pipeline.Section> sections = List.of();
        private List<String> synthesis = List.of();
        private List<String> conceptSlugs = List.of();
        private List<String> readCoverage = List.of();
        private List<String> notRead = List.of();
        private List<String> evidenceRequests = List.of();

        private Builder() {
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder sections(List<Pipeline.Section> sections) {
            this.sections = sections;
            return this;
        }

        public Builder synthesis(List<String> synthesis) {
            this.synthesis = synthesis;
            return this;
        }

        public Builder conceptSlugs(List<String> conceptSlugs) {
            this.conceptSlugs = conceptSlugs;
            return this;
        }

        public Builder readCoverage(List<String> readCoverage) {
            this.readCoverage = readCoverage;
            return this;
        }

        public Builder notRead(List<String> notRead) {
            this.notRead = notRead;
            return this;
        }

        public Builder evidenceRequests(List<String> evidenceRequests) {
            this.evidenceRequests = evidenceRequests;
            return this;
        }

        public WriterOutput build() {
            return new WriterOutput(Objects.requireNonNull(title, "title"),
                    Objects.requireNonNull(description, "description"), sections, synthesis, conceptSlugs,
                    readCoverage, notRead, evidenceRequests);
        }
    }
}
