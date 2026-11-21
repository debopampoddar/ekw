package ekw.llm;

import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageParam;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 21, step B5b: the cache point chapter 7's Anthropic model sets, and where it sets none. No network. */
@Tag("ch21")
class CachedEvidenceTest {

    static Models.Message user(String... texts) {
        return new Models.Message("user", java.util.Arrays.stream(texts).map(t -> (Models.Part) new Models.Text(t)).toList());
    }

    static List<Boolean> cachePoints(MessageParam p) {
        return p.content().asBlockParams().stream().map(ContentBlockParam::asText).map(t -> t.cacheControl().isPresent())
                .toList();
    }

    @Test
    void theEvidenceIsCachedAndTheInstructionsAreNot() {
        Models.Message m = user("Evidence:\n[E1] ...", "[E2] ...", "Write the page.");
        assertThat(AnthropicChatModel.cachesPrefix(m)).isTrue();
        MessageParam p = AnthropicChatModel.withCachedPrefix(m);
        assertThat(p.role()).isEqualTo(MessageParam.Role.USER);
        assertThat(cachePoints(p)).containsExactly(false, true, false);
        assertThat(p.content().asBlockParams()).extracting(b -> b.asText().text())
                .containsExactly("Evidence:\n[E1] ...", "[E2] ...", "Write the page.");
    }

    @Test
    void oneTextOrToolResultsOrAnAssistantTurnAreSentAsBefore() {
        assertThat(AnthropicChatModel.cachesPrefix(user("What calls Pool.borrow()?"))).isFalse();
        assertThat(AnthropicChatModel.cachesPrefix(new Models.Message("assistant",
                List.of(new Models.Text("a"), new Models.Text("b"))))).isFalse();
        assertThat(AnthropicChatModel.cachesPrefix(new Models.Message("user", List.of(new Models.Text("a"),
                new Models.ToolResult("c1", "result", false))))).isFalse();
    }
}
