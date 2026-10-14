package ekw.eval;

import ekw.core.Todo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Reads a question suite (design: stage 2, section 11.1; files in eval/suites/). Path aliases such as
 * "P/Task.java#L13" are expanded with the suite's "paths" map.
 *
 * <p>Chapter 11 of the development guide.
 */
public final class SuiteLoader {

    public record Suite(String name, String system, String commit, List<Eval.EvalQuestion> questions) {
    }

    private SuiteLoader() {
    }

    public static Suite load(Path file) throws IOException {
        throw Todo.chapter(11, "SuiteLoader.load");
    }

    /**
     * "P/Task.java#L13" with P -> "src/main/java/x" gives "src/main/java/x/Task.java#L13"; unknown prefixes stay.
     */
    static String expand(String location, Map<String, String> aliases) {
        throw Todo.chapter(11, "SuiteLoader.expand");
    }
}
