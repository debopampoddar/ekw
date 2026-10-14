package ekw.llm;

import ekw.core.Todo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Versioned prompts shipped as resources (design: stage 2, section 10): {@code prompts/<id>@<version>.md}, listed in
 * {@code prompts/prompts.txt}, with optional output schemas {@code prompts/<id>@<version>.schema.json}.
 * Templates use {@code {{name}}} placeholders only; there is no other template syntax.
 *
 * <p>Chapter 10 of the development guide.
 */
public final class PromptRegistry {

    /**
     * One prompt template.
     */
    public record Prompt(Models.PromptRef ref, String template) {

        /** The placeholder names in the template, e.g. "evidence_items". */
        public Set<String> placeholders () {
            throw Todo.chapter(10, "Prompt.placeholders");
        }

        /**
         * Replaces every {@code {{name}}} with its value. Throws IllegalArgumentException if a placeholder has no value,
         * or a value has no placeholder: both are bugs in the calling code.
         */
        public String fill (Map < String, String > values){
            throw Todo.chapter(10, "Prompt.fill");
        }
    }

    private PromptRegistry() {
    }

    /**
     * Loads every prompt listed in prompts/prompts.txt (blank lines and # comments ignored).
     */
    public static PromptRegistry fromClasspath() {
        throw Todo.chapter(10, "PromptRegistry.fromClasspath");
    }

    /**
     * The prompt, or IllegalArgumentException if it is not listed.
     */
    public Prompt prompt(String id, int version) {
        throw Todo.chapter(10, "PromptRegistry.prompt");
    }

    /**
     * The JSON schema prompts/<id>@<version>.schema.json as a map, if the file exists.
     */
    public Optional<Map<String, Object>> schema(String id, int version) {
        throw Todo.chapter(10, "PromptRegistry.schema");
    }

    /**
     * Every listed prompt, in file order.
     */
    public List<Models.PromptRef> all() {
        throw Todo.chapter(10, "PromptRegistry.all");
    }
}
