/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.tools.msmsscore;

import io.github.mzmine.datamodel.DataPoint;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Wrapper class for a score of MS/MS evaluation, with a mapping from MS/MS data points to
 * interpreted formulas
 */
public record MSMSScore(Result result, float explainedIntensity, float explainedSignals,
                        @NotNull Map<DataPoint, String> annotation) {

  public MSMSScore(Result result) {
    this(result, -1, -1, Map.of());
  }

  public boolean isFailed(boolean requireFormulaMatch) {
    return !isSuccess(requireFormulaMatch);
  }


  public boolean isFailed() {
    return isFailed(false);
  }

  public boolean isSuccess(boolean requireFormulaMatch) {
    return result == Result.SUCCESS || (!requireFormulaMatch
        && result == Result.SUCCESS_WITHOUT_FORMULA);
  }

  public boolean isSuccess() {
    return isSuccess(false);
  }

  public enum Result {
    /**
     * Success means all requirements met
     */
    SUCCESS,
    /**
     * Means that there was no formula, so annotation was not possible
     */
    SUCCESS_WITHOUT_FORMULA, //
    FAILED, FAILED_MIN_SIGNALS, FAILED_MIN_EXPLAINED_SIGNALS, FAILED_MIN_EXPLAINED_INTENSITY
  }

  /**
   * All explained signals
   */
  @NotNull
  public DataPoint[] getAnnotatedDataPoints() {
    return annotation().keySet().toArray(DataPoint[]::new);
  }
}
