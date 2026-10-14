package ekw.eval;

import ekw.core.Todo;
import ekw.query.AnswerLoop;

import java.nio.file.Path;
import java.util.List;

/**
 * The RAW_AGENT baseline's tools (design: stage 2, section 11.2): grep, glob and read_file over the raw checkout, with
 * no wiki. Limits keep it comparable with our tools: grep at most 50 hits, glob at most 200 paths, read_file at most
 * 200 lines per call. Nothing outside the checkout can be read.
 *
 * <p>Chapter 11 of the development guide.
 */
public final class RawAgentTools {

    public RawAgentTools(Path checkout) {
        // TODO (chapter 11): keep the checkout folder.
    }

    /** grep, glob, read_file, in this order. */
    public List<AnswerLoop.Tool> tools() {
        throw Todo.chapter(11, "RawAgentTools.tools");
    }
}
