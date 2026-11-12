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

import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

/**
 * The valid data points of the current scan sorted by m/z. Both passes of the
 * {@link FastChromatogramBuilder} load the scans this way, so a data point has the same index in
 * both passes.
 */
final class ScanDataPoints {

  private double[] mzs = new double[1024];
  private double[] intensities = new double[1024];
  private int size = 0;
  private long numDataPoints = 0;
  private double maxIntensity = 0d;

  /**
   * Loads the current scan of scans.
   */
  void load(@NotNull MzIntensityScans scans) {
    final int n = scans.getNumberOfDataPoints();
    if (mzs.length < n) {
      final int capacity = Math.max(n, mzs.length + (mzs.length >> 1));
      mzs = new double[capacity];
      intensities = new double[capacity];
    }
    int valid = 0;
    boolean sorted = true;
    double lastMz = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < n; i++) {
      final double mz = scans.getMz(i);
      final double intensity = scans.getIntensity(i);
      // decision: zero, negative and non finite values carry no signal and would break the
      // intensity weighted center. A missing signal is represented by the absence of a data point.
      if (!(intensity > 0d) || !Double.isFinite(intensity) || !Double.isFinite(mz)) {
        continue;
      }
      if (mz < lastMz) {
        sorted = false;
      }
      if (intensity > maxIntensity) {
        maxIntensity = intensity;
      }
      lastMz = mz;
      mzs[valid] = mz;
      intensities[valid] = intensity;
      valid++;
    }
    size = valid;
    numDataPoints += valid;
    if (!sorted) {
      // assumption: mass lists are sorted by m/z, this is only a safety net
      sortByMz();
    }
  }

  private void sortByMz() {
    final int[] order = ChannelConsolidation.identity(size);
    final double[] unsortedMzs = Arrays.copyOf(mzs, size);
    final double[] unsortedIntensities = Arrays.copyOf(intensities, size);
    IntArrays.quickSort(order, 0, size, (a, b) -> {
      final int result = Double.compare(unsortedMzs[a], unsortedMzs[b]);
      return result != 0 ? result : Integer.compare(a, b);
    });
    for (int i = 0; i < size; i++) {
      mzs[i] = unsortedMzs[order[i]];
      intensities[i] = unsortedIntensities[order[i]];
    }
  }

  /**
   * @return the m/z values, valid up to {@link #size()}, replaced by the next load
   */
  @NotNull double[] mzs() {
    return mzs;
  }

  /**
   * @return the intensities, valid up to {@link #size()}, replaced by the next load
   */
  @NotNull double[] intensities() {
    return intensities;
  }

  /**
   * @return number of valid data points of the current scan
   */
  int size() {
    return size;
  }

  /**
   * @return number of valid data points of all loaded scans
   */
  long getNumDataPoints() {
    return numDataPoints;
  }

  /**
   * @return the max intensity of all loaded valid data points
   */
  double getMaxIntensity() {
    return maxIntensity;
  }
}
