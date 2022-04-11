/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

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
