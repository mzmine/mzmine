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
 * Histogram of the data point intensities of sampled scans on a log scale, for quantiles without
 * keeping the intensities.
 */
final class IntensityHistogram {

  private static final int BINS_PER_DECADE = 100;
  // log10 of the lowest bin, lower intensities go to the first bin
  private static final int MIN_LOG10 = -3;
  private static final int NUM_BINS = 25 * BINS_PER_DECADE;

  private final long[] counts = new long[NUM_BINS];
  private long total = 0;
  private double minIntensity = Double.POSITIVE_INFINITY;

  /**
   * @param scans  the scans
   * @param stride only every stride-th scan is used, 1 uses all
   */
  void addScans(@NotNull MzIntensityScans scans, int stride) {
    final int step = Math.max(1, stride);
    scans.reset();
    int index = -1;
    while (scans.nextScan()) {
      index++;
      if (index % step != 0) {
        continue;
      }
      final int n = scans.getNumberOfDataPoints();
      for (int i = 0; i < n; i++) {
        add(scans.getIntensity(i));
      }
    }
  }

  void add(double intensity) {
    if (!(intensity > 0d) || !Double.isFinite(intensity)) {
      return;
    }
    final int bin = (int) Math.floor((Math.log10(intensity) - MIN_LOG10) * BINS_PER_DECADE);
    counts[Math.clamp(bin, 0, NUM_BINS - 1)]++;
    total++;
    minIntensity = Math.min(minIntensity, intensity);
  }

  /**
   * @return the upper edge of the bin of the quantile, 0 without data points
   */
  double quantile(double quantile) {
    if (total == 0) {
      return 0d;
    }
    final double target = quantile * total;
    long cumulative = 0;
    for (int b = 0; b < NUM_BINS; b++) {
      cumulative += counts[b];
      if (cumulative >= target && cumulative > 0) {
        return Math.pow(10d, MIN_LOG10 + (b + 1d) / BINS_PER_DECADE);
      }
    }
    return Math.pow(10d, MIN_LOG10 + (double) NUM_BINS / BINS_PER_DECADE);
  }

  long getNumDataPoints() {
    return total;
  }

  /**
   * @return the lowest intensity, 0 without data points
   */
  double getMinIntensity() {
    return total == 0 ? 0d : minIntensity;
  }
}
