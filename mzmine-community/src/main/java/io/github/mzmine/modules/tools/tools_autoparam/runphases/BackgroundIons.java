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

package io.github.mzmine.modules.tools.tools_autoparam.runphases;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Ubiquitous background ions (solvent clusters, contaminants) and the traces derived from them.
 * Their intensity follows the solvent composition and drops when nothing reaches the source.
 */
final class BackgroundIons {

  // decision: the absolute part keeps the window usable for low mass background ions
  static final MZTolerance ION_TOLERANCE = new MZTolerance(0.01, 15);
  private static final int SAMPLED_SCANS = 40;
  // an ion has to be present in 80% of the sampled scans
  private static final double UBIQUITY = 0.8;
  private static final int MAX_IONS = 60;
  // correlation with the scan index to count as rising or falling
  private static final double CORRELATION_THRESHOLD = 0.3;
  // the correlation is computed on the middle part of the run, away from void and wash
  private static final double CORRELATION_FROM = 0.15;
  private static final double CORRELATION_TO = 0.75;
  private static final int MIN_GROUP_IONS = 3;

  private BackgroundIons() {
  }

  /**
   * Clusters the peaks of evenly sampled scans by m/z and keeps the clusters present in most of
   * them.
   *
   * @return m/z of the most intense ubiquitous ions, sorted ascending
   */
  static double @NotNull [] ubiquitousIons(@NotNull List<? extends Scan> scans) {
    final int n = scans.size();
    final int sampled = Math.min(SAMPLED_SCANS, n);
    // mz, intensity, sampled scan index
    final List<double[]> peaks = new ArrayList<>();
    for (int k = 0; k < sampled; k++) {
      final MassSpectrum spectrum = TraceMath.spectrum(scans.get((int) ((k + 0.5) * n / sampled)));
      for (int j = 0; j < spectrum.getNumberOfDataPoints(); j++) {
        // zero intensity mass lists (auto param) keep zeros, they do not count as present
        if (spectrum.getIntensityValue(j) > 0) {
          peaks.add(new double[]{spectrum.getMzValue(j), spectrum.getIntensityValue(j), k});
        }
      }
    }
    peaks.sort((a, b) -> Double.compare(a[0], b[0]));

    // greedy clustering, count distinct sampled scans per cluster. mz, summed intensity
    final List<double[]> clusters = new ArrayList<>();
    int i = 0;
    while (i < peaks.size()) {
      final double start = peaks.get(i)[0];
      // cluster width is the full tolerance window (+-tol) anchored at its lowest m/z
      final double width = 2 * ION_TOLERANCE.getMzToleranceForMass(start);
      final boolean[] seen = new boolean[sampled];
      int count = 0;
      double weightedMz = 0;
      double intensity = 0;
      int j = i;
      while (j < peaks.size() && peaks.get(j)[0] - start < width) {
        final double[] peak = peaks.get(j);
        if (!seen[(int) peak[2]]) {
          seen[(int) peak[2]] = true;
          count++;
        }
        weightedMz += peak[0] * peak[1];
        intensity += peak[1];
        j++;
      }
      if (count >= UBIQUITY * sampled && intensity > 0) {
        clusters.add(new double[]{weightedMz / intensity, intensity});
      }
      i = j;
    }
    clusters.sort((a, b) -> Double.compare(b[1], a[1]));
    return clusters.stream().limit(MAX_IONS).mapToDouble(c -> c[0]).sorted().toArray();
  }

  /**
   * @param ions        background ion m/z
   * @param intensities intensity per ion and scan
   * @param saltMzs     sodium formate cluster m/z, kept out of the rising and falling groups
   * @param window      rolling median window in scans
   */
  static @NotNull BackgroundTraces traces(double @NotNull [] ions, double @NotNull [][] intensities,
      double @NotNull [] saltMzs, int window, int numScans) {
    final int n = numScans;
    final double[][] norm = new double[ions.length][];
    final double[] correlation = new double[ions.length];
    final boolean[] saltIon = new boolean[ions.length];
    final int from = (int) (n * CORRELATION_FROM);
    final int to = (int) (n * CORRELATION_TO);
    for (int k = 0; k < ions.length; k++) {
      final double[] log = TraceMath.log10(intensities[k]);
      final double median = TraceMath.median(log);
      norm[k] = Arrays.stream(log).map(v -> v - median).toArray();
      final double mz = ions[k];
      // decision: salt clusters follow salt elution or a calibrant, not the solvent composition
      saltIon[k] = Arrays.stream(saltMzs)
          .anyMatch(saltMz -> ION_TOLERANCE.checkWithinTolerance(mz, saltMz));
      correlation[k] = saltIon[k] ? 0 : TraceMath.spearman(Arrays.copyOfRange(log, from, to));
    }

    final int numRising = (int) Arrays.stream(correlation).filter(r -> r > CORRELATION_THRESHOLD)
        .count();
    final int numFalling = (int) Arrays.stream(correlation).filter(r -> r < -CORRELATION_THRESHOLD)
        .count();
    final double[] suppression = new double[n];
    final double[] gradient = new double[n];
    for (int i = 0; i < n; i++) {
      final List<Double> all = new ArrayList<>(ions.length);
      final List<Double> up = new ArrayList<>();
      final List<Double> down = new ArrayList<>();
      for (int k = 0; k < ions.length; k++) {
        // S keeps missing ions, their disappearance is the flow-off and void signal
        all.add(norm[k][i]);
        // decision: G leaves missing ions out, otherwise a few vanishing ions collapse the group
        // median and dominate the G range
        if (intensities[k][i] <= 0) {
          continue;
        }
        if (correlation[k] > CORRELATION_THRESHOLD) {
          up.add(norm[k][i]);
        } else if (correlation[k] < -CORRELATION_THRESHOLD) {
          down.add(norm[k][i]);
        }
      }
      // assumption: without background ions nothing indicates no flow, S stays at its level
      suppression[i] = all.isEmpty() ? 0 : TraceMath.median(all);
      gradient[i] = groupValue(numRising, up) - groupValue(numFalling, down);
    }
    return new BackgroundTraces(TraceMath.rollingMedian(suppression, window),
        TraceMath.rollingMedian(gradient, window));
  }

  /**
   * @param groupSize number of ions assigned to the group
   * @param present   normalized intensities of the group's ions detected in this scan
   * @return 0 for a group too small to use, NaN when too few of its ions are detected in the scan
   */
  private static double groupValue(int groupSize, @NotNull List<Double> present) {
    if (groupSize < MIN_GROUP_IONS) {
      return 0;
    }
    return present.size() < MIN_GROUP_IONS ? Double.NaN : TraceMath.median(present);
  }
}
