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

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collects the m/z scatter of the instrument from the data. The same ion yields a data point in
 * consecutive scans, so the m/z differences of mutual nearest neighbors in consecutive scans sample
 * the scan to scan scatter. Unrelated neighbors, e.g., noise, form a flat background, see
 * {@link MzScatterModel}.
 */
final class ConsecutiveSignalPairs {

  private static final int SPACING_SAMPLE_STEP = 8;

  private final FloatArrayList pairPpm = new FloatArrayList();
  private final FloatArrayList pairMz = new FloatArrayList();
  private final FloatArrayList pairIntensity = new FloatArrayList();
  // distances between neighbors in the same scan
  private final FloatArrayList spacingPpm = new FloatArrayList();
  private long numDataPoints = 0;
  private int numScanPairs = 0;

  // scan buffers
  private double[] previousMzs = new double[1024];
  private double[] previousIntensities = new double[1024];
  private int numPrevious = 0;
  private double[] currentMzs = new double[1024];
  private double[] currentIntensities = new double[1024];
  private int numCurrent = 0;
  private int[] nearestInCurrent = new int[1024];
  private int[] nearestInPrevious = new int[1024];

  /**
   * Adds the mutual nearest neighbors of consecutive scans.
   *
   * @param scans  the scans in retention time order
   * @param stride only every stride-th pair of consecutive scans is used, 1 uses all
   */
  void addScans(@NotNull MzIntensityScans scans, int stride) {
    final int step = Math.max(1, stride);
    scans.reset();
    boolean hasPrevious = false;
    int index = -1;
    while (scans.nextScan()) {
      index++;
      final boolean startsPair = index % step == 0;
      final boolean endsPair = hasPrevious && (index - 1) % step == 0;
      if (!startsPair && !endsPair) {
        hasPrevious = false;
        continue;
      }
      loadCurrent(scans);
      if (endsPair) {
        addPairs();
        numScanPairs++;
      }
      swapScans();
      hasPrevious = true;
    }
  }

  private void loadCurrent(@NotNull MzIntensityScans scans) {
    final int n = scans.getNumberOfDataPoints();
    if (currentMzs.length < n) {
      currentMzs = new double[n];
      currentIntensities = new double[n];
    }
    int valid = 0;
    boolean sorted = true;
    for (int i = 0; i < n; i++) {
      final double mz = scans.getMz(i);
      final double intensity = scans.getIntensity(i);
      if (!(intensity > 0d) || !Double.isFinite(intensity) || !(mz > 0d) || !Double.isFinite(mz)) {
        continue;
      }
      if (valid > 0 && mz < currentMzs[valid - 1]) {
        sorted = false;
      }
      currentMzs[valid] = mz;
      currentIntensities[valid] = intensity;
      valid++;
    }
    numCurrent = valid;
    numDataPoints += valid;
    if (!sorted) {
      sortCurrent();
    }
    // distance to the closest neighbor in the same scan, a sample is enough
    for (int i = 0; i < numCurrent; i += SPACING_SAMPLE_STEP) {
      double distance = Double.POSITIVE_INFINITY;
      if (i > 0) {
        distance = currentMzs[i] - currentMzs[i - 1];
      }
      if (i + 1 < numCurrent) {
        distance = Math.min(distance, currentMzs[i + 1] - currentMzs[i]);
      }
      if (Double.isFinite(distance)) {
        spacingPpm.add((float) (distance / currentMzs[i] * 1E6));
      }
    }
  }

  /**
   * @return median distance between neighboring data points of the same scan in ppm, NaN if there
   * are none
   */
  double medianSpacingPpm() {
    if (spacingPpm.isEmpty()) {
      return Double.NaN;
    }
    final float[] sorted = spacingPpm.toFloatArray();
    Arrays.sort(sorted);
    return sorted[sorted.length / 2];
  }

  private void sortCurrent() {
    final int[] order = ChannelConsolidation.identity(numCurrent);
    final double[] mzs = Arrays.copyOf(currentMzs, numCurrent);
    final double[] intensities = Arrays.copyOf(currentIntensities, numCurrent);
    IntArrays.quickSort(order, 0, numCurrent, (a, b) -> Double.compare(mzs[a], mzs[b]));
    for (int i = 0; i < numCurrent; i++) {
      currentMzs[i] = mzs[order[i]];
      currentIntensities[i] = intensities[order[i]];
    }
  }

  private void swapScans() {
    final double[] mzs = previousMzs;
    final double[] intensities = previousIntensities;
    previousMzs = currentMzs;
    previousIntensities = currentIntensities;
    numPrevious = numCurrent;
    currentMzs = mzs;
    currentIntensities = intensities;
    numCurrent = 0;
  }

  /**
   * Both scans are sorted by m/z, so the nearest neighbors are found with two pointers in linear
   * time.
   */
  private void addPairs() {
    if (numPrevious == 0 || numCurrent == 0) {
      return;
    }
    if (nearestInCurrent.length < numPrevious) {
      nearestInCurrent = new int[Math.max(numPrevious, nearestInCurrent.length * 2)];
    }
    if (nearestInPrevious.length < numCurrent) {
      nearestInPrevious = new int[Math.max(numCurrent, nearestInPrevious.length * 2)];
    }
    findNearest(previousMzs, numPrevious, currentMzs, numCurrent, nearestInCurrent);
    findNearest(currentMzs, numCurrent, previousMzs, numPrevious, nearestInPrevious);
    for (int i = 0; i < numPrevious; i++) {
      final int j = nearestInCurrent[i];
      if (nearestInPrevious[j] != i) {
        continue;
      }
      final double mz = previousMzs[i];
      pairPpm.add((float) ((currentMzs[j] - mz) / mz * 1E6));
      pairMz.add((float) mz);
      pairIntensity.add((float) Math.min(previousIntensities[i], currentIntensities[j]));
    }
  }

  private static void findNearest(@NotNull double[] values, int n, @NotNull double[] targets,
      int numTargets, @NotNull int[] nearest) {
    int j = 0;
    for (int i = 0; i < n; i++) {
      final double value = values[i];
      while (j + 1 < numTargets && targets[j + 1] <= value) {
        j++;
      }
      // ties go to the lower m/z to stay deterministic
      nearest[i] =
          j + 1 < numTargets && targets[j + 1] - value < Math.abs(value - targets[j]) ? j + 1 : j;
    }
  }

  int getNumPairs() {
    return pairPpm.size();
  }

  long getNumDataPoints() {
    return numDataPoints;
  }

  int getNumScanPairs() {
    return numScanPairs;
  }

  /**
   * @return the scatter model of the pairs or null if there are too few pairs of consecutive
   * signals
   */
  @Nullable MzScatterModel fitScatter() {
    // decision: a data point is compared to the center of its trace, which is averaged over many
    // data points, so the scatter of a data point is the scatter of a difference over sqrt(2)
    return MzScatterModel.fit(pairPpm.elements(), pairMz.elements(), pairIntensity.elements(),
        pairPpm.size(), 1d / Math.sqrt(2d));
  }
}
