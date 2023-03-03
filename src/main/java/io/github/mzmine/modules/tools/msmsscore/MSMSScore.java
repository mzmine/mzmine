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
public record MSMSScore(float explainedIntensity, float explainedSignals,
                        @NotNull Map<DataPoint, String> annotation) {

  public static final MSMSScore FAILED_FILTERS = new MSMSScore(-10, -10, Map.of());
  /**
   * Success although without formula means that minimum number of signals matched
   */
  public static final MSMSScore SUCCESS_WITHOUT_FORMULA = new MSMSScore(-2, -2, Map.of());
  public static final MSMSScore SUCCESS_WITHOUT_PRECURSOR_MZ = new MSMSScore(-3, -3, Map.of());

  public boolean isFailed() {
    return isFailed(false);
  }

  public boolean isFailed(boolean requireFormulaMatch) {
    return FAILED_FILTERS.equals(this) || (requireFormulaMatch && SUCCESS_WITHOUT_FORMULA.equals(
        this));
  }

  /**
   * All explained signals
   */
  @NotNull
  public DataPoint[] getAnnotatedDataPoints() {
    return annotation().keySet().toArray(DataPoint[]::new);
  }
}
