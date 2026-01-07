/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.maths.similarity;

public enum SimilarityMeasure {
  PEARSON, COSINE_SIM, SPEARMAN, //
  LOG_RATIO_VARIANCE_1, LOG_RATIO_VARIANCE_2, //
  SLOPE, SLOPE_ALPHA_TO_ZERO;

  /**
   * @param data [dp][x,y]
   */
  public double calc(double[][] data) {
    return getSimilarityFunction().calc(data);
  }

  public double calc(double[] x, double[] y) {
    return getSimilarityFunction().calc(x, y);
  }

  public Similarity getSimilarityFunction() {
    return switch (this) {
      case PEARSON -> Similarity.PEARSONS_CORR;
      case COSINE_SIM -> Similarity.COSINE;
      case LOG_RATIO_VARIANCE_1 -> Similarity.LOG_VAR_PROPORTIONALITY;
      case LOG_RATIO_VARIANCE_2 -> Similarity.LOG_VAR_CONCORDANCE;
      case SPEARMAN -> Similarity.SPEARMANS_CORR;
      case SLOPE -> Similarity.REGRESSION_SLOPE;
      case SLOPE_ALPHA_TO_ZERO -> Similarity.REGRESSION_SLOPE_SIGNIFICANCE;
    };
  }

  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }

}
