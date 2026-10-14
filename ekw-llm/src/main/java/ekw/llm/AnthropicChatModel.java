package ekw.llm;

import com.anthropic.client.AnthropicClient;
import ekw.core.Todo;
import ekw.llm.Models.ChatRequest;
import ekw.llm.Models.ChatResult;
import ekw.llm.Models.ModelInfo;
import ekw.llm.Models.TokenEstimate;

/**
 * Claude through the official Java SDK ({@code com.anthropic:anthropic-java}). Translates our provider-neutral
 * {@link ChatRequest} into a Messages API request and the reply back into a {@link ChatResult}.
 *
 * <p>Chapter 7 of the development guide.
 */
public final class AnthropicChatModel implements Models.ChatModel {

    public AnthropicChatModel(AnthropicClient client, ModelInfo info) {
        // TODO (chapter 7): keep the arguments.
    }

    @Override
    public ModelInfo info() {
        throw Todo.chapter(7, "AnthropicChatModel.info");
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        throw Todo.chapter(7, "AnthropicChatModel.chat");
    }

    /**
     * Exact input-token count from the free count_tokens endpoint.
     */
    @Override
    public TokenEstimate estimate(ChatRequest request) {
        throw Todo.chapter(7, "AnthropicChatModel.estimate");
    }
}
