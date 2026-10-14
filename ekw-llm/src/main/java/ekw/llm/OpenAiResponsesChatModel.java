package ekw.llm;

import com.openai.client.OpenAIClient;
import ekw.core.Todo;
import ekw.llm.Models.ChatRequest;
import ekw.llm.Models.ChatResult;
import ekw.llm.Models.ModelInfo;
import ekw.llm.Models.TokenEstimate;

/**
 * OpenAI through the Responses API ({@code com.openai:openai-java}). Used in milestone 1 as the judge, which must come
 * from a different vendor than the answering model.
 *
 * <p>Chapter 7 of the development guide.
 */
public final class OpenAiResponsesChatModel implements Models.ChatModel {

    public OpenAiResponsesChatModel(OpenAIClient client, ModelInfo info) {
        // TODO (chapter 7): keep the arguments.
    }

    @Override
    public ModelInfo info() {
        throw Todo.chapter(7, "OpenAiResponsesChatModel.info");
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        throw Todo.chapter(7, "OpenAiResponsesChatModel.chat");
    }

    /**
     * OpenAI cannot count tokens before a call: estimate characters / 3, rounded up, and mark it inexact.
     */
    @Override
    public TokenEstimate estimate(ChatRequest request) {
        throw Todo.chapter(7, "OpenAiResponsesChatModel.estimate");
    }
}
