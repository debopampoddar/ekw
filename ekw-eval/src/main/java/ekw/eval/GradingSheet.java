package ekw.eval;

import ekw.core.Todo;

import java.util.List;

/**
 * The hand-grading sheet of spike S1: a Markdown file with one block per answer, where you score each key point 1,
 * 0.5 or 0; and its summary per setup and model.
 *
 * <p>Chapter 12 of the development guide (the exact format is described there).
 */
public final class GradingSheet {

    private GradingSheet() {
    }

    /**
     * The sheet to fill in, with empty score cells.
     */
    public static String render(List<Answer> answers) {
        throw Todo.chapter(12, "GradingSheet.render");
    }

    /**
     * Reads the filled sheet. An empty or invalid score is an IllegalStateException naming the answer and key point.
     */
    public static List<Summary> summarize(String filledSheet, List<Answer> answers) {
        throw Todo.chapter(12, "GradingSheet.summarize");
    }
}
