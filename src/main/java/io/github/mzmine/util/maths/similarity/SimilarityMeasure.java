package io.github.mzmine.util.maths.similarity;

import io.github.mzmine.util.maths.similarity.Similarity;

public enum SimilarityMeasure {
        PEARSON, COSINE_SIM, SPEARMAN, //
        LOG_RATIO_VARIANCE_1, LOG_RATIO_VARIANCE_2, //
        SLOPE, SLOPE_ALPHA_TO_ZERO;

        /**
         *
         * @param data [dp][x,y]
         */
        public double calc(double[][] data) {
            switch (this) {
                case PEARSON:
                    return Similarity.PEARSONS_CORR.calc(data);
                case COSINE_SIM:
                    return Similarity.COSINE.calc(data);
                case LOG_RATIO_VARIANCE_1:
                    return Similarity.LOG_VAR_PROPORTIONALITY.calc(data);
                case LOG_RATIO_VARIANCE_2:
                    return Similarity.LOG_VAR_CONCORDANCE.calc(data);
                case SPEARMAN:
                    return Similarity.SPEARMANS_CORR.calc(data);
                case SLOPE:
                    return Similarity.REGRESSION_SLOPE.calc(data);
                case SLOPE_ALPHA_TO_ZERO:
                    return Similarity.REGRESSION_SLOPE_SIGNIFICANCE.calc(data);
                default:
                    return Double.NaN;
            }
        }

        @Override
        public String toString() {
            return super.toString().replaceAll("_", " ");
        }

}
