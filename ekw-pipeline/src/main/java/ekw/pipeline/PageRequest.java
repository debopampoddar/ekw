package ekw.pipeline;

import ekw.core.Evidence;

import java.util.List;
import java.util.Objects;

/**
 * What the page is and what it may use. The strings fill the placeholders of writer.page@1.
 */
public record PageRequest(String pageType, String titleHint, String system, String commitShort,
                           String templateSections, String typeSpecificInstructions, String linkablePages,
                           Evidence.Packet packet) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String pageType;
        private String titleHint;
        private String system;
        private String commitShort;
        private String templateSections;
        private String typeSpecificInstructions;
        private String linkablePages;
        private Evidence.Packet packet = new Evidence.Packet(List.of());

        private Builder() {
        }

        public Builder pageType(String pageType) {
            this.pageType = pageType;
            return this;
        }

        public Builder titleHint(String titleHint) {
            this.titleHint = titleHint;
            return this;
        }

        public Builder system(String system) {
            this.system = system;
            return this;
        }

        public Builder commitShort(String commitShort) {
            this.commitShort = commitShort;
            return this;
        }

        public Builder templateSections(String templateSections) {
            this.templateSections = templateSections;
            return this;
        }

        public Builder typeSpecificInstructions(String typeSpecificInstructions) {
            this.typeSpecificInstructions = typeSpecificInstructions;
            return this;
        }

        public Builder linkablePages(String linkablePages) {
            this.linkablePages = linkablePages;
            return this;
        }

        public Builder packet(Evidence.Packet packet) {
            this.packet = packet;
            return this;
        }

        public PageRequest build() {
            return new PageRequest(Objects.requireNonNull(pageType, "pageType"),
                    Objects.requireNonNull(titleHint, "titleHint"), Objects.requireNonNull(system, "system"),
                    Objects.requireNonNull(commitShort, "commitShort"),
                    Objects.requireNonNull(templateSections, "templateSections"),
                    Objects.requireNonNull(typeSpecificInstructions, "typeSpecificInstructions"),
                    Objects.requireNonNull(linkablePages, "linkablePages"), packet);
        }
    }
}
