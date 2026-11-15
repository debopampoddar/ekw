package ekw.eval;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Evaluation harness: question sets, setups (ours + baselines), judge, human calibration.
 */
public final class Eval {
    private Eval() {
    }

    /**
     * A required piece of evidence: "path#Lstart-Lend" at the suite's commit, a doc section, or a ticket URL.
     */
    public record RequiredEvidence(String kind, String location, String why) {
    }

    public enum Setup {
        WIKI_TOOLS,          // ours: wiki + agent tools
        RAG,                 // B0: BM25 chunks of code and docs, same answer model
        WHOLE_REPO,          // B1: whole repository in the prompt (small repository only)
        RAW_AGENT,           // B2: agent with grep/glob/read over the raw checkout, no wiki
        FACTS_ONLY,          // B3: wiki without LLM-written pages (fact pages + tools)
        NO_CONTEXT           // memorization probe: the model alone
    }

    public record RunResult(String questionId, Setup setup, int repeat, ekw.query.Answer answer) {
    }

    public record HumanLabel(String itemId, String kind, String label, Optional<String> note) {
    }

    public record Agreement(String kind, int n, double percentAgreement, double cohensKappa) {
    }

    public interface Harness {
        List<RunResult> run(String suite, List<Setup> setups, int repeats, BigDecimal budgetUsd);

        List<Grade> grade(List<RunResult> results);

        List<Agreement> calibrate(List<Grade> grades, List<HumanLabel> labels);

        Report report(String suite);
    }
}
