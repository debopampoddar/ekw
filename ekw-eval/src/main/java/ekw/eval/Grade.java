package ekw.eval;

import ekw.eval.Eval.Setup;

import java.util.Objects;

/**
 * Judge scores, 0..1 unless stated.
 */
public record Grade(String questionId, Setup setup, int repeat, double keyPointRecall, double evidenceRecall,
                     double citationPrecision, int unsupportedClaims, boolean notFoundCorrect,
                     boolean violatedMustNotSay, String judgeModel, String rationale) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String questionId;
        private Setup setup;
        private int repeat;
        private double keyPointRecall;
        private double evidenceRecall;
        private double citationPrecision;
        private int unsupportedClaims;
        private boolean notFoundCorrect;
        private boolean violatedMustNotSay;
        private String judgeModel;
        private String rationale;

        private Builder() {
        }

        public Builder questionId(String questionId) {
            this.questionId = questionId;
            return this;
        }

        public Builder setup(Setup setup) {
            this.setup = setup;
            return this;
        }

        public Builder repeat(int repeat) {
            this.repeat = repeat;
            return this;
        }

        public Builder keyPointRecall(double keyPointRecall) {
            this.keyPointRecall = keyPointRecall;
            return this;
        }

        public Builder evidenceRecall(double evidenceRecall) {
            this.evidenceRecall = evidenceRecall;
            return this;
        }

        public Builder citationPrecision(double citationPrecision) {
            this.citationPrecision = citationPrecision;
            return this;
        }

        public Builder unsupportedClaims(int unsupportedClaims) {
            this.unsupportedClaims = unsupportedClaims;
            return this;
        }

        public Builder notFoundCorrect(boolean notFoundCorrect) {
            this.notFoundCorrect = notFoundCorrect;
            return this;
        }

        public Builder violatedMustNotSay(boolean violatedMustNotSay) {
            this.violatedMustNotSay = violatedMustNotSay;
            return this;
        }

        public Builder judgeModel(String judgeModel) {
            this.judgeModel = judgeModel;
            return this;
        }

        public Builder rationale(String rationale) {
            this.rationale = rationale;
            return this;
        }

        public Grade build() {
            return new Grade(Objects.requireNonNull(questionId, "questionId"), Objects.requireNonNull(setup, "setup"),
                    repeat, keyPointRecall, evidenceRecall, citationPrecision, unsupportedClaims, notFoundCorrect,
                    violatedMustNotSay, Objects.requireNonNull(judgeModel, "judgeModel"),
                    Objects.requireNonNull(rationale, "rationale"));
        }
    }
}
