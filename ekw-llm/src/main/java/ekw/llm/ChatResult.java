package ekw.llm;

import ekw.llm.Models.Part;
import ekw.llm.Models.StopReason;
import ekw.llm.Models.Usage;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record ChatResult(List<Part> content, StopReason stop, Usage usage, String servedByModelId,
                          Duration latency) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<Part> content = List.of();
        private StopReason stop;
        private Usage usage;
        private String servedByModelId;
        private Duration latency;

        private Builder() {
        }

        public Builder content(List<Part> content) {
            this.content = content;
            return this;
        }

        public Builder stop(StopReason stop) {
            this.stop = stop;
            return this;
        }

        public Builder usage(Usage usage) {
            this.usage = usage;
            return this;
        }

        public Builder servedByModelId(String servedByModelId) {
            this.servedByModelId = servedByModelId;
            return this;
        }

        public Builder latency(Duration latency) {
            this.latency = latency;
            return this;
        }

        public ChatResult build() {
            return new ChatResult(content, Objects.requireNonNull(stop, "stop"),
                    Objects.requireNonNull(usage, "usage"), Objects.requireNonNull(servedByModelId, "servedByModelId"),
                    Objects.requireNonNull(latency, "latency"));
        }
    }
}
