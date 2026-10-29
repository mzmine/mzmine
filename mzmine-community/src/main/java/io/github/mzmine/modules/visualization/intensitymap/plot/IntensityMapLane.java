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

package io.github.mzmine.modules.visualization.intensitymap.plot;

/**
 * One m/z window of {@link IntensityMapLanes}.
 *
 * @param low  lower end of the m/z window
 * @param high upper end of the m/z window
 */
record IntensityMapLane(double low, double high) {

  boolean contains(final double mz) {
    return mz >= low && mz <= high;
  }

  double width() {
    // assumption: a single m/z bin spans a narrow band around its value
    return high > low ? high - low : Math.max(Math.abs(low) * 1e-6, 1e-6);
  }

  double center() {
    return (low + high) / 2;
  }

  /**
   * @return share of the lane height below the m/z
   */
  double share(final double mz) {
    return high > low ? (mz - low) / (high - low) : 0.5;
  }

  double at(final double share) {
    return low + share * (high - low);
  }
}
