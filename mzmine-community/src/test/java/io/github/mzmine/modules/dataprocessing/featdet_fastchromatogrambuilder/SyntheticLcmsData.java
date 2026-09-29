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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.jetbrains.annotations.NotNull;

/**
 * Centroided LC-MS scans with known ground truth: every data point is labeled with the index of the
 * ion that produced it or {@link #NOISE}. Ions have gaussian elution profiles and an m/z error that
 * grows for weaker signals, like on FT and TOF instruments. Optionally the detector saturates, which
 * caps the intensity and shifts the m/z of intense signals, and two close ions coalesce into one
 * centroid that belongs to both ions.
 */
final class SyntheticLcmsData {

  static final int NOISE = -1;

  final double[][] mzs;
  final double[][] intensities;
  final int[][] labels;
  /**
   * The second ion of a coalesced centroid, {@link #NOISE} for all other data points. The label is
   * the more intense ion.
   */
  final int[][] sharedLabels;
  final List<Ion> ions;

  private SyntheticLcmsData(@NotNull double[][] mzs, @NotNull double[][] intensities,
      @NotNull int[][] labels, @NotNull int[][] sharedLabels, @NotNull List<Ion> ions) {
    this.mzs = mzs;
    this.intensities = intensities;
    this.labels = labels;
    this.sharedLabels = sharedLabels;
    this.ions = ions;
  }

  @NotNull
  static Builder builder(int numScans) {
    return new Builder(numScans);
  }

  @NotNull ArrayScans scans() {
    return new ArrayScans(mzs, intensities);
  }

  int numScans() {
    return mzs.length;
  }

  long numDataPoints() {
    long n = 0;
    for (final double[] scan : mzs) {
      n += scan.length;
    }
    return n;
  }

  /**
   * @return the label of the data point with exactly this m/z in the scan or null if there is none
   */
  Integer findLabel(int scan, double mz) {
    final int index = findIndex(scan, mz);
    return index < 0 ? null : labels[scan][index];
  }

  /**
   * @return the index of the data point with exactly this m/z in the scan or -1 if there is none
   */
  int findIndex(int scan, double mz) {
    final double[] scanMzs = mzs[scan];
    int lo = 0;
    int hi = scanMzs.length - 1;
    while (lo <= hi) {
      final int mid = (lo + hi) >>> 1;
      if (scanMzs[mid] < mz) {
        lo = mid + 1;
      } else if (scanMzs[mid] > mz) {
        hi = mid - 1;
      } else {
        // equal m/z values of different signals are practically impossible with random errors
        return mid;
      }
    }
    return -1;
  }

  /**
   * @return true if the data point belongs to the ion, as label or as shared label of a coalesced
   * centroid
   */
  boolean belongsTo(int scan, int index, int ion) {
    return labels[scan][index] == ion || sharedLabels[scan][index] == ion;
  }

  /**
   * @param mz              m/z without error
   * @param apexScan        scan index of the apex, may be fractional
   * @param sigmaScans      standard deviation of the gaussian peak in scans
   * @param height          apex intensity
   * @param mzErrorPpm      standard deviation of the m/z error at the apex, grows with
   *                        1/sqrt(intensity) for weaker signals
   * @param driftPpmPerScan systematic m/z drift, zero at the apex
   */
  record Ion(double mz, double apexScan, double sigmaScans, double height, double mzErrorPpm,
             double driftPpmPerScan) {

    Ion(double mz, double apexScan, double sigmaScans, double height) {
      this(mz, apexScan, sigmaScans, height, 1d, 0d);
    }
  }

  static final class Builder {

    private final int numScans;
    private final List<Ion> ions = new ArrayList<>();
    private final List<double[]> explicitPoints = new ArrayList<>();
    private int noisePerScan = 0;
    private double noiseMinMz = 100;
    private double noiseMaxMz = 1000;
    private double noiseMinIntensity = 1E2;
    private double noiseMaxIntensity = 1E4;
    private int additionalNoisePerScan = 0;
    private double additionalNoiseMinIntensity = 1E1;
    private double additionalNoiseMaxIntensity = 1E2;
    private double detectionThreshold = 0;
    private double intensityNoise = 0.05;
    private double maxErrorFactor = 5;
    private double saturationIntensity = 0;
    private double saturationShiftPpmPerDecade = 0;
    private double coalescenceMaxPpm = 0;
    private double coalescenceMinRatio = 1;
    private long seed = 42;

    private Builder(int numScans) {
      this.numScans = numScans;
    }

    @NotNull Builder ion(@NotNull Ion ion) {
      ions.add(ion);
      return this;
    }

    @NotNull Builder ions(@NotNull List<Ion> ions) {
      this.ions.addAll(ions);
      return this;
    }

    /**
     * Adds a data point with a fixed value, label is an ion index or {@link #NOISE}
     */
    @NotNull Builder dataPoint(int scan, double mz, double intensity, int label) {
      explicitPoints.add(new double[]{scan, mz, intensity, label});
      return this;
    }

    @NotNull Builder noise(int perScan, double minMz, double maxMz, double minIntensity,
        double maxIntensity) {
      noisePerScan = perScan;
      noiseMinMz = minMz;
      noiseMaxMz = maxMz;
      noiseMinIntensity = minIntensity;
      noiseMaxIntensity = maxIntensity;
      return this;
    }

    /**
     * A second noise band in the m/z range of {@link #noise}, e.g., the dense weak noise of data
     * without noise filter in the mass detection.
     */
    @NotNull Builder additionalNoise(int perScan, double minIntensity, double maxIntensity) {
      additionalNoisePerScan = perScan;
      additionalNoiseMinIntensity = minIntensity;
      additionalNoiseMaxIntensity = maxIntensity;
      return this;
    }

    /**
     * Ion signals below this intensity are not detected, like the noise level of mass detection.
     */
    @NotNull Builder detectionThreshold(double threshold) {
      detectionThreshold = threshold;
      return this;
    }

    @NotNull Builder intensityNoise(double relativeStandardDeviation) {
      intensityNoise = relativeStandardDeviation;
      return this;
    }

    /**
     * Caps the m/z error of weak signals at this multiple of the apex error.
     */
    @NotNull Builder maxErrorFactor(double factor) {
      maxErrorFactor = factor;
      return this;
    }

    /**
     * The detector saturates: signals above the intensity report about this intensity and their m/z
     * shifts up by the shift for each factor 10 above it, like the apex of intense ions on GC-EI-QTOF
     * data at ~7.4E6.
     */
    @NotNull Builder saturation(double intensity, double shiftPpmPerDecade) {
      saturationIntensity = intensity;
      saturationShiftPpmPerDecade = shiftPpmPerDecade;
      return this;
    }

    /**
     * Two neighboring ion signals within the m/z distance give one centroid at the intensity weighted
     * m/z with the summed intensity if the weaker one reaches the ratio of the stronger one, e.g., two
     * ions that the instrument does not resolve. The centroid belongs to both ions.
     */
    @NotNull Builder coalescence(double maxPpm, double minIntensityRatio) {
      coalescenceMaxPpm = maxPpm;
      coalescenceMinRatio = minIntensityRatio;
      return this;
    }

    @NotNull Builder seed(long seed) {
      this.seed = seed;
      return this;
    }

    @NotNull SyntheticLcmsData build() {
      final Random random = new Random(seed);
      final double[][] mzs = new double[numScans][];
      final double[][] intensities = new double[numScans][];
      final int[][] labels = new int[numScans][];
      final int[][] sharedLabels = new int[numScans][];
      final double logNoiseMin = Math.log(noiseMinIntensity);
      final double logNoiseMax = Math.log(noiseMaxIntensity);

      final List<double[]> points = new ArrayList<>();
      for (int s = 0; s < numScans; s++) {
        points.clear();
        for (int k = 0; k < ions.size(); k++) {
          final Ion ion = ions.get(k);
          final double z = (s - ion.apexScan()) / ion.sigmaScans();
          if (Math.abs(z) > 8) {
            continue;
          }
          double intensity = ion.height() * Math.exp(-0.5 * z * z);
          intensity *= Math.max(0.01, 1 + intensityNoise * random.nextGaussian());
          if (intensity < detectionThreshold || intensity <= 0) {
            continue;
          }
          final double errorPpm =
              ion.mzErrorPpm() * Math.min(maxErrorFactor, Math.sqrt(ion.height() / intensity));
          double ppm =
              ion.driftPpmPerScan() * (s - ion.apexScan()) + errorPpm * random.nextGaussian();
          if (saturationIntensity > 0 && intensity > saturationIntensity) {
            ppm += saturationShiftPpmPerDecade * Math.log10(intensity / saturationIntensity);
            intensity = saturationIntensity * Math.max(0.5, 1 + 0.01 * random.nextGaussian());
          }
          points.add(new double[]{ion.mz() * (1 + ppm * 1E-6), intensity, k, NOISE});
        }
        if (coalescenceMaxPpm > 0) {
          coalesce(points);
        }
        for (int i = 0; i < noisePerScan; i++) {
          final double mz = noiseMinMz + random.nextDouble() * (noiseMaxMz - noiseMinMz);
          final double intensity = Math.exp(
              logNoiseMin + random.nextDouble() * (logNoiseMax - logNoiseMin));
          points.add(new double[]{mz, intensity, NOISE, NOISE});
        }
        for (int i = 0; i < additionalNoisePerScan; i++) {
          final double mz = noiseMinMz + random.nextDouble() * (noiseMaxMz - noiseMinMz);
          final double intensity = Math.exp(
              Math.log(additionalNoiseMinIntensity) + random.nextDouble() * Math.log(
                  additionalNoiseMaxIntensity / additionalNoiseMinIntensity));
          points.add(new double[]{mz, intensity, NOISE, NOISE});
        }
        for (final double[] explicit : explicitPoints) {
          if ((int) explicit[0] == s) {
            points.add(new double[]{explicit[1], explicit[2], explicit[3], NOISE});
          }
        }

        final int n = points.size();
        final int[] order = ChannelConsolidation.identity(n);
        final List<double[]> finalPoints = points;
        IntArrays.quickSort(order, 0, n,
            (a, b) -> Double.compare(finalPoints.get(a)[0], finalPoints.get(b)[0]));
        mzs[s] = new double[n];
        intensities[s] = new double[n];
        labels[s] = new int[n];
        sharedLabels[s] = new int[n];
        for (int i = 0; i < n; i++) {
          final double[] point = points.get(order[i]);
          mzs[s][i] = point[0];
          intensities[s][i] = point[1];
          labels[s][i] = (int) point[2];
          sharedLabels[s][i] = (int) point[3];
        }
      }
      return new SyntheticLcmsData(mzs, intensities, labels, sharedLabels, List.copyOf(ions));
    }

    /**
     * Merges neighboring ion signals of one scan into one centroid, each signal at most once.
     *
     * @param points the ion signals of the scan {mz, intensity, label, shared label}
     */
    private void coalesce(@NotNull List<double[]> points) {
      points.sort(Comparator.comparingDouble(p -> p[0]));
      final List<double[]> merged = new ArrayList<>(points.size());
      for (int i = 0; i < points.size(); i++) {
        final double[] p = points.get(i);
        if (i + 1 < points.size()) {
          final double[] q = points.get(i + 1);
          final double ratio = Math.min(p[1], q[1]) / Math.max(p[1], q[1]);
          if ((q[0] - p[0]) / p[0] * 1E6 <= coalescenceMaxPpm && ratio >= coalescenceMinRatio) {
            final double sum = p[1] + q[1];
            final boolean pStronger = p[1] >= q[1];
            merged.add(new double[]{(p[0] * p[1] + q[0] * q[1]) / sum, sum, pStronger ? p[2] : q[2],
                pStronger ? q[2] : p[2]});
            i++;
            continue;
          }
        }
        merged.add(p);
      }
      points.clear();
      points.addAll(merged);
    }
  }
}
