package ekw.analysis;

import ekw.analysis.Facts.SymbolId;
import ekw.analysis.Facts.TypeKind;
import ekw.core.Evidence.CodeLocation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public record TypeFact(SymbolId id, String module, TypeKind kind, Set<String> modifiers, CodeLocation span,
                        Optional<String> javadocFirstSentence, List<SymbolId> supertypes, boolean test) {

    public static Builder builder() {
        return new Builder();
    }

    public TypeFact {
        modifiers =
                Collections.unmodifiableSortedSet(
                        new TreeSet<>(null == modifiers ? Set.of() : modifiers));
    }

    public static final class Builder {
        private SymbolId id;
        private String module;
        private TypeKind kind;
        private Set<String> modifiers = Set.of();
        private CodeLocation span;
        private Optional<String> javadocFirstSentence = Optional.empty();
        private List<SymbolId> supertypes = List.of();
        private boolean test;

        private Builder() {
        }

        public Builder id(SymbolId id) {
            this.id = id;
            return this;
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder kind(TypeKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder modifiers(Set<String> modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public Builder span(CodeLocation span) {
            this.span = span;
            return this;
        }

        public Builder javadocFirstSentence(Optional<String> javadocFirstSentence) {
            this.javadocFirstSentence = javadocFirstSentence;
            return this;
        }

        public Builder javadocFirstSentence(String javadocFirstSentence) {
            this.javadocFirstSentence = Optional.ofNullable(javadocFirstSentence);
            return this;
        }

        public Builder supertypes(List<SymbolId> supertypes) {
            this.supertypes = supertypes;
            return this;
        }

        public Builder test(boolean test) {
            this.test = test;
            return this;
        }

        public TypeFact build() {
            return new TypeFact(Objects.requireNonNull(id, "id"), module, Objects.requireNonNull(kind, "kind"),
                    modifiers, Objects.requireNonNull(span, "span"), javadocFirstSentence, supertypes, test);
        }
    }
}
