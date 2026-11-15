package ekw.pipeline;

import ekw.core.Bundle;
import ekw.core.CodeLocation;
import ekw.core.Evidence;
import ekw.core.Item;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Ingest and lint. Plain code decides; models are called only by PageWriter, SupportChecker and model lint rules.
 */
public final class Pipeline {
    private Pipeline() {
    }

    // ---- sources ----------------------------------------------------------------------------------------------

    public enum TriageDecision {
        ANALYZE, ANALYZE_TESTS, BUILD_FACTS, SPI_FACTS, CONFIG_FACTS, DOCUMENT,
        INJECTION_SURFACE, METADATA, INDEX_ONLY, CITE_CANDIDATE, SKIP
    }

    public record TriageEntry(String pathOrId, TriageDecision decision, String why, boolean byModel) {
    }

    public record Redaction(String pathOrId, int line, String rule) {
    }

    public interface SourceAdapter {
        String kind();                                   // git-repository, markdown-folder, confluence-export, github-issues

        Receipt fetch(SourceConfig source, Optional<Receipt> previous);
    }

    // ---- planning ---------------------------------------------------------------------------------------------

    public enum WorkKind {WRITE_PAGE, REWRITE_SECTIONS, DEPRECATE_PAGE, RELOCATE_ONLY}

    public record WorkPlan(String runId, List<WorkItem> items, BigDecimal estimateUsd, List<String> cutByBudget) {
    }

    public interface Planner {
        WorkPlan plan(String system, String commit, Bundle bundle, BigDecimal limitUsd);
    }

    // ---- writing ----------------------------------------------------------------------------------------------

    /**
     * What the writer model returns (structured output). Sentences cite evidence ids; no locations, no Markdown.
     */
    public record Sentence(String text, List<Evidence.Id> evidence) {
    }

    public record Paragraph(List<Sentence> sentences) {
    }

    public record Section(String heading, List<Paragraph> paragraphs, Optional<String> diagramEvidenceId) {
    }

    public interface PageWriter {
        WriterOutput write(WorkItem item, Optional<Bundle.Page> current);
    }

    /**
     * Turns writer output into an OKF page: ids -> sources + footnotes, x-ekw keys, generated diagrams.
     */
    public interface Renderer {
        Bundle.Page render(WorkItem item, WriterOutput output, Optional<Bundle.Page> current);
    }

    // ---- checks -----------------------------------------------------------------------------------------------

    public enum Verdict {SUPPORTED, PARTIAL, UNSUPPORTED}

    public record Claim(String pagePath, String sentence, List<Item> cited) {
    }

    public record ClaimVerdict(Claim claim, Verdict verdict, String reason, String missing) {
    }

    public record CitationProblem(String pagePath, String sourceId, String problem) {
    }

    public interface CitationChecker {
        List<CitationProblem> check(Bundle.Page page);   // existence, ranges, fingerprints, footnote<->sources, identifiers
    }

    public interface SupportChecker {
        List<ClaimVerdict> check(List<Claim> claims);    // batched, cached by (sentence, fingerprints, prompt version)
    }

    /**
     * Deterministic checks before an ingest-owned page is written (ADR-020): frontmatter valid, generated and
     * read-only sections unchanged, no citation with unchanged evidence dropped, no model-written page cited
     * (ADR-019). An empty list means the write may proceed.
     */
    public interface WriteGuard {
        List<String> violations(Optional<Bundle.Page> current, Bundle.Page next, Set<String> readOnlySections,
                                Set<String> unchangedSourceIds);
    }

    /**
     * Moves citations whose code moved but did not change; reports the ones whose evidence changed.
     */
    public interface EvidenceRelocator {
        record Result(Map<String, CodeLocation> moved, Set<String> changed) {
        }

        Result relocate(String system, String oldCommit, String newCommit, List<Item> citations);
    }

    // ---- lint and runs ----------------------------------------------------------------------------------------

    public enum Severity {INFO, WARNING, ERROR, BLOCKER}

    public interface Linter {
        List<Finding> lint(Bundle bundle, Set<String> pagePaths, boolean includeModelRules);
    }

    public enum AutoMerge {OFF, WHEN_CLEAN}

    /**
     * Limits for one run: the smallest of the source budget, the per-run ceiling ($15), and, for runs started by a
     * scheduled job, what is left of the nightly total.
     */
    public record RunLimits(BigDecimal sourceBudgetUsd, BigDecimal runCeilingUsd,
                            Optional<BigDecimal> nightlyRemainingUsd) {
        public BigDecimal effectiveUsd () {
            BigDecimal limit = sourceBudgetUsd.min(runCeilingUsd);
            return nightlyRemainingUsd.map(limit::min).orElse(limit);
        }
    }

    /**
     * How a run was started; unattended runs never prompt and carry cut items over as pending.
     */
    public enum Trigger {MANUAL, ASSISTANT, SCHEDULE, WEBHOOK}

    public interface IngestService {
        WorkPlan plan(String sourceId);

        RunSummary run(String sourceId, Trigger trigger, boolean assumeYes, boolean useBatch, RunLimits limits);

        RunSummary resume(String runId);

        /**
         * Runs every due source in dependency order under one nightly total; pending items go first.
         */
        List<RunSummary> runAllDue(Trigger trigger, BigDecimal nightlyUsd, AutoMerge autoMerge);
    }
}
