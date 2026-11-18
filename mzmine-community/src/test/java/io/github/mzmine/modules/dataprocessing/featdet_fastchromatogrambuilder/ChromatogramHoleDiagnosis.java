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

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.ProjectService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;
import testutils.TaskResult;

/**
 * Temporary diagnosis of holes in one chromatogram.
 */
@Tag("benchmark")
class ChromatogramHoleDiagnosis {

  @Test
  void diagnose() throws Exception {
    final String p = ChromatogramBenchmarkDatasets.PROPERTY + "diag.";
    final String path = System.getProperty(p + "file",
        "D:\\OneDrive - mzio GmbH\\Example data - Documents\\Agilent\\GC_TOF\\022_KR10_20220809.mzML");
    final double mz = Double.parseDouble(System.getProperty(p + "mz", "262.1204"));
    final double rtMin = Double.parseDouble(System.getProperty(p + "rtmin", "21.40"));
    final double rtMax = Double.parseDouble(System.getProperty(p + "rtmax", "21.70"));
    final double window = Double.parseDouble(System.getProperty(p + "window", "0.03"));

    final java.io.PrintStream out = new java.io.PrintStream(
        new java.io.FileOutputStream(System.getProperty(p + "out", "build/reports/diag.txt")),
        true);
    MZmineTestUtil.startMzmineCore();
    final ChromatogramBenchmarkDataset gc = ChromatogramBenchmarkDatasets.wizardDatasets().stream()
        .filter(d -> d.name().equals("GC-EI-QTOF")).findFirst().orElseThrow();
    final ChromatogramBenchmarkDataset dataset = new ChromatogramBenchmarkDataset("diag",
        List.of(path), gc.detector(), gc.noise(), gc.minConsecutive(), gc.minGroup(),
        gc.minHeight(), gc.preset(), gc.ppmGrid(), gc.cropRt(), gc.polarity());
    MZmineTestUtil.clearProjectAndLibraries();
    final TaskResult imported = MZmineTestUtil.importFiles(dataset.paths(), 3600,
        dataset.importParameters());
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, imported, imported.description());
    final RawDataFile file = ProjectService.getProject().getCurrentRawDataFiles().getFirst();
    final Scan[] scans = dataset.scanSelection().getMatchingScans(file);

    MZTolerance tolerance;
    final String tolProp = System.getProperty(p + "ppm");
    if (tolProp != null) {
      tolerance = new MZTolerance(0, Double.parseDouble(tolProp));
    } else {
      final MzToleranceEstimate estimate = MzToleranceEstimation.estimate(List.of(
              new ScanDataAccessScans(
                  EfficientDataAccess.of(file, ScanDataType.MASS_LIST, Arrays.asList(scans)))),
          dataset.minConsecutive(), dataset.minGroup(), dataset.minHeight(), null);
      tolerance = estimate.tolerance();
      out.println("estimate " + estimate);
    }
    out.println("tolerance " + tolerance);

    final FastChromatogramBuilder builder = new FastChromatogramBuilder(tolerance,
        dataset.minConsecutive(), dataset.minGroup(), dataset.minHeight());
    final List<BuiltChromatogram> chroms = builder.build(new ScanDataAccessScans(
        EfficientDataAccess.of(file, ScanDataType.MASS_LIST, Arrays.asList(scans))), null, null);
    out.println(builder.getStatistics());

    final List<BuiltChromatogram> near = new ArrayList<>();
    for (BuiltChromatogram c : chroms) {
      if (Math.abs(c.getCenterMz() - mz) <= window) {
        near.add(c);
      }
    }
    final double peakMin = Double.parseDouble(System.getProperty(p + "peakmin", "21.49"));
    final double peakMax = Double.parseDouble(System.getProperty(p + "peakmax", "21.60"));
    final List<BuiltChromatogram> without = new FastChromatogramBuilder(tolerance,
        dataset.minConsecutive(), dataset.minGroup(), dataset.minHeight(),
        FastChromatogramBuilderOptions.DEFAULT.withCoalescedMaxHoleScans(0)).build(
        new ScanDataAccessScans(
            EfficientDataAccess.of(file, ScanDataType.MASS_LIST, Arrays.asList(scans))), null,
        null);
    for (int k = 0; k < near.size(); k++) {
      final BuiltChromatogram c = near.get(k);
      BuiltChromatogram before = null;
      for (BuiltChromatogram w : without) {
        if (Math.abs(w.getCenterMz() - c.getCenterMz()) < 1E-3) {
          before = w;
        }
      }
      out.printf("chrom %d center %.5f n %d max %.0f, peak m/z %.2f ppm (without fill %.2f ppm)%n",
          k, c.getCenterMz(), c.getNumberOfDataPoints(), c.getMaxIntensity(),
          (peakMz(c, scans, peakMin, peakMax) - mz) / mz * 1E6,
          before == null ? Double.NaN : (peakMz(before, scans, peakMin, peakMax) - mz) / mz * 1E6);
    }

    for (int s = 0; s < scans.length; s++) {
      final Scan scan = scans[s];
      final float rt = scan.getRetentionTime();
      if (rt < rtMin || rt > rtMax) {
        continue;
      }
      final MassList ml = scan.getMassList();
      final StringBuilder line = new StringBuilder("scan %4d rt %.4f:".formatted(s, rt));
      for (int i = 0; i < ml.getNumberOfDataPoints(); i++) {
        final double m = ml.getMzValue(i);
        if (Math.abs(m - mz) > window) {
          continue;
        }
        final double in = ml.getIntensityValue(i);
        String owner = "-";
        for (int k = 0; k < near.size(); k++) {
          final BuiltChromatogram c = near.get(k);
          for (int j = 0; j < c.getNumberOfDataPoints(); j++) {
            if (c.getScanIndex(j) == s && c.getMz(j) == m) {
              owner = owner.equals("-") ? "c" + k : owner + ",c" + k;
            }
          }
        }
        line.append("  %.5f (%+.1f ppm) %.0f [%s]".formatted(m, (m - mz) / mz * 1e6, in, owner));
      }
      out.println(line);
    }
  }

  /**
   * Compares the chromatograms with and without the coalesced fill: bumps (fills above both flanks)
   * and the shift of the weighted m/z around each filled hole.
   */
  @Test
  void coalescedFills() throws Exception {
    final String p = ChromatogramBenchmarkDatasets.PROPERTY + "diag.";
    final String path = System.getProperty(p + "file",
        "D:\\OneDrive - mzio GmbH\\Example data - Documents\\Agilent\\GC_TOF\\021_ZR5_20220808.mzML");
    final java.io.PrintStream out = new java.io.PrintStream(
        new java.io.FileOutputStream(System.getProperty(p + "out", "build/reports/diag-fills.txt")),
        true);
    MZmineTestUtil.startMzmineCore();
    final ChromatogramBenchmarkDataset gc = ChromatogramBenchmarkDatasets.wizardDatasets().stream()
        .filter(d -> d.name().equals("GC-EI-QTOF")).findFirst().orElseThrow();
    MZmineTestUtil.clearProjectAndLibraries();
    final TaskResult imported = MZmineTestUtil.importFiles(List.of(path), 3600,
        gc.importParameters());
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, imported, imported.description());
    final RawDataFile file = ProjectService.getProject().getCurrentRawDataFiles().getFirst();
    final Scan[] scans = gc.scanSelection().getMatchingScans(file);
    final String tolProp = System.getProperty(p + "ppm");
    final MZTolerance tolerance =
        tolProp != null ? new MZTolerance(0, Double.parseDouble(tolProp)) : gc.preset();
    out.println(path + ", tolerance " + tolerance);

    final List<BuiltChromatogram> with = new FastChromatogramBuilder(tolerance, gc.minConsecutive(),
        gc.minGroup(), gc.minHeight()).build(new ScanDataAccessScans(
        EfficientDataAccess.of(file, ScanDataType.MASS_LIST, Arrays.asList(scans))), null, null);
    final List<BuiltChromatogram> without = new FastChromatogramBuilder(tolerance,
        gc.minConsecutive(), gc.minGroup(), gc.minHeight(),
        FastChromatogramBuilderOptions.DEFAULT.withCoalescedMaxHoleScans(0)).build(
        new ScanDataAccessScans(
            EfficientDataAccess.of(file, ScanDataType.MASS_LIST, Arrays.asList(scans))), null,
        null);
    Assertions.assertEquals(without.size(), with.size());

    int runs = 0;
    int fills = 0;
    int bumps15 = 0;
    int bumps3 = 0;
    final List<Double> shifts = new ArrayList<>();
    final List<String> bumpExamples = new ArrayList<>();
    final List<String> shiftExamples = new ArrayList<>();
    final int[] runLengths = new int[20];
    for (int c = 0; c < with.size(); c++) {
      final BuiltChromatogram w = with.get(c);
      final BuiltChromatogram o = without.get(c);
      Assertions.assertEquals(o.getCenterMz(), w.getCenterMz());
      if (w.getNumberOfDataPoints() == o.getNumberOfDataPoints()) {
        continue;
      }
      // walk o and w, runs of data points in w that are not in o
      int j = 0;
      int i = 0;
      while (i < w.getNumberOfDataPoints()) {
        if (j < o.getNumberOfDataPoints() && o.getScanIndex(j) == w.getScanIndex(i)) {
          i++;
          j++;
          continue;
        }
        final int runStart = i;
        double fillMax = 0;
        while (i < w.getNumberOfDataPoints() && (j >= o.getNumberOfDataPoints()
            || o.getScanIndex(j) != w.getScanIndex(i))) {
          fillMax = Math.max(fillMax, w.getIntensity(i));
          i++;
        }
        final int runEnd = i;
        runs++;
        fills += runEnd - runStart;
        runLengths[Math.min(19, runEnd - runStart)]++;
        final double before = o.getIntensity(j - 1);
        final double after = o.getIntensity(j);
        final double ratio = fillMax / Math.max(before, after);
        final int firstScan = w.getScanIndex(runStart);
        final double rt = scans[firstScan].getRetentionTime();
        if (ratio > 1.5) {
          bumps15++;
          if (bumpExamples.size() < 25) {
            bumpExamples.add(
                "m/z %.4f rt %.3f fills %d max %.0f flanks %.0f/%.0f".formatted(w.getCenterMz(), rt,
                    runEnd - runStart, fillMax, before, after));
          }
        }
        if (ratio > 3) {
          bumps3++;
        }
        // m/z shift around the hole, +-10 scans
        final double mzWith = weightedMz(w, firstScan - 10, w.getScanIndex(runEnd - 1) + 10);
        final double mzWithout = weightedMz(o, firstScan - 10, w.getScanIndex(runEnd - 1) + 10);
        final double shift = (mzWith - mzWithout) / mzWithout * 1E6;
        shifts.add(Math.abs(shift));
        if (Math.abs(shift) > 5 && shiftExamples.size() < 25) {
          shiftExamples.add(
              "m/z %.4f rt %.3f fills %d max %.0f shift %.1f ppm".formatted(w.getCenterMz(), rt,
                  runEnd - runStart, fillMax, shift));
        }
      }
    }
    shifts.sort(null);
    out.printf("filled runs %d, data points %d, bumps >1.5x flanks %d, >3x %d%n", runs, fills,
        bumps15, bumps3);
    out.println("run lengths " + Arrays.toString(runLengths));
    out.printf("|m/z shift| +-10 scans around the fill: median %.2f, p90 %.2f, p99 %.2f, max %.2f "
            + "ppm, >5 ppm %d%n", shifts.get(shifts.size() / 2),
        shifts.get((int) (shifts.size() * 0.9)), shifts.get((int) (shifts.size() * 0.99)),
        shifts.getLast(), shifts.stream().filter(s -> s > 5).count());
    out.println("bump examples");
    bumpExamples.forEach(out::println);
    out.println("shift examples");
    shiftExamples.forEach(out::println);
  }

  private static double weightedMz(BuiltChromatogram c, int fromScan, int toScan) {
    double sum = 0;
    double weights = 0;
    for (int i = 0; i < c.getNumberOfDataPoints(); i++) {
      if (c.getScanIndex(i) >= fromScan && c.getScanIndex(i) <= toScan) {
        sum += c.getMz(i) * c.getIntensity(i);
        weights += c.getIntensity(i);
      }
    }
    return sum / weights;
  }

  /**
   * @return intensity weighted m/z of the data points in the retention time range, like the feature
   * m/z
   */
  private static double peakMz(BuiltChromatogram c, Scan[] scans, double rtMin, double rtMax) {
    double sum = 0;
    double weights = 0;
    for (int i = 0; i < c.getNumberOfDataPoints(); i++) {
      final float rt = scans[c.getScanIndex(i)].getRetentionTime();
      if (rt >= rtMin && rt <= rtMax) {
        sum += c.getMz(i) * c.getIntensity(i);
        weights += c.getIntensity(i);
      }
    }
    return sum / weights;
  }
}
