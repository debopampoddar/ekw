package ekw.core;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ch02")
class MarkdownPageTest {

    private static String resource(String name) throws IOException {
        try (InputStream in = MarkdownPageTest.class.getResourceAsStream(name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void keepsUnknownKeysAndTheirOrder() {
        String page = """
                ---
                type: Component
                x-future-key: {a: 1}
                title: Retry policies
                ---
                # Retry policies
                """;
        MarkdownPage p = MarkdownPage.parse(page);
        assertThat(p.frontmatter().keySet()).containsExactly("type", "x-future-key", "title");
        assertThat(MarkdownPage.parse(p.render()).frontmatter()).isEqualTo(p.frontmatter());
        assertThat(p.body()).isEqualTo("# Retry policies\n");
    }

    @Test
    void renderingIsStable() {
        MarkdownPage p = new MarkdownPage(new java.util.LinkedHashMap<>(Map.of("type", "Java Type")), "body\n");
        String once = p.render();
        assertThat(MarkdownPage.parse(once).render()).isEqualTo(once);
    }

    /** The stage 1 example page: every field, including x-ekw and sources, survives a round trip. */
    @Test
    void exampleComponentPageRoundTrips() throws IOException {
        MarkdownPage p = MarkdownPage.parse(resource("/retry-policies.md"));
        assertThat(p.type()).isEqualTo("Component");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sources = (List<Map<String, Object>>) p.frontmatter().get("sources");
        assertThat(sources).extracting(s -> s.get("id")).contains("rp-linear");
        MarkdownPage again = MarkdownPage.parse(p.render());
        assertThat(again.frontmatter()).isEqualTo(p.frontmatter());
        assertThat(again.body()).isEqualTo(p.body());
    }
}
