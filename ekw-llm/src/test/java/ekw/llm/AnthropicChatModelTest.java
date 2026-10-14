package ekw.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import ekw.llm.Models.ChatResult;
import ekw.llm.Models.Message;
import ekw.llm.Models.ToolCall;
import ekw.llm.Models.ToolResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Request and response mapping against a local stub of the Messages API. No real API call is made.
 */
@Tag("ch07")
@WireMockTest
class AnthropicChatModelTest {

    private static final String TOOL_USE = """
            {"id": "msg_1", "type": "message", "role": "assistant", "model": "claude-sonnet-5-5",
             "content": [
               {"type": "thinking", "thinking": "Read the page first.", "signature": "sig-abc"},
               {"type": "tool_use", "id": "toolu_1", "name": "read_page", "input": {"path": "systems/a/overview.md"}}],
             "stop_reason": "tool_use", "stop_sequence": null,
             "usage": {"input_tokens": 1200, "output_tokens": 80, "cache_read_input_tokens": 3000,
                       "cache_creation_input_tokens": 0}}""";

    private static final String FINAL = """
            {"id": "msg_2", "type": "message", "role": "assistant", "model": "claude-sonnet-5-5",
             "content": [{"type": "text", "text": "Done [E1]."}],
             "stop_reason": "end_turn", "stop_sequence": null,
             "usage": {"input_tokens": 1400, "output_tokens": 40, "cache_read_input_tokens": 3000,
                       "cache_creation_input_tokens": 0}}""";

    @Test
    void toolTurnRoundTripKeepsThinkingBlocks(WireMockRuntimeInfo wm) {
        stubFor(post("/v1/messages").inScenario("loop")
                                    .whenScenarioStateIs(Scenario.STARTED)
                                    .willReturn(okJson(TOOL_USE))
                                    .willSetStateTo("second"));
        stubFor(post("/v1/messages").inScenario("loop")
                                    .whenScenarioStateIs("second")
                                    .willReturn(okJson(FINAL)));
        AnthropicClient client = AnthropicOkHttpClient.builder()
                                                      .apiKey("test-key")
                                                      .baseUrl(wm.getHttpBaseUrl())
                                                      .maxRetries(0)
                                                      .build();
        AnthropicChatModel model = new AnthropicChatModel(client, TestModels.info("claude-sonnet-5-5", "anthropic"));

        List<Message> messages = new ArrayList<>(List.of(new Message("user", List.of(new Models.Text("What is A?")))));
        ChatResult first = model.chat(TestModels.request(messages, List.of(TestModels.readPage())));

        assertThat(first.stop()).isEqualTo(Models.StopReason.TOOL_USE);
        ToolCall call = first.content()
                             .stream()
                             .filter(p -> p instanceof ToolCall)
                             .map(p -> (ToolCall) p)
                             .findFirst()
                             .orElseThrow();
        assertThat(call.name()).isEqualTo("read_page");
        assertThat(call.arguments()).containsEntry("path", "systems/a/overview.md");
        assertThat(first.usage()).isEqualTo(new Models.Usage(1200, 80, 3000, 0));

        verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("claude-sonnet-5-5")))
                .withRequestBody(matchingJsonPath("$.max_tokens", equalTo("2000")))
                .withRequestBody(matchingJsonPath("$.output_config.effort", equalTo("high")))
                .withRequestBody(matchingJsonPath("$.system[0].cache_control.type", equalTo("ephemeral")))
                .withRequestBody(matchingJsonPath("$.tools[0].strict", equalTo("true")))
                .withRequestBody(matchingJsonPath("$.tools[0].input_schema.additionalProperties", equalTo("false")))
                .withRequestBody(matchingJsonPath("$.tools[0].input_schema.required[0]", equalTo("path")))
                .withRequestBody(matchingJsonPath("$.tools[0].cache_control.type", equalTo("ephemeral")))
                .withRequestBody(notContaining("tool_choice")));

        messages.add(new Message("assistant", first.content()));
        messages.add(new Message("user", List.of(new ToolResult(call.callId(), "E1: page text", false))));
        ChatResult second = model.chat(TestModels.request(messages, List.of(TestModels.readPage())));

        assertThat(second.stop()).isEqualTo(Models.StopReason.END_TURN);
        verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.messages[1].content[0].type", equalTo("thinking")))
                .withRequestBody(matchingJsonPath("$.messages[1].content[0].signature", equalTo("sig-abc")))
                .withRequestBody(matchingJsonPath("$.messages[2].content[0].type", equalTo("tool_result")))
                .withRequestBody(matchingJsonPath("$.messages[2].content[0].tool_use_id", equalTo("toolu_1"))));
    }
}
