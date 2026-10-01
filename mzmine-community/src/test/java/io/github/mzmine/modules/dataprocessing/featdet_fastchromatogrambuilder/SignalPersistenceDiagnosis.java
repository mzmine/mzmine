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

import static io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.ChromatogramBenchmarkDatasets.PROPERTY;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.ProjectService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import testutils.MZmineTestUtil;
import testutils.TaskResult;

/**
 * Compares rules for the continuation of a data point in the {@link SignalPersistenceProfile} on
 * the benchmark data sets: per intensity bin the near hits, control hits and signal fraction of
 * each rule, and the noise level (N50) and signal level (N95) that each rule gives. The near window
 * is the one of the Medium estimate, the scans are sampled like in the estimation. Used to find why
 * narrow peaks did not reach the signal level, see the implementation note of the fast
 * chromatogram builder. Select data sets with {@code -Dmzmine.test.chrombench.only=...}.
 */
@Tag("benchmark")
@TestInstance(Lifecycle.PER_CLASS)
class SignalPersistenceDiagnosis {

  private static final double LOG_JUMP = Math.log(SignalPersistenceProfile.INTENSITY_JUMP);
  private static final double ANY = Double.MAX_VALUE;
  private static final int BINS = 250;

  /**
   * A partner of a data point in one scan relative to the data point.
   *
   * @param scanOffset  -2 to 2, the scan relative to the scan of the data point
   * @param windowScale multiple of the near window, the control windows scale with it
   * @param maxLogRatio max |log intensity ratio| to the data point, {@link #ANY} for any
   */
  private record Partner(int scanOffset, double windowScale, double maxLogRatio) {

  }

  /**
   * A data point continues if any of the alternatives has all of its partners.
   */
  private record Rule(@NotNull String name, @NotNull Partner[][] alternatives) {

  }

  private static final Partner NEXT = new Partner(1, 1, LOG_JUMP);
  private static final Partner PREVIOUS = new Partner(-1, 1, LOG_JUMP);
  private static final Partner NEXT_2 = new Partner(2, 1, LOG_JUMP);
  private static final Partner PREVIOUS_2 = new Partner(-2, 1, LOG_JUMP);

  private static final List<Rule> RULES = List.of( //
      new Rule("next 5x", new Partner[][]{{NEXT}}),
      new Rule("prev or next 5x", new Partner[][]{{PREVIOUS}, {NEXT}}),
      new Rule("next any", new Partner[][]{{new Partner(1, 1, ANY)}}),
      new Rule("prev or next any",
          new Partner[][]{{new Partner(-1, 1, ANY)}, {new Partner(1, 1, ANY)}}),
      new Rule("next 5x 2x window", new Partner[][]{{new Partner(1, 2, LOG_JUMP)}}),
      new Rule("next but one 5x", new Partner[][]{{NEXT_2}}),
      new Rule("trace of 3 5x",
          new Partner[][]{{PREVIOUS, NEXT}, {PREVIOUS, PREVIOUS_2}, {NEXT, NEXT_2}}));

  private final List<String> report = new ArrayList<>();

  @BeforeAll
  void init() {
    MZmineTestUtil.startMzmineCore();
    report.add("# Signal persistence rules\n");
  }

  @NotNull List<ChromatogramBenchmarkDataset> datasets() {
    return ChromatogramBenchmarkDatasets.selected();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("datasets")
  void compareRules(@NotNull ChromatogramBenchmarkDataset dataset) throws Exception {
    final String unavailable = ChromatogramBenchmarkDatasets.unavailableReason(dataset);
    Assumptions.assumeTrue(unavailable == null, unavailable);
    MZmineTestUtil.clearProjectAndLibraries();
    final TaskResult imported = MZmineTestUtil.importFiles(dataset.paths(), 3600,
        dataset.importParameters());
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, imported, imported.description());
    final RawDataFile[] files = ProjectService.getProject().getCurrentRawDataFiles()
        .toArray(RawDataFile[]::new);
    final List<RawDataFile> fileList = List.of(files);
    final List<Scan[]> selected = fileList.stream()
        .map(f -> dataset.scanSelection().getMatchingScans(f)).toList();
    final List<MzIntensityScans> samples = new ArrayList<>();
    for (final int i : FastChromatogramBuilderTask.selectSampleFiles(fileList, selected,
        FastChromatogramBuilderTask.MAX_SAMPLE_FILES)) {
      samples.add(new ScanDataAccessScans(EfficientDataAccess.of(files[i], ScanDataType.MASS_LIST,
          Arrays.asList(selected.get(i)))));
    }
    final BuilderParameterEstimate estimate = Objects.requireNonNull(
        BuilderParameterEstimation.estimate(samples, ChromatogramBuilderSensitivity.MEDIUM,
            ChromatogramBenchmarkDatasets.ORBITRAP_PRESET, null));
    final MZTolerance window = estimate.nearWindow();

    final long[] counts = new long[BINS];
    final long[][] near = new long[RULES.size()][BINS];
    final long[][] control = new long[RULES.size()][BINS];
    for (final MzIntensityScans scans : samples) {
      final List<double[][]> all = load(scans);
      final int stride = Math.max(1, all.size() / MzToleranceEstimation.MAX_SCAN_PAIRS_PER_FILE);
      for (int k = 2; k + 2 < all.size(); k += stride) {
        classify(all.subList(k - 2, k + 3), window, counts, near, control);
      }
    }

    report.add("## %s\n".formatted(dataset.name()));
    report.add("near window %s, estimate N50 %.3G, N95 %.3G, peak width %.0f scans\n".formatted(
        window, estimate.noiseLevel(), estimate.signalLevel(), estimate.peakWidthScans()));
    report.add("| rule | N50 | N95 |\n|---|---|---|");
    for (int r = 0; r < RULES.size(); r++) {
      report.add("| %s | %.3G | %.3G |".formatted(RULES.get(r).name(),
          level(counts, near[r], control[r], BuilderParameterEstimation.NOISE_SIGNAL_FRACTION),
          level(counts, near[r], control[r], BuilderParameterEstimation.SIGNAL_FRACTION)));
    }
    report.add("\nnear / control / signal fraction per intensity bin\n");
    final StringBuilder header = new StringBuilder("| intensity from | data points |");
    for (final Rule rule : RULES) {
      header.append(' ').append(rule.name()).append(" |");
    }
    report.add(header.toString());
    report.add("|---|---|" + "---|".repeat(RULES.size()));
    for (int b = 0; b < BINS; b++) {
      if (counts[b] < SignalPersistenceProfile.MIN_DATA_POINTS_PER_BIN) {
        continue;
      }
      final StringBuilder row = new StringBuilder(
          "| %.3G | %d |".formatted(SignalPersistenceProfile.binLowerEdge(b), counts[b]));
      for (int r = 0; r < RULES.size(); r++) {
        final double n = (double) near[r][b] / counts[b];
        final double c = (double) control[r][b] / counts[b];
        row.append(" %.2f / %.2f / %.2f |".formatted(n, c, signal(n, c)));
      }
      report.add(row.toString());
    }
    report.add("");
    writeReport();
  }

  private void writeReport() throws IOException {
    final Path out = Path.of(
        System.getProperty(PROPERTY + "out", "build/reports/chromatogram-builder-benchmark"));
    Files.createDirectories(out);
    Files.writeString(out.resolve("signal-persistence.md"), String.join("\n", report));
  }

  private static double signal(double near, double control) {
    return control >= 1d ? 0d : Math.max(0d, (near - control) / (1d - control));
  }

  /**
   * The search of {@link SignalPersistenceProfile#intensityAtSignalFraction}.
   */
  private static double level(long[] counts, long[] near, long[] control, double fraction) {
    final List<Integer> bins = new ArrayList<>();
    final List<Double> fractions = new ArrayList<>();
    for (int b = 0; b < BINS; b++) {
      if (counts[b] >= SignalPersistenceProfile.MIN_DATA_POINTS_PER_BIN) {
        bins.add(b);
        fractions.add(signal((double) near[b] / counts[b], (double) control[b] / counts[b]));
      }
    }
    for (int k = bins.size() - 1; k >= 0; k--) {
      if (fractions.get(k) >= fraction || (k > 0 && fractions.get(k - 1) >= fraction)) {
        continue;
      }
      if (k == bins.size() - 1) {
        return Double.NaN;
      }
      int above = k + 1;
      while (above < bins.size() && fractions.get(above) < fraction) {
        above++;
      }
      if (above == bins.size()) {
        return Double.NaN;
      }
      final double logBelow = Math.log10(SignalPersistenceProfile.binCenter(bins.get(k)));
      final double logAbove = Math.log10(SignalPersistenceProfile.binCenter(bins.get(above)));
      final double t = (fraction - fractions.get(k)) / (fractions.get(above) - fractions.get(k));
      return Math.pow(10d, logBelow + Math.clamp(t, 0d, 1d) * (logAbove - logBelow));
    }
    return bins.isEmpty() ? Double.NaN : SignalPersistenceProfile.binLowerEdge(bins.getFirst());
  }

  /**
   * @param scans five consecutive scans, the data points of the middle one are classified
   */
  private static void classify(@NotNull List<double[][]> scans, @NotNull MZTolerance window,
      long[] counts, long[][] near, long[][] control) {
    final double[][] center = scans.get(2);
    for (int i = 0; i < center[0].length; i++) {
      final double mz = center[0][i];
      final double intensity = center[1][i];
      final double w = window.getMzToleranceForMass(mz);
      final int bin = SignalPersistenceProfile.bin(intensity);
      counts[bin]++;
      for (int r = 0; r < RULES.size(); r++) {
        if (continues(RULES.get(r), scans, mz, intensity, w, false)) {
          near[r][bin]++;
        }
        if (continues(RULES.get(r), scans, mz, intensity, w, true)) {
          control[r][bin]++;
        }
      }
    }
  }

  /**
   * @param inControl true for the control windows, 2-3 near windows away on both sides
   */
  private static boolean continues(@NotNull Rule rule, @NotNull List<double[][]> scans, double mz,
      double intensity, double window, boolean inControl) {
    for (final Partner[] alternative : rule.alternatives()) {
      boolean all = true;
      for (final Partner partner : alternative) {
        final double w = partner.windowScale() * window;
        final double from = inControl ? SignalPersistenceProfile.CONTROL_START * w : 0d;
        final double to = inControl ? from + w : w;
        if (!hasPartner(scans.get(2 + partner.scanOffset()), mz, intensity, from, to,
            partner.maxLogRatio())) {
          all = false;
          break;
        }
      }
      if (all) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return true if the scan has a data point at an m/z distance in [from, to] within the intensity
   * ratio
   */
  private static boolean hasPartner(@NotNull double[][] scan, double mz, double intensity,
      double from, double to, double maxLogRatio) {
    final double[] mzs = scan[0];
    int j = Arrays.binarySearch(mzs, mz - to);
    j = j >= 0 ? j : -j - 1;
    for (; j < mzs.length && mzs[j] <= mz + to; j++) {
      final double distance = Math.abs(mzs[j] - mz);
      if (distance >= from && distance <= to
          && Math.abs(Math.log(scan[1][j] / intensity)) <= maxLogRatio) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return the valid data points of all scans, sorted by m/z like the mass lists
   */
  @NotNull
  private static List<double[][]> load(@NotNull MzIntensityScans scans) {
    final List<double[][]> all = new ArrayList<>();
    scans.reset();
    while (scans.nextScan()) {
      final int n = scans.getNumberOfDataPoints();
      final double[] mzs = new double[n];
      final double[] intensities = new double[n];
      int valid = 0;
      for (int i = 0; i < n; i++) {
        final double intensity = scans.getIntensity(i);
        if (intensity > 0 && Double.isFinite(intensity)) {
          mzs[valid] = scans.getMz(i);
          intensities[valid++] = intensity;
        }
      }
      all.add(new double[][]{Arrays.copyOf(mzs, valid), Arrays.copyOf(intensities, valid)});
    }
    return all;
  }
}
