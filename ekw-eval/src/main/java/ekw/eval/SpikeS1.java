package ekw.eval;

import ekw.core.Todo;

import java.math.BigDecimal;

/**
 * Spike S1 (design: stage 3 milestone 1; stage 2, section 12.1): do model-written pages help an answering model more
 * than an agent reading raw code, per dollar? Wires together everything from chapters 2-11.
 * Writes everything to {@code <work>/} (default .runs/spike-s1/).
 *
 * <p>Chapter 12 of the development guide describes each step and how to check it.
 */
public final class SpikeS1 {

    public SpikeS1(Config config) {
        // TODO (chapter 12): keep the configuration; build the price table, guard and models.
    }

    /**
     * Step 1-2 (free): clone, analyze, store facts, write fact pages into wiki-facts/ and wiki-full-<model>/.
     */
    public void prepare() {
        throw Todo.chapter(12, "SpikeS1.prepare");
    }

    /**
     * Prints every paid call it would make and the worst-case total. Makes no paid call.
     */
    public BigDecimal estimate() {
        throw Todo.chapter(12, "SpikeS1.estimate");
    }

    /**
     * Step 3 (paid): the writer calls; pages rendered with citations into wiki-full-<model>/.
     */
    public void writePages() {
        throw Todo.chapter(12, "SpikeS1.writePages");
    }

    /**
     * Step 4 (paid): answers in WIKI_TOOLS, RAW_AGENT and FACTS_ONLY; writes answers.json and grading.md.
     */
    public void answer() {
        throw Todo.chapter(12, "SpikeS1.answer");
    }

    /**
     * Step 5 (free): reads your filled grading.md and prints the summary table.
     */
    public String summarize() {
        throw Todo.chapter(12, "SpikeS1.summarize");
    }
}
