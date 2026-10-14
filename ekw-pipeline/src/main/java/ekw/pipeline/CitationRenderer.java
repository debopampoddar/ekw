package ekw.pipeline;

import ekw.core.Evidence;
import ekw.core.Todo;
import ekw.pipeline.Pipeline.WriterOutput;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Turns the writer's structured output into page Markdown (design: ADR-004): evidence ids become OKF footnotes and
 * sources entries with commit-pinned locations and fingerprints. The model never writes a location.
 *
 * <p>Chapter 9 of the development guide.
 */
public final class CitationRenderer {

    /**
     * The output cited evidence ids that were not in the packet: the whole output is invalid.
     */
    public static final class UnknownEvidence extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UnknownEvidence(Set<String> ids) {
            super("unknown evidence ids: " + ids);
        }
    }

    /**
     * Body Markdown (without frontmatter) and the frontmatter's sources list, in first-citation order.
     */
    public record Rendered(String body, List<Map<String, Object>> sources) {
    }

    /**
     * @param repositoryUrlForSystem e.g. "asyncexecutorutil" -> "https://github.com/debopampoddar/asyncexecutorutil"
     */
    public CitationRenderer(Function<String, String> repositoryUrlForSystem) {
        // TODO (chapter 9): keep the function.
    }

    public Rendered render(WriterOutput out, Evidence.Packet packet) {
        throw Todo.chapter(9, "CitationRenderer.render");
    }
}
