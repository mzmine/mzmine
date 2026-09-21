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

package io.github.mzmine.modules.tools.qualityparameters;

import org.jetbrains.annotations.Nullable;

/**
 * All peak shape quality parameters of one peak, calculated from a single
 * {@link ThresholdCrossings}.
 * <p>
 * decision: an undetermined parameter is null rather than NaN so that it can be written to a
 * feature directly. Setting null removes the value of an earlier run instead of leaving a stale
 * number behind.
 *
 * @param fwhm      full width at half maximum in x units, null if it cannot be determined
 * @param asymmetry asymmetry factor at 10 % of the apex intensity, null if it cannot be determined
 * @param tailing   tailing factor at 5 % of the apex intensity, null if it cannot be determined
 */
public record PeakQuality(@Nullable Float fwhm, @Nullable Float asymmetry,
                          @Nullable Float tailing) {

  /**
   * No parameter could be determined, e.g. because the peak holds too few data points.
   */
  public static final PeakQuality EMPTY = new PeakQuality(null, null, null);

  public boolean hasFwhm() {
    return fwhm != null;
  }

  public boolean hasAsymmetry() {
    return asymmetry != null;
  }

  public boolean hasTailing() {
    return tailing != null;
  }
}
