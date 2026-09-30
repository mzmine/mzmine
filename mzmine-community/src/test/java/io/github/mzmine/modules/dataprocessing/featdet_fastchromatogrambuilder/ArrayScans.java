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

import org.jetbrains.annotations.NotNull;

/**
 * {@link MzIntensityScans} over plain arrays for tests and benchmarks.
 */
final class ArrayScans implements MzIntensityScans {

  private final double[][] mzs;
  private final double[][] intensities;
  private int index = -1;

  ArrayScans(@NotNull double[][] mzs, @NotNull double[][] intensities) {
    if (mzs.length != intensities.length) {
      throw new IllegalArgumentException("Different number of scans");
    }
    this.mzs = mzs;
    this.intensities = intensities;
  }

  @Override
  public int getNumberOfScans() {
    return mzs.length;
  }

  @Override
  public void reset() {
    index = -1;
  }

  @Override
  public boolean nextScan() {
    if (index + 1 >= mzs.length) {
      return false;
    }
    index++;
    return true;
  }

  @Override
  public int getNumberOfDataPoints() {
    return mzs[index].length;
  }

  @Override
  public double getMz(int i) {
    return mzs[index][i];
  }

  @Override
  public double getIntensity(int i) {
    return intensities[index][i];
  }
}
