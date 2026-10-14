package ekw.query;

import ekw.core.Todo;
import ekw.llm.Models;
import ekw.llm.Models.ChatModel;
import ekw.llm.Models.ToolSpec;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * The answer-mode tool loop (design: stage 2, section 9.4 "Question loop"). The model reads through tools and finishes
 * by calling {@code submit_answer}, whose input schema is the answer schema. Every model call goes through the budget
 * guard first.
 *
 * <p>Chapter 8 of the development guide.
 */
public final class AnswerLoop {

    /**
     * A read tool's output: text (with evidence ids), and whether it is an error the model should correct.
     */
    public record ToolOutput(String text, boolean isError) {
    }

    /**
     * A tool the model may call: its spec, and the code that runs it.
     */
    public record Tool(ToolSpec spec, Function<Map<String, Object>, ToolOutput> run) {
    }

    public enum End {SUBMITTED, CONVERTED, STEP_LIMIT, BUDGET}

    /**
     * The answer object, how the loop ended, and how many model and tool calls it made.
     */
    public record Result(Map<String, Object> answer, End end, int modelCalls, int toolCalls) {
    }

    public AnswerLoop(ChatModel model, Models.BudgetGuard guard, Map<String, Object> answerSchema) {
        // TODO (chapter 8): keep the arguments.
    }

    /**
     * Runs one question: offers the tools plus submit_answer, executes tool calls, stops on submit_answer. A model that
     * ends with plain text gets one conversion call (no tools, structured output). A refused reservation ends the loop
     * with an "inconclusive" answer.
     */
    public Result run(String system, String question, List<Tool> tools, int maxToolCalls, int maxOutputTokens,
                      Optional<Models.Effort> effort) {
        throw Todo.chapter(8, "AnswerLoop.run");
    }
}
