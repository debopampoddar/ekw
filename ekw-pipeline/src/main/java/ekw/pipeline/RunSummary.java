package ekw.pipeline;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record RunSummary(String runId, String sourceId, List<String> pagesAdded, List<String> pagesChanged,
                          int pagesKept, BigDecimal estimateUsd, BigDecimal spentUsd, List<Finding> findings,
                          List<String> reviewSample, boolean partial, List<String> pendingItems, boolean firstBuild,
                          boolean autoMerged) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String runId;
        private String sourceId;
        private List<String> pagesAdded = List.of();
        private List<String> pagesChanged = List.of();
        private int pagesKept;
        private BigDecimal estimateUsd = BigDecimal.ZERO;
        private BigDecimal spentUsd = BigDecimal.ZERO;
        private List<Finding> findings = List.of();
        private List<String> reviewSample = List.of();
        private boolean partial;
        private List<String> pendingItems = List.of();
        private boolean firstBuild;
        private boolean autoMerged;

        private Builder() {
        }

        public Builder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder pagesAdded(List<String> pagesAdded) {
            this.pagesAdded = pagesAdded;
            return this;
        }

        public Builder pagesChanged(List<String> pagesChanged) {
            this.pagesChanged = pagesChanged;
            return this;
        }

        public Builder pagesKept(int pagesKept) {
            this.pagesKept = pagesKept;
            return this;
        }

        public Builder estimateUsd(BigDecimal estimateUsd) {
            this.estimateUsd = estimateUsd;
            return this;
        }

        public Builder spentUsd(BigDecimal spentUsd) {
            this.spentUsd = spentUsd;
            return this;
        }

        public Builder findings(List<Finding> findings) {
            this.findings = findings;
            return this;
        }

        public Builder reviewSample(List<String> reviewSample) {
            this.reviewSample = reviewSample;
            return this;
        }

        public Builder partial(boolean partial) {
            this.partial = partial;
            return this;
        }

        public Builder pendingItems(List<String> pendingItems) {
            this.pendingItems = pendingItems;
            return this;
        }

        public Builder firstBuild(boolean firstBuild) {
            this.firstBuild = firstBuild;
            return this;
        }

        public Builder autoMerged(boolean autoMerged) {
            this.autoMerged = autoMerged;
            return this;
        }

        public RunSummary build() {
            return new RunSummary(Objects.requireNonNull(runId, "runId"), Objects.requireNonNull(sourceId, "sourceId"),
                    pagesAdded, pagesChanged, pagesKept, estimateUsd, spentUsd, findings, reviewSample, partial,
                    pendingItems, firstBuild, autoMerged);
        }
    }
}
