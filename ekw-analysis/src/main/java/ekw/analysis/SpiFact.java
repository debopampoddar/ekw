package ekw.analysis;

import ekw.analysis.Facts.SymbolId;
import ekw.core.Evidence.CodeLocation;

import java.util.Objects;

/**
 * SPI registration: interface -> name -> implementation (META-INF/services, META-INF/dubbo/**).
 */
public record SpiFact(SymbolId extensionInterface, String name, SymbolId implementation, CodeLocation file) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private SymbolId extensionInterface;
        private String name;
        private SymbolId implementation;
        private CodeLocation file;

        private Builder() {
        }

        public Builder extensionInterface(SymbolId extensionInterface) {
            this.extensionInterface = extensionInterface;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder implementation(SymbolId implementation) {
            this.implementation = implementation;
            return this;
        }

        public Builder file(CodeLocation file) {
            this.file = file;
            return this;
        }

        public SpiFact build() {
            return new SpiFact(Objects.requireNonNull(extensionInterface, "extensionInterface"),
                    Objects.requireNonNull(name, "name"), Objects.requireNonNull(implementation, "implementation"),
                    Objects.requireNonNull(file, "file"));
        }
    }
}
