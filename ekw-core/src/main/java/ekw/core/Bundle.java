package ekw.core;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** The OKF bundle: the only way any component reads or writes wiki pages. */
public interface Bundle {

    enum Regime { FACTS, INGEST, QUERY, PEOPLE }

    enum Status { DRAFT, STABLE, DEPRECATED }

    enum TrustTier { UNVERIFIED, MACHINE_CONFIRMED, HUMAN_REVIEWED }

    enum PageType {
        JAVA_TYPE("Java Type", Regime.FACTS, false),
        MODULE("Module", Regime.FACTS, false),
        BUILD("Build", Regime.FACTS, false),
        EXTENSION_POINT("Extension Point", Regime.FACTS, false),
        CONFIGURATION("Configuration", Regime.FACTS, false),
        ERROR_CODES("Error Codes", Regime.FACTS, false),
        EXECUTION_FLOW("Execution Flow", Regime.FACTS, false),
        SYSTEM_OVERVIEW("System Overview", Regime.INGEST, true),
        COMPONENT("Component", Regime.INGEST, true),
        DOCUMENT("Document", Regime.INGEST, true),
        DECISION("Decision", Regime.INGEST, true),
        RUNBOOK("Runbook", Regime.INGEST, true),
        CONCEPT("Concept", Regime.INGEST, false),
        HOW_TO("How-To", Regime.INGEST, false),
        KB_OVERVIEW("Knowledge Base Overview", Regime.INGEST, false),
        ANSWER("Answer", Regime.QUERY, false),
        OPEN_QUESTIONS("Open Questions", Regime.QUERY, false);

        private final String okfName;
        private final Regime regime;
        private final boolean sourceLike;

        PageType(String okfName, Regime regime, boolean sourceLike) {
            this.okfName = okfName;
            this.regime = regime;
            this.sourceLike = sourceLike;
        }

        public String okfName() { return okfName; }
        public Regime regime() { return regime; }
        public boolean sourceLike() { return sourceLike; }
    }

    /** Actor convention from OKF section 7: producer/version, human:id, process:id. */
    record Actor(String value) {
        public static Actor human(String id) { return new Actor("human:" + id); }
        public static Actor process(String id) { return new Actor("process:" + id); }
        public static Actor tool(String producer, String version) { return new Actor(producer + "/" + version); }
        public boolean isHuman() { return value.startsWith("human:"); }
    }

    record Stamp(Actor by, Instant at) {}

    /** One OKF sources[] entry. ekw holds our x-ekw keys (kind, path, lines, sha256, symbol...). */
    record SourceEntry(String id, String resource, String title, Map<String, Object> ekw) {}

    record Frontmatter(PageType type, String title, String description, Optional<String> resource,
                       List<SourceEntry> sources, Optional<Stamp> generated, List<Stamp> verified,
                       Status status, Optional<Instant> staleAfter,
                       Map<String, Object> ekw, Map<String, Object> unknownKeys) {
        public TrustTier tier() {
            if (verified.isEmpty()) return TrustTier.UNVERIFIED;
            return verified.stream().anyMatch(s -> s.by().isHuman())
                    ? TrustTier.HUMAN_REVIEWED : TrustTier.MACHINE_CONFIRMED;
        }

        public boolean isStale(Instant now) {
            return staleAfter.map(t -> !now.isBefore(t)).orElse(false)
                    || "stale".equals(ekw.get("evidence_state"));
        }
    }

    /** path is bundle-relative, e.g. "systems/dubbo/components/load-balancing.md". */
    record Page(String path, Frontmatter frontmatter, String body) {}

    Optional<Page> read(String path);

    Stream<Page> pages();

    /** Writes a page if the writer's regime owns its path; throws otherwise. Never hand-edits generated files. */
    void write(Page page, Regime writer);

    /** Regenerates every index.md and the catalog card from frontmatter. Deterministic. */
    void regenerateIndexes();

    void appendLog(String kind, String markdownLine, Instant at);
}
