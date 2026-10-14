package ekw.llm;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
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
 * Request and response mapping against a local stub of the Responses API. No real API call is made.
 */
@Tag("ch07")
@WireMockTest
class OpenAiResponsesChatModelTest {

    private static String response(String id, String output, String status) {
        return """
                {"id": "%s", "object": "response", "created_at": 1760000000, "status": "%s", "model": "gpt-6.1-sol",
                 "output": %s, "parallel_tool_calls": true, "tool_choice": "auto", "tools": [],
                 "error": null, "incomplete_details": null, "instructions": null, "metadata": {},
                 "temperature": 1.0, "top_p": 1.0,
                 "usage": {"input_tokens": 1000, "input_tokens_details": {"cached_tokens": 400},
                           "output_tokens": 50, "output_tokens_details": {"reasoning_tokens": 20},
                           "total_tokens": 1050}}""".formatted(id, status, output);
    }

    @Test
    void toolTurnRoundTripKeepsReasoningItems(WireMockRuntimeInfo wm) {
        stubFor(post("/responses").inScenario("loop")
                                  .whenScenarioStateIs(Scenario.STARTED)
                                  .willReturn(okJson(response("resp_1", """
                                                  [{"type": "reasoning", "id": "rs_1", "summary": [], "encrypted_content": "enc-123"},
                                                   {"type": "function_call", "id": "fc_1", "call_id": "call_1", "name": "read_page",
                                                    "arguments": "{\\"path\\": \\"systems/a/overview.md\\"}", "status": "completed"}]""",
                                          "completed")))
                                  .willSetStateTo("second"));
        stubFor(post("/responses").inScenario("loop")
                                  .whenScenarioStateIs("second")
                                  .willReturn(okJson(response("resp_2", """
                                                  [{"type": "message", "id": "msg_1", "role": "assistant", "status": "completed",
                                                    "content": [{"type": "output_text", "text": "Done [E1].", "annotations": []}]}]""",
                                          "completed"))));
        OpenAIClient client = OpenAIOkHttpClient.builder()
                                                .apiKey("test-key")
                                                .baseUrl(wm.getHttpBaseUrl())
                                                .maxRetries(0)
                                                .build();
        OpenAiResponsesChatModel model = new OpenAiResponsesChatModel(client, TestModels.info("gpt-6.1-sol", "openai"));

        List<Message> messages = new ArrayList<>(List.of(new Message("user", List.of(new Models.Text("What is A?")))));
        ChatResult first = model.chat(TestModels.request(messages, List.of(TestModels.readPage())));

        ToolCall call = first.content()
                             .stream()
                             .filter(p -> p instanceof ToolCall)
                             .map(p -> (ToolCall) p)
                             .findFirst()
                             .orElseThrow();
        assertThat(first.stop()).isEqualTo(Models.StopReason.TOOL_USE);
        assertThat(call.arguments()).containsEntry("path", "systems/a/overview.md");
        assertThat(first.usage()).isEqualTo(new Models.Usage(600, 50, 400, 0));
        verify(postRequestedFor(urlEqualTo("/responses"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("gpt-6.1-sol")))
                .withRequestBody(matchingJsonPath("$.instructions", equalTo("You answer.")))
                .withRequestBody(matchingJsonPath("$.reasoning.effort", equalTo("high")))
                .withRequestBody(matchingJsonPath("$.max_output_tokens", equalTo("2000")))
                .withRequestBody(matchingJsonPath("$.store", equalTo("false")))
                .withRequestBody(matchingJsonPath("$.include[0]", equalTo("reasoning.encrypted_content")))
                .withRequestBody(matchingJsonPath("$.tools[0].type", equalTo("function")))
                .withRequestBody(matchingJsonPath("$.tools[0].strict", equalTo("true"))));

        messages.add(new Message("assistant", first.content()));
        messages.add(new Message("user", List.of(new ToolResult("call_1", "E1: page text", false))));
        ChatResult second = model.chat(TestModels.request(messages, List.of(TestModels.readPage())));

        assertThat(second.content()).first()
                                    .isEqualTo(new Models.Text("Done [E1]."));
        verify(postRequestedFor(urlEqualTo("/responses"))
                .withRequestBody(matchingJsonPath("$.input[1].type", equalTo("reasoning")))
                .withRequestBody(matchingJsonPath("$.input[1].encrypted_content", equalTo("enc-123")))
                .withRequestBody(matchingJsonPath("$.input[2].type", equalTo("function_call")))
                .withRequestBody(matchingJsonPath("$.input[3].type", equalTo("function_call_output")))
                .withRequestBody(matchingJsonPath("$.input[3].call_id", equalTo("call_1"))));
    }
}
