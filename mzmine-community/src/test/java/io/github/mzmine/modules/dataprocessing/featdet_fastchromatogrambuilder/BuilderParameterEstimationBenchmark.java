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
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ChromatogramBuilderSettings;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.SignalPersistenceProfile.IntensityBin;
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
 * Compares the parameters that {@link BuilderParameterEstimation} determines for each
 * {@link ChromatogramBuilderSensitivity} with the batch wizard settings of the data sets, and the
 * features after resolving. Run with
 * <pre>
 * gradlew :mzmine-community:benchmark --tests "*BuilderParameterEstimationBenchmark*"
 * </pre>
 * The data sets and their options are in {@link ChromatogramBenchmarkDatasets}, e.g.,
 * {@code -Dmzmine.test.chrombench.only=GC-EI-QTOF}, the report folder is
 * {@code -Dmzmine.test.chrombench.out=<folder>}. {@code -Dmzmine.test.chrombench.resolve=false}
 * only reports the estimates.
 */
@Tag("benchmark")
@TestInstance(Lifecycle.PER_CLASS)
class BuilderParameterEstimationBenchmark {

  private static final Logger logger = Logger.getLogger(
      BuilderParameterEstimationBenchmark.class.getName());

  private static final float RT_TOLERANCE = 0.03f;

  private final List<String> report = new ArrayList<>();
  private final List<String> summary = new ArrayList<>();

  @BeforeAll
  void init() {
    MZmineTestUtil.startMzmineCore();
    report.add("# Chromatogram builder parameter estimation\n");
    report.add("""
        Fast (auto) per sensitivity versus the batch wizard settings of each data set. Features \
        after the local minimum resolver (chromatographic threshold 0.9, search range 0.04 min, \
        top/edge 2, min height and min scans of the run). Found: features of one list with a \
        feature of the other within the preset tolerance and %.2f min.
        """.formatted(RT_TOLERANCE));
  }

  @NotNull List<ChromatogramBenchmarkDataset> datasets() {
    return ChromatogramBenchmarkDatasets.selected();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("datasets")
  void estimateVersusWizard(@NotNull ChromatogramBenchmarkDataset dataset) throws Exception {
    final String unavailable = ChromatogramBenchmarkDatasets.unavailableReason(dataset);
    if (unavailable != null) {
      report.add("## %s\n\nSkipped: %s\n".formatted(dataset.name(), unavailable));
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
    final RawDataFile[] files = ProjectService.getProject().getCurrentRawDataFiles()
        .toArray(RawDataFile[]::new);

    report.add("## %s\n".formatted(dataset.name()));
    report.add(dataset.describe() + "\n");

    final List<MzIntensityScans> samples = sampleScans(files, dataset);
    final ChromatogramBuilderSensitivity[] sensitivities = ChromatogramBuilderSensitivity.values();
    final BuilderParameterEstimate[] estimates = new BuilderParameterEstimate[sensitivities.length];
    final double[] millis = new double[sensitivities.length];
    for (int s = 0; s < sensitivities.length; s++) {
      final long start = System.nanoTime();
      estimates[s] = Objects.requireNonNull(
          BuilderParameterEstimation.estimate(samples, sensitivities[s],
              ChromatogramBenchmarkDatasets.ORBITRAP_PRESET, null));
      millis[s] = (System.nanoTime() - start) / 1e6;
    }

    final BuilderParameterEstimate first = estimates[0];
    report.add("""
        Noise level %.3G, signal level %.3G%s, lowest intensity %.3G, near window %s, peak width %s
        """.formatted(first.noiseLevel(), first.signalLevel(),
        first.levelsFound() ? "" : " (fallback)",
        first.minIntensity(), format(first.nearWindow()),
        Double.isFinite(first.peakWidthScans()) ? "%.0f scans (%d peaks)".formatted(
            first.peakWidthScans(), first.numWidthPeaks()) : "unknown"));
    final BuilderParameterEstimate medium = estimates[ChromatogramBuilderSensitivity.MEDIUM.ordinal()];
    report.add("Estimation of Medium: %s, of which test build %d ms\n".formatted(medium.timings(),
        medium.toleranceEstimate() == null ? 0 : medium.toleranceEstimate().testBuildMs()));
    report.add("""
        | intensity from | data points | near hits | control hits | signal fraction |
        |---|---|---|---|---|""");
    for (final IntensityBin bin : first.profile()) {
      report.add("| %.3G | %d | %.3f | %.3f | %.3f |".formatted(bin.lowerEdge(),
          bin.numDataPoints(), bin.nearFraction(), bin.controlFraction(), bin.signalFraction()));
    }
    report.add("");

    final boolean resolve = Boolean.parseBoolean(System.getProperty(PROPERTY + "resolve", "true"));
    final ChromatogramBuilderSettings wizard = new ChromatogramBuilderSettings(
        dataset.minConsecutive(), dataset.minGroup(), dataset.minHeight(), dataset.preset());
    final List<FeatureList> wizardResolved =
        resolve ? buildAndResolve(files, dataset, wizard, "wizard") : List.of();
    report.add("""
        | settings | min consecutive | min group intensity | min height | m/z tolerance | peak width | estimation ms | features | found in wizard | wizard found |
        |---|---|---|---|---|---|---|---|---|---|""");
    report.add("| wizard | %d | %.3G | %.3G | %s |  |  | %d |  |  |".formatted(
        wizard.minConsecutiveScans(), wizard.minGroupIntensity(), wizard.minHeight(),
        format(wizard.mzTolerance()), countFeatures(wizardResolved)));
    final StringBuilder summaryRow = new StringBuilder(
        "| %s | %.3G / %.3G | %d / %.2G / %.2G | %d |".formatted(dataset.name(),
            first.noiseLevel(), first.signalLevel(),
            wizard.minConsecutiveScans(), wizard.minGroupIntensity(), wizard.minHeight(),
            countFeatures(wizardResolved)));
    for (int s = 0; s < sensitivities.length; s++) {
      final ChromatogramBuilderSettings settings = estimates[s].settings();
      long features = 0;
      long foundInWizard = 0;
      long wizardFound = 0;
      long wizardFeatures = 0;
      if (resolve) {
        final List<FeatureList> resolved = buildAndResolve(files, dataset, settings,
            sensitivities[s].name());
        for (int f = 0; f < resolved.size(); f++) {
          final var own = ResolvedFeatureMetrics.evaluate(resolved.get(f), wizardResolved.get(f),
              dataset.preset(), RT_TOLERANCE);
          final var back = ResolvedFeatureMetrics.evaluate(wizardResolved.get(f), resolved.get(f),
              dataset.preset(), RT_TOLERANCE);
          features += own.features();
          foundInWizard += own.matched();
          wizardFound += back.matched();
          wizardFeatures += back.features();
        }
        removeAll(resolved);
      }
      report.add("| %s | %d | %.3G | %.3G | %s | %s | %.0f | %d | %.4f | %.4f |".formatted(
          sensitivities[s], settings.minConsecutiveScans(), settings.minGroupIntensity(),
          settings.minHeight(), format(settings.mzTolerance()),
          Double.isFinite(estimates[s].peakWidthScans()) ? "%.0f".formatted(
              estimates[s].peakWidthScans()) : "-", millis[s], features,
          foundInWizard / (double) Math.max(1, features),
          wizardFound / (double) Math.max(1, wizardFeatures)));
      summaryRow.append(" %d / %.2G / %.2G, %d (%.0f%%) |".formatted(
          settings.minConsecutiveScans(), settings.minGroupIntensity(), settings.minHeight(),
          features, 100d * wizardFound / Math.max(1, wizardFeatures)));
    }
    report.add("");
    removeAll(wizardResolved);
    summary.add(summaryRow.toString());
  }

  /**
   * Builds with the fast algorithm and the settings, resolves and removes the chromatograms.
   */
  @NotNull
  private static List<FeatureList> buildAndResolve(@NotNull RawDataFile[] files,
      @NotNull ChromatogramBenchmarkDataset dataset, @NotNull ChromatogramBuilderSettings settings,
      @NotNull String suffix) throws InterruptedException {
    final ParameterSet parameters = ADAPChromatogramBuilderParameters.createFast(
        new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES), dataset.scanSelection(),
        settings.minConsecutiveScans(), MZToleranceOrAuto.custom(settings.mzTolerance()), suffix,
        settings.minGroupIntensity(), settings.minHeight(), false);
    final FastChromatogramBuilderTask task = new FastChromatogramBuilderTask(
        ProjectService.getProject(), files, parameters, null, Instant.now(),
        ModularADAPChromatogramBuilderModule.class);
    task.run();
    Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    final List<FeatureList> resolved = new ArrayList<>();
    for (final ModularFeatureList flist : task.getFeatureLists()) {
      resolved.add(resolve(flist, settings));
    }
    removeAll(task.getFeatureLists());
    return resolved;
  }

  @NotNull
  private static FeatureList resolve(@NotNull ModularFeatureList flist,
      @NotNull ChromatogramBuilderSettings settings) throws InterruptedException {
    final String suffix = "resolved";
    final MinimumSearchFeatureResolverParameters parameters = MinimumSearchFeatureResolverParameters.create(
        new FeatureListsSelection(flist), suffix, OriginalFeatureListOption.KEEP, false,
        GroupMS2SubParameters.createDefault(), ResolvingDimension.RETENTION_TIME, 0.9, 0.04, 0d,
        settings.minHeight(), 2d, Range.closed(0d, 1.2d), settings.minConsecutiveScans());
    final TaskResult resolved = MZmineTestUtil.callModuleWithTimeout(1200,
        MinimumSearchFeatureResolverModule.class, parameters);
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, resolved, resolved.description());
    final FeatureList result = find(flist.getName() + " " + suffix);
    Assertions.assertNotNull(result, "No resolved list for " + flist.getName());
    return result;
  }

  private static long countFeatures(@NotNull List<FeatureList> lists) {
    return lists.stream().mapToLong(FeatureList::getNumberOfRows).sum();
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

        min consecutive / min group intensity / min height, features (wizard features found)

        | data set | noise / signal level | wizard | wizard features | sensitive | medium | abundant |
        |---|---|---|---|---|---|---|""");
    lines.addAll(summary);
    final String text = String.join("\n", lines) + "\n";
    Files.writeString(out.resolve("parameter-estimation.md"), text);
    logger.info("Chromatogram builder parameter estimation benchmark\n" + text);
  }
}
