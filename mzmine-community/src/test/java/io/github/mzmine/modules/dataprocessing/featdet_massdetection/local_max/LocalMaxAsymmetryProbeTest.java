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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.gui.preferences.AgilentImportOptions;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.gui.preferences.WatersLockmassParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.io.import_rawdata_agilent_d.AgilentDataAccess;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import testutils.MZmineTestUtil;

/**
 * Diagnostic probe (not an assertion test): loads a single Agilent frame, runs the
 * {@link AdaptiveLocalMaxMassDetector}, and prints a per-peak symmetry classification to the
 * console. The goal is to see whether high-intensity, asymmetric peaks can be singled out (e.g. by
 * % of base peak and a left/right point asymmetry) so that an expensive apex fit could be applied
 * only to those, keeping the cheap centroid for the rest.
 * <p>
 * Windows-only: the Agilent reader uses the Windows-only MassHunter SDK via a .NET subprocess. The
 * test is skipped if the local data file is not present.
 */
@EnabledOnOs(OS.WINDOWS)
class LocalMaxAsymmetryProbeTest {

  private static final String FILE_PATH = "D:\\Testdaten\\MSV000091638_bakerlab_mse\\220701_PFAC2_GenX10.d";
  private static final int FRAME_ID = 556;
  // smoothing used for max/edge detection - matches the Gaussian width 10 used in the preview
  private static final int GAUSSIAN_WIDTH = 10;
  // apex-centered fit window sizes (number of points, apex +/- (N-1)/2) to compare
  private static final int[] FIT_WINDOWS = {3, 5, 7};
  // only print peaks at or above this % of base peak (below is noise with negligible shift)
  private static final double MIN_PERCENT_BASE_PRINT = 0.1;
  // flat-top (detector saturation) probe: this mobility scan in FRAME_ID shows a saturated plateau
  private static final int MOBILITY_SCAN = 210;
  // reference m/z (the frame total-spectrum centroid for the 598.9 ion) - the apex is unreliable for
  // a flat top, so estimators are compared to this instead
  private static final double REFERENCE_MZ = 598.9419;

  @BeforeAll
  static void init() {
    MZmineTestUtil.startMzmineCore();
  }

  @Test
  void probeFrame556Symmetry() {
    final File file = new File(FILE_PATH);
    Assumptions.assumeTrue(file.exists(), "Agilent test file not present: " + FILE_PATH);

    // profile data, native Agilent reader, no auto-centroiding
    final VendorImportParameters vendorParam = VendorImportParameters.create(false,
        VendorImportParameters.DEFAULT_WATERS_OPTION,
        VendorImportParameters.DEFAULT_WATERS_LOCKMASS_ENABLED,
        WatersLockmassParameters.createDefault(),
        VendorImportParameters.DEFAULT_THERMO_EXCEPTION_SIGNALS,
        VendorImportParameters.DEFAULT_AGILENT_OPTION,
        VendorImportParameters.DEFAULT_PRESSURE_COMPENSATION);

    try (final AgilentDataAccess access = new AgilentDataAccess(file, vendorParam, null, null)) {
      Assumptions.assumeTrue(access.isIms(), "Expected an IMS .d file");
      Assumptions.assumeTrue(access.getFrameCount() >= FRAME_ID,
          "File has fewer than " + FRAME_ID + " frames");

      final IMSRawDataFileImpl imsFile = (IMSRawDataFileImpl) access.createDataFile();
      final SimpleFrame frame = access.readFrame(imsFile, FRAME_ID);
      Assumptions.assumeTrue(frame != null, "Frame " + FRAME_ID + " could not be read");

      final int numPoints = frame.getNumberOfDataPoints();

      // spectrum-level reference intensities
      double specMax = 0d;
      double specMinNonZero = Double.MAX_VALUE;
      for (int i = 0; i < numPoints; i++) {
        final double intensity = frame.getIntensityValue(i);
        if (intensity > specMax) {
          specMax = intensity;
        }
        if (intensity > 0 && intensity < specMinNonZero) {
          specMinNonZero = intensity;
        }
      }
      if (specMinNonZero == Double.MAX_VALUE) {
        specMinNonZero = 0d;
      }

      System.out.println("=== LocalMax asymmetry probe ===");
      System.out.printf("File: %s%n", file.getName());
      System.out.printf("Frame %d, points=%d, base peak intensity=%.1f, min non-zero=%.1f%n%n",
          FRAME_ID, numPoints, specMax, specMinNonZero);

      // compare detection with and without smoothing on the same frame
      printProbe("NO SMOOTHING", new AdaptiveLocalMaxMassDetector(0, AbundanceMeasure.Height, 3),
          frame, specMax, specMinNonZero);
      printProbe("GAUSSIAN width " + GAUSSIAN_WIDTH,
          new AdaptiveLocalMaxMassDetector(0, AbundanceMeasure.Height, 3,
              new LocalMaxGaussianModule(GAUSSIAN_WIDTH)), frame, specMax, specMinNonZero);
    }
  }

  private static void printProbe(final String label, final AdaptiveLocalMaxMassDetector detector,
      final SimpleFrame frame, final double specMax, final double specMinNonZero) {
    final List<LocalMaxPeak> peaks = detector.detectPeaks(frame);

    // most intense first - those are the candidates that matter for an adaptive fit
    final List<LocalMaxPeak> sorted = peaks.stream()
        .sorted(Comparator.comparingDouble(LocalMaxPeak::height).reversed()).toList();

    System.out.printf("=== config: %s | detected peaks: %d (printing >= %.2f%% base) ===%n", label,
        peaks.size(), MIN_PERCENT_BASE_PRINT);
    // fitN = apex-centered parabola vertex over N points; cN = fitN - centroid (the mass
    // correction an adaptive fit would apply), in mDa. biasCen = centroid - apex (current bias).
    System.out.printf("%-11s\t%-11s\t%7s\t%6s\t%8s\t%-11s\t%-11s\t%-11s\t%7s\t%7s\t%7s\t%-9s%n",
        "apex", "centroid", "%base", "asym", "biasCen", "fitN3", "fitN5", "fitN7", "c3", "c5", "c7",
        "class");

    for (final LocalMaxPeak peak : sorted) {
      final double percentOfBase = specMax > 0 ? peak.height() / specMax * 100d : 0d;
      if (percentOfBase < MIN_PERCENT_BASE_PRINT) {
        continue;
      }

      final double apexMz = frame.getMzValue(peak.apexIndex());
      final int leftPoints = peak.leftPoints();
      final int rightPoints = peak.rightPoints();
      final double asym =
          leftPoints == 0 ? Double.POSITIVE_INFINITY : (double) rightPoints / leftPoints;
      final String classification = asym > 1.43 ? "TAILING" : asym < 0.7 ? "FRONTING" : "symmetric";

      final double biasCen = (peak.centroidMz() - apexMz) * 1000d;

      // apex-centered parabola fit for each window size; correction relative to current centroid
      final double[] fit = new double[FIT_WINDOWS.length];
      final double[] corr = new double[FIT_WINDOWS.length];
      for (int w = 0; w < FIT_WINDOWS.length; w++) {
        fit[w] = apexCenteredParabolaVertex(frame, peak, apexMz, FIT_WINDOWS[w]);
        corr[w] = Double.isNaN(fit[w]) ? Double.NaN : (fit[w] - peak.centroidMz()) * 1000d;
      }

      System.out.printf(
          "%-11.4f\t%-11.4f\t%6.2f%%\t%6.2f\t%8.1f\t%-11.4f\t%-11.4f\t%-11.4f\t%7.1f\t%7.1f\t%7.1f\t%-9s%n",
          apexMz, peak.centroidMz(), percentOfBase, asym, biasCen, fit[0], fit[1], fit[2], corr[0],
          corr[1], corr[2], classification);
    }
    System.out.println();
  }

  /**
   * Vertex of the least-squares parabola over the {@code n} points centered on the apex
   * ({@code apex +/- (n-1)/2}), clamped to the peak's edges. apex-centered (not the n highest
   * points) so a tailing flank does not skew the window. NaN if fewer than 3 points are available.
   */
  private static double apexCenteredParabolaVertex(final MassSpectrum spectrum,
      final LocalMaxPeak peak, final double apexMz, final int n) {
    final int half = (n - 1) / 2;
    final int from = Math.max(peak.leftIndex(), peak.apexIndex() - half);
    final int to = Math.min(peak.rightIndexExclusive() - 1, peak.apexIndex() + half);
    final int count = to - from + 1;
    if (count < 3) {
      return Double.NaN;
    }
    final double[] xs = new double[count];
    final double[] ys = new double[count];
    for (int i = from, k = 0; i <= to; i++, k++) {
      xs[k] = spectrum.getMzValue(i);
      ys[k] = spectrum.getIntensityValue(i);
    }
    return lsParabolaVertex(xs, ys, count, apexMz);
  }

  /**
   * Vertex (extremum x) of the least-squares parabola {@code y = a*u^2 + b*u + c} with
   * {@code u = x - xCenter} (centering improves conditioning). Returns NaN if degenerate (too flat
   * or collinear).
   */
  private static double lsParabolaVertex(final double[] xs, final double[] ys, final int n,
      final double xCenter) {
    final double s0 = n;
    double s1 = 0, s2 = 0, s3 = 0, s4 = 0, t0 = 0, t1 = 0, t2 = 0;
    for (int i = 0; i < n; i++) {
      final double u = xs[i] - xCenter;
      final double y = ys[i];
      final double u2 = u * u;
      s1 += u;
      s2 += u2;
      s3 += u2 * u;
      s4 += u2 * u2;
      t0 += y;
      t1 += u * y;
      t2 += u2 * y;
    }
    // normal equations: [[s4 s3 s2],[s3 s2 s1],[s2 s1 s0]] * [a b c]^T = [t2 t1 t0]^T
    final double det = det3(s4, s3, s2, s3, s2, s1, s2, s1, s0);
    if (Math.abs(det) < 1e-30) {
      return Double.NaN;
    }
    final double a = det3(t2, s3, s2, t1, s2, s1, t0, s1, s0) / det;
    final double b = det3(s4, t2, s2, s3, t1, s1, s2, t0, s0) / det;
    if (Math.abs(a) < 1e-30) {
      return Double.NaN;
    }
    return xCenter - b / (2 * a);
  }

  private static double det3(final double a, final double b, final double c, final double d,
      final double e, final double f, final double g, final double h, final double i) {
    return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
  }

  /**
   * Simple timed throughput comparison of the baseline vs the adaptive detector over a realistic
   * workload: every mobility scan of one frame, centroided with the same Gaussian preprocessor (the
   * production IMS auto-centroiding path). Not a microbenchmark - just enough to gauge the relative
   * cost of the adaptive m/z. Reports the median over several iterations after a warmup.
   */
  @Test
  void profileAdaptiveVsBaseline() {
    final File file = new File(FILE_PATH);
    Assumptions.assumeTrue(file.exists(), "Agilent test file not present: " + FILE_PATH);

    final VendorImportParameters vendorParam = VendorImportParameters.create(false,
        VendorImportParameters.DEFAULT_WATERS_OPTION,
        VendorImportParameters.DEFAULT_WATERS_LOCKMASS_ENABLED,
        WatersLockmassParameters.createDefault(),
        VendorImportParameters.DEFAULT_THERMO_EXCEPTION_SIGNALS,
        VendorImportParameters.DEFAULT_AGILENT_OPTION,
        VendorImportParameters.DEFAULT_PRESSURE_COMPENSATION);

    final int warmup = 3;
    final int measured = 7;

    try (final AgilentDataAccess access = new AgilentDataAccess(file, vendorParam, null, null)) {
      Assumptions.assumeTrue(access.isIms(), "Expected an IMS .d file");
      Assumptions.assumeTrue(access.getFrameCount() >= FRAME_ID, "Too few frames");

      final IMSRawDataFileImpl imsFile = (IMSRawDataFileImpl) access.createDataFile();
      final SimpleFrame frame = access.readFrame(imsFile, FRAME_ID);
      Assumptions.assumeTrue(frame != null, "Frame " + FRAME_ID + " could not be read");

      // workload: all mobility scans of the frame (the per-spectrum IMS centroiding workload)
      final List<MassSpectrum> spectra = new ArrayList<>(frame.getNumberOfMobilityScans());
      long totalPoints = 0;
      for (int i = 0; i < frame.getNumberOfMobilityScans(); i++) {
        final MobilityScan ms = frame.getMobilityScan(i);
        if (ms != null) {
          spectra.add(ms);
          totalPoints += ms.getNumberOfDataPoints();
        }
      }
      Assumptions.assumeTrue(!spectra.isEmpty(), "No mobility scans");

      // separate preprocessor instances so the two detectors are fully independent
      final MassDetector baseline = new LocalMaxMassDetector(0, AbundanceMeasure.Height, 3,
          new LocalMaxGaussianModule(GAUSSIAN_WIDTH));
      final MassDetector adaptive = new AdaptiveLocalMaxMassDetector(0, AbundanceMeasure.Height, 3,
          new LocalMaxGaussianModule(GAUSSIAN_WIDTH));

      long sink = 0;
      for (int w = 0; w < warmup; w++) {
        sink += runAll(baseline, spectra);
        sink += runAll(adaptive, spectra);
      }

      final long[] baseNs = new long[measured];
      final long[] adaptNs = new long[measured];
      long basePeaks = 0;
      long adaptPeaks = 0;
      for (int it = 0; it < measured; it++) {
        long t0 = System.nanoTime();
        basePeaks = runAll(baseline, spectra);
        baseNs[it] = System.nanoTime() - t0;

        t0 = System.nanoTime();
        adaptPeaks = runAll(adaptive, spectra);
        adaptNs[it] = System.nanoTime() - t0;
      }
      sink += basePeaks + adaptPeaks;

      final double baseMs = median(baseNs) / 1_000_000.0;
      final double adaptMs = median(adaptNs) / 1_000_000.0;
      final int n = spectra.size();

      System.out.println("=== Throughput: adaptive vs baseline (frame " + FRAME_ID
          + " mobility scans, Gaussian width " + GAUSSIAN_WIDTH + ") ===");
      System.out.printf("spectra=%d, total points=%d, warmup=%d, measured=%d%n", n, totalPoints,
          warmup, measured);
      System.out.printf("baseline : %7.2f ms/iter  %6.2f us/spectrum  (peaks/iter=%d)%n", baseMs,
          baseMs * 1000.0 / n, basePeaks);
      System.out.printf("adaptive : %7.2f ms/iter  %6.2f us/spectrum  (peaks/iter=%d)%n", adaptMs,
          adaptMs * 1000.0 / n, adaptPeaks);
      System.out.printf("adaptive / baseline = %.3fx%n", adaptMs / baseMs);
      System.out.printf("(checksum %d)%n%n", sink);
    }
  }

  private static long runAll(final MassDetector detector, final List<MassSpectrum> spectra) {
    long count = 0;
    for (final MassSpectrum spectrum : spectra) {
      final double[][] result = detector.getMassValues(spectrum);
      count += result[0].length;
    }
    return count;
  }

  private static long median(final long[] values) {
    final long[] sorted = values.clone();
    Arrays.sort(sorted);
    return sorted[sorted.length / 2];
  }

  /**
   * Flat-top / detector-saturation case: a single mobility scan whose 598.9 ion is a saturated
   * plateau (with a noise spike near the left edge, so the raw apex is misleading). Compares every
   * estimator against the frame centroid {@link #REFERENCE_MZ} and dumps the plateau points.
   */
  @Test
  void probeFlatTopMobilityScan() {
    final File file = new File(FILE_PATH);
    Assumptions.assumeTrue(file.exists(), "Agilent test file not present: " + FILE_PATH);

    final VendorImportParameters vendorParam = VendorImportParameters.create(false,
        VendorImportParameters.DEFAULT_WATERS_OPTION,
        VendorImportParameters.DEFAULT_WATERS_LOCKMASS_ENABLED,
        WatersLockmassParameters.createDefault(),
        VendorImportParameters.DEFAULT_THERMO_EXCEPTION_SIGNALS,
        VendorImportParameters.DEFAULT_AGILENT_OPTION,
        VendorImportParameters.DEFAULT_PRESSURE_COMPENSATION);

    try (final AgilentDataAccess access = new AgilentDataAccess(file, vendorParam, null, null)) {
      Assumptions.assumeTrue(access.isIms(), "Expected an IMS .d file");
      Assumptions.assumeTrue(access.getFrameCount() >= FRAME_ID, "Too few frames");

      final IMSRawDataFileImpl imsFile = (IMSRawDataFileImpl) access.createDataFile();
      final SimpleFrame frame = access.readFrame(imsFile, FRAME_ID);
      Assumptions.assumeTrue(frame != null, "Frame " + FRAME_ID + " could not be read");
      Assumptions.assumeTrue(frame.getNumberOfMobilityScans() > MOBILITY_SCAN,
          "Frame has fewer than " + MOBILITY_SCAN + " mobility scans");

      final MobilityScan scan = frame.getMobilityScan(MOBILITY_SCAN);
      Assumptions.assumeTrue(scan != null, "Mobility scan " + MOBILITY_SCAN + " missing");

      System.out.printf("=== Flat-top probe: frame %d, mobility scan %d ===%n", FRAME_ID,
          MOBILITY_SCAN);
      System.out.printf("reference m/z (frame centroid) = %.4f%n%n", REFERENCE_MZ);

      printFlatTop("NO SMOOTHING", new AdaptiveLocalMaxMassDetector(0, AbundanceMeasure.Height, 3),
          scan);
      printFlatTop("GAUSSIAN width 7",
          new AdaptiveLocalMaxMassDetector(0, AbundanceMeasure.Height, 3,
              new LocalMaxGaussianModule(7)), scan);
    }
  }

  private static void printFlatTop(final String label, final AdaptiveLocalMaxMassDetector detector,
      final MassSpectrum scan) {
    final List<LocalMaxPeak> peaks = detector.detectPeaks(scan);

    // the peak whose apex is closest to the reference - the saturated 598.9 ion
    LocalMaxPeak target = null;
    double best = Double.MAX_VALUE;
    for (final LocalMaxPeak p : peaks) {
      final double d = Math.abs(scan.getMzValue(p.apexIndex()) - REFERENCE_MZ);
      if (d < best) {
        best = d;
        target = p;
      }
    }

    System.out.printf("--- %s: %d peaks ---%n", label, peaks.size());
    if (target == null) {
      System.out.printf("no peak detected%n%n");
      return;
    }

    final double apexMz = scan.getMzValue(target.apexIndex());
    System.out.printf("apex idx=%d mz=%.4f height=%.1f | left=%d right=%d | edges %.4f .. %.4f%n",
        target.apexIndex(), apexMz, target.height(), target.leftPoints(), target.rightPoints(),
        scan.getMzValue(target.leftIndex()), scan.getMzValue(target.rightIndexExclusive() - 1));

    System.out.printf("%-16s\t%12s\t%12s%n", "estimator", "m/z", "dRef(mDa)");
    printEstimator("apex", apexMz);
    printEstimator("centroid(40%)", target.centroidMz());
    printEstimator("parab N3", apexCenteredParabolaVertex(scan, target, apexMz, 3));
    printEstimator("parab N5", apexCenteredParabolaVertex(scan, target, apexMz, 5));
    printEstimator("parab N7", apexCenteredParabolaVertex(scan, target, apexMz, 7));
    printEstimator("plateau>=95%", plateauCenter(scan, target, 0.95));
    printEstimator("plateau>=80%", plateauCenter(scan, target, 0.80));

    System.out.println("points (mz : intensity):");
    for (int i = target.leftIndex(); i < target.rightIndexExclusive(); i++) {
      System.out.printf("  %.4f : %.1f%n", scan.getMzValue(i), scan.getIntensityValue(i));
    }
    System.out.println();
  }

  private static void printEstimator(final String name, final double mz) {
    if (Double.isNaN(mz)) {
      System.out.printf("%-16s\t%12s\t%12s%n", name, "NaN", "-");
    } else {
      System.out.printf("%-16s\t%12.4f\t%12.1f%n", name, mz, (mz - REFERENCE_MZ) * 1000d);
    }
  }

  /**
   * Unweighted mean m/z of the points at or above {@code frac} of the apex height within the peak
   * edges - the geometric center of a (flat) plateau.
   */
  private static double plateauCenter(final MassSpectrum scan, final LocalMaxPeak peak,
      final double frac) {
    final double cutoff = peak.height() * frac;
    double sum = 0;
    int n = 0;
    for (int i = peak.leftIndex(); i < peak.rightIndexExclusive(); i++) {
      if (scan.getIntensityValue(i) >= cutoff) {
        sum += scan.getMzValue(i);
        n++;
      }
    }
    return n == 0 ? Double.NaN : sum / n;
  }
}
