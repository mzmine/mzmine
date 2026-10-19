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

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterDefinition;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblem;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.OptimizationMetrics;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.ShapeScoreDiagnostic;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.MoeadOptimizerParameters;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OptimizerOptions;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OrdinalIntegerVariable;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.PatternSearchOptimizerParameters;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.SolutionOrigin;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.WarmStartSampling;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.MzToleranceSearchOptions;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.moeaframework.core.Solution;
import testutils.MZmineTestUtil;

/**
 * Runs a real optimization per dataset and tabulates what the single-pass estimator guessed against
 * what the optimization found, so the estimator's bias can be compared across datasets.
 * <p>
 * This is a measurement harness, not a regression test: it asserts only that a run completed, and
 * its output is the printed table plus configuration-specific csv files. Every dataset costs a full
 * optimization, so it is minutes to hours depending on {@link #ITERATIONS_PROPERTY}.
 * <p>
 * Enable it explicitly with {@code -Dmzmine.test.autoparam.run=true}, then point it at data with
 * {@code -Dmzmine.test.autoparam.dataRoot=<folder>}. Generated CSV files go to
 * {@code build/autoparam-benchmarks} unless {@code mzmine.test.autoparam.outputDir} is set.
 */
@Tag("benchmark")
@TestInstance(Lifecycle.PER_CLASS)
public class EstimateVsOptimumTest {

  private static final Logger logger = Logger.getLogger(EstimateVsOptimumTest.class.getName());

  static final String RUN_PROPERTY = "mzmine.test.autoparam.run";

  private static final String THERMO_20_YEARS = "Thermo/20 years mzmine/";
  private static final String ZENOTOF_PLASMA = "SCIEX/ZenoTOF/RawData/1_Srm1950_DDA/";
  private static final String ZENOTOF_FECES = "SCIEX/ZenoTOF/RawData/3_Feces_DDA/";
  private static final String TIMSTOF = "Bruker/"
      + "timsTOF/2026_asms_nist_srm_1950_timsMetabo/for_mzio/";
  private static final String AGILENT_6550 = "Agilent/Agilent 6550_Zamboni/";
  private static final String MSV000094173 = "MSV000094173/";

  /**
   * Datasets to compare. Paths are relative to {@code mzmine.test.autoparam.dataRoot}; select a
   * compatible subset when the example-data and MSV collections have different roots.
   * <p>
   * assumption: UHPLC throughout. None of these are labelled with their column, and the ion
   * interface preset only seeds the estimator's retention time ranges, so a wrong choice shows up
   * as an outlier in the FWHM and retention time tolerance rows rather than as a failure. Switch
   * the ones that are actually HPLC once known.
   */
  static final List<BenchmarkDataset> DATASETS = List.of(new BenchmarkDataset("thermo-20y-qc",
          List.of(THERMO_20_YEARS + "171103_PMA_TK_QC_03.mzML",
              THERMO_20_YEARS + "171103_PMA_TK_QC_04.mzML",
              THERMO_20_YEARS + "171103_PMA_TK_QC_05.mzML",
              THERMO_20_YEARS + "171103_PMA_TK_QC_06.mzML",
              THERMO_20_YEARS + "171103_PMA_TK_QC_07.mzML",
              THERMO_20_YEARS + "171103_PMA_TK_QC_08.mzML"), IonInterfaceWizardParameterFactory.UHPLC,
          MassSpectrometerWizardParameterFactory.Orbitrap, IonMobilityWizardParameterFactory.NO_IMS,
          null),

      new BenchmarkDataset("zenotof-plasma-pos",
          List.of(ZENOTOF_PLASMA + "Pos/20230407_plasma_1_POS.mzML",
              ZENOTOF_PLASMA + "Pos/20230407_plasma_2_POS.mzML",
              ZENOTOF_PLASMA + "Pos/20230407_plasma_3_POS.mzML",
              ZENOTOF_PLASMA + "Pos/20230407_plasma_4_POS.mzML",
              ZENOTOF_PLASMA + "Pos/20230407_plasma_5_POS.mzML",
              ZENOTOF_PLASMA + "Pos/20230407_plasma_6_POS.mzML",
              ZENOTOF_PLASMA + "Pos/20230407_plasma_7_POS.mzML"),
          IonInterfaceWizardParameterFactory.UHPLC, MassSpectrometerWizardParameterFactory.QTOF,
          IonMobilityWizardParameterFactory.NO_IMS, null),

      new BenchmarkDataset("zenotof-plasma-neg",
          List.of(ZENOTOF_PLASMA + "Neg/20230407_plasma_1_NEG.mzML",
              ZENOTOF_PLASMA + "Neg/20230407_plasma_2_NEG.mzML",
              ZENOTOF_PLASMA + "Neg/20230407_plasma_3_NEG.mzML",
              ZENOTOF_PLASMA + "Neg/20230407_plasma_4_NEG.mzML",
              ZENOTOF_PLASMA + "Neg/20230407_plasma_5_NEG.mzML",
              ZENOTOF_PLASMA + "Neg/20230407_plasma_6_NEG.mzML",
              ZENOTOF_PLASMA + "Neg/20230407_plasma_7_NEG.mzML",
              ZENOTOF_PLASMA + "Neg/20230407_plasma_8_NEG.mzML"),
          IonInterfaceWizardParameterFactory.UHPLC, MassSpectrometerWizardParameterFactory.QTOF,
          IonMobilityWizardParameterFactory.NO_IMS, null),

      new BenchmarkDataset("zenotof-feces-pos",
          List.of(ZENOTOF_FECES + "Pos/20230406_feces_1_POS.mzML",
              ZENOTOF_FECES + "Pos/20230406_feces_2_POS.mzML",
              ZENOTOF_FECES + "Pos/20230406_feces_3_POS.mzML",
              ZENOTOF_FECES + "Pos/20230406_feces_4_POS.mzML",
              ZENOTOF_FECES + "Pos/20230406_feces_5_POS.mzML",
              ZENOTOF_FECES + "Pos/20230406_feces_6_POS.mzML",
              ZENOTOF_FECES + "Pos/20230406_feces_7_POS.mzML"),
          IonInterfaceWizardParameterFactory.UHPLC, MassSpectrometerWizardParameterFactory.QTOF,
          IonMobilityWizardParameterFactory.NO_IMS, null),

      new BenchmarkDataset("timstof-srm1950-pos",
          List.of(TIMSTOF + "3D-pos_AIPon/SRM1950_50ul_in_100ul_3D-AIPon_pos_2ul_P1-F-3_1_6244.d",
              TIMSTOF + "3D-pos_AIPon/SRM1950_50ul_in_100ul_3D-AIPon_pos_2ul_P1-F-3_1_6245.d",
              TIMSTOF + "3D-pos_AIPon/SRM1950_50ul_in_100ul_3D-AIPon_pos_2ul_P1-F-3_1_6246.d",
              TIMSTOF + "3D-pos_AIPon/SRM1950_50ul_in_100ul_3D-AIPon_pos_5ul_P1-F-3_1_6247.d",
              TIMSTOF + "3D-pos_AIPon/SRM1950_50ul_in_100ul_3D-AIPon_pos_5ul_P1-F-3_1_6248.d",
              TIMSTOF + "3D-pos_AIPon/SRM1950_50ul_in_100ul_3D-AIPon_pos_5ul_P1-F-3_1_6249.d"),
          IonInterfaceWizardParameterFactory.UHPLC, MassSpectrometerWizardParameterFactory.QTOF,
          IonMobilityWizardParameterFactory.NO_IMS, null),

      new BenchmarkDataset("timstof-srm1950-neg",
          List.of(TIMSTOF + "3D-neg_AIPon/SRM1950_50ul_in_100ul_3D-AIPon_neg_2ul_P1-F-4_1_6276.d",
              TIMSTOF + "3D-neg_AIPon/SRM1950_50ul_in_100ul_3D-AIPon_neg_2ul_P1-F-4_1_6277.d",
              TIMSTOF + "3D-neg_AIPon/SRM1950_50ul_in_100ul_3D-AIPon_neg_2ul_P1-F-4_1_6278.d",
              TIMSTOF + "3D-neg_AIPon/SRM1950_50ul_in_100ul_3D-AIPon_neg_5ul_P1-F-4_1_6279.d",
              TIMSTOF + "3D-neg_AIPon/SRM1950_50ul_in_100ul_3D-AIPon_neg_5ul_P1-F-4_1_6280.d",
              TIMSTOF + "3D-neg_AIPon/SRM1950_50ul_in_100ul_3D-AIPon_neg_5ul_P1-F-4_1_6281.d"),
          IonInterfaceWizardParameterFactory.UHPLC, MassSpectrometerWizardParameterFactory.QTOF,
          IonMobilityWizardParameterFactory.NO_IMS, null),

      new BenchmarkDataset("agilent6550-lipidmix",
          List.of(AGILENT_6550 + "LipidMix_0_1x_1.mzML", AGILENT_6550 + "LipidMix_0_1x_2.mzML",
              AGILENT_6550 + "LipidMix_0_1x_3.mzML", AGILENT_6550 + "LipidMix_0_1x_4.mzML",
              AGILENT_6550 + "LipidMix_0_1x_5.mzML"), IonInterfaceWizardParameterFactory.UHPLC,
          MassSpectrometerWizardParameterFactory.QTOF, IonMobilityWizardParameterFactory.NO_IMS,
          null),

      new BenchmarkDataset("msv94173-qc",
          List.of(MSV000094173 + "QC_12.raw", MSV000094173 + "QC_13.raw",
              MSV000094173 + "QC_14.raw", MSV000094173 + "QC_15.raw", MSV000094173 + "QC_20.raw"),
          IonInterfaceWizardParameterFactory.UHPLC, MassSpectrometerWizardParameterFactory.Orbitrap,
          IonMobilityWizardParameterFactory.NO_IMS, null),

      new BenchmarkDataset("msv94173-qc-yellow",
          List.of(MSV000094173 + "QC_yellow_2122.raw", MSV000094173 + "QC_yellow_2207.raw",
              MSV000094173 + "QC_yellow_2264.raw", MSV000094173 + "QC_yellow_2534.raw",
              MSV000094173 + "QC_yellow_2690.raw", MSV000094173 + "QC_yellow_E2_1739.raw",
              MSV000094173 + "QC_yellow_E5_758.raw", MSV000094173 + "QC_yellow_E7_1469.raw",
              MSV000094173 + "QC_yellow_E9_815.raw"), IonInterfaceWizardParameterFactory.UHPLC,
          MassSpectrometerWizardParameterFactory.Orbitrap, IonMobilityWizardParameterFactory.NO_IMS,
          null));

  /**
   * Metric that drives the optimization. Single objective on purpose: with several objectives the
   * front is a trade-off set and there is no single "the optimization found this" value to compare
   * the estimate against.
   */
  private static final SweepMetric METRIC = OptimizationMetrics.ISOTOPE_RATIO_CONSISTENCY_SCORE;

  private static final String OPTIMIZER_PROPERTY = "mzmine.test.autoparam.optimizer";

  /**
   * Full-batch budget. The floor of 30 gives one estimate plus the 20 warm-start solutions and room
   * for part of the first generation, which is enough to see the estimate against its neighbourhood.
   */
  private static final String ITERATIONS_PROPERTY = "mzmine.test.autoparam.iterations";
  private static final int DEFAULT_ITERATIONS = 30;

  private static final String SUMMARY_CSV_STEM = "autoparam-estimate-vs-optimum";

  /**
   * Every proposal of every run, in full: the parameters it ran, every metric, and every diagnostic
   * attribute. The same content the results window exports, with the dataset, seed, proposal index
   * and full-batch index in front of it, so the whole sweep can be inspected in one table.
   */
  private static final String TRAJECTORY_CSV_STEM = "autoparam-all-evaluations";

  /**
   * Optional short filename suffix. Without it, the relevant settings form the campaign id.
   */
  private static final String CAMPAIGN_PROPERTY = "mzmine.test.autoparam.campaign";

  /**
   * Attributes the results window also hides: MOEA internals and its penalty bookkeeping.
   */
  private static boolean isReportableAttribute(@NotNull String name) {
    return !name.startsWith("_") && !name.equalsIgnoreCase("penalty") && !name.equals(
        WizardOptimizationProblem.ATTR_PROPOSAL_INDEX) && !name.equals(
        WizardOptimizationProblem.ATTR_BATCH_EXECUTION_INDEX) && !name.equals(
        WizardOptimizationProblem.ATTR_ELAPSED_OPTIMIZATION_SECONDS) && !name.equals(
        SolutionOrigin.ATTRIBUTE);
  }

  /**
   * Comma separated dataset names to run, or unset for all of them. Lets a single dataset be redone
   * after a preset was corrected, instead of paying for the whole sweep again.
   */
  private static final String ONLY_PROPERTY = "mzmine.test.autoparam.only";

  /**
   * Comma separated parameter names to optimize, e.g. {@code Min height,MS1 noise level}, or unset
   * for every parameter the sequence exposes. Fewer variables converge in fewer batches.
   */
  private static final String PARAMS_PROPERTY = "mzmine.test.autoparam.params";

  /**
   * Comma separated random seeds to repeat every dataset with. One seed gives every dataset the
   * same warm-start draws, so a measured optimum can only ever be one of the twenty values that
   * draw produced - enough to see the direction of an estimate's error, not its size. Several seeds
   * turn each of those single observations into a distribution.
   */
  private static final String SEEDS_PROPERTY = "mzmine.test.autoparam.seeds";

  /**
   * Warm start sampling to benchmark, by enum name. Comparing them is the point of the property:
   * with independent draws a single run tells nothing, with a space-filling design it should.
   */
  private static final String SAMPLING_PROPERTY = "mzmine.test.autoparam.sampling";

  private static @NotNull WarmStartSampling warmStartSampling() {
    final String configured = System.getProperty(SAMPLING_PROPERTY);
    return configured == null || configured.isBlank() ? WarmStartSampling.GAUSSIAN
        : WarmStartSampling.valueOf(configured.trim().toUpperCase(java.util.Locale.ROOT));
  }

  private static @NotNull OptimizerOptions optimizer() {
    final String configured = System.getProperty(OPTIMIZER_PROPERTY);
    return configured == null || configured.isBlank() ? OptimizerOptions.MOEAD
        : OptimizerOptions.valueOf(configured.trim().toUpperCase(Locale.ROOT));
  }

  private static @NotNull List<Long> seeds() {
    final String configured = System.getProperty(SEEDS_PROPERTY);
    if (configured == null || configured.isBlank()) {
      return List.of(BatchOptimizationMainTask.DEFAULT_RANDOM_SEED);
    }
    return java.util.Arrays.stream(configured.split(",")).map(String::trim)
        .filter(s -> !s.isEmpty()).map(Long::parseLong).toList();
  }

  private static @NotNull BenchmarkCampaign campaign() {
    return BenchmarkCampaign.create(optimizer().name(), METRIC.name(), warmStartSampling().name(),
        Integer.getInteger(ITERATIONS_PROPERTY, DEFAULT_ITERATIONS), seeds(),
        System.getProperty(ONLY_PROPERTY), System.getProperty(CAMPAIGN_PROPERTY));
  }

  static boolean isSelected(@NotNull BenchmarkDataset dataset) {
    final String only = System.getProperty(ONLY_PROPERTY);
    if (only == null || only.isBlank()) {
      return true;
    }
    return java.util.Arrays.stream(only.split(",")).map(String::trim)
        .anyMatch(name -> name.equalsIgnoreCase(dataset.name()));
  }

  /**
   * Name of the ordinal that picks an m/z tolerance preset. Reported as the tolerance it resolves
   * to rather than as its index, because the selectable range depends on the instrument and the
   * same index therefore means different tolerances on different datasets.
   */
  private static final String MZ_TOLERANCE_OPTION = "MZ tolerance option";

  /**
   * Smallest heap a real run needs. {@link BatchOptimizationMainTask} enables
   * {@link MemoryMapStorage#setStoreAllInRam(boolean)} so a batch result never touches disk, which
   * is what makes an optimization fast and also what makes it heap hungry.
   */
  private static final long MIN_HEAP_BYTES = 4L << 30;

  /**
   * Unique id of the workflow preset, looked up rather than indexed, because the workflow factories
   * are a service-restricted list rather than an enum.
   */
  private static final String WORKFLOW_ID = "DDA";

  @BeforeAll
  public void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  /**
   * Resolves the m/z tolerance ordinal to the tolerance it selects.
   */
  private static @Nullable MZTolerance mzTolerance(@Nullable Solution solution) {
    if (solution == null) {
      return null;
    }
    for (int i = 0; i < solution.getNumberOfVariables(); i++) {
      if (!MZ_TOLERANCE_OPTION.equals(solution.getVariable(i).getName())) {
        continue;
      }
      final int index = OrdinalIntegerVariable.getInt(solution, i);
      final List<MZTolerance> options = MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS;
      return index >= 0 && index < options.size() ? options.get(index) : null;
    }
    return null;
  }

  @Test
  @DisplayName("tabulate the raw data estimate against the optimization result per dataset")
  void compareEstimateAgainstOptimum() {
    Assumptions.assumeTrue(Boolean.getBoolean(RUN_PROPERTY),
        "real-data optimizer benchmark is opt-in; enable with -D%s=true".formatted(RUN_PROPERTY));
    // a typo in a long path looks exactly like an absent dataset, so say which files are missing
    // instead of only reporting that nothing ran
    final List<String> unavailable = DATASETS.stream().map(BenchmarkDataset::unavailableReason)
        .filter(java.util.Objects::nonNull).toList();
    unavailable.forEach(logger::warning);

    final List<BenchmarkDataset> available = DATASETS.stream()
        .filter(EstimateVsOptimumTest::isSelected).filter(BenchmarkDataset::isAvailable).toList();
    Assumptions.assumeFalse(available.isEmpty(),
        "no configured dataset is present, see the warnings above. Paths are relative to -D%s".formatted(
            BenchmarkDataset.DATA_ROOT_PROPERTY));

    // checked before the first batch, because the optimizer keeps every result list in memory and
    // gradle's 512 MB test default dies deep inside a batch with a heap error that names no cause
    final long maxHeap = Runtime.getRuntime().maxMemory();
    Assertions.assertTrue(maxHeap >= MIN_HEAP_BYTES,
        "only %.1f GB of heap available, a real optimization needs at least %.0f GB. Rerun with -Dmzmine.test.maxHeap=16g".formatted(
            maxHeap / (double) (1 << 30), MIN_HEAP_BYTES / (double) (1 << 30)));

    final List<ComparisonRow> rows = new ArrayList<>();
    // decision: rows as name to value maps, written with the union of all keys as the header, so a
    // dataset that exposes an extra parameter or attribute widens the table instead of breaking it
    final List<Map<String, String>> trajectory = new ArrayList<>();
    final List<String> failed = new ArrayList<>();
    final List<Long> seeds = seeds();
    logger.info("running %d dataset(s) with %d seed(s) %s".formatted(available.size(), seeds.size(),
        seeds));
    for (final BenchmarkDataset dataset : available) {
      for (final long seed : seeds) {
        try {
          rows.addAll(run(dataset, seed, trajectory));
        } catch (Exception | AssertionError e) {
          // decision: one run must not take the sweep down with it. A reader that cannot open one
          // vendor format, or an optimization that finds nothing feasible, still leaves every other
          // run measurable, and the whole point is the comparison between them.
          failed.add("%s seed %d: %s".formatted(dataset.name(), seed, e));
          logger.log(Level.WARNING,
              "dataset %s seed %d failed, continuing".formatted(dataset.name(), seed), e);
        }
        // decision: cleared per run, not per dataset. Each run imports the files again, and the
        // optimizer keeps everything in memory, so a dataset repeated over five seeds would
        // otherwise hold five copies of its raw data at once.
        ProjectService.getProjectManager().clearProject();
        System.gc();
        // written after every run, so a crash or a timeout later does not lose what already ran
        writeCsv(rows);
        writeTrajectory(trajectory);
      }
    }

    printCrossDatasetSummary(rows);
    if (!failed.isEmpty()) {
      logger.warning("%d of %d datasets failed:%n  %s".formatted(failed.size(), available.size(),
          String.join("%n  ".formatted(), failed)));
    }
    Assertions.assertFalse(rows.isEmpty(), "every dataset failed: " + String.join(" | ", failed));
  }

  private @NotNull List<ComparisonRow> run(@NotNull BenchmarkDataset dataset, long seed,
      @NotNull List<Map<String, String>> trajectory) {
    final File[] files = dataset.rawFiles();
    final WizardSequence sequence = createSequence(dataset);
    final OptimizerParameters params = createParameters(sequence);
    final int batchBudget = params.getValue(OptimizerParameters.iterations);

    logger.info(
        "Optimizing %s: %d files, %d full batches, seed %d, %s sampling, metric %s".formatted(
            dataset.name(), files.length, batchBudget, seed, warmStartSampling(), METRIC.name()));

    final BatchOptimizationMainTask task = new BatchOptimizationMainTask(
        MemoryMapStorage.forRawDataFile(), Instant.now(), files, dataset.metadataFile(), sequence,
        params, seed);
    task.run();

    if (task.getStatus() != TaskStatus.FINISHED) {
      throw new IllegalStateException(
          "%s did not finish: %s".formatted(dataset.name(), task.getErrorMessage()));
    }
    final OptimizationOutcome outcome = task.getOutcome();
    if (outcome == null) {
      throw new IllegalStateException("%s produced no outcome".formatted(dataset.name()));
    }

    // decision: single objective, so index 0 is the only metric and the front holds at most one
    // meaningfully different solution
    final Solution estimate = outcome.estimateSolution();
    final Solution perturbed = bestFeasible(outcome, 0, SolutionOrigin.PERTURBED);
    final Solution front = bestOfFront(outcome);

    final List<ComparisonRow> rows = new ArrayList<>();
    rows.add(new ComparisonRow(dataset.name(), seed, "metric", METRIC.name(),
        estimate.getObjectiveValue(0), objectiveOrNull(perturbed), objectiveOrNull(front)));
    rows.add(new ComparisonRow(dataset.name(), seed, "diagnostic", "Total features",
        attributeAsDouble(estimate, WizardOptimizationProblem.ATTR_TOTAL_FEATURES),
        attributeAsDouble(perturbed, WizardOptimizationProblem.ATTR_TOTAL_FEATURES),
        attributeAsDouble(front, WizardOptimizationProblem.ATTR_TOTAL_FEATURES)));
    rows.add(new ComparisonRow(dataset.name(), seed, "diagnostic",
        ShapeScoreDiagnostic.ATTR_REMOVE_PERCENT,
        attributeAsDouble(estimate, ShapeScoreDiagnostic.ATTR_REMOVE_PERCENT),
        attributeAsDouble(perturbed, ShapeScoreDiagnostic.ATTR_REMOVE_PERCENT),
        attributeAsDouble(front, ShapeScoreDiagnostic.ATTR_REMOVE_PERCENT)));

    // the list is in proposal order; the solution also records the independent batch-cost index
    final List<Solution> evaluated = outcome.evaluatedSolutions();
    for (final Solution solution : evaluated) {
      trajectory.add(describeEvaluation(dataset, seed, solution));
    }

    final Map<String, Double> estimateValues = parameterValues(estimate);
    final Map<String, Double> perturbedValues =
        perturbed == null ? Map.of() : parameterValues(perturbed);
    final Map<String, Double> frontValues = front == null ? Map.of() : parameterValues(front);
    for (final Map.Entry<String, Double> entry : estimateValues.entrySet()) {
      if (MZ_TOLERANCE_OPTION.equals(entry.getKey())) {
        // the ordinal is an index into a per-instrument list, so index 3 is a different tolerance on
        // a TOF than on an Orbitrap. Only the resolved values are comparable across datasets.
        rows.add(new ComparisonRow(dataset.name(), seed, "parameter", "MZ tolerance / m/z",
            absoluteTolerance(estimate), absoluteTolerance(perturbed), absoluteTolerance(front)));
        rows.add(new ComparisonRow(dataset.name(), seed, "parameter", "MZ tolerance / ppm",
            ppmTolerance(estimate), ppmTolerance(perturbed), ppmTolerance(front)));
        continue;
      }
      rows.add(
          new ComparisonRow(dataset.name(), seed, "parameter", entry.getKey(), entry.getValue(),
              perturbedValues.get(entry.getKey()), frontValues.get(entry.getKey())));
    }

    printDatasetTable(dataset, seed, files.length, batchBudget, outcome, rows);
    return rows;
  }

  /**
   * Best solution of the non-dominated front on the single objective. Falls back to the best
   * feasible solution of the whole run when the front came back empty, which happens when the
   * optimizer was cancelled before any feasible solution was found.
   */
  private @Nullable Solution bestOfFront(@NotNull OptimizationOutcome outcome) {
    Solution best = null;
    for (final Solution solution : outcome.front()) {
      if (best == null || solution.getObjective(0).compareTo(best.getObjective(0)) < 0) {
        best = solution;
      }
    }
    return best != null ? best : bestFeasible(outcome, 0, null);
  }

  private static @Nullable Solution bestFeasible(@NotNull OptimizationOutcome outcome,
      int objectiveIndex, @Nullable SolutionOrigin origin) {
    Solution best = null;
    for (final Solution solution : outcome.evaluatedSolutions()) {
      if (!solution.isFeasible() || (origin != null && SolutionOrigin.of(solution) != origin)) {
        continue;
      }
      if (best == null
          || solution.getObjective(objectiveIndex).compareTo(best.getObjective(objectiveIndex))
          < 0) {
        best = solution;
      }
    }
    return best;
  }

  private static @NotNull Map<String, Double> parameterValues(@NotNull Solution solution) {
    final Map<String, Double> values = new LinkedHashMap<>();
    for (int index = 0; index < solution.getNumberOfVariables(); index++) {
      values.put(solution.getVariable(index).getName(),
          OrdinalIntegerVariable.effectiveValue(solution, index));
    }
    return values;
  }

  private static @Nullable Double objectiveOrNull(@Nullable Solution solution) {
    return solution == null ? null : solution.getObjectiveValue(0);
  }

  private static @Nullable Double absoluteTolerance(@Nullable Solution solution) {
    final MZTolerance tolerance = mzTolerance(solution);
    return tolerance == null ? null : tolerance.getMzTolerance();
  }

  private static @Nullable Double ppmTolerance(@Nullable Solution solution) {
    final MZTolerance tolerance = mzTolerance(solution);
    return tolerance == null ? null : tolerance.getPpmTolerance();
  }

  /**
   * Runs without any data, so the headless setup path is covered even on a machine that has none.
   * Everything here would otherwise only fail minutes into a real run.
   */
  @Test
  @DisplayName("sequence and optimizer parameters build headlessly")
  void setupWorksWithoutAGui() {
    final WizardSequence sequence = createSequence(DATASETS.getFirst());
    Assertions.assertEquals(WizardPart.values().length, sequence.size(),
        "every wizard part needs a preset, the problem dereferences all of them");

    final OptimizerParameters params = createParameters(sequence);
    Assertions.assertEquals(List.of(METRIC), OptimizerParameters.getOptimizationTargets(params));
    // the shape rejection guard is deliberately off: it was measured inert and it costs a peak
    // fitting pass on every evaluation
    Assertions.assertFalse(params.getValue(OptimizerParameters.maxShapeRejectionFactor));
    final ParameterSet optimizerParameters = OptimizerParameters.getSelectedOptimizerParameters(
        params);
    switch (optimizer()) {
      case PATTERN_SEARCH ->
          Assertions.assertInstanceOf(PatternSearchOptimizerParameters.class, optimizerParameters);
      case MOEAD -> {
        Assertions.assertTrue(
            optimizerParameters.getValue(MoeadOptimizerParameters.rawDataInitialization));
        Assertions.assertEquals(warmStartSampling(), optimizerParameters.getEmbeddedParameterValue(
            MoeadOptimizerParameters.rawDataInitialization));
      }
    }

    final List<ParameterDefinition<?>> optimized = params.getValue(
        OptimizerParameters.paramToOptimize);
    Assertions.assertFalse(optimized.isEmpty(),
        "the sequence exposes no optimizable parameter, so a run would have nothing to search");
    logger.info(
        "%s optimizes %d parameters: %s".formatted(DATASETS.getFirst().name(), optimized.size(),
            optimized.stream().map(ParameterDefinition::name).toList()));
  }

  private static @Nullable Double attributeAsDouble(@Nullable Solution solution,
      @NotNull String attribute) {
    if (solution == null) {
      return null;
    }
    return solution.getAttribute(attribute) instanceof Number n ? n.doubleValue() : null;
  }

  // ---------------------------------------------------------------------------- setup

  /**
   * Builds a full wizard sequence without a
   * {@link io.github.mzmine.modules.tools.batchwizard.BatchWizardTab}, using the dataset's presets
   * and the first available preset for every part that has only one sensible choice.
   */
  private @NotNull WizardSequence createSequence(@NotNull BenchmarkDataset dataset) {
    final WizardSequence sequence = new WizardSequence();
    for (final WizardPart part : WizardPart.values()) {
      sequence.set(part, factoryFor(part, dataset).create());
    }
    return sequence;
  }

  private @NotNull WizardParameterFactory factoryFor(@NotNull WizardPart part,
      @NotNull BenchmarkDataset dataset) {
    return switch (part) {
      case ION_INTERFACE -> dataset.ionInterface();
      case MS -> dataset.massSpectrometer();
      case IMS -> dataset.ionMobility();
      case WORKFLOW -> {
        final Optional<WizardParameterFactory> workflow = part.getParameterFactory(WORKFLOW_ID);
        yield workflow.orElseThrow(() -> new IllegalStateException(
            "workflow preset %s is not available".formatted(WORKFLOW_ID)));
      }
      // DATA_IMPORT, FILTER, ANNOTATION and CUSTOMIZATION each have a single preset
      case DATA_IMPORT, FILTER, ANNOTATION, CUSTOMIZATION -> part.getDefaultPresets()[0];
    };
  }

  /**
   * Restricts the search to the parameters named in {@link #PARAMS_PROPERTY}. The others are not
   * searched and stay at their raw data estimates.
   */
  private static @NotNull List<ParameterDefinition<?>> selectedParameters(
      @NotNull List<ParameterDefinition<?>> exposed) {
    final String configured = System.getProperty(PARAMS_PROPERTY);
    if (configured == null || configured.isBlank()) {
      return exposed;
    }
    final List<String> names = java.util.Arrays.stream(configured.split(",")).map(String::trim)
        .filter(s -> !s.isEmpty()).toList();
    final List<ParameterDefinition<?>> selected = exposed.stream()
        .filter(p -> names.stream().anyMatch(name -> name.equalsIgnoreCase(p.name()))).toList();
    Assertions.assertEquals(names.size(), selected.size(),
        "-D%s=%s does not match the exposed parameters %s".formatted(PARAMS_PROPERTY, configured,
            exposed.stream().map(ParameterDefinition::name).toList()));
    return selected;
  }

  private @NotNull OptimizerParameters createParameters(@NotNull WizardSequence sequence) {
    final OptimizerParameters params = new OptimizerParameters();
    final OptimizerOptions optimizer = optimizer();
    OptimizerParameters.setOptimizerAndTargets(params, optimizer, List.of(METRIC));
    params.setParameter(OptimizerParameters.iterations,
        Integer.getInteger(ITERATIONS_PROPERTY, DEFAULT_ITERATIONS));
    if (optimizer == OptimizerOptions.MOEAD) {
      final ParameterSet optimizerParameters = OptimizerParameters.getSelectedOptimizerParameters(
          params);
      optimizerParameters.setParameter(MoeadOptimizerParameters.rawDataInitialization, true);
      optimizerParameters.getParameter(MoeadOptimizerParameters.rawDataInitialization)
          .getEmbeddedParameter().setValue(warmStartSampling());
    }
    params.setParameter(OptimizerParameters.benchmarkFeatures, false);
    // decision: off. It was measured inert - three factors from 1.0 to 2.0 reached the identical
    // optimum - and leaving it on costs the peak fitting pass on every evaluation for nothing.
    params.setParameter(OptimizerParameters.maxShapeRejectionFactor, false);
    // only the parameters this sequence actually exposes, same as the wizard's checklist
    params.setParameter(OptimizerParameters.paramToOptimize,
        selectedParameters(OptimizerParameters.collectSolutions(sequence)));
    return params;
  }

  // ---------------------------------------------------------------------------- output

  private void printDatasetTable(@NotNull BenchmarkDataset dataset, long seed, int fileCount,
      int batchBudget, @NotNull OptimizationOutcome outcome, @NotNull List<ComparisonRow> rows) {
    final StringBuilder sb = new StringBuilder("\n");
    sb.append("=".repeat(104)).append('\n');
    sb.append(
        "%s   seed %d   %d files   budget %d batches   %d proposals   %d batches run%n".formatted(
            dataset.name(), seed, fileCount, batchBudget, outcome.evaluatedSolutions().size(),
            outcome.problem().getBatchExecutionCount()));
    sb.append("-".repeat(104)).append('\n');
    sb.append(
        "%-30s %14s %16s %14s %11s %11s%n".formatted("", "estimate", "best perturbed", "front",
            "pert/est", "front/est"));
    for (final ComparisonRow row : rows) {
      sb.append("%-30s %14s %16s %14s %11s %11s%n".formatted(row.name(), format(row.estimate()),
          format(row.perturbed()), format(row.front()), ratio(row.perturbed(), row.estimate()),
          ratio(row.front(), row.estimate())));
    }
    logger.info(sb.toString());
  }

  /**
   * The reason this test exists: the same ratio per parameter side by side across datasets. A
   * consistent column means the estimator is off by a fixed factor and can be re-centred; a column
   * that changes sign means the offset is dataset specific.
   */
  private void printCrossDatasetSummary(@NotNull List<ComparisonRow> rows) {
    final List<String> datasets = rows.stream().map(ComparisonRow::dataset).distinct().toList();
    if (datasets.size() < 2) {
      logger.info("only one dataset was run, so there is nothing to compare across datasets yet");
      return;
    }

    final Map<String, Map<String, ComparisonRow>> byName = new LinkedHashMap<>();
    final java.util.Set<String> names = new LinkedHashSet<>();
    for (final ComparisonRow row : rows) {
      byName.computeIfAbsent(row.name(), _ -> new LinkedHashMap<>()).put(row.dataset(), row);
      names.add(row.name());
    }

    final StringBuilder sb = new StringBuilder("\n");
    sb.append("=".repeat(104)).append('\n');
    sb.append("front / estimate ratio per dataset - is the estimator's offset comparable?\n");
    sb.append("-".repeat(104)).append('\n');
    sb.append("%-30s".formatted("")).append(
            datasets.stream().map(d -> "%16s".formatted(trim(d, 16))).reduce("", String::concat))
        .append('\n');
    for (final String name : names) {
      sb.append("%-30s".formatted(trim(name, 30)));
      for (final String dataset : datasets) {
        final ComparisonRow row = byName.get(name).get(dataset);
        sb.append("%16s".formatted(row == null ? "-" : ratio(row.front(), row.estimate())));
      }
      sb.append('\n');
    }
    logger.info(sb.toString());
  }

  private void writeCsv(@NotNull List<ComparisonRow> rows) {
    final File csv = campaign().outputFile(SUMMARY_CSV_STEM);
    try (final PrintWriter writer = new PrintWriter(
        Files.newBufferedWriter(csv.toPath(), StandardCharsets.UTF_8))) {
      writer.println("dataset,seed,kind,name,estimate,bestPerturbed,front");
      for (final ComparisonRow row : rows) {
        writer.printf(Locale.ROOT, "%s,%d,%s,%s,%s,%s,%s%n", row.dataset(), row.seed(), row.kind(),
            row.name(), csvValue(row.estimate()), csvValue(row.perturbed()), csvValue(row.front()));
      }
    } catch (IOException e) {
      throw new RuntimeException("cannot write " + csv, e);
    }
    logger.info("wrote %s".formatted(csv));
  }

  /**
   * One evaluated solution as a flat name to value map: what identifies the run, the parameters it
   * ran with, every metric, and every diagnostic the problem recorded.
   */
  private @NotNull Map<String, String> describeEvaluation(@NotNull BenchmarkDataset dataset,
      long seed, @NotNull Solution solution) {
    final Map<String, String> row = new LinkedHashMap<>();
    row.put("dataset", dataset.name());
    row.put("seed", Long.toString(seed));
    row.put("proposal",
        Objects.toString(solution.getAttribute(WizardOptimizationProblem.ATTR_PROPOSAL_INDEX), ""));
    row.put("batchExecution", Objects.toString(
        solution.getAttribute(WizardOptimizationProblem.ATTR_BATCH_EXECUTION_INDEX), ""));
    row.put("elapsedSeconds", Objects.toString(
        solution.getAttribute(WizardOptimizationProblem.ATTR_ELAPSED_OPTIMIZATION_SECONDS), ""));
    row.put("sampling", warmStartSampling().name());
    row.put("origin", Objects.toString(SolutionOrigin.of(solution), ""));
    row.put("feasible", Boolean.toString(solution.isFeasible()));

    // the effective values, so a row says what the batch actually ran with
    parameterValues(solution)
        .forEach((name, value) -> row.put(name, Double.toString(value)));
    // the tolerance ordinal is an index into a per-instrument list, so resolve it as well
    final MZTolerance tolerance = mzTolerance(solution);
    if (tolerance != null) {
      row.put("MZ tolerance / m/z", Double.toString(tolerance.getMzTolerance()));
      row.put("MZ tolerance / ppm", Double.toString(tolerance.getPpmTolerance()));
    }

    for (int i = 0; i < solution.getNumberOfObjectives(); i++) {
      row.put(solution.getObjective(i).getName(), Double.toString(solution.getObjectiveValue(i)));
    }
    for (int i = 0; i < solution.getNumberOfConstraints(); i++) {
      row.put("constraint " + i, Double.toString(solution.getConstraintValue(i)));
    }
    solution.getAttributes().forEach((name, value) -> {
      if (isReportableAttribute(name)) {
        row.put(name, Objects.toString(value, ""));
      }
    });
    return row;
  }

  private void writeTrajectory(@NotNull List<Map<String, String>> trajectory) {
    if (trajectory.isEmpty()) {
      return;
    }
    final List<String> header = new ArrayList<>();
    for (final Map<String, String> row : trajectory) {
      row.keySet().forEach(k -> {
        if (!header.contains(k)) {
          header.add(k);
        }
      });
    }

    final File csv = campaign().outputFile(TRAJECTORY_CSV_STEM);
    try (final PrintWriter writer = new PrintWriter(
        Files.newBufferedWriter(csv.toPath(), StandardCharsets.UTF_8))) {
      writer.println(String.join(",", header));
      for (final Map<String, String> row : trajectory) {
        writer.println(
            header.stream().map(k -> quote(row.getOrDefault(k, ""))).reduce((a, b) -> a + "," + b)
                .orElse(""));
      }
    } catch (IOException e) {
      throw new RuntimeException("cannot write " + csv, e);
    }
    logger.info("wrote %s".formatted(csv));
  }

  /**
   * Values such as the resolved m/z tolerance carry commas, so anything risky is quoted.
   */
  private static @NotNull String quote(@NotNull String value) {
    return value.indexOf(',') < 0 && value.indexOf('"') < 0 ? value
        : '"' + value.replace("\"", "\"\"") + '"';
  }

  private static @NotNull String csvValue(@Nullable Double value) {
    return value == null ? "" : Double.toString(value);
  }

  private static @NotNull String format(@Nullable Double value) {
    if (value == null) {
      return "-";
    }
    final double abs = Math.abs(value);
    // one format cannot show both an m/z tolerance and a min height readably
    if (abs != 0 && abs < 0.01) {
      return "%.5f".formatted(value);
    }
    return abs < 1000 ? "%.4f".formatted(value) : "%.0f".formatted(value);
  }

  private static @NotNull String ratio(@Nullable Double value, @Nullable Double reference) {
    if (value == null || reference == null || reference == 0) {
      return "-";
    }
    return "%.2fx".formatted(value / reference);
  }

  private static @NotNull String trim(@NotNull String text, int width) {
    return text.length() <= width ? text : text.substring(0, width - 1);
  }
}
