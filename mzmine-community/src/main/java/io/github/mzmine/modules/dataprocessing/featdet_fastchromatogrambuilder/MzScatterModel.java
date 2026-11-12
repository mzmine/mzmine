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
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import org.apache.commons.math3.special.Erf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The m/z scatter of signals as a function of m/z and intensity, fitted to m/z deviations in ppm.
 * The samples are split into m/z bins and, within each m/z bin, into intensity bins. A gaussian
 * with a uniform background is fitted to the absolute deviations of each cell, the background
 * absorbs unrelated signals. Weak signals scatter more, the cells keep this heavy tail when the
 * required tolerance of an m/z bin is computed over all its cells.
 */
final class MzScatterModel {

  private static final int MIN_SAMPLES = 1000;
  private static final int MAX_SAMPLES = 500_000;
  private static final int MIN_SAMPLES_PER_CELL = 250;
  private static final int MAX_MZ_BINS = 8;
  private static final int MAX_INTENSITY_BINS = 6;
  private static final int HISTOGRAM_BINS = 200;
  // window of the fit as multiple of the scatter of all deviations
  private static final double WINDOW_SCALES = 16d;
  private static final int MAX_WINDOW_ROUNDS = 5;
  private static final int MAX_ITERATIONS = 200;

  private final @NotNull double[] binMzs;
  private final @NotNull double[] binWindows;
  private final @NotNull ScatterFit[][] cells;
  private final @NotNull ScatterFit all;
  private final double pointScale;
  private final int numSamples;

  private MzScatterModel(@NotNull double[] binMzs, @NotNull double[] binWindows,
      @NotNull ScatterFit[][] cells, @NotNull ScatterFit all, double pointScale, int numSamples) {
    this.binMzs = binMzs;
    this.binWindows = binWindows;
    this.cells = cells;
    this.all = all;
    this.pointScale = pointScale;
    this.numSamples = numSamples;
  }

  /**
   * Fits with a window chosen from the data. It starts with a multiple of the median deviation and
   * shrinks to a multiple of the fitted scatter, unrelated neighbors may dominate the median.
   *
   * @param ppm         m/z deviation of each sample in ppm
   * @param mzs         m/z of each sample
   * @param intensities intensity of each sample
   * @param n           number of samples
   * @param pointScale  scatter of a single data point relative to the scatter of the samples, e.g.,
   *                    1/sqrt(2) for differences of two data points
   * @return the model or null if there are too few samples
   */
  @Nullable
  static MzScatterModel fit(@NotNull float[] ppm, @NotNull float[] mzs,
      @NotNull float[] intensities, int n, double pointScale) {
    final int[] used = subsample(n);
    if (used.length < MIN_SAMPLES) {
      return null;
    }
    final float[] absolute = new float[used.length];
    for (int k = 0; k < used.length; k++) {
      absolute[k] = Math.abs(ppm[used[k]]);
    }
    Arrays.sort(absolute);
    final double medianAbsolute = absolute[used.length / 2];
    if (!(medianAbsolute > 0d)) {
      return null;
    }
    double window = WINDOW_SCALES * medianAbsolute / 0.6745;
    for (int round = 0; round < MAX_WINDOW_ROUNDS; round++) {
      final double current = window;
      final ScatterFit fit = fitCell(ppm, mzs, used, 0, used.length, _ -> current);
      final double next = WINDOW_SCALES * fit.sigma();
      if (!(next < 0.95 * window)) {
        break;
      }
      window = next;
    }
    final double finalWindow = window;
    return fit(ppm, mzs, intensities, n, pointScale, _ -> finalWindow);
  }

  /**
   * @param windowForMz max deviation in ppm at an m/z, larger deviations are ignored. Use the
   *                    tolerance that limited the deviations, then the background is uniform within
   *                    the window.
   * @see #fit(float[], float[], float[], int, double)
   */
  @Nullable
  static MzScatterModel fit(@NotNull float[] ppm, @NotNull float[] mzs,
      @NotNull float[] intensities, int n, double pointScale,
      @NotNull DoubleUnaryOperator windowForMz) {
    final int[] used = subsample(n);
    int numWithin = 0;
    final int[] within = new int[used.length];
    for (final int i : used) {
      if (Math.abs(ppm[i]) < windowForMz.applyAsDouble(mzs[i])) {
        within[numWithin++] = i;
      }
    }
    if (numWithin < MIN_SAMPLES) {
      return null;
    }
    IntArrays.quickSort(within, 0, numWithin, (a, b) -> {
      final int result = Float.compare(mzs[a], mzs[b]);
      return result != 0 ? result : Integer.compare(a, b);
    });

    final ScatterFit all = fitCell(ppm, mzs, within, 0, numWithin, windowForMz);
    final int intensityBins = Math.max(1,
        Math.min(MAX_INTENSITY_BINS, numWithin / (MIN_SAMPLES_PER_CELL * MAX_MZ_BINS)));
    final int mzBins = Math.max(1,
        Math.min(MAX_MZ_BINS, numWithin / (MIN_SAMPLES_PER_CELL * intensityBins)));

    final double[] binMzs = new double[mzBins];
    final double[] binWindows = new double[mzBins];
    final ScatterFit[][] cells = new ScatterFit[mzBins][];
    final int[] byIntensity = Arrays.copyOf(within, numWithin);
    for (int b = 0; b < mzBins; b++) {
      final int from = (int) ((long) numWithin * b / mzBins);
      final int to = (int) ((long) numWithin * (b + 1) / mzBins);
      binMzs[b] = mzs[within[(from + to) >>> 1]];
      final double binWindow = windowForMz.applyAsDouble(binMzs[b]);
      binWindows[b] = binWindow;
      IntArrays.quickSort(byIntensity, from, to, (x, y) -> {
        final int result = Float.compare(intensities[x], intensities[y]);
        return result != 0 ? result : Integer.compare(x, y);
      });
      cells[b] = new ScatterFit[intensityBins];
      for (int c = 0; c < intensityBins; c++) {
        final int cellFrom = from + (int) ((long) (to - from) * c / intensityBins);
        final int cellTo = from + (int) ((long) (to - from) * (c + 1) / intensityBins);
        cells[b][c] = fitCell(ppm, mzs, byIntensity, cellFrom, cellTo, _ -> binWindow);
      }
    }
    return new MzScatterModel(binMzs, binWindows, cells, all, pointScale, n);
  }

  /**
   * @return indices of a regular subsample, keeps the fit fast. The samples are in scan order.
   */
  @NotNull
  private static int[] subsample(int n) {
    final int stride = Math.max(1, (n + MAX_SAMPLES - 1) / MAX_SAMPLES);
    final int[] used = new int[(n + stride - 1) / stride];
    for (int k = 0; k < used.length; k++) {
      used[k] = k * stride;
    }
    return used;
  }

  int numBins() {
    return binMzs.length;
  }

  double binMz(int bin) {
    return binMzs[bin];
  }

  @NotNull ScatterFit[] cells(int bin) {
    return cells[bin];
  }

  /**
   * @return scatter of all samples within the window, in ppm
   */
  double sigmaPpm() {
    return all.sigma();
  }

  double signalFraction() {
    return all.signalFraction();
  }

  /**
   * @return the fit window of the first bin in ppm
   */
  double windowPpm() {
    return binWindows.length == 0 ? Double.NaN : binWindows[0];
  }

  int numSamples() {
    return numSamples;
  }

  /**
   * Only reliable cells are used, see {@link ScatterFit#isReliable(double)}.
   *
   * @return distance in ppm that contains the coverage fraction of the signal data points of all
   * cells of the bin around their true m/z, NaN if the bin has no reliable cell
   */
  double requiredPpm(int bin, double coverage) {
    final ScatterFit[] binCells = cells[bin];
    final double window = binWindows[bin];
    double totalSignal = 0d;
    for (final ScatterFit cell : binCells) {
      if (cell.isReliable(window)) {
        totalSignal += cell.numSignal();
      }
    }
    if (!(totalSignal > 0d)) {
      return Double.NaN;
    }
    double lo = 0d;
    double hi = window;
    for (int it = 0; it < 60; it++) {
      final double mid = 0.5 * (lo + hi);
      double covered = 0d;
      for (final ScatterFit cell : binCells) {
        if (cell.isReliable(window)) {
          // P(|x| <= t) of a gaussian with the scatter of a data point
          covered += cell.numSignal() * Erf.erf(mid / (cell.sigma() * pointScale * Math.sqrt(2d)));
        }
      }
      if (covered / totalSignal >= coverage) {
        hi = mid;
      } else {
        lo = mid;
      }
    }
    return hi;
  }

  /**
   * @return the required tolerance of each bin, NaN for bins without reliable cells
   */
  @NotNull double[] requiredPpm(double coverage) {
    final double[] required = new double[binMzs.length];
    for (int b = 0; b < required.length; b++) {
      required[b] = requiredPpm(b, coverage);
    }
    return required;
  }

  /**
   * @return the highest requirement of all bins in ppm, NaN if no bin has reliable cells
   */
  double maxRequiredPpm(double coverage) {
    double max = Double.NaN;
    for (final double required : requiredPpm(coverage)) {
      if (Double.isFinite(required) && !(required <= max)) {
        max = required;
      }
    }
    return max;
  }

  /**
   * @return the tolerance that covers the requirement of all bins with reliable cells or null if
   * there is none
   */
  @Nullable MZTolerance tolerance(double coverage) {
    final double[] required = requiredPpm(coverage);
    int numValid = 0;
    for (final double value : required) {
      if (Double.isFinite(value)) {
        numValid++;
      }
    }
    if (numValid == 0) {
      return null;
    }
    final double[] validMzs = new double[numValid];
    final double[] validRequired = new double[numValid];
    int k = 0;
    for (int b = 0; b < required.length; b++) {
      if (Double.isFinite(required[b])) {
        validMzs[k] = binMzs[b];
        validRequired[k++] = required[b];
      }
    }
    return envelope(validMzs, validRequired);
  }

  /**
   * The {@link MZTolerance} uses the maximum of an absolute and a relative tolerance. Chooses the
   * combination that covers all bins with the lowest sum of log ratios between tolerance and
   * requirement. Candidates for the absolute part are zero and the requirement of each bin, the
   * relative part then covers all bins above the absolute part.
   */
  @NotNull
  static MZTolerance envelope(@NotNull double[] binMzs, @NotNull double[] binRequiredPpm) {
    final int n = binMzs.length;
    final double[] requiredDa = new double[n];
    for (int b = 0; b < n; b++) {
      requiredDa[b] = binRequiredPpm[b] * binMzs[b] / 1E6;
    }
    double bestAbsolute = 0d;
    double bestPpm = 0d;
    double bestCost = Double.POSITIVE_INFINITY;
    for (int candidate = -1; candidate < n; candidate++) {
      final double absolute = candidate < 0 ? 0d : requiredDa[candidate];
      double ppm = 0d;
      for (int b = 0; b < n; b++) {
        if (requiredDa[b] > absolute) {
          ppm = Math.max(ppm, binRequiredPpm[b]);
        }
      }
      double cost = 0d;
      for (int b = 0; b < n; b++) {
        final double tolerance = Math.max(absolute, ppm * binMzs[b] / 1E6);
        cost += Math.log(tolerance / requiredDa[b]);
      }
      if (cost < bestCost - 1E-9) {
        bestCost = cost;
        bestAbsolute = absolute;
        bestPpm = ppm;
      }
    }
    return new MZTolerance(bestAbsolute, bestPpm);
  }

  @NotNull
  private static ScatterFit fitCell(@NotNull float[] ppm, @NotNull float[] mzs,
      @NotNull int[] indices, int from, int to, @NotNull DoubleUnaryOperator windowForMz) {
    // the histogram spans the largest window, each sample is limited by its own window
    double window = 0d;
    for (int k = from; k < to; k++) {
      window = Math.max(window, windowForMz.applyAsDouble(mzs[indices[k]]));
    }
    if (!(window > 0d)) {
      return new ScatterFit(Double.NaN, 0d, 0);
    }
    final double binWidth = window / HISTOGRAM_BINS;
    final double[] histogram = new double[HISTOGRAM_BINS];
    int count = 0;
    for (int k = from; k < to; k++) {
      final int i = indices[k];
      final double absolute = Math.abs(ppm[i]);
      if (absolute >= windowForMz.applyAsDouble(mzs[i])) {
        continue;
      }
      histogram[Math.min(HISTOGRAM_BINS - 1, (int) (absolute / binWidth))]++;
      count++;
    }
    return fitHistogram(histogram, binWidth, count);
  }

  /**
   * Expectation maximization of a folded gaussian centered at zero plus a uniform background.
   */
  @NotNull
  static ScatterFit fitHistogram(@NotNull double[] histogram, double binWidth, int count) {
    final int numBins = histogram.length;
    final double window = binWidth * numBins;
    if (count == 0) {
      return new ScatterFit(window, 0d, 0);
    }
    // start with the robust scale of the histogram
    double cumulative = 0d;
    int medianBin = 0;
    for (int j = 0; j < numBins; j++) {
      cumulative += histogram[j];
      if (cumulative >= 0.5 * count) {
        medianBin = j;
        break;
      }
    }
    double sigma = Math.max(binWidth, (medianBin + 0.5) * binWidth / 0.6745);
    double signal = 0.5;
    final double backgroundMass = binWidth / window;
    for (int it = 0; it < MAX_ITERATIONS; it++) {
      double sumR = 0d;
      double sumRxx = 0d;
      for (int j = 0; j < numBins; j++) {
        if (histogram[j] == 0d) {
          continue;
        }
        final double x = (j + 0.5) * binWidth;
        final double z = x / sigma;
        // probability mass of the folded gaussian in this bin
        final double gaussianMass =
            signal * 2d * binWidth / (sigma * Math.sqrt(2d * Math.PI)) * Math.exp(-0.5 * z * z);
        final double r = gaussianMass / (gaussianMass + (1d - signal) * backgroundMass);
        sumR += histogram[j] * r;
        sumRxx += histogram[j] * r * (x * x + binWidth * binWidth / 12d);
      }
      if (!(sumR > 0d)) {
        return new ScatterFit(window, 0d, count);
      }
      final double newSigma = Math.max(binWidth, Math.sqrt(sumRxx / sumR));
      final double newSignal = sumR / count;
      final boolean converged =
          Math.abs(newSigma - sigma) < 1E-6 * sigma && Math.abs(newSignal - signal) < 1E-6;
      sigma = newSigma;
      signal = newSignal;
      if (converged) {
        break;
      }
    }
    return new ScatterFit(sigma, signal, count);
  }
}
