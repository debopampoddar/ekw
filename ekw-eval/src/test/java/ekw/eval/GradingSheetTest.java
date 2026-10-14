package ekw.eval;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ch12")
class GradingSheetTest {

    private static GradingSheet.Answer answer(String id, String setup, String cost) {
        return new GradingSheet.Answer(id, setup, "claude-sonnet-5-5", "Question " + id + "?",
                List.of("first point", "second point"), "An answer\nover two lines.", new BigDecimal(cost), 1000, 200, 3);
    }

    private static final List<GradingSheet.Answer> ANSWERS = List.of(
            answer("A01", "WIKI_TOOLS", "0.04"), answer("A02", "WIKI_TOOLS", "0.06"), answer("A01", "RAW_AGENT", "0.10"));

    /** Fills the empty score cells in order with the given scores. */
    private static String fill(String sheet, String... scores) {
        String out = sheet;
        for (String s : scores) out = out.replaceFirst("\\|\\s*\\|\\n", "| " + s + " |\n");
        return out;
    }

    @Test
    void rendersOneBlockPerAnswerWithEmptyScoreCells() {
        String sheet = GradingSheet.render(ANSWERS);
        assertThat(sheet).contains("## A01 · WIKI_TOOLS · claude-sonnet-5-5")
                .contains("<!-- answer: A01|WIKI_TOOLS|claude-sonnet-5-5 -->")
                .contains("**Question:** Question A01?")
                .contains("> An answer\n> over two lines.")
                .contains("| # | Key point | Score (1, 0.5, 0) |")
                .contains("| 1 | first point | |\n")
                .contains("Cost $0.04 · 1000 in / 200 out · 3 tool calls");
        assertThat(sheet.split("<!-- answer: ", -1)).hasSize(4);
    }

    @Test
    void summarizesPerSetupAndModel() {
        String filled = fill(GradingSheet.render(ANSWERS), "1", "0.5", "1", "1", "0", "0.5");
        List<GradingSheet.Summary> summary = GradingSheet.summarize(filled, ANSWERS);

        GradingSheet.Summary wiki = summary.stream().filter(s -> s.setup().equals("WIKI_TOOLS")).findFirst().orElseThrow();
        assertThat(wiki.answers()).isEqualTo(2);
        assertThat(wiki.meanScore()).isEqualTo(0.875);                  // (0.75 + 1.0) / 2
        assertThat(wiki.correct()).isEqualTo(2);                        // 0.75 counts as correct
        assertThat(wiki.totalCostUsd()).isEqualByComparingTo("0.10");
        assertThat(wiki.costPerCorrectUsd()).hasValueSatisfying(c -> assertThat(c).isEqualByComparingTo("0.05"));

        GradingSheet.Summary raw = summary.stream().filter(s -> s.setup().equals("RAW_AGENT")).findFirst().orElseThrow();
        assertThat(raw.meanScore()).isEqualTo(0.25);
        assertThat(raw.correct()).isZero();
        assertThat(raw.costPerCorrectUsd()).isEmpty();
    }

    @Test
    void refusesAnUngradedSheet() {
        String partly = fill(GradingSheet.render(ANSWERS), "1", "0.5");
        assertThatThrownBy(() -> GradingSheet.summarize(partly, ANSWERS))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("A02");
        String wrong = fill(GradingSheet.render(ANSWERS), "1", "0.5", "1", "1", "0", "maybe");
        assertThatThrownBy(() -> GradingSheet.summarize(wrong, ANSWERS)).isInstanceOf(IllegalStateException.class);
    }
}
