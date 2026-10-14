package ekw.pipeline;

import ekw.analysis.Facts;
import ekw.core.MarkdownPage;
import ekw.core.Todo;

/**
 * Minimal "Java Type" fact pages for spike S1, rendered by code from facts (no model). Part C replaces this with the
 * full templates of stage 2, section 2.9.
 *
 * <p>Chapter 11 of the development guide.
 */
public final class SimpleFactPages {

    /** @param commitTime ISO-8601 commit time; used as generated.at so regeneration gives the same bytes */
    public SimpleFactPages(Facts.FactsStore facts, String system, String commit, String commitTime) {
        // TODO (chapter 11): keep the arguments.
    }

    /**
     * The page for one type. Frontmatter (in this order): type "Java Type", title (simple name), description (the
     * Javadoc's first sentence, else "Java type <qualified name>."), generated {by: "ekw-analyzer/0.1", at:
     * commitTime}, status "stable", x-ekw {layer: facts, system, commit, symbol, span: {path, start, end}}.
     * Body: "# <simple name>", then "## Members", "## Supertypes", "## Implementations", "## Used by", "## Calls"
     * (the chapter lists the line formats).
     */
    public MarkdownPage typePage(Facts.SymbolId type) {
        throw Todo.chapter(11, "SimpleFactPages.typePage");
    }

    /** "java:demo.Circle" -> "demo.Circle.md". */
    public static String fileName(Facts.SymbolId type) {
        throw Todo.chapter(11, "SimpleFactPages.fileName");
    }
}
