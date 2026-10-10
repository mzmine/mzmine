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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimationContext;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimators;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameterSet;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.RawDataAnalysis;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.DataFileStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.MzToleranceSearchOptions;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataPreparation;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.ProjectService;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestInstance;
import testutils.MZmineTestUtil;

/**
 * Dumps the raw distributions {@link ParameterEstimators} takes its quantiles from, per
 * dataset, so alternative derivations can be evaluated offline.
 * <p>
 * Why this exists: the estimate and the variable's own bounds are quantiles of the <em>same</em>
 * array - min height is the median of the lowest isotope heights while its range is the 5th to 95th
 * percentile of them - so the question "is the estimate biased" is really "which quantile should it
 * be". Answering that by rerunning optimizations costs a batch per candidate; answering it from a
 * dump costs nothing.
 * <p>
 * This runs the statistics pass only, no batch and no optimizer, so it is minutes for every dataset
 * together rather than per dataset. Datasets come from {@link EstimateVsOptimumTest#DATASETS}.
 */
@TestInstance(Lifecycle.PER_CLASS)
@Tag("benchmark")
public class EstimatorStatisticsDumpTest {

  private static final Logger logger = Logger.getLogger(
      EstimatorStatisticsDumpTest.class.getName());

  private static final String VALUES_CSV = "autoparam-statistics-values.csv";
  private static final String SUMMARY_CSV = "autoparam-statistics-summary.csv";

  /**
   * Quantiles reported per distribution. Deliberately dense at the bottom, because every parameter
   * whose optimum was measurable landed at or just above the 5th percentile.
   */
  private static final double[] QUANTILES = {0.0, 0.01, 0.02, 0.05, 0.10, 0.15, 0.20, 0.25, 0.30,
      0.40, 0.50, 0.60, 0.70, 0.80, 0.90, 0.95, 0.98, 0.99, 0.995, 1.0};

  /**
   * Share of the files a row has to be detected in before its deviations count, matching what
   * {@link ParameterEstimators} uses.
   */
  private static final double MIN_DETECTION_SHARE = 0.8;

  @BeforeAll
  public void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  @Test
  @DisplayName("dump the statistics distributions the estimator derives its values from")
  void dumpStatistics() {
    Assumptions.assumeTrue(Boolean.getBoolean(EstimateVsOptimumTest.RUN_PROPERTY),
        "real-data optimizer benchmark is opt-in; enable with -D%s=true".formatted(
            EstimateVsOptimumTest.RUN_PROPERTY));
    final List<BenchmarkDataset> available = EstimateVsOptimumTest.DATASETS.stream()
        .filter(EstimateVsOptimumTest::isSelected).filter(BenchmarkDataset::isAvailable).toList();
    Assumptions.assumeFalse(available.isEmpty(), "no configured dataset is present");

    final File values = BenchmarkCampaign.outputDirectory().resolve(VALUES_CSV).toFile()
        .getAbsoluteFile();
    final File summary = BenchmarkCampaign.outputDirectory().resolve(SUMMARY_CSV).toFile()
        .getAbsoluteFile();
    int dumped = 0;

    try (final PrintWriter valueWriter = new PrintWriter(Files.newBufferedWriter(values.toPath(),
        StandardCharsets.UTF_8)); final PrintWriter summaryWriter = new PrintWriter(
        Files.newBufferedWriter(summary.toPath(), StandardCharsets.UTF_8))) {
      valueWriter.println("dataset,distribution,value");
      summaryWriter.println("dataset,detector,distribution,n," + Arrays.stream(QUANTILES)
          .mapToObj(q -> "q%02.0f".formatted(q * 100)).reduce((a, b) -> a + "," + b).orElse(""));

      for (final BenchmarkDataset dataset : available) {
        try {
          dump(dataset, valueWriter, summaryWriter);
          dumped++;
        } catch (Exception e) {
          logger.log(Level.WARNING, "dataset %s failed, continuing".formatted(dataset.name()), e);
        }
        ProjectService.getProjectManager().clearProject();
      }
    } catch (IOException e) {
      throw new RuntimeException("cannot write the dump", e);
    }

    logger.info("dumped %d datasets to%n  %s%n  %s".formatted(dumped, values, summary));
    Assertions.assertTrue(dumped > 0, "no dataset produced statistics");
  }

  private void dump(@NotNull BenchmarkDataset dataset, @NotNull PrintWriter valueWriter,
      @NotNull PrintWriter summaryWriter) {
    final List<RawDataFile> files = RawDataPreparation.importFilesBlocking(dataset.rawFiles(),
        dataset.metadataFile());
    final List<DataFileStatistics> stats = RawDataPreparation.computeFileStatistics(files, null,
        null, PolarityType.ANY);

    final RawDataAnalysis analysis = RawDataAnalysis.analyze(stats);
    final WizardSequence sequence = new WizardSequence();
    sequence.set(WizardPart.IMS, dataset.ionMobility().create());
    sequence.set(WizardPart.ION_INTERFACE, dataset.ionInterface().create());
    sequence.set(WizardPart.MS, dataset.massSpectrometer().create());
    final ParameterEstimationContext context = new ParameterEstimationContext(analysis, sequence);
    final PreparedParameterSet estimates = PreparedParameterSet.prepare(context);

    final Map<String, double[]> distributions = new LinkedHashMap<>();
    distributions.put("edgeIntensities", flatten(stats, DataFileStatistics::getEdgeIntensities));
    distributions.put("isotopeFwhms", flatten(stats, DataFileStatistics::getIsotopePeakFwhms));
    distributions.put("lowestIsotopeHeights",
        flatten(stats, DataFileStatistics::getLowestIsotopeHeights));
    distributions.put("lowestIsotopeDataPoints",
        stats.stream().map(DataFileStatistics::getNumberOfLowestIsotopeDataPoints)
            .flatMapToInt(Arrays::stream).mapToDouble(i -> i).toArray());

    distributions.put("rtDeviations", analysis.rtDeviations());

    final MassDetectorWizardOptions detector = context.massDetectorType();
    for (final Map.Entry<String, double[]> entry : distributions.entrySet()) {
      final double[] sorted = entry.getValue().clone();
      Arrays.sort(sorted);
      for (final double value : sorted) {
        valueWriter.printf(Locale.ROOT, "%s,%s,%s%n", dataset.name(), entry.getKey(), value);
      }
      summaryWriter.printf(Locale.ROOT, "%s,%s,%s,%d", dataset.name(), detector, entry.getKey(),
          sorted.length);
      for (final double q : QUANTILES) {
        summaryWriter.printf(Locale.ROOT, ",%s", sorted.length == 0 ? ""
            : io.github.mzmine.util.MathUtils.calcQuantileSorted(sorted, q));
      }
      summaryWriter.println();
    }

    // the m/z tolerance is a discrete choice rather than a distribution, so it is dumped as counts
    final Map<MZTolerance, Integer> counts = new LinkedHashMap<>();
    for (final DataFileStatistics stat : stats) {
      stat.extractToleranceCounts().forEach((tol, count) -> counts.merge(tol, count, Integer::sum));
    }
    counts.forEach((tol, count) -> {
      for (int i = 0; i < count; i++) {
        valueWriter.printf(Locale.ROOT, "%s,mzToleranceIndex,%d%n", dataset.name(),
            MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS.indexOf(tol));
      }
    });

    logger.info(
        "%s (%s, %d files): %s | estimates %s".formatted(dataset.name(), detector, files.size(),
            distributions.entrySet().stream()
                .map(e -> "%s n=%d".formatted(e.getKey(), e.getValue().length)).toList(),
            estimates));
  }

  private static double[] flatten(@NotNull List<DataFileStatistics> stats,
      @NotNull java.util.function.Function<DataFileStatistics, double[]> accessor) {
    return stats.stream().map(accessor).flatMapToDouble(Arrays::stream).toArray();
  }
}
