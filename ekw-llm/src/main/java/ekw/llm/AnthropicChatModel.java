package ekw.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCountTokensParams;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.fasterxml.jackson.core.type.TypeReference;
import ekw.llm.Models.Native;
import ekw.llm.Models.Part;
import ekw.llm.Models.StopReason;
import ekw.llm.Models.Text;
import ekw.llm.Models.TokenEstimate;
import ekw.llm.Models.ToolCall;
import ekw.llm.Models.ToolResult;
import ekw.llm.Models.ToolSpec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Claude through the official Java SDK ({@code com.anthropic:anthropic-java}). Translates our provider-neutral
 * {@link ChatRequest} into a Messages API request and the reply back into a {@link ChatResult}.
 *
 */
public final class AnthropicChatModel implements Models.ChatModel {

    static final String PROVIDER = "anthropic";

    private final AnthropicClient anthropicClient;
    private final ModelInfo modelInfo;

    public AnthropicChatModel(AnthropicClient anthropicClient, ModelInfo modelInfo) {
        this.anthropicClient = anthropicClient;
        this.modelInfo = modelInfo;
    }

    @Override
    public ModelInfo info() {
       return modelInfo;
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        MessageCreateParams.Builder messageCreateParamsBuilder =
                MessageCreateParams.builder()
                                   .model(modelInfo.modelId())
                                   .maxTokens(request.maxOutputTokens())
                                   .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                                                                                  .text(request.system())
                                                                                  .cacheControl(CacheControlEphemeral
                                                                                          .builder()
                                                                                          .build())
                                                                                  .build()));

        OutputConfig output = outputConfig(request);
        if (output != null)
            messageCreateParamsBuilder.outputConfig(output);
        tools(request.tools()).forEach(messageCreateParamsBuilder::addTool);
        request.messages().forEach(m -> addMessage(messageCreateParamsBuilder, m));

        long start = System.nanoTime();
        Message reply = anthropicClient.messages().create(messageCreateParamsBuilder.build());
        Duration latency = Duration.ofNanos(System.nanoTime() - start);

        List<Part> parts = new ArrayList<>();

        for (ContentBlock block : reply.content()) {
            if (block.isText()) {
                parts.add(new Text(block.asText().text()));
            } else if (block.isToolUse()) {
                Map<String, Object> args = block.asToolUse()
                                                ._input()
                                                .convert(new TypeReference<Map<String, Object>>() {
                                                });
                parts.add(new ToolCall(block.asToolUse().id(), block.asToolUse()
                                                                           .name(), args));
            }
        }
        parts.add(new Native(PROVIDER, reply));
        com.anthropic.models.messages.Usage anthropicUsage = reply.usage();
        Models.Usage usage = new Models.Usage(anthropicUsage.inputTokens()
                , anthropicUsage.outputTokens()
                , anthropicUsage.cacheReadInputTokens().orElse(0L)
                , anthropicUsage.cacheCreationInputTokens().orElse(0L));

        return new ChatResult(parts, stop(reply), usage, reply.model().asString(), latency);
    }

    private static void addMessage(MessageCreateParams.Builder messageCreateParamsBuilder,
                            Models.Message message) {
        for (Part messagePart : message.parts()) {
            if (messagePart instanceof Native _native && PROVIDER.equals(_native.provider())) {
                messageCreateParamsBuilder.addMessage((Message) _native.turn());
                return;
            }
        }
        messageCreateParamsBuilder.addMessage(toParam(message));
    }

    private static MessageParam toParam(Models.Message m) {
        for (Part p : m.parts()) {
            if (p instanceof Native _native && PROVIDER.equals(_native.provider()))
                return ((Message) _native.turn()).toParam();
        }

        List<ContentBlockParam> blocks = new ArrayList<>();
        for (Part p : m.parts()) {
            switch (p) {
                case Text t -> blocks.add(ContentBlockParam
                        .ofText(TextBlockParam.builder()
                                              .text(t.text())
                                              .build()));
                case ToolResult tr -> blocks.add(ContentBlockParam
                        .ofToolResult(ToolResultBlockParam.builder()
                                                          .toolUseId(tr.callId())
                                                          .content(tr.content())
                                                          .isError(tr.isError())
                                                          .build()));
                case ToolCall tc ->
                        throw new IllegalArgumentException("assistant tool calls must come as a Native turn");
                case Native n ->
                        throw new IllegalArgumentException("turn from another provider: " + n.provider());
            }
        }
        return MessageParam.builder()
                           .role("assistant".equals(m.role()) ? MessageParam.Role.ASSISTANT
                                                              : MessageParam.Role.USER)
                           .contentOfBlockParams(blocks).build();
    }

    private static OutputConfig outputConfig(ChatRequest chatRequest) {
        if (chatRequest.effort().isEmpty() && chatRequest.outputSchema().isEmpty())
            return null;
        OutputConfig.Builder outputConfigBuilder = OutputConfig.builder();
        chatRequest.effort().ifPresent(effort ->
                outputConfigBuilder.effort(OutputConfig.Effort.of(effort.name().toLowerCase())));
        chatRequest.outputSchema().ifPresent(stringObjectMap ->
                outputConfigBuilder.format(JsonOutputFormat
                        .builder()
                        .schema(JsonOutputFormat.Schema
                                .builder()
                                .putAllAdditionalProperties(json(stringObjectMap))
                                .build())
                        .build()));
        return outputConfigBuilder.build();
    }

    static Map<String, JsonValue> json(Map<String, Object> map) {
        Map<String, JsonValue> out = new LinkedHashMap<>();
        map.forEach((k, v) -> out.put(k, JsonValue.from(v)));
        return out;
    }

    static List<Tool> tools(List<ToolSpec> specs) {
        List<Tool> out = new ArrayList<>();
        int index = 0; //to find last record for cache control
        for (ToolSpec toolSpec : specs) {
            Map<String, Object> rest = new LinkedHashMap<>(toolSpec.inputSchema());
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) rest.remove("properties");
            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) rest.remove("required");
            rest.remove("type");  // the SDK always sends type: object
            Tool.InputSchema.Builder toolInputSchemaBuilder =
                    Tool.InputSchema.builder()
                                    .properties(Tool.InputSchema.Properties
                                            .builder()
                                            .putAllAdditionalProperties(json(
                                                    properties == null
                                                    ? Map.of()
                                                    : properties))
                                            .build())
                                    .putAllAdditionalProperties(json(rest));
            if (required != null)
                toolInputSchemaBuilder.required(required);

            Tool.Builder toolBuilder = Tool.builder()
                                           .name(toolSpec.name())
                                           .description(toolSpec.description())
                                           .inputSchema(toolInputSchemaBuilder.build())
                                           .strict(true);
            if (index == specs.size() - 1) {
                toolBuilder.cacheControl(CacheControlEphemeral.builder().build());
            }
            out.add(toolBuilder.build());
            index++;
        }
        return out;
    }

    /**
     * Exact input-token count from the free count_tokens endpoint.
     */
    @Override
    public TokenEstimate estimate(ChatRequest chatRequest) {
        MessageCountTokensParams.Builder b = MessageCountTokensParams
                .builder()
                .model(modelInfo.modelId())
                .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                                                               .text(chatRequest.system())
                                                               .build()));
        tools(chatRequest.tools()).forEach(b::addTool);
        for (Models.Message m : chatRequest.messages())
            b.addMessage(toParam(m));
        return new TokenEstimate(anthropicClient.messages().countTokens(b.build()).inputTokens()
                , true);
    }

    private static StopReason stop(Message reply) {
        if (reply.stopReason().isEmpty()) return StopReason.ERROR;
        com.anthropic.models.messages.StopReason s = reply.stopReason().get();
        if (s.equals(com.anthropic.models.messages.StopReason.END_TURN)) return StopReason.END_TURN;
        if (s.equals(com.anthropic.models.messages.StopReason.TOOL_USE)) return StopReason.TOOL_USE;
        if (s.equals(com.anthropic.models.messages.StopReason.MAX_TOKENS))
            return StopReason.MAX_TOKENS;
        if (s.equals(com.anthropic.models.messages.StopReason.REFUSAL)) return StopReason.REFUSAL;
        return StopReason.ERROR;
    }
}
