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
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.NotificationService;
import io.github.mzmine.javafx.dialogs.NotificationService.NotificationType;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.KeepInMemory;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.batchwizard.BatchWizardTab;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatisticsDashboardPane;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.BenchmarkFeatureLoader;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.FeatureRecord;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimationContext;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimators;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameterSet;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PresetSelection;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.RawDataAnalysis;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.RawDataPreparation;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.BatchExecutionLimitReachedException;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.OptimizationSearchStoppedException;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.TaskStatusTerminationCondition;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblem;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.gui.OptimizationResultsController;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.ShapeScoreDiagnostic;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OptimizerAlgorithmModule;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.SolutionOrigin;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.Preclassification;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationConflicts;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationParameters;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationResolved;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.RawDataPreclassificationTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Logger;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moeaframework.algorithm.AbstractAlgorithm;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.population.NondominatedPopulation;

public class BatchOptimizationMainTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(BatchOptimizationMainTask.class.getName());

  /**
   * Safety limit for cheap duplicate proposals per expensive batch execution.
   */
  private static final int PROPOSAL_BUDGET_MULTIPLIER = 10;

  /**
   * Lowest shape rejection limit in percent. Without a floor a dataset whose estimate rejects almost
   * nothing would make every candidate infeasible.
   */
  private static final double MIN_SHAPE_REJECTION_LIMIT = 5d;

  /**
   * Seed every run uses unless a caller asks for another one. Fixed on purpose: the same data and
   * the same settings have to give the same result for every user on every machine, without anyone
   * having to configure anything. Only a programmatic caller that deliberately wants to vary the
   * random draws - a benchmark measuring how much of a result is chance - passes its own.
   */
  public static final long DEFAULT_RANDOM_SEED = 42;

  private final File[] files;
  @Nullable
  private final File metadata;
  /**
   * Source of the parameter presets. Captured up front instead of read from the tab inside
   * {@link #run()}, so the task does not touch a GUI object from its own thread.
   */
  private final @NotNull WizardSequence sequence;
  /**
   * Null when there is no wizard to return to, i.e. when the optimization is driven headlessly. The
   * results window is then skipped and the outcome is read through {@link #getOutcome()}.
   */
  private final @Nullable BatchWizardTab tab;
  private final OptimizerParameters params;
  /**
   * Set once the optimization finished, so a headless caller can read the estimate, the front and
   * every evaluated solution back.
   */
  private @Nullable OptimizationOutcome outcome;
  private final long randomSeed;
  /**
   * Settings fixed by the pre-classification, see {@link PreclassificationParameters}. Null for
   * headless runs, which pre-classify the imported files themselves.
   */
  private final @Nullable ParameterSet preclassification;
  private final AtomicReference<TaskStatus> externalStatus = new AtomicReference<>(
      TaskStatus.PROCESSING);
  private final AtomicBoolean stopSearchRequested = new AtomicBoolean();
  /**
   * Maximum number of uncached full batch executions, including the raw-data estimate.
   */
  private int totalBatchExecutions;

  @Nullable
  private AbstractAlgorithm optimizer;
  @Nullable
  private WizardOptimizationProblem problem;

  /**
   * @param files             the files that were pre-classified, see
   *                          {@link RawDataPreclassificationTask}
   * @param preclassification the settings fixed by the pre-classification, see
   *                          {@link PreclassificationParameters}
   */
  public BatchOptimizationMainTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull File[] files, @Nullable File metadata,
      @NotNull BatchWizardTab tab, @NotNull OptimizerParameters params,
      @NotNull ParameterSet preclassification) {
    // a copy, so wizard edits during the run do not change later candidates. Must be called on
    // the JavaFX thread
    this(storage, moduleCallDate, files, metadata, tab.getSequence().copy(), tab, params,
        preclassification, DEFAULT_RANDOM_SEED);
  }

  /**
   * Runs an optimization without a wizard tab, for tests and scripted comparisons. No results
   * window is opened; read the result through {@link #getOutcome()} once the task finished.
   */
  public BatchOptimizationMainTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull File[] files, @Nullable File metadata,
      @NotNull WizardSequence sequence, @NotNull OptimizerParameters params) {
    this(storage, moduleCallDate, files, metadata, sequence, params, DEFAULT_RANDOM_SEED);
  }

  /**
   * Runs headlessly with an explicit random seed, so a caller can measure how much of a result
   * comes from the data and how much from the draw. Use {@link #DEFAULT_RANDOM_SEED} to reproduce
   * what a user would get. The pre-classification runs on the imported files and fails the task
   * if it needs a user choice.
   */
  public BatchOptimizationMainTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull File[] files, @Nullable File metadata,
      @NotNull WizardSequence sequence, @NotNull OptimizerParameters params, long randomSeed) {
    this(storage, moduleCallDate, files, metadata, sequence, null, params, null, randomSeed);
  }

  /**
   * @param preclassification null to pre-classify the imported files without user interaction
   */
  private BatchOptimizationMainTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull File[] files, @Nullable File metadata,
      @NotNull WizardSequence sequence, @Nullable BatchWizardTab tab,
      @NotNull OptimizerParameters params, @Nullable ParameterSet preclassification,
      long randomSeed) {
    super(storage, moduleCallDate);
    this.files = files;
    this.metadata = metadata;
    this.sequence = sequence;
    this.tab = tab;
    this.params = params;
    this.preclassification = preclassification;
    this.randomSeed = randomSeed;

    addTaskStatusListener((_, newStatus, _) -> {
      if (newStatus == TaskStatus.CANCELED && optimizer != null) {
        optimizer.terminate();
      }
    });
  }

  /**
   * @return the finished optimization's estimate, front and evaluated solutions, or null while the
   * task has not completed.
   */
  public @Nullable OptimizationOutcome getOutcome() {
    return outcome;
  }

  /**
   * Requests a graceful end to the search. The currently running batch may finish, but no new
   * candidate is started and all completed evaluations are retained in the final result.
   */
  public void requestStopSearch() {
    if (getStatus() == TaskStatus.PROCESSING && stopSearchRequested.compareAndSet(false, true)) {
      logger.info("Stopping parameter search after the current evaluation.");
    }
  }

  @Override
  public String getTaskDescription() {
    final int max = totalBatchExecutions > 0 ? totalBatchExecutions : 100;
    final WizardOptimizationProblem currentProblem = problem;
    final int completed = currentProblem != null ? currentProblem.getBatchExecutionCount() : 0;
    return "Performing batch optimization. Full batch %d/%d".formatted(completed, max);
  }

  @Override
  public double getFinishedPercentage() {
    final int max = totalBatchExecutions > 0 ? totalBatchExecutions : 100;
    final WizardOptimizationProblem currentProblem = problem;
    return currentProblem != null ? Math.min(1d,
        (double) currentProblem.getBatchExecutionCount() / max) : 0d;
  }

  /**
   * Pre-classifies the imported files for headless runs.
   * <p>
   * decision: a run without a wizard tab cannot ask the user, so a needed choice fails the task
   * instead of silently picking a value
   *
   * @return the decided settings, or null if the task was set to error
   */
  private @Nullable ParameterSet preclassifyWithoutUser(@NotNull List<RawDataFile> importedFiles) {
    return switch (Preclassification.resolve(importedFiles, sequence)) {
      case PreclassificationConflicts conflicts -> {
        error(conflicts.describe());
        yield null;
      }
      case PreclassificationResolved resolved -> {
        if (resolved.needsUserChoice()) {
          error("The raw data require a user choice before the optimization:\n" + String.join("\n",
              resolved.choiceMessages()));
          yield null;
        }
        yield resolved.parameters();
      }
    };
  }

  /**
   * Asks for the preset switch on the JavaFX thread and waits on the task thread for the answer.
   */
  private static boolean confirmPresetsOnFxThread(@NotNull BatchWizardTab wizardTab,
      @NotNull PresetSelection presets) {
    final AtomicBoolean confirmed = new AtomicBoolean(false);
    FxThread.runOnFxThreadAndWait(() -> confirmed.set(wizardTab.confirmAndSwitchPresets(presets)));
    return confirmed.get();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    addTaskStatusListener((_, newStatus, _) -> externalStatus.set(newStatus));

    // store all in ram while optimizing
    MemoryMapStorage.setStoreAllInRam(true);
    // restore to initial value on change
    final KeepInMemory initialMemoryOption = ConfigService.getPreference(
        MZminePreferences.memoryOption);
    addTaskStatusListener((_, _, _) -> initialMemoryOption.enforceToMemoryMapping());

    final List<RawDataFile> importedFiles = RawDataPreparation.importFilesBlocking(files, metadata);
    final ParameterSet runPreclassification =
        preclassification != null ? preclassification : preclassifyWithoutUser(importedFiles);
    if (runPreclassification == null) {
      return;
    }
    final List<FeatureRecord> benchmarkFeatures =
        params.getValue(OptimizerParameters.benchmarkFeaturesFile)
            ? BenchmarkFeatureLoader.fromFile(null,
            params.getEmbeddedParameterValue(OptimizerParameters.benchmarkFeaturesFile),
            params.getValue(OptimizerParameters.benchmarkFeatureTypes)) : List.of();

    final PolarityType polarity = runPreclassification.getValue(
        PreclassificationParameters.polarity).toScanPolaritySelection();
    final List<DataFileStatistics> stats = RawDataPreparation.computeFileStatistics(importedFiles,
        benchmarkFeatures, getMemoryMapStorage(), polarity);
    stats.forEach(stat -> logger.info(stat.getMzToleranceForIsotopes().toString()));
    RawDataPreparation.requireIsotopeSignals(stats);

    // set a specific seed to make the results deterministic, see DEFAULT_RANDOM_SEED
    PRNG.setSeed(randomSeed);

    totalBatchExecutions = Math.max(params.getValue(OptimizerParameters.iterations), 30);
    final RawDataAnalysis analysis = RawDataAnalysis.analyze(stats);
    // if confirmed, the wizard switches to the presets that fit the raw data and every candidate is
    // evaluated with them.
    // decision: headless runs keep the given presets, so scripted comparisons stay reproducible
    final Predicate<PresetSelection> presetConfirmation =
        tab != null ? presetSelection -> confirmPresetsOnFxThread(tab, presetSelection)
            : _ -> false;
    final ParameterEstimationContext estimationContext = ParameterEstimationContext.withFittingPresets(
        analysis, sequence, runPreclassification, presetConfirmation);
    if (getStatus() != TaskStatus.PROCESSING) {
      return;
    }
    final PresetSelection presets = estimationContext.presetSelection();
    if (!presets.isEmpty()) {
      logger.info("Optimizing with presets that fit the raw data:\n" + presets.describe());
    }
    final PreparedParameterSet singlePassEstimates = PreparedParameterSet.prepare(
        estimationContext);
    final WizardOptimizationProblem optimizationProblem = new WizardOptimizationProblem(
        estimationContext, singlePassEstimates, params, externalStatus, totalBatchExecutions,
        stopSearchRequested::get);
    problem = optimizationProblem;
    final boolean showExtendedStatistics = params.getValue(
        OptimizerParameters.showExtendedStatistics);
    if (DesktopService.isGUI() && showExtendedStatistics) {
      final List<DataFileStatistics> dashboardStats = List.copyOf(stats);
      FxThread.runLater(() -> MZmineCore.getDesktop().addTab(new SimpleTab("Auto Param Statistics",
          new DataFileStatisticsDashboardPane(dashboardStats,
              ParameterEstimators.interSampleRtStatistics(analysis),
              estimationContext.massDetectorType()))));
    }
    final AtomicReference<OptimizationResultsController> resultsController = new AtomicReference<>();
    final AtomicReference<NondominatedPopulation> completedResult = new AtomicReference<>();

    final Solution singlePassSolution = optimizationProblem.newSolution();

    // decision: always derive and evaluate the raw data estimate, also when it is not used to
    // warm-start the optimizer, so the results table can always show it next to the optimized
    // solutions and the logged comparison is meaningful in both cases
    SolutionOrigin.ESTIMATE.applyTo(singlePassSolution);
    optimizationProblem.evaluate(singlePassSolution);

    // decision: derive the shape rejection limit from the estimate's own measured rate, so the
    // limit adapts to the dataset instead of being an absolute guess
    if (params.getValue(OptimizerParameters.maxShapeRejectionFactor)) {
      final double factor = params.getEmbeddedParameterValue(
          OptimizerParameters.maxShapeRejectionFactor);
      final Object measured = singlePassSolution.getAttribute(
          ShapeScoreDiagnostic.ATTR_REMOVE_PERCENT);
      final double baseline = measured instanceof Number n ? n.doubleValue() : 0d;
      // assumption: a floor keeps a near-perfect baseline from making everything infeasible
      optimizationProblem.setShapeRejectionLimitPercent(
          Math.max(baseline * factor, MIN_SHAPE_REJECTION_LIMIT));
    }

    if (tab != null) {
      optimizationProblem.setEvaluationListener(_ -> {
        final OptimizationResultsController controller = resultsController.get();
        if (controller != null) {
          controller.refreshEvaluatedSolutions();
        }
      });
      showLiveResultsWindow(tab, optimizationProblem, singlePassSolution, showExtendedStatistics,
          resultsController, completedResult);
    }

    // created after the estimate, so start solutions use the final shape rejection limit
    final OptimizerAlgorithmModule optimizerModule = params.getValue(OptimizerParameters.optimizers)
        .getModuleInstance();
    optimizer = optimizerModule.createAlgorithm(optimizationProblem,
        OptimizerParameters.getSelectedOptimizerParameters(params));

    logger.info("Starting %s with a batch budget of %d".formatted(optimizerModule.getName(),
        totalBatchExecutions));
    final String presetText =
        presets.isEmpty() ? "" : "Presets:\n%s\n".formatted(presets.describe());
    NotificationService.show(NotificationType.INFO, "Starting optimizer", """
        Using %s with %d full batch executions.
        %sEstimates:
        %s""".formatted(optimizerModule.getName(), totalBatchExecutions, presetText,
        singlePassEstimates.describe()));

    try {
      final int maxProposals = Math.multiplyExact(totalBatchExecutions, PROPOSAL_BUDGET_MULTIPLIER);
      optimizer.run(new TaskStatusTerminationCondition(totalBatchExecutions, maxProposals,
          optimizationProblem::getBatchExecutionCount, this::getStatus, stopSearchRequested::get));
    } catch (BatchExecutionLimitReachedException e) {
      // MOEA checks termination between generations, so the problem stops a partial generation at
      // the exact full-batch boundary.
      logger.fine(e.getMessage());
      optimizer.terminate();
    } catch (OptimizationSearchStoppedException e) {
      logger.info(e.getMessage());
      if (!optimizer.isTerminated()) {
        optimizer.terminate();
      }
    } catch (RuntimeException e) {
      if (getStatus() != TaskStatus.CANCELED && getStatus() != TaskStatus.ERROR) {
        throw e;
      }
    }

    // A hard budget stop can interrupt a generation after some offspring were evaluated but before
    // the algorithm incorporated them. Build the result from every completed observation so those
    // expensive final batches cannot be lost.
    final NondominatedPopulation result = new NondominatedPopulation();
    result.addAll(optimizer.getResult());
    result.addAll(optimizationProblem.getEvaluatedSolutions());

    // log comparison: single-pass estimate versus the best optimizer result
    OptimizationResultLogger.logResults(singlePassSolution, singlePassEstimates,
        optimizationProblem.getEnabledMetrics());
    OptimizationResultLogger.logComparison(singlePassSolution, result,
        optimizationProblem.getEnabledMetrics());

    outcome = new OptimizationOutcome(singlePassEstimates, singlePassSolution, result,
        optimizationProblem);
    completedResult.set(result);
    optimizationProblem.setEvaluationListener(null);
    final OptimizationResultsController controller = resultsController.get();
    if (controller != null) {
      controller.completeOptimization(result);
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void showLiveResultsWindow(@NotNull BatchWizardTab resultTab,
      @NotNull WizardOptimizationProblem optimizationProblem, @NotNull Solution singlePassSolution,
      final boolean showExtendedStatistics,
      @NotNull AtomicReference<OptimizationResultsController> resultsController,
      @NotNull AtomicReference<NondominatedPopulation> completedResult) {
    FxThread.runLater(() -> {
      final Stage stage = new Stage();
      final OptimizationResultsController controller = new OptimizationResultsController(resultTab,
          optimizationProblem, singlePassSolution, showExtendedStatistics, stage,
          this::requestStopSearch);
      resultsController.set(controller);
      controller.refreshEvaluatedSolutions();
      final NondominatedPopulation alreadyCompleted = completedResult.get();
      if (alreadyCompleted != null) {
        controller.completeOptimization(alreadyCompleted);
      }

      final Region region = controller.buildView();
      stage.setTitle("Parameter optimization - running");
      stage.initOwner(MZmineCore.getDesktop().getMainWindow());
      final Scene scene = new Scene(region);
      ConfigService.getConfiguration().getTheme().apply(scene.getStylesheets());
      stage.setScene(scene);
      stage.show();
      final double screenWidth = Screen.getPrimary().getBounds().getWidth();
      final double screenHeight = Screen.getPrimary().getBounds().getHeight();
      // the compact view sizes the stage to its table columns when shown
      if (showExtendedStatistics) {
        stage.setWidth(Math.min(1400d, screenWidth * 0.9d));
        stage.setHeight(Math.min(900d, screenHeight * 0.85d));
      }
      stage.centerOnScreen();
    });
  }

}
