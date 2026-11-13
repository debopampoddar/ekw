package ekw.analysis;

import ekw.analysis.Facts.SymbolId;
import ekw.core.Evidence.CodeLocation;

import java.util.Map;
import java.util.Objects;

public record AnnotationFact(SymbolId target, String annotationType, Map<String, String> values,
                              CodeLocation site) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private SymbolId target;
        private String annotationType;
        private Map<String, String> values = Map.of();
        private CodeLocation site;

        private Builder() {
        }

        public Builder target(SymbolId target) {
            this.target = target;
            return this;
        }

        public Builder annotationType(String annotationType) {
            this.annotationType = annotationType;
            return this;
        }

        public Builder values(Map<String, String> values) {
            this.values = values;
            return this;
        }

        public Builder site(CodeLocation site) {
            this.site = site;
            return this;
        }

        public AnnotationFact build() {
            return new AnnotationFact(Objects.requireNonNull(target, "target"),
                    Objects.requireNonNull(annotationType, "annotationType"), values,
                    Objects.requireNonNull(site, "site"));
        }
    }
}
