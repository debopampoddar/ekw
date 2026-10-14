package ekw.llm;

import ekw.core.Evidence;
import ekw.llm.Models.ModelInfo;
import ekw.llm.Models.Prices;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class TestModels {
    private TestModels() {}

    static final PriceTable PRICES = new PriceTable(Map.of(
            "claude-sonnet-5-5", new PriceTable.Entry(prices("2.00", "10.00", "0.20", "2.50"), Optional.empty()),
            "claude-opus-5-5", new PriceTable.Entry(prices("4.00", "20.00", "0.20", "5.00"), Optional.empty()),
            "claude-haiku-5-5", new PriceTable.Entry(prices("0.10", "0.50", "0.01", "0.125"),
                    Optional.of(new PriceTable.Surcharge(100_000, new BigDecimal("5"), new BigDecimal("5")))),
            "gpt-6.1-sol", new PriceTable.Entry(prices("2.00", "10.00", "0.10", "2.50"),
                    Optional.of(new PriceTable.Surcharge(272_000, new BigDecimal("2.0"), new BigDecimal("1.5"))))));

    static Prices prices(String in, String out, String cacheRead, String cacheWrite) {
        return new Prices(new BigDecimal(in), new BigDecimal(out), new BigDecimal(cacheRead), new BigDecimal(cacheWrite));
    }

    static ModelInfo info(String id, String provider) {
        return new ModelInfo(id, provider, id, 1_000_000, 128_000, PRICES.entry(id).prices(), false, List.of());
    }

    static Models.ChatRequest request(List<Models.Message> messages, List<Models.ToolSpec> tools) {
        return new Models.ChatRequest(Models.Role.ANSWERER, new Models.PromptRef("answer.system", 1), "You answer.",
                messages, tools, Optional.empty(), 2000, Optional.of(Models.Effort.HIGH), new Evidence.Packet(List.of()), 2);
    }

    static Models.ToolSpec readPage() {
        return new Models.ToolSpec("read_page", "Returns one wiki page.", Map.of(
                "type", "object", "additionalProperties", false, "required", List.of("path"),
                "properties", Map.of("path", Map.of("type", "string"))));
    }
}
