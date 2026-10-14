package ekw.pipeline;

import ekw.core.Bundle;
import ekw.core.Evidence;
import ekw.llm.Models;

import java.math.BigDecimal;
import java.time.Instant;
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

    public record SourceConfig(String id, String kind, String url, Optional<String> branch, String owner,
                               Evidence.ModelPolicy policy, BigDecimal budgetUsdPerRun, Map<String, Object> options) {
    }

    public enum TriageDecision {
        ANALYZE, ANALYZE_TESTS, BUILD_FACTS, SPI_FACTS, CONFIG_FACTS, DOCUMENT,
        INJECTION_SURFACE, METADATA, INDEX_ONLY, CITE_CANDIDATE, SKIP
    }

    public record TriageEntry(String pathOrId, TriageDecision decision, String why, boolean byModel) {
    }

    public record Redaction(String pathOrId, int line, String rule) {
    }

    /**
     * One URI in, one raw artifact plus this receipt out (the workshop's adapter contract). Stored as JSON in raw/
     * (snake_case field names). It records the read and never contains code.
     *
     * @param version         commit SHA for repositories (JSON "commit"); content hash for documents and tickets
     * @param previousVersion the version read by the previous run (JSON "previous_commit"); empty on the first run
     * @param checkout        repositories only: the git-ignored checkout under .cache/repos/ at {@code version}
     * @param versionTime     commit time for repositories; last-modified time for documents, when known
     * @param fileCounts      e.g. java_main, java_test, build, docs, other, skipped
     */
    public record Receipt(String sourceId, String origin, String originalUri, String rawPath,
                          Optional<String> checkout, Optional<String> branch, String version,
                          Optional<String> previousVersion, Optional<Instant> versionTime, Instant fetchedAt,
                          List<String> changedPaths, Map<String, Integer> fileCounts, List<TriageEntry> triage,
                          List<Redaction> redactions, int injectionFindings) {
    }

    public interface SourceAdapter {
        String kind();                                   // git-repository, markdown-folder, confluence-export, github-issues

        Receipt fetch(SourceConfig source, Optional<Receipt> previous);
    }

    // ---- planning ---------------------------------------------------------------------------------------------

    public enum WorkKind {WRITE_PAGE, REWRITE_SECTIONS, DEPRECATE_PAGE, RELOCATE_ONLY}

    public record WorkItem(String id, WorkKind kind, Bundle.PageType type, String targetPath,
                           Set<String> sectionsToWrite, Set<String> readOnlySections, Evidence.Packet packet,
                           double rank, Models.PromptRef prompt, String inputHash) {
    }

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

    public record WriterOutput(String title, String description, List<Section> sections, List<String> synthesis,
                               List<String> conceptSlugs, List<String> readCoverage, List<String> notRead,
                               List<String> evidenceRequests) {
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

    public record Claim(String pagePath, String sentence, List<Evidence.Item> cited) {
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
        record Result(Map<String, Evidence.CodeLocation> moved, Set<String> changed) {
        }

        Result relocate(String system, String oldCommit, String newCommit, List<Evidence.Item> citations);
    }

    // ---- lint and runs ----------------------------------------------------------------------------------------

    public enum Severity {INFO, WARNING, ERROR, BLOCKER}

    public record Finding(String ruleId, Severity severity, String pagePath, String message, boolean needsPerson) {
    }

    public interface Linter {
        List<Finding> lint(Bundle bundle, Set<String> pagePaths, boolean includeModelRules);
    }

    public record RunSummary(String runId, String sourceId, List<String> pagesAdded, List<String> pagesChanged,
                             int pagesKept, BigDecimal estimateUsd, BigDecimal spentUsd, List<Finding> findings,
                             List<String> reviewSample, boolean partial, List<String> pendingItems,
                             boolean firstBuild, boolean autoMerged) {
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
