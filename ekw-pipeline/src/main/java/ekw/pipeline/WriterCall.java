package ekw.pipeline;

import ekw.core.Evidence;
import ekw.core.Todo;
import ekw.llm.Models;
import ekw.llm.PromptRegistry;

/**
 * One writer call (design: stage 2, section 3.2.6): an evidence packet in, the page's structured content out.
 * The model only cites evidence ids; it never writes locations or Markdown structure.
 *
 * <p>Chapter 10 of the development guide.
 */
public final class WriterCall {

    /**
     * What the page is and what it may use. The strings fill the placeholders of writer.page@1.
     */
    public record PageRequest(String pageType, String titleHint, String system, String commitShort,
                              String templateSections, String typeSpecificInstructions, String linkablePages,
                              Evidence.Packet packet) {
    }

    /**
     * @param conventionsExcerpt the text that fills writer.system@1's {{conventions_sections_2_5_8}}
     */
    public WriterCall(Models.ChatModel model, Models.BudgetGuard guard, PromptRegistry prompts,
                      String conventionsExcerpt, Models.Effort effort, int maxOutputTokens) {
        // TODO (chapter 10): keep the arguments.
    }

    /**
     * The request: role WRITER, prompt writer.page@1, system = filled writer.system@1, one user message = filled
     * writer.page@1 (evidence items rendered with {@link #evidenceBlock}), output schema writer.page@1, no tools.
     */
    public Models.ChatRequest request(PageRequest page) {
        throw Todo.chapter(10, "WriterCall.request");
    }

    /**
     * Reserve, call, settle; then parse the model's JSON text into WriterOutput.
     */
    public Pipeline.WriterOutput write(PageRequest page) {
        throw Todo.chapter(10, "WriterCall.write");
    }

    /**
     * One evidence item in the format of stage 2, section 10:
     * <pre>{@code
     * <evidence id="E7" kind="code" location="RetryPolicy.java L71-L96" source-policy="hosted-allowed">
     * ...content...
     * </evidence>
     * }</pre>
     */
    public static String evidenceBlock(Evidence.Item item) {
        throw Todo.chapter(10, "WriterCall.evidenceBlock");
    }

    /**
     * The writer's JSON (schema writer.page@1, snake_case) as a WriterOutput.
     */
    public static Pipeline.WriterOutput parse(String json) {
        throw Todo.chapter(10, "WriterCall.parse");
    }
}
