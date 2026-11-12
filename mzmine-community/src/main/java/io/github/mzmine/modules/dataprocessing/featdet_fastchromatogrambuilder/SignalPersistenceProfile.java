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
 * The fraction of data points that continue in a neighboring scan, as a function of their
 * intensity. An ion yields a data point of similar m/z and intensity in the next scan, noise does so
 * only by chance. A data point continues if the scan has a data point within the near window of its
 * m/z and within the intensity jump. The chance hits are measured with control windows of the same
 * width that are shifted away from the m/z, the signal fraction is
 * {@code (near - control) / (1 - control)}.
 * <p>
 * The noise level is the lowest intensity from which at least half of the data points are signals
 * that continue in the next scan, see {@link #intensityAtSignalFraction(double, Continuation)}.
 * Data with a noise filter in the mass detection may already have signals at their lowest
 * intensity, then the noise level is the lowest intensity. The signal level counts the data points
 * that continue in the previous or the next scan, see {@link Continuation}.
 */
final class SignalPersistenceProfile {

  /**
   * The neighboring scans in which a data point continues.
   */
  enum Continuation {
    /**
     * The next scan. The last data point of each trace never continues, which separates noise from
     * signals near a noise filter: noise above the filter forms traces of one or two data points.
     */
    NEXT,
    /**
     * The previous or the next scan, also the last data point of each trace continues. Narrow
     * peaks have short traces, a peak of 3 scans full width at half maximum has about 6 data points
     * above the noise, and the next scan alone does not reach nearly all data points even far above
     * the noise.
     */
    ADJACENT
  }

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
  // hits of a data point in one scan
  private static final int NEAR = 1;
  private static final int CONTROL = 2;

  private final @NotNull MZTolerance nearWindow;
  private final long[] counts = new long[NUM_BINS];
  // hits in the next scan
  private final long[] nearHits = new long[NUM_BINS];
  private final long[] controlHits = new long[NUM_BINS];
  // hits in the previous or the next scan
  private final long[] adjacentNearHits = new long[NUM_BINS];
  private final long[] adjacentControlHits = new long[NUM_BINS];
  private double minIntensity = Double.POSITIVE_INFINITY;
  private int numCenterScans = 0;

  // the scan before the center, the center scan whose data points are counted and the scan after
  private ScanBuffer before = new ScanBuffer();
  private ScanBuffer center = new ScanBuffer();
  private ScanBuffer after = new ScanBuffer();

  /**
   * @param nearWindow max m/z distance of the same signal in consecutive scans
   */
  SignalPersistenceProfile(@NotNull MZTolerance nearWindow) {
    this.nearWindow = nearWindow;
  }

  /**
   * Adds the data points of the center scans 1, 1 + stride, 1 + 2 stride, ... with the scans before
   * and after them.
   *
   * @param scans  the scans in retention time order
   * @param stride only every stride-th scan is a center scan, 1 uses all
   */
  void addScans(@NotNull MzIntensityScans scans, int stride) {
    final int step = Math.max(1, stride);
    scans.reset();
    before.index = center.index = after.index = Integer.MIN_VALUE;
    int index = -1;
    while (scans.nextScan()) {
      index++;
      // only the scans before, at and after a center scan are loaded
      final int offset = Math.floorMod(index - 1, step);
      if (offset != 0 && offset != 1 % step && offset != step - 1) {
        continue;
      }
      final ScanBuffer reused = before;
      before = center;
      center = after;
      after = reused;
      after.load(scans, index);
      if (center.index == index - 1 && before.index == index - 2
          && Math.floorMod(center.index - 1, step) == 0) {
        addCenter();
        numCenterScans++;
      }
    }
  }

  /**
   * Each data point of the center scan looks for data points of the scans before and after within
   * the near and the control windows.
   */
  private void addCenter() {
    before.lower = 0;
    after.lower = 0;
    for (int i = 0; i < center.size; i++) {
      final double mz = center.mzs[i];
      final double intensity = center.intensities[i];
      final double window = nearWindow.getMzToleranceForMass(mz);
      // one control window on each side, each as wide as half the near window
      final double controlFrom = CONTROL_START * window;
      final double controlTo = controlFrom + window;
      final int next = after.hits(mz, intensity, window, controlFrom, controlTo);
      final int adjacent = next | before.hits(mz, intensity, window, controlFrom, controlTo);
      final int bin = bin(intensity);
      counts[bin]++;
      if ((next & NEAR) != 0) {
        nearHits[bin]++;
      }
      if ((next & CONTROL) != 0) {
        controlHits[bin]++;
      }
      if ((adjacent & NEAR) != 0) {
        adjacentNearHits[bin]++;
      }
      if ((adjacent & CONTROL) != 0) {
        adjacentControlHits[bin]++;
      }
      minIntensity = Math.min(minIntensity, intensity);
    }
  }

  /**
   * The valid data points of one scan sorted by m/z.
   */
  private static final class ScanBuffer {

    private double[] mzs = new double[1024];
    private double[] intensities = new double[1024];
    private int size = 0;
    private int index = Integer.MIN_VALUE;
    // lower bound of the windows, both the queries and the data points are sorted by m/z
    private int lower = 0;

    private void load(@NotNull MzIntensityScans scans, int scanIndex) {
      index = scanIndex;
      final int n = scans.getNumberOfDataPoints();
      if (mzs.length < n) {
        mzs = new double[n];
        intensities = new double[n];
      }
      int valid = 0;
      boolean sorted = true;
      for (int i = 0; i < n; i++) {
        final double mz = scans.getMz(i);
        final double intensity = scans.getIntensity(i);
        if (!(intensity > 0d) || !Double.isFinite(intensity) || !(mz > 0d)
            || !Double.isFinite(mz)) {
          continue;
        }
        if (valid > 0 && mz < mzs[valid - 1]) {
          sorted = false;
        }
        mzs[valid] = mz;
        intensities[valid] = intensity;
        valid++;
      }
      size = valid;
      if (!sorted) {
        sort();
      }
    }

    private void sort() {
      final int[] order = ChannelConsolidation.identity(size);
      final double[] unsortedMzs = Arrays.copyOf(mzs, size);
      final double[] unsortedIntensities = Arrays.copyOf(intensities, size);
      IntArrays.quickSort(order, 0, size,
          (a, b) -> Double.compare(unsortedMzs[a], unsortedMzs[b]));
      for (int i = 0; i < size; i++) {
        mzs[i] = unsortedMzs[order[i]];
        intensities[i] = unsortedIntensities[order[i]];
      }
    }

    /**
     * @return {@link #NEAR} and {@link #CONTROL} if a data point within the intensity jump lies in
     * the near or the control windows around the m/z. Call with ascending m/z.
     */
    private int hits(double mz, double intensity, double window, double controlFrom,
        double controlTo) {
      while (lower < size && mzs[lower] < mz - controlTo) {
        lower++;
      }
      int hits = 0;
      for (int j = lower; j < size; j++) {
        final double distance = mzs[j] - mz;
        if (distance > controlTo) {
          break;
        }
        final double ratio = intensities[j] / intensity;
        if (ratio > INTENSITY_JUMP || ratio < 1d / INTENSITY_JUMP) {
          continue;
        }
        final double absolute = Math.abs(distance);
        if (absolute <= window) {
          hits |= NEAR;
        } else if (absolute >= controlFrom) {
          hits |= CONTROL;
        }
      }
      return hits;
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
      final double adjacent = signalFraction((double) adjacentNearHits[b] / counts[b],
          (double) adjacentControlHits[b] / counts[b]);
      bins.add(new IntensityBin(b, counts[b], near, control, signalFraction(near, control),
          adjacent));
    }
    return bins;
  }

  private static double signalFraction(double near, double control) {
    return control >= 1d ? 0d : Math.max(0d, (near - control) / (1d - control));
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
   * @param continuation the neighboring scans in which the signals continue
   * @return the lowest intensity from which the data points have at least this signal fraction, the
   * lowest intensity of all data points if all bins reach it, NaN if the most intense bins do not
   * reach it
   */
  double intensityAtSignalFraction(double fraction, @NotNull Continuation continuation) {
    final List<IntensityBin> bins = bins();
    final double[] signals = new double[bins.size()];
    for (int k = 0; k < signals.length; k++) {
      signals[k] = bins.get(k).signalFraction(continuation);
    }
    for (int k = bins.size() - 1; k >= 0; k--) {
      if (signals[k] >= fraction) {
        continue;
      }
      if (k > 0 && signals[k - 1] >= fraction) {
        // a single bin below
        continue;
      }
      if (k == bins.size() - 1) {
        return Double.NaN;
      }
      // the bin above may be a single bin below the fraction
      int aboveIndex = k + 1;
      while (aboveIndex < bins.size() && signals[aboveIndex] < fraction) {
        aboveIndex++;
      }
      if (aboveIndex == bins.size()) {
        return Double.NaN;
      }
      final double logBelow = Math.log10(binCenter(bins.get(k).bin()));
      final double logAbove = Math.log10(binCenter(bins.get(aboveIndex).bin()));
      final double t = (fraction - signals[k]) / (signals[aboveIndex] - signals[k]);
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

  int getNumCenterScans() {
    return numCenterScans;
  }

  long getNumDataPoints() {
    long sum = 0;
    for (final long count : counts) {
      sum += count;
    }
    return sum;
  }

  /**
   * @param bin                    index of the bin, see {@link #binLowerEdge(int)}
   * @param numDataPoints          data points of the bin
   * @param nearFraction           fraction of data points with a hit in the near window of the next
   *                               scan
   * @param controlFraction        fraction of data points with a hit in the control windows of the
   *                               next scan
   * @param signalFraction         estimated fraction of signals that continue in the next scan
   * @param adjacentSignalFraction estimated fraction of signals that continue in the previous or
   *                               the next scan
   */
  record IntensityBin(int bin, long numDataPoints, double nearFraction, double controlFraction,
                      double signalFraction, double adjacentSignalFraction) {

    double lowerEdge() {
      return binLowerEdge(bin);
    }

    double signalFraction(@NotNull Continuation continuation) {
      return switch (continuation) {
        case NEXT -> signalFraction;
        case ADJACENT -> adjacentSignalFraction;
      };
    }
  }
}
