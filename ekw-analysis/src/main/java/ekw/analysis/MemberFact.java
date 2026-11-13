package ekw.analysis;

import ekw.analysis.Facts.MemberKind;
import ekw.analysis.Facts.SymbolId;
import ekw.core.Evidence.CodeLocation;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * bodyHash lets relocated code be recognized (see evidence relocation).
 */
public record MemberFact(SymbolId id, SymbolId owner, MemberKind kind, String signature, Set<String> modifiers,
                          CodeLocation span, String bodyHash, Optional<String> javadocFirstSentence) {

    public MemberFact {
        modifiers =
                Collections.unmodifiableSortedSet(
                        new TreeSet<>(null == modifiers ? Set.of() : modifiers));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private SymbolId id;
        private SymbolId owner;
        private MemberKind kind;
        private String signature;
        private Set<String> modifiers = Set.of();
        private CodeLocation span;
        private String bodyHash;
        private Optional<String> javadocFirstSentence = Optional.empty();

        private Builder() {
        }

        public Builder id(SymbolId id) {
            this.id = id;
            return this;
        }

        public Builder owner(SymbolId owner) {
            this.owner = owner;
            return this;
        }

        public Builder kind(MemberKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = signature;
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

        public Builder bodyHash(String bodyHash) {
            this.bodyHash = bodyHash;
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

        public MemberFact build() {
            return new MemberFact(Objects.requireNonNull(id, "id"), Objects.requireNonNull(owner, "owner"),
                    Objects.requireNonNull(kind, "kind"), signature, modifiers, Objects.requireNonNull(span, "span"),
                    bodyHash, javadocFirstSentence);
        }
    }
}
