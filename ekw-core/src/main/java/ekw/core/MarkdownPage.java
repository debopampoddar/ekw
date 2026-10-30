package ekw.core;

import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A page file: YAML frontmatter between "---" lines, then the Markdown body. Keys keep their order, and keys we do not
 * know are kept (OKF requires consumers to preserve unknown keys). Output is deterministic, so regenerating a page from
 * the same inputs gives the same bytes.
 *
 * <p>Chapter 2 of the development guide.
 */
public record MarkdownPage(Map<String, Object> frontmatter, String body) {

    private static final String FENCE = "---\n";
    // Thread-safe settings configuration
    private static final DumpSettings SETTINGS =
            DumpSettings.builder()
                        // Force clean block formatting instead of JSON-like inline structures ({a: 1})
                        .setDefaultFlowStyle(FlowStyle.BLOCK)
                        .setDefaultScalarStyle(ScalarStyle.PLAIN)
                        .setWidth(4096)
                        .build();

    // Reusable dump instance
    private static final Dump DUMP = new Dump(SETTINGS);

    /** Splits frontmatter and body; a file without frontmatter has an empty map and the whole text as body. */
    public static MarkdownPage parse(String text) {
        Objects.requireNonNull(text, "text is null");
        text = text.replace("\r\n", "\n");
        if (!text.startsWith(FENCE)) {
            return new MarkdownPage(new LinkedHashMap<String, Object>(), text);
        }
        int end = text.indexOf("\n---\n", FENCE.length() - 1);
        if (end < 0)
            throw new IllegalArgumentException("frontmatter not closed");

        String yaml = text.substring(FENCE.length(), end + 1);

        Object loaded = new Load(LoadSettings.builder()
                                             .build()).loadFromString(yaml);
        if (null == loaded)
            loaded = new LinkedHashMap<String, Object>();

        if (!(loaded instanceof Map<?, ?> map))
            throw new IllegalArgumentException("frontmatter is not a mapping");

        Map<String, Object> fm = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            fm.put(String.valueOf(k), v);
        });
        return new MarkdownPage(fm, text.substring(end + "\n---\n".length()));
    }

    /** The page as text: "---", the frontmatter as block-style YAML, "---", the body. */
    public String render() {
        if (frontmatter.isEmpty())
            return body;

        return FENCE + DUMP.dumpToString(frontmatter()) + FENCE + body;
    }

    /** The frontmatter's "type" value, or "" when absent. */
    public String type() {
        Object t = frontmatter.get("type");
        return t == null ? "" : t.toString();
    }
}
