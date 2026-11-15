package ekw.llm;

import ekw.core.Evidence;
import ekw.llm.Models.Effort;
import ekw.llm.Models.Message;
import ekw.llm.Models.PromptRef;
import ekw.llm.Models.Role;
import ekw.llm.Models.ToolSpec;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * cacheablePrefixParts: how many leading system/tool parts form the stable, cached prefix.
 */
public record ChatRequest(Role role, PromptRef prompt, String system, List<Message> messages, List<ToolSpec> tools,
                           Optional<Map<String, Object>> outputSchema, int maxOutputTokens, Optional<Effort> effort,
                           Evidence.Packet evidence, int cacheablePrefixParts) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Role role;
        private PromptRef prompt;
        private String system;
        private List<Message> messages = List.of();
        private List<ToolSpec> tools = List.of();
        private Optional<Map<String, Object>> outputSchema = Optional.empty();
        private int maxOutputTokens;
        private Optional<Effort> effort = Optional.empty();
        private Evidence.Packet evidence = new Evidence.Packet(List.of());
        private int cacheablePrefixParts;

        private Builder() {
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder prompt(PromptRef prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder system(String system) {
            this.system = system;
            return this;
        }

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Builder tools(List<ToolSpec> tools) {
            this.tools = tools;
            return this;
        }

        public Builder outputSchema(Optional<Map<String, Object>> outputSchema) {
            this.outputSchema = outputSchema;
            return this;
        }

        public Builder outputSchema(Map<String, Object> outputSchema) {
            this.outputSchema = Optional.ofNullable(outputSchema);
            return this;
        }

        public Builder maxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
            return this;
        }

        public Builder effort(Optional<Effort> effort) {
            this.effort = effort;
            return this;
        }

        public Builder effort(Effort effort) {
            this.effort = Optional.ofNullable(effort);
            return this;
        }

        public Builder evidence(Evidence.Packet evidence) {
            this.evidence = evidence;
            return this;
        }

        public Builder cacheablePrefixParts(int cacheablePrefixParts) {
            this.cacheablePrefixParts = cacheablePrefixParts;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(Objects.requireNonNull(role, "role"), Objects.requireNonNull(prompt, "prompt"),
                    Objects.requireNonNull(system, "system"), messages, tools, outputSchema, maxOutputTokens, effort,
                    evidence, cacheablePrefixParts);
        }
    }
}
