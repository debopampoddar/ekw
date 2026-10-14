package ekw.core;

import java.util.List;
import java.util.Optional;

/** Evidence: everything a model may cite. Locations are computed by code; models only ever see and cite ids. */
public final class Evidence {
    private Evidence() {}

    /** Session- or packet-scoped id shown to models: E1, E2, ... */
    public record Id(String value) {
        public Id {
            if (!value.matches("E[1-9][0-9]*")) throw new IllegalArgumentException("bad evidence id: " + value);
        }
    }

    public enum ModelPolicy { HOSTED_ALLOWED, LOCAL_ONLY }

    public sealed interface Location permits CodeLocation, DocLocation, TicketLocation, PageLocation, FactLocation {}

    /** Lines are 1-based and inclusive. symbol is the analyzer's stable id when the range is a declaration. */
    public record CodeLocation(String system, String commit, String path, int startLine, int endLine,
                               Optional<String> symbol) implements Location {
        public CodeLocation {
            if (startLine < 1 || endLine < startLine) throw new IllegalArgumentException("bad range");
        }
    }

    public record DocLocation(String source, String docId, String sectionAnchor, int startLine, int endLine)
            implements Location {}

    public record TicketLocation(String source, String ticketId, String url) implements Location {}

    public record PageLocation(String bundlePath, Optional<String> section) implements Location {}

    /** A fact row (call edge, config key, error code...) anchored at its code site. */
    public record FactLocation(String factKind, String key, CodeLocation site) implements Location {}

    /**
     * One citable item.
     * @param fingerprint first 16 hex chars of SHA-256 over the cited lines joined with LF plus a trailing LF
     */
    public record Item(Id id, Location location, String title, String content, String fingerprint,
                       ModelPolicy policy, String sourceId) {}

    public record Packet(List<Item> items) {
        public boolean requiresLocalModel() {
            return items.stream().anyMatch(i -> i.policy() == ModelPolicy.LOCAL_ONLY);
        }

        public Optional<Item> get(Id id) {
            return items.stream().filter(i -> i.id().equals(id)).findFirst();
        }
    }
}
