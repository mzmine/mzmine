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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

/**
 * Gaussian with a uniform background fitted to m/z deviations of one cell of the
 * {@link MzScatterModel}.
 *
 * @param sigma          standard deviation of the deviations of the signal, in ppm
 * @param signalFraction fraction of the deviations that are signal, the rest is background
 * @param count          number of fitted deviations
 */
record ScatterFit(double sigma, double signalFraction, int count) {

  // decision: the signal peak must stand out of the background and be clearly narrower than the
  // window, otherwise the cell is mainly background, e.g., sparse noise at high m/z. Both are
  // independent of the window size, unlike the signal fraction.
  private static final double MIN_PEAK_TO_BACKGROUND = 3d;
  private static final double MIN_WINDOW_SIGMAS = 4d;

  /**
   * @return estimated number of signal deviations
   */
  double numSignal() {
    return signalFraction * count;
  }

  /**
   * @param window the fit window
   * @return true if the cell has a clear signal peak
   */
  boolean isReliable(double window) {
    if (!(signalFraction > 0d) || !(sigma * MIN_WINDOW_SIGMAS <= window)) {
      return false;
    }
    // density of the folded gaussian at zero and of the uniform background
    final double peak = signalFraction * 2d / (sigma * Math.sqrt(2d * Math.PI));
    final double background = (1d - signalFraction) / window;
    return peak >= MIN_PEAK_TO_BACKGROUND * background;
  }
}
