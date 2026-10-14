package ekw.query;

import ekw.analysis.Facts;
import ekw.core.Todo;

import java.nio.file.Path;
import java.util.List;

/**
 * The four reading tools of spike S1, a subset of stage 2, section 4.2: read_index, read_page, code_facts and
 * read_code_lines. Results are plain text with locations. Part C adds evidence ids, search and the other tools.
 *
 * <p>Chapter 11 of the development guide.
 */
public final class SimpleKnowledgeTools {

    /**
     * @param wiki     the wiki folder the tools may read (pages and index.md files)
     * @param checkout the code checkout read_code_lines may read
     */
    public SimpleKnowledgeTools(Path wiki, Facts.FactsStore facts, Path checkout, String system) {
        // TODO (chapter 11): keep the arguments.
    }

    /**
     * read_index, read_page, code_facts, read_code_lines, in this order.
     */
    public List<AnswerLoop.Tool> tools() {
        throw Todo.chapter(11, "SimpleKnowledgeTools.tools");
    }
}
