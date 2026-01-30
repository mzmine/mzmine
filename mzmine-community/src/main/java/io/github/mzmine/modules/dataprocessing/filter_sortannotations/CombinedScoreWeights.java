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

package io.github.mzmine.modules.dataprocessing.filter_sortannotations;

import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary.Scores;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummarySortConfig;
import org.jetbrains.annotations.NotNull;

/**
 * The weights to combine all scores to a meta-score
 *
 * @param mz mz is already transformed into a 0-1 score by using an MZTolerance as the maximum
 *           distance. See {@link AnnotationSummarySortConfig}
 */
public record CombinedScoreWeights(double mz, double rt, double ri, double ccs, double ms2,
                                   double isotopes) {

  public static final double DEFAULT_MS2 = 6.0;
  public static final double DEFAULT_ISOTOPES = 4.0;
  public static final double DEFAULT_MZ = 3.0;
  public static final double DEFAULT_RI = 2;
  public static final double DEFAULT_RT = 1.0;
  public static final double DEFAULT_CCS = 1.0;

  public static final CombinedScoreWeights DEFAULT_WEIGHTS = new CombinedScoreWeights(DEFAULT_MZ,
      DEFAULT_RT, DEFAULT_RI, DEFAULT_CCS, DEFAULT_MS2, DEFAULT_ISOTOPES);

  /**
   * @return Weight for type
   */
  public double get(@NotNull Scores type) {
    return switch (type) {
      case MZ -> mz;
      case RT -> rt;
      case RI -> ri;
      case CCS -> ccs;
      case MS2 -> ms2;
      case ISOTOPE -> isotopes;
      case COMBINED -> 0; // is calculated by this but never uses a weight itself
    };
  }
}
