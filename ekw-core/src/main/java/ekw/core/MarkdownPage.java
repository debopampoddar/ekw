package ekw.core;

import java.util.Map;

/**
 * A page file: YAML frontmatter between "---" lines, then the Markdown body. Keys keep their order, and keys we do not
 * know are kept (OKF requires consumers to preserve unknown keys). Output is deterministic, so regenerating a page from
 * the same inputs gives the same bytes.
 *
 * <p>Chapter 2 of the development guide.
 */
public record MarkdownPage(Map<String, Object> frontmatter, String body) {

    /** Splits frontmatter and body; a file without frontmatter has an empty map and the whole text as body. */
    public static MarkdownPage parse(String text) {
        throw Todo.chapter(2, "MarkdownPage.parse");
    }

    /** The page as text: "---", the frontmatter as block-style YAML, "---", the body. */
    public String render() {
        throw Todo.chapter(2, "MarkdownPage.render");
    }

    /** The frontmatter's "type" value, or "" when absent. */
    public String type() {
        throw Todo.chapter(2, "MarkdownPage.type");
    }
}
