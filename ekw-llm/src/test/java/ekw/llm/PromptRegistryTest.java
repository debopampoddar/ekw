package ekw.llm;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ch10")
class PromptRegistryTest {
    private final PromptRegistry registry = PromptRegistry.fromClasspath();

    @Test
    void loadsTheListedPrompts() {
        assertThat(registry.all()).containsExactly(
                new Models.PromptRef("writer.system", 1), new Models.PromptRef("writer.page", 1),
                new Models.PromptRef("writer.page.update", 1), new Models.PromptRef("answer.system", 1));
        assertThat(registry.prompt("writer.page", 1)
                           .placeholders()).containsExactlyInAnyOrder(
                "page_type", "title_hint", "system", "commit_short", "template_sections",
                "type_specific_instructions", "linkable_pages", "evidence_items");
    }

    @Test
    void fillsEveryPlaceholderAndNothingElse() {
        PromptRegistry.Prompt p = registry.prompt("answer.system", 1);
        Map<String, String> values = new HashMap<>();
        for (String name : p.placeholders()) values.put(name, "<" + name + ">");
        String text = p.fill(values);
        assertThat(text).doesNotContain("{{")
                        .contains("<root_index>")
                        .contains("You answer questions about software");

        values.remove("route_type");
        assertThatThrownBy(() -> p.fill(values)).isInstanceOf(IllegalArgumentException.class)
                                                .hasMessageContaining("route_type");
        values.put("route_type", "T1");
        values.put("unused", "x");
        assertThatThrownBy(() -> p.fill(values)).isInstanceOf(IllegalArgumentException.class)
                                                .hasMessageContaining("unused");
    }

    @Test
    @SuppressWarnings("unchecked")
    void schemasAreStrictJson() {
        Map<String, Object> writer = registry.schema("writer.page", 1)
                                             .orElseThrow();
        assertThat(writer).containsEntry("additionalProperties", false);
        assertThat((List<Object>) writer.get("required")).containsExactly("title", "description", "sections",
                "keep_sections", "synthesis", "concepts", "read", "not_read", "evidence_requests");
        assertThat(registry.schema("answer", 1)).isPresent();
        assertThat(registry.schema("writer.system", 1)).isEmpty();
        assertThatThrownBy(() -> registry.prompt("nope", 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
