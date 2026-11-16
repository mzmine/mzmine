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

package io.github.mzmine.modules.tools.batch_resource_estimation;

/**
 * Validation of the scaling model: the largest subset run is predicted from the smaller runs and
 * compared to its measurement.
 *
 * @param files            number of files of the largest subset
 * @param predictedHeapGB  heap predicted from the smaller subsets
 * @param measuredHeapGB   heap measured in the largest subset
 * @param predictedTempGB  temp file data predicted from the smaller subsets
 * @param measuredTempGB   temp file data measured in the largest subset
 * @param predictedSeconds time predicted from the smaller subsets
 * @param measuredSeconds  time measured in the largest subset
 */
record FitCheck(int files, double predictedHeapGB, double measuredHeapGB,
                       double predictedTempGB, double measuredTempGB, double predictedSeconds,
                       double measuredSeconds) {

  /**
   * @return the largest relative deviation of heap, temp and time, 0.1 = 10 %
   */
  double maxRelativeError() {
    return Math.max(relativeError(predictedHeapGB, measuredHeapGB),
        Math.max(relativeError(predictedTempGB, measuredTempGB),
            relativeError(predictedSeconds, measuredSeconds)));
  }

  /**
   * @return the larger relative deviation of heap and temp, 0.1 = 10 %
   */
  double maxResourceError() {
    return Math.max(relativeError(predictedHeapGB, measuredHeapGB),
        relativeError(predictedTempGB, measuredTempGB));
  }

  /**
   * @return the largest resource underestimate as a fraction of measured usage. Overestimates are
   * conservative and contribute zero; processing time does not affect resource sufficiency.
   */
  double maxResourceUnderestimation() {
    return Math.max(underestimation(predictedHeapGB, measuredHeapGB),
        underestimation(predictedTempGB, measuredTempGB));
  }

  private static double underestimation(final double predicted, final double measured) {
    return predicted < measured ? relativeError(predicted, measured) : 0;
  }

  private static double relativeError(final double predicted, final double measured) {
    // small absolute values like 1 MB of temp files would dominate a pure relative error
    final double denominator = Math.max(Math.abs(measured), ScalingFit.MIN_POSITIVE * 100);
    return Math.abs(predicted - measured) / denominator;
  }
}
