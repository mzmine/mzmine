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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.combowithinput.MZToleranceOrAuto;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
 * Compares the automatically estimated m/z tolerance with the preset of the batch wizard and a
 * sweep over tolerances. The quality measure is the number of features after resolving, the
 * estimate should reach the plateau. Run with
 * <pre>
 * gradlew :mzmine-community:benchmark --tests "*ToleranceEstimationBenchmark*"
 * </pre>
 * The data sets and their options are in {@link ChromatogramBenchmarkDatasets}, e.g.,
 * {@code -Dmzmine.test.chrombench.only=GC-EI-QTOF}, the report folder is
 * {@code -Dmzmine.test.chrombench.out=<folder>}.
 */
@Tag("benchmark")
@TestInstance(Lifecycle.PER_CLASS)
class ToleranceEstimationBenchmark {

  private static final Logger logger = Logger.getLogger(
      ToleranceEstimationBenchmark.class.getName());

  private static final float RT_TOLERANCE = 0.03f;
  // decision: the estimate reached the feature plateau if it has 99% of the features of the best
  // tolerance of the sweep
  private static final double PLATEAU = 0.99;

  private final List<String> report = new ArrayList<>();
  private final List<String> summary = new ArrayList<>();

  @BeforeAll
  void init() {
    MZmineTestUtil.startMzmineCore();
    report.add("# m/z tolerance estimation\n");
    report.add("""
        Features after the local minimum resolver (chromatographic threshold 0.9, search range \
        0.04 min, top/edge 2). Relative features are relative to the maximum of all tolerances of \
        a data set. Features are matched within the preset tolerance and %.2f min. The estimate \
        is at the plateau with at least %.0f%% of the features of the best tolerance.
        """.formatted(RT_TOLERANCE, PLATEAU * 100));
  }

  @NotNull List<ChromatogramBenchmarkDataset> datasets() {
    return ChromatogramBenchmarkDatasets.selected();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("datasets")
  void estimateVersusSweep(@NotNull ChromatogramBenchmarkDataset dataset) throws Exception {
    final String unavailable = ChromatogramBenchmarkDatasets.unavailableReason(dataset);
    if (unavailable != null) {
      report.add("## %s\n\nSkipped: %s\n".formatted(dataset.name(), unavailable));
      summary.add("| %s | skipped |  |  |  |  |  |  |".formatted(dataset.name()));
      writeReport();
    }
    Assumptions.assumeTrue(unavailable == null, unavailable);
    run(dataset);
    writeReport();
  }

  private void run(@NotNull ChromatogramBenchmarkDataset dataset) throws Exception {
    MZmineTestUtil.clearProjectAndLibraries();
    final TaskResult imported = MZmineTestUtil.importFiles(dataset.paths(), 3600,
        dataset.importParameters());
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, imported, imported.description());
    final MZmineProject project = ProjectService.getProject();
    final RawDataFile[] files = project.getCurrentRawDataFiles().toArray(RawDataFile[]::new);
    long numDataPoints = 0;
    for (final RawDataFile file : files) {
      for (final Scan scan : dataset.scanSelection().getMatchingScans(file)) {
        final MassList masses = scan.getMassList();
        numDataPoints += masses == null ? 0 : masses.getNumberOfDataPoints();
      }
    }

    report.add("## %s\n".formatted(dataset.name()));
    report.add(
        "%s, %d data points in the builder scans\n".formatted(dataset.describe(), numDataPoints));

    // the main task on all files, warm up first
    runMainTask(files, dataset, MZToleranceOrAuto.custom(dataset.preset()), "warmup", true);
    long start = System.nanoTime();
    final FastChromatogramBuilderTask customTask = runMainTask(files, dataset,
        MZToleranceOrAuto.custom(dataset.preset()), "custom", false);
    final long customNanos = System.nanoTime() - start;
    start = System.nanoTime();
    final FastChromatogramBuilderTask autoTask = runMainTask(files, dataset,
        MZToleranceOrAuto.auto(null), "auto", false);
    final long autoNanos = System.nanoTime() - start;
    final MzToleranceEstimate estimate = autoTask.getToleranceEstimate();
    // without estimate, auto uses the fallback tolerance
    final MZTolerance autoTolerance = Objects.requireNonNull(autoTask.getUsedTolerance());
    report.add("""
        Estimate: %s
        
        Main task on all files: custom %.0f ms, auto %.0f ms including the estimation
        """.formatted(estimate != null ? estimate
        : "none, auto uses %s. %s".formatted(format(autoTolerance),
            diagnoseMissingEstimate(files, dataset)), customNanos / 1e6, autoNanos / 1e6));
    removeAll(customTask.getFeatureLists());
    removeAll(autoTask.getFeatureLists());

    final List<MZTolerance> tolerances = new ArrayList<>();
    final List<String> sources = new ArrayList<>();
    tolerances.add(dataset.preset());
    sources.add("preset");
    tolerances.add(autoTolerance);
    sources.add(estimate != null ? "estimate" : "auto fallback");
    for (final double ppm : dataset.ppmGrid()) {
      tolerances.add(new MZTolerance(0, ppm));
      sources.add("");
    }

    final List<String> rows = new ArrayList<>();
    final long[] features = new long[tolerances.size()];
    List<FeatureList> presetResolved = List.of();
    for (int t = 0; t < tolerances.size(); t++) {
      final FastChromatogramBuilderTask task = runMainTask(files, dataset,
          MZToleranceOrAuto.custom(tolerances.get(t)), "t" + t, false);
      long chromatograms = 0;
      long dataPoints = 0;
      long holeFills = 0;
      for (final FastChromatogramBuilderStatistics statistics : task.getStatistics()) {
        chromatograms += statistics.numChromatograms();
        holeFills += statistics.numHoleFills();
      }
      final List<FeatureList> resolved = new ArrayList<>();
      for (final ModularFeatureList flist : task.getFeatureLists()) {
        dataPoints += flist.getRows().stream()
            .mapToLong(r -> r.getBestFeature().getFeatureData().getNumberOfValues()).sum();
        resolved.add(resolve(flist, dataset));
      }
      if (t == 0) {
        presetResolved = resolved;
      }
      long numFeatures = 0;
      long duplicates = 0;
      long matchedInPreset = 0;
      long presetFound = 0;
      long presetFeatures = 0;
      for (int f = 0; f < resolved.size(); f++) {
        final var own = ResolvedFeatureMetrics.evaluate(resolved.get(f), presetResolved.get(f),
            dataset.preset(), RT_TOLERANCE);
        final var back = ResolvedFeatureMetrics.evaluate(presetResolved.get(f), resolved.get(f),
            dataset.preset(), RT_TOLERANCE);
        numFeatures += own.features();
        duplicates += own.duplicatePairs();
        matchedInPreset += own.matched();
        presetFound += back.matched();
        presetFeatures += back.features();
      }
      features[t] = numFeatures;
      rows.add("| %s | %s | %d | %d | %d | %d | %%s | %d | %.4f | %.4f |".formatted(
          format(tolerances.get(t)), sources.get(t), chromatograms, dataPoints, holeFills,
          numFeatures, duplicates, matchedInPreset / (double) Math.max(1, numFeatures),
          presetFound / (double) Math.max(1, presetFeatures)));
      removeAll(task.getFeatureLists());
      if (t > 0) {
        removeAll(resolved);
      }
    }
    int best = 0;
    for (int t = 1; t < features.length; t++) {
      if (features[t] > features[best]) {
        best = t;
      }
    }
    final double maxFeatures = Math.max(1, features[best]);
    report.add("""
        | tolerance | source | chromatograms | chromatogram data points | hole fills | features | relative features | duplicate pairs | found in preset | preset found |
        |---|---|---|---|---|---|---|---|---|---|""");
    for (int t = 0; t < rows.size(); t++) {
      report.add(rows.get(t).formatted("%.4f".formatted(features[t] / maxFeatures)));
    }
    report.add("");
    removeAll(presetResolved);

    final double estimateRelative = features[1] / maxFeatures;
    summary.add(
        "| %s | %d | %s | %.4f | %.4f | %s | %s | %.0f |".formatted(dataset.name(), numDataPoints,
            estimate != null ? format(autoTolerance) : "none, fallback " + format(autoTolerance),
            estimateRelative, features[0] / maxFeatures, format(tolerances.get(best)),
            estimateRelative >= PLATEAU ? "yes" : "no", estimationNanos(files, dataset) / 1e6));
  }

  /**
   * @return the time of the estimation alone on the sample files of the task, median of 3
   */
  private static long estimationNanos(@NotNull RawDataFile[] files,
      @NotNull ChromatogramBenchmarkDataset dataset) {
    final List<MzIntensityScans> samples = sampleScans(files, dataset);
    final long[] nanos = new long[3];
    for (int i = 0; i < nanos.length; i++) {
      final long start = System.nanoTime();
      MzToleranceEstimation.estimate(samples, dataset.minConsecutive(), dataset.minGroup(),
          dataset.minHeight(), null);
      nanos[i] = System.nanoTime() - start;
    }
    Arrays.sort(nanos);
    return nanos[1];
  }

  /**
   * @return the scans of the files that the task samples for the estimation
   */
  @NotNull
  private static List<MzIntensityScans> sampleScans(@NotNull RawDataFile[] files,
      @NotNull ChromatogramBenchmarkDataset dataset) {
    final List<RawDataFile> fileList = List.of(files);
    final List<Scan[]> selectedScans = fileList.stream()
        .map(f -> dataset.scanSelection().getMatchingScans(f)).toList();
    final List<MzIntensityScans> samples = new ArrayList<>();
    for (final int i : FastChromatogramBuilderTask.selectSampleFiles(fileList, selectedScans,
        FastChromatogramBuilderTask.MAX_SAMPLE_FILES)) {
      samples.add(new ScanDataAccessScans(EfficientDataAccess.of(files[i], ScanDataType.MASS_LIST,
          Arrays.asList(selectedScans.get(i)))));
    }
    return samples;
  }

  /**
   * The estimation needs pairs of consecutive signals that scatter much less than neighboring data
   * points of one scan are apart, see {@link MzToleranceEstimation}.
   *
   * @return the scatter of the pairs and the spacing of the sample files
   */
  @NotNull
  private static String diagnoseMissingEstimate(@NotNull RawDataFile[] files,
      @NotNull ChromatogramBenchmarkDataset dataset) {
    final ConsecutiveSignalPairs pairs = new ConsecutiveSignalPairs();
    for (final MzIntensityScans scans : sampleScans(files, dataset)) {
      pairs.addScans(scans,
          Math.max(1, scans.getNumberOfScans() / MzToleranceEstimation.MAX_SCAN_PAIRS_PER_FILE));
    }
    final MzScatterModel scatter = pairs.fitScatter();
    return """
        Consecutive scans: %d pairs in %d scan pairs, scatter %s ppm, median spacing of \
        neighboring data points %.2f ppm, the estimate needs a spacing of at least %.0f times the \
        scatter""".formatted(pairs.getNumPairs(), pairs.getNumScanPairs(),
        scatter == null ? "none" : "%.2f".formatted(scatter.sigmaPpm()), pairs.medianSpacingPpm(),
        MzToleranceEstimation.MIN_SPACING_TO_SCATTER);
  }

  @NotNull
  private FastChromatogramBuilderTask runMainTask(@NotNull RawDataFile[] files,
      @NotNull ChromatogramBenchmarkDataset dataset, @NotNull MZToleranceOrAuto tolerance,
      @NotNull String suffix, boolean removeLists) {
    final ParameterSet parameters = FastChromatogramBuilderParameters.create(
        new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES), dataset.scanSelection(),
        dataset.minConsecutive(), tolerance, suffix, dataset.minGroup(), dataset.minHeight(),
        false);
    final FastChromatogramBuilderTask task = new FastChromatogramBuilderTask(
        ProjectService.getProject(), files, parameters, null, Instant.now(),
        FastChromatogramBuilderModule.class);
    task.run();
    Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    if (removeLists) {
      removeAll(task.getFeatureLists());
    }
    return task;
  }

  @NotNull
  private static FeatureList resolve(@NotNull ModularFeatureList flist,
      @NotNull ChromatogramBenchmarkDataset dataset) throws InterruptedException {
    final String suffix = "resolved";
    final MinimumSearchFeatureResolverParameters parameters = MinimumSearchFeatureResolverParameters.create(
        new FeatureListsSelection(flist), suffix, OriginalFeatureListOption.KEEP, false,
        GroupMS2SubParameters.createDefault(), ResolvingDimension.RETENTION_TIME, 0.9, 0.04, 0d,
        dataset.minHeight(), 2d, Range.closed(0d, 1.2d), dataset.minConsecutive());
    final TaskResult resolved = MZmineTestUtil.callModuleWithTimeout(1200,
        MinimumSearchFeatureResolverModule.class, parameters);
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, resolved, resolved.description());
    final FeatureList result = find(flist.getName() + " " + suffix);
    Assertions.assertNotNull(result, "No resolved list for " + flist.getName());
    return result;
  }

  @Nullable
  private static FeatureList find(@NotNull String name) {
    for (final FeatureList flist : ProjectService.getProject().getCurrentFeatureLists()) {
      if (flist.getName().equals(name)) {
        return flist;
      }
    }
    return null;
  }

  private static void removeAll(@NotNull List<? extends FeatureList> lists) {
    if (!lists.isEmpty()) {
      ProjectService.getProject().removeFeatureList(lists.toArray(FeatureList[]::new));
    }
  }

  @NotNull
  private static String format(@NotNull MZTolerance tolerance) {
    return "%.4f m/z or %.1f ppm".formatted(tolerance.getMzTolerance(),
        tolerance.getPpmTolerance());
  }

  private void writeReport() throws IOException {
    final Path out = Path.of(
        System.getProperty(PROPERTY + "out", "build/reports/chromatogram-builder-benchmark"));
    Files.createDirectories(out);
    final List<String> lines = new ArrayList<>(report);
    lines.add("""
        ## Summary
        
        The estimation time is measured alone on the sample files of the task.
        
        | data set | data points | estimate | estimate relative features | preset relative features | best tolerance | estimate at plateau | estimation ms |
        |---|---|---|---|---|---|---|---|""");
    lines.addAll(summary);
    final String text = String.join("\n", lines) + "\n";
    Files.writeString(out.resolve("tolerance-estimation.md"), text);
    logger.info("m/z tolerance estimation benchmark\n" + text);
  }
}
