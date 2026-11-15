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

    public record DocLocation(String source, String docId, String sectionAnchor, int startLine, int endLine)
            implements Location {}

    public record TicketLocation(String source, String ticketId, String url) implements Location {}

    public record PageLocation(String bundlePath, Optional<String> section) implements Location {}

    /** A fact row (call edge, config key, error code...) anchored at its code site. */
    public record FactLocation(String factKind, String key, CodeLocation site) implements Location {}

    public record Packet(List<Item> items) {
        public boolean requiresLocalModel() {
            return items.stream().anyMatch(i -> i.policy() == ModelPolicy.LOCAL_ONLY);
        }

        public Optional<Item> get(Id id) {
            return items.stream().filter(i -> i.id().equals(id)).findFirst();
        }
    }
}
