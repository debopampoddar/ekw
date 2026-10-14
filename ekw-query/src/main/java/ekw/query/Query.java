package ekw.query;

import ekw.core.Bundle;
import ekw.core.Evidence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Question answering. The same KnowledgeTools back the MCP server (tools mode) and the QuestionEngine (answer mode). */
public final class Query {
    private Query() {}

    public enum QuestionType {
        T1_PURPOSE, T2_ARCHITECTURE, T3_RATIONALE, T4_FLOW, T5_HOW_TO, T6_EXTENSION, T7_BUILD_CONFIG,
        T8_RUNBOOK, T9_DRIFT, T10_EXISTENCE, T11_LOCATE, T12_IMPACT, T13_HISTORY, T14_CROSS_SYSTEM
    }

    public record Route(QuestionType type, List<String> entities, List<String> systems, List<String> startPages,
                        int maxToolCalls, long inputTokenBudget, boolean supportCheck) {}

    public record IndexEntry(String path, String title, String type, String description, Bundle.Status status,
                             Bundle.TrustTier tier) {}

    public record PageView(String path, String type, Bundle.Status status, Bundle.TrustTier tier, boolean stale,
                           String text, List<Evidence.Item> citedEvidence) {}

    public record SearchHit(Evidence.Item item, double score, String snippet) {}

    public record FactsQuery(String op, String name, Optional<String> system, int limit) {}

    public record DiagramRequest(String kind, String focus, int depth) {}

    public record DraftAnswer(String question, String title, String answerMarkdown, List<Evidence.Id> cited,
                              Optional<String> promoteTo) {}

    public record OpenQuestion(String question, String whyOpen, List<String> checked) {}

    /** Every read result carries evidence items with ids valid for the session. */
    public interface KnowledgeTools {
        List<IndexEntry> readIndex(String folder);
        PageView readPage(String path, List<String> sections);
        List<SearchHit> search(String query, String scope, Optional<String> system, int limit);
        List<Evidence.Item> codeFacts(FactsQuery query);
        Evidence.Item readCodeLines(String system, String path, int start, int end);
        Evidence.Item readDocSection(String doc, String section);
        Evidence.Item diagram(DiagramRequest request);
        String saveAnswerDraft(DraftAnswer draft);       // returns the draft's bundle path
        void logOpenQuestion(OpenQuestion question);
    }

    public enum Confidence { HIGH, MEDIUM, LOW }

    public enum Outcome { ANSWERED, NOT_FOUND, DOCUMENTED_UNSUPPORTED, INCONCLUSIVE, INCOMPLETE_BUDGET }

    public record ClaimView(String sentence, List<Evidence.Id> cited, boolean weak, Optional<String> weakReason) {}

    public record Answer(Outcome outcome, String markdown, List<ClaimView> claims, List<String> synthesis,
                         List<String> checked, List<Evidence.Item> sources, Confidence confidence,
                         BigDecimal costUsd, long inputTokens, long outputTokens, Optional<String> savedDraft) {}

    public record AskOptions(Optional<String> sessionId, boolean save, Optional<String> modelProfile,
                             Optional<BigDecimal> budgetUsd) {}

    public interface QuestionEngine {
        Answer ask(String question, AskOptions options);
    }

    public interface Router {
        Route route(String question);
    }
}
