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

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

/**
 * Data points of one scan that no chromatogram used, sorted by m/z. {@link ChannelDataCollector}
 * keeps them for a few scans to fill holes of intense chromatograms with a wider tolerance.
 */
final class UnusedDataPoints {

  private double[] mzs = new double[64];
  private double[] intensities = new double[64];
  private boolean[] used = new boolean[64];
  private int size = 0;
  private int scanIndex = -1;
  // target of the merge sort, as long as the data arrays
  private double[] bufferMzs = new double[64];
  private double[] bufferIntensities = new double[64];

  void reset(int scanIndex) {
    this.scanIndex = scanIndex;
    size = 0;
  }

  int scanIndex() {
    return scanIndex;
  }

  void add(double mz, double intensity) {
    if (size == mzs.length) {
      final int capacity = size * 2;
      mzs = Arrays.copyOf(mzs, capacity);
      intensities = Arrays.copyOf(intensities, capacity);
      used = Arrays.copyOf(used, capacity);
    }
    mzs[size] = mz;
    intensities[size] = intensity;
    used[size] = false;
    size++;
  }

  /**
   * Sorts by m/z, equal values keep the insertion order. Call before {@link #findClosest}.
   * <p>
   * The data points are added in a few sorted runs, the loose data points of existing and of new
   * traces of the scan are each in m/z order. A natural merge sort merges the runs in linear time
   * per merge round.
   */
  void sort() {
    int runs = 1;
    for (int i = 1; i < size; i++) {
      if (mzs[i] < mzs[i - 1]) {
        runs++;
      }
    }
    if (runs == 1) {
      return;
    }
    if (bufferMzs.length != mzs.length) {
      bufferMzs = new double[mzs.length];
      bufferIntensities = new double[mzs.length];
    }
    double[] fromMzs = mzs;
    double[] fromIntensities = intensities;
    double[] toMzs = bufferMzs;
    double[] toIntensities = bufferIntensities;
    while (runs > 1) {
      runs = 0;
      int start = 0;
      while (start < size) {
        final int middle = runEnd(fromMzs, start);
        final int end = middle < size ? runEnd(fromMzs, middle) : size;
        // stable: the left run wins ties
        int i = start;
        int j = middle;
        int k = start;
        while (i < middle && j < end) {
          if (fromMzs[j] < fromMzs[i]) {
            toMzs[k] = fromMzs[j];
            toIntensities[k++] = fromIntensities[j++];
          } else {
            toMzs[k] = fromMzs[i];
            toIntensities[k++] = fromIntensities[i++];
          }
        }
        for (; i < middle; i++, k++) {
          toMzs[k] = fromMzs[i];
          toIntensities[k] = fromIntensities[i];
        }
        for (; j < end; j++, k++) {
          toMzs[k] = fromMzs[j];
          toIntensities[k] = fromIntensities[j];
        }
        runs++;
        start = end;
      }
      final double[] swapMzs = fromMzs;
      final double[] swapIntensities = fromIntensities;
      fromMzs = toMzs;
      fromIntensities = toIntensities;
      toMzs = swapMzs;
      toIntensities = swapIntensities;
    }
    // the merged data points end in the arrays last written
    if (fromMzs != mzs) {
      bufferMzs = mzs;
      bufferIntensities = intensities;
      mzs = fromMzs;
      intensities = fromIntensities;
    }
  }

  /**
   * @return exclusive end of the sorted run starting at start
   */
  private int runEnd(@NotNull double[] values, int start) {
    int end = start + 1;
    while (end < size && values[end] >= values[end - 1]) {
      end++;
    }
    return end;
  }

  /**
   * Binary search for the window, the scan has many unused data points but few queries.
   *
   * @return the unused data point closest to mz within the tolerance and the intensity range or -1
   */
  int findClosest(double mz, double tolerance, double minIntensity, double maxIntensity) {
    int lo = 0;
    int hi = size;
    final double min = mz - tolerance;
    while (lo < hi) {
      final int mid = (lo + hi) >>> 1;
      if (mzs[mid] < min) {
        lo = mid + 1;
      } else {
        hi = mid;
      }
    }
    int best = -1;
    double bestDistance = Double.POSITIVE_INFINITY;
    final double max = mz + tolerance;
    for (int i = lo; i < size && mzs[i] <= max; i++) {
      final double distance = Math.abs(mzs[i] - mz);
      if (!used[i] && distance < bestDistance && intensities[i] >= minIntensity
          && intensities[i] <= maxIntensity) {
        best = i;
        bestDistance = distance;
      }
    }
    return best;
  }

  void markUsed(int index) {
    used[index] = true;
  }

  double mz(int index) {
    return mzs[index];
  }

  double intensity(int index) {
    return intensities[index];
  }
}
