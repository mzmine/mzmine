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

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The fraction of data points that continue in the next scan, as a function of their intensity. An
 * ion yields a data point of similar m/z and intensity in the next scan, noise does so only by
 * chance. A data point continues if the next scan has a data point within the near window of its
 * m/z and within the intensity jump. The chance hits are measured with control windows of the same
 * width that are shifted away from the m/z, the signal fraction is
 * {@code (near - control) / (1 - control)}.
 * <p>
 * The noise level is the lowest intensity from which at least half of the data points are signals,
 * see {@link #intensityAtSignalFraction(double)}. Data with a noise filter in the mass detection
 * may already have signals at their lowest intensity, then the noise level is the lowest
 * intensity.
 */
final class SignalPersistenceProfile {

  // decision: 10 bins per decade resolve the noise level well enough for factors of 2 and more
  static final int BINS_PER_DECADE = 10;
  // log10 of the lowest bin, lower intensities go to the first bin
  private static final int MIN_LOG10 = -3;
  private static final int NUM_BINS = 25 * BINS_PER_DECADE;
  // decision: the intensity jump factor of the builder, a trace takes such neighbors without cost
  static final double INTENSITY_JUMP = FastChromatogramBuilderOptions.DEFAULT.intensityJumpFactor();
  // decision: the control windows start at twice the near window, the scatter of signals is
  // negligible there. Both control windows together have the width of the near window.
  static final double CONTROL_START = 2d;
  static final int MIN_DATA_POINTS_PER_BIN = 200;

  private final @NotNull MZTolerance nearWindow;
  private final long[] counts = new long[NUM_BINS];
  private final long[] nearHits = new long[NUM_BINS];
  private final long[] controlHits = new long[NUM_BINS];
  private double minIntensity = Double.POSITIVE_INFINITY;
  private int numScanPairs = 0;

  // scan buffers
  private double[] previousMzs = new double[1024];
  private double[] previousIntensities = new double[1024];
  private int numPrevious = 0;
  private double[] currentMzs = new double[1024];
  private double[] currentIntensities = new double[1024];
  private int numCurrent = 0;

  /**
   * @param nearWindow max m/z distance of the same signal in consecutive scans
   */
  SignalPersistenceProfile(@NotNull MZTolerance nearWindow) {
    this.nearWindow = nearWindow;
  }

  /**
   * Adds the data points of pairs of consecutive scans.
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
        addPair();
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
    if (!sorted) {
      sortCurrent();
    }
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
   * Each data point of the previous scan looks for data points of the current scan within the near
   * and the control windows. Both scans are sorted by m/z, a lower bound pointer moves along.
   */
  private void addPair() {
    int lower = 0;
    for (int i = 0; i < numPrevious; i++) {
      final double mz = previousMzs[i];
      final double intensity = previousIntensities[i];
      final double window = nearWindow.getMzToleranceForMass(mz);
      // one control window on each side, each as wide as half the near window
      final double controlFrom = CONTROL_START * window;
      final double controlTo = controlFrom + window;
      while (lower < numCurrent && currentMzs[lower] < mz - controlTo) {
        lower++;
      }
      boolean near = false;
      boolean control = false;
      for (int j = lower; j < numCurrent; j++) {
        final double distance = currentMzs[j] - mz;
        if (distance > controlTo) {
          break;
        }
        final double ratio = currentIntensities[j] / intensity;
        if (ratio > INTENSITY_JUMP || ratio < 1d / INTENSITY_JUMP) {
          continue;
        }
        final double absolute = Math.abs(distance);
        if (absolute <= window) {
          near = true;
        } else if (absolute >= controlFrom) {
          control = true;
        }
      }
      final int bin = bin(intensity);
      counts[bin]++;
      if (near) {
        nearHits[bin]++;
      }
      if (control) {
        controlHits[bin]++;
      }
      minIntensity = Math.min(minIntensity, intensity);
    }
  }

  static int bin(double intensity) {
    final int bin = (int) Math.floor((Math.log10(intensity) - MIN_LOG10) * BINS_PER_DECADE);
    return Math.clamp(bin, 0, NUM_BINS - 1);
  }

  static double binLowerEdge(int bin) {
    return Math.pow(10d, MIN_LOG10 + (double) bin / BINS_PER_DECADE);
  }

  static double binCenter(int bin) {
    return Math.pow(10d, MIN_LOG10 + (bin + 0.5d) / BINS_PER_DECADE);
  }

  /**
   * @return the bins with at least {@link #MIN_DATA_POINTS_PER_BIN} data points in ascending
   * intensity
   */
  @NotNull List<IntensityBin> bins() {
    final List<IntensityBin> bins = new ArrayList<>();
    for (int b = 0; b < NUM_BINS; b++) {
      if (counts[b] < MIN_DATA_POINTS_PER_BIN) {
        continue;
      }
      final double near = (double) nearHits[b] / counts[b];
      final double control = (double) controlHits[b] / counts[b];
      final double signal = control >= 1d ? 0d : Math.max(0d, (near - control) / (1d - control));
      bins.add(new IntensityBin(b, counts[b], near, control, signal));
    }
    return bins;
  }

  /**
   * Searches the bins in descending intensity for the first two consecutive bins below the signal
   * fraction, single bins scatter. The intensity is interpolated on the log scale between the
   * centers of the upper of both bins and the bin above it.
   * <p>
   * decision: from the top, the level holds for all higher intensities. Persistent data points far
   * below the noise, e.g., the tails of ions in synthetic data without detection threshold, do not
   * set the level.
   *
   * @return the lowest intensity from which the data points have at least this signal fraction, the
   * lowest intensity of all data points if all bins reach it, NaN if the most intense bins do not
   * reach it
   */
  double intensityAtSignalFraction(double fraction) {
    final List<IntensityBin> bins = bins();
    for (int k = bins.size() - 1; k >= 0; k--) {
      if (bins.get(k).signalFraction() >= fraction) {
        continue;
      }
      if (k > 0 && bins.get(k - 1).signalFraction() >= fraction) {
        // a single bin below
        continue;
      }
      if (k == bins.size() - 1) {
        return Double.NaN;
      }
      // the bin above may be a single bin below the fraction
      int aboveIndex = k + 1;
      while (aboveIndex < bins.size() && bins.get(aboveIndex).signalFraction() < fraction) {
        aboveIndex++;
      }
      if (aboveIndex == bins.size()) {
        return Double.NaN;
      }
      final IntensityBin below = bins.get(k);
      final IntensityBin above = bins.get(aboveIndex);
      final double logBelow = Math.log10(binCenter(below.bin()));
      final double logAbove = Math.log10(binCenter(above.bin()));
      final double t = (fraction - below.signalFraction()) / (above.signalFraction()
          - below.signalFraction());
      return Math.pow(10d, logBelow + Math.clamp(t, 0d, 1d) * (logAbove - logBelow));
    }
    if (bins.isEmpty()) {
      return Double.NaN;
    }
    return Math.max(minIntensity, binLowerEdge(bins.getFirst().bin()));
  }

  /**
   * @return the lowest intensity of all data points, infinity if there were none
   */
  double getMinIntensity() {
    return minIntensity;
  }

  int getNumScanPairs() {
    return numScanPairs;
  }

  long getNumDataPoints() {
    long sum = 0;
    for (final long count : counts) {
      sum += count;
    }
    return sum;
  }

  /**
   * @param bin            index of the bin, see {@link #binLowerEdge(int)}
   * @param numDataPoints  data points of the bin
   * @param nearFraction   fraction of data points with a hit in the near window
   * @param controlFraction fraction of data points with a hit in the control windows
   * @param signalFraction estimated fraction of signals
   */
  record IntensityBin(int bin, long numDataPoints, double nearFraction, double controlFraction,
                      double signalFraction) {

    double lowerEdge() {
      return binLowerEdge(bin);
    }
  }
}
