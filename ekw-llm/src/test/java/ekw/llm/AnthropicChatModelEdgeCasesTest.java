package ekw.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import ekw.core.Evidence;
import ekw.llm.Models.Message;
import ekw.llm.Models.Native;
import ekw.llm.Models.StopReason;
import ekw.llm.Models.Text;
import ekw.llm.Models.TokenEstimate;
import ekw.llm.Models.ToolCall;
import ekw.llm.Models.ToolSpec;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Paths of {@link AnthropicChatModel} that {@link AnthropicChatModelTest} does not reach: every stop reason, missing
 * cache counts, the output schema, the request without output_config, the cache breakpoint with several tools,
 * the impossible message shapes, and {@code estimate}. No real API call is made.
 */
@Tag("ch07")
@WireMockTest
class AnthropicChatModelEdgeCasesTest {

    private static final String QUESTION = "What is A?";

    private static String reply(String stopReason, String usage) {
        return """
                {"id": "msg_1", "type": "message", "role": "assistant", "model": "claude-haiku-5-5",
                 "content": [{"type": "text", "text": "Hello"}],
                 "stop_reason": %s, "stop_sequence": null, "usage": %s}"""
                .formatted(stopReason, usage);
    }

    private static final String USAGE = """
            {"input_tokens": 10, "output_tokens": 5, "cache_read_input_tokens": 0, "cache_creation_input_tokens": 0}""";

    private static AnthropicChatModel model(WireMockRuntimeInfo wm) {
        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey("test-key").baseUrl(wm.getHttpBaseUrl()).maxRetries(0).build();
        return new AnthropicChatModel(client, TestModels.info("claude-sonnet-5-5", "anthropic"));
    }

    private static ChatRequest.Builder plain(List<Message> messages) {
        return ChatRequest.builder()
                .role(Models.Role.ANSWERER).prompt(new Models.PromptRef("answer.system", 1)).system("You answer.")
                .messages(messages).maxOutputTokens(2000)
                .evidence(new Evidence.Packet(List.of())).cacheablePrefixParts(0);
    }

    private static List<Message> question() {
        return List.of(new Message("user", List.of(new Text(QUESTION))));
    }

    @ParameterizedTest
    @CsvSource({
            "'\"end_turn\"',   END_TURN",
            "'\"tool_use\"',   TOOL_USE",
            "'\"max_tokens\"', MAX_TOKENS",
            "'\"refusal\"',    REFUSAL",
            "'\"pause_turn\"', ERROR",
            "null,             ERROR"})
    void mapsEveryStopReason(String claudeStop, StopReason expected, WireMockRuntimeInfo wm) {
        stubFor(post("/v1/messages").willReturn(okJson(reply(claudeStop, USAGE))));

        ChatResult result = model(wm).chat(plain(question()).build());

        assertThat(result.stop()).isEqualTo(expected);
    }

    @Test
    void missingCacheCountsAreZeroAndNativeComesLast(WireMockRuntimeInfo wm) {
        stubFor(post("/v1/messages").willReturn(okJson(reply("\"end_turn\"",
                "{\"input_tokens\": 10, \"output_tokens\": 5}"))));

        ChatResult result = model(wm).chat(plain(question()).build());

        assertThat(result.usage()).isEqualTo(new Models.Usage(10, 5, 0, 0));
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0)).isEqualTo(new Text("Hello"));
        assertThat(result.content().get(1)).isInstanceOfSatisfying(Native.class,
                n -> assertThat(n.provider()).isEqualTo("anthropic"));
        assertThat(result.servedByModelId()).isEqualTo("claude-haiku-5-5");   // from the reply, not from ModelInfo
        assertThat(result.latency().isNegative()).isFalse();
    }

    @Test
    void noEffortAndNoSchemaSendsNoOutputConfigAndNoTools(WireMockRuntimeInfo wm) {
        stubFor(post("/v1/messages").willReturn(okJson(reply("\"end_turn\"", USAGE))));

        model(wm).chat(plain(question()).build());

        verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(notContaining("output_config"))
                .withRequestBody(notContaining("\"tools\""))
                .withRequestBody(matchingJsonPath("$.messages[0].role", equalTo("user")))
                .withRequestBody(matchingJsonPath("$.messages[0].content[0].text", equalTo(QUESTION))));
    }

    @Test
    void outputSchemaGoesIntoOutputConfigFormat(WireMockRuntimeInfo wm) {
        stubFor(post("/v1/messages").willReturn(okJson(reply("\"end_turn\"", USAGE))));
        Map<String, Object> schema = Map.of("type", "object", "additionalProperties", false,
                "required", List.of("title"), "properties", Map.of("title", Map.of("type", "string")));

        model(wm).chat(plain(question()).outputSchema(schema).effort(Models.Effort.LOW).build());

        verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.output_config.effort", equalTo("low")))
                .withRequestBody(matchingJsonPath("$.output_config.format.type", equalTo("json_schema")))
                .withRequestBody(matchingJsonPath("$.output_config.format.schema.required[0]", equalTo("title")))
                .withRequestBody(matchingJsonPath("$.output_config.format.schema.properties.title.type",
                        equalTo("string"))));
    }

    @Test
    void onlyTheLastToolCarriesACacheBreakpoint(WireMockRuntimeInfo wm) {
        stubFor(post("/v1/messages").willReturn(okJson(reply("\"end_turn\"", USAGE))));
        ToolSpec readIndex = new ToolSpec("read_index", "Returns an index.", Map.of(
                "type", "object", "additionalProperties", false, "properties", Map.of()));

        model(wm).chat(plain(question()).tools(List.of(readIndex, TestModels.readPage())).build());

        verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.tools[0].name", equalTo("read_index")))
                .withRequestBody(matchingJsonPath("$.tools[0].strict", equalTo("true")))
                .withRequestBody(matchingJsonPath("$.tools[0].cache_control", absent()))
                .withRequestBody(matchingJsonPath("$.tools[1].name", equalTo("read_page")))
                .withRequestBody(matchingJsonPath("$.tools[1].cache_control.type", equalTo("ephemeral")))
                .withRequestBody(notContaining("tool_choice")));
    }

    @Test
    void aToolCallOutsideANativeTurnIsRejectedBeforeAnyRequest(WireMockRuntimeInfo wm) {
        List<Message> messages = List.of(
                new Message("user", List.of(new Text(QUESTION))),
                new Message("assistant", List.of(new ToolCall("toolu_1", "read_page", Map.of("path", "x.md")))));

        assertThatThrownBy(() -> model(wm).chat(plain(messages).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Native");
        verify(0, postRequestedFor(urlEqualTo("/v1/messages")));
    }

    @Test
    void aTurnFromAnotherProviderIsRejectedBeforeAnyRequest(WireMockRuntimeInfo wm) {
        List<Message> messages = List.of(
                new Message("user", List.of(new Text(QUESTION))),
                new Message("assistant", List.of(new Text("Hi"), new Native("openai", List.of()))));

        assertThatThrownBy(() -> model(wm).chat(plain(messages).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openai");
        verify(0, postRequestedFor(urlEqualTo("/v1/messages")));
    }

    @Test
    void estimateUsesCountTokensAndIsExact(WireMockRuntimeInfo wm) {
        stubFor(post("/v1/messages/count_tokens").willReturn(okJson("{\"input_tokens\": 42}")));

        TokenEstimate estimate = model(wm).estimate(plain(question()).tools(List.of(TestModels.readPage())).build());

        assertThat(estimate).isEqualTo(new TokenEstimate(42, true));
        verify(postRequestedFor(urlEqualTo("/v1/messages/count_tokens"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("claude-sonnet-5-5")))
                .withRequestBody(matchingJsonPath("$.system[0].text", equalTo("You answer.")))
                .withRequestBody(matchingJsonPath("$.tools[0].name", equalTo("read_page")))
                .withRequestBody(matchingJsonPath("$.messages[0].content[0].text", equalTo(QUESTION))));
        verify(0, postRequestedFor(urlEqualTo("/v1/messages")));
    }
}
