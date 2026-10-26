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
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatisticsDashboardPane;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimationContext;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameterSet;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PresetSelection;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.WizardParameterEstimationResult;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.BatchExecutionLimitReachedException;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.OptimizationSearchStoppedException;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.TaskStatusTerminationCondition;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblem;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.gui.OptimizationResultsController;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.ShapeScoreDiagnostic;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OptimizerAlgorithmModule;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.SolutionOrigin;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.Preclassification;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationParameters;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationResult;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.RawDataPreclassificationTask;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.DataFileStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.FeatureRecord;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataPreparation;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Logger;
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
  private volatile @Nullable OptimizationOutcome outcome;
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
  private final int totalBatchExecutions;
  /**
   * The current preparation step for the task description, null once the batches run.
   */
  private volatile @Nullable String preparationStep = "importing raw data files";

  @Nullable
  private AbstractAlgorithm optimizer;
  @Nullable
  private volatile WizardOptimizationProblem problem;

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
    totalBatchExecutions = Math.max(params.getValue(OptimizerParameters.iterations),
        OptimizerParameters.MIN_ITERATIONS);

    addTaskStatusListener((_, newStatus, _) -> {
      if (newStatus == TaskStatus.CANCELED) {
        if (optimizer != null && !optimizer.isTerminated()) {
          optimizer.terminate();
        }
        final WizardOptimizationProblem currentProblem = problem;
        if (currentProblem != null) {
          currentProblem.cancel();
        }
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

  /** Full batches reserved for execution, including a currently running batch. */
  public int getBatchExecutionCount() {
    final WizardOptimizationProblem current = problem;
    return current == null ? 0 : current.getBatchExecutionCount();
  }

  /** Completed full batch evaluations; cached proposals do not increment this count. */
  public int getCompletedEvaluationCount() {
    final WizardOptimizationProblem current = problem;
    return current == null ? 0 : current.getCompletedBatchExecutionCount();
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
    final String step = preparationStep;
    final WizardOptimizationProblem currentProblem = problem;
    if (step != null || currentProblem == null) {
      return "Parameter optimization: " + Objects.requireNonNullElse(step, "preparing");
    }
    return "Parameter optimization: full batch %d/%d".formatted(
        currentProblem.getBatchExecutionCount(), currentProblem.getMaxBatchExecutions());
  }

  @Override
  public double getFinishedPercentage() {
    final WizardOptimizationProblem currentProblem = problem;
    return currentProblem != null ? Math.min(1d,
        (double) currentProblem.getBatchExecutionCount() / currentProblem.getMaxBatchExecutions()) : 0d;
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
    final PreclassificationResult resolved = Preclassification.resolve(importedFiles, sequence);
    if (resolved.hasConflicts()) {
      error(resolved.describeConflicts());
      return null;
    }
    if (resolved.needsUserChoice()) {
      error("The raw data require a user choice before the optimization:\n" + String.join("\n",
          resolved.choiceMessages()));
      return null;
    }
    return resolved.parameters();
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
    if (isCanceled()) {
      return;
    }
    setStatus(TaskStatus.PROCESSING);

    try {
      WizardParameterEstimationResult.requireSupportedWorkflow(sequence);
    } catch (IllegalArgumentException exception) {
      error(exception.getMessage());
      return;
    }

    // store all in ram while optimizing
    MemoryMapStorage.setStoreAllInRam(true);
    // restore to initial value on change
    final KeepInMemory initialMemoryOption = ConfigService.getPreference(
        MZminePreferences.memoryOption);
    addTaskStatusListener((_, _, _) -> initialMemoryOption.enforceToMemoryMapping());
    addTaskStatusListener((_, newStatus, _) -> externalStatus.set(newStatus));

    final List<RawDataFile> importedFiles = RawDataPreparation.importFilesBlocking(files, metadata);
    preparationStep = "classifying raw data files";
    final ParameterSet runPreclassification =
        preclassification != null ? preclassification : preclassifyWithoutUser(importedFiles);
    if (runPreclassification == null) {
      return;
    }
    final List<FeatureRecord> benchmarkFeatures = BenchmarkFeatureParameters.loadBenchmarkFeatures(
        params);

    preparationStep = "computing raw data statistics and estimates";
    // if confirmed, the wizard switches to the presets that fit the raw data and every candidate is
    // evaluated with them.
    // decision: headless runs keep the given presets, so scripted comparisons stay reproducible
    final Predicate<PresetSelection> presetConfirmation =
        tab != null ? presetSelection -> confirmPresetsOnFxThread(tab, presetSelection)
            : _ -> false;
    final WizardParameterEstimationResult estimation = WizardParameterEstimationResult.estimate(
        importedFiles, benchmarkFeatures, sequence, runPreclassification, presetConfirmation,
        getMemoryMapStorage(), () -> getStatus() != TaskStatus.PROCESSING);
    if (estimation == null) {
      if (getStatus() == TaskStatus.PROCESSING) {
        error("Parameter estimation ended without a result.");
      }
      return;
    }
    final List<DataFileStatistics> stats = estimation.statistics();
    final ParameterEstimationContext estimationContext = estimation.context();
    final PreparedParameterSet singlePassEstimates = estimation.estimates();
    final PresetSelection presets = estimationContext.presetSelection();
    if (!presets.isEmpty()) {
      logger.info("Optimizing with presets that fit the raw data:\n" + presets.describe());
    }

    // set a specific seed to make the results deterministic, see DEFAULT_RANDOM_SEED
    PRNG.setSeed(randomSeed);
    final WizardOptimizationProblem optimizationProblem = new WizardOptimizationProblem(
        estimationContext, singlePassEstimates, params, externalStatus, totalBatchExecutions,
        stopSearchRequested::get);
    problem = optimizationProblem;
    if (getStatus() == TaskStatus.CANCELED) {
      optimizationProblem.cancel();
      optimizationProblem.close();
      initialMemoryOption.enforceToMemoryMapping();
      return;
    }
    preparationStep = null;
    final boolean showExtendedStatistics = params.getValue(
        OptimizerParameters.showExtendedStatistics);
    if (DesktopService.isGUI() && showExtendedStatistics) {
      final List<DataFileStatistics> dashboardStats = List.copyOf(stats);
      FxThread.runLater(() -> MZmineCore.getDesktop().addTab(new SimpleTab("Auto Param Statistics",
          new DataFileStatisticsDashboardPane(dashboardStats, estimation.interSampleRtStatistics(),
              estimationContext.massDetectorType()))));
    }
    final AtomicReference<OptimizationResultsController> resultsController = new AtomicReference<>();
    final Solution singlePassSolution = optimizationProblem.newSolution();
    @Nullable Solution initialUserSettingsSolution = null;

    try {
      // decision: always derive and evaluate the raw data estimate, also when it is not used to
      // warm-start the optimizer, so the results table can always show it next to the optimized
      // solutions and the logged comparison is meaningful in both cases
      SolutionOrigin.ESTIMATE.applyTo(singlePassSolution);
      optimizationProblem.evaluate(singlePassSolution);
      // decision: without features every score is 0, so the search has nothing to improve on
      if (singlePassSolution.getAttribute(
          WizardOptimizationProblem.ATTR_TOTAL_FEATURES) instanceof Number features
          && features.longValue() == 0) {
        error("""
            The batch with the raw data estimates produced no features, the optimization was \
            stopped. Check the selected files and the wizard settings, e.g., the minimum feature \
            height.""");
        return;
      }

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

      if (params.getValue(OptimizerParameters.evaluateInitialUserSettings)) {
        // decision: this is an explicitly requested extra batch, outside the unchanged estimate
        // and search budget. It preserves the user's full original Data settings.
        initialUserSettingsSolution = optimizationProblem.evaluateInitialUserSettings(sequence);
      }

      if (tab != null) {
        optimizationProblem.setEvaluationListener(_ -> {
          final OptimizationResultsController controller = resultsController.get();
          if (controller != null) {
            controller.refreshEvaluatedSolutions();
          }
        });
        showLiveResultsWindow(tab, optimizationProblem, singlePassSolution,
            initialUserSettingsSolution, showExtendedStatistics, resultsController);
      }

      // created after the estimate, so start solutions use the final shape rejection limit
      final OptimizerAlgorithmModule optimizerModule = params.getValue(OptimizerParameters.optimizers)
          .getModuleInstance();
      optimizer = optimizerModule.createAlgorithm(optimizationProblem,
          OptimizerParameters.getSelectedOptimizerParameters(params));

      logger.info("Starting %s with a search budget of %d%s".formatted(optimizerModule.getName(),
          totalBatchExecutions, initialUserSettingsSolution == null ? ""
              : " plus one initial-user-settings comparison batch"));
      final String presetText =
          presets.isEmpty() ? "" : "Presets:\n%s\n".formatted(presets.describe());
      NotificationService.show(NotificationType.INFO, "Starting optimizer", """
          Using %s with %d full batch executions%s.
          %sEstimates:
          %s""".formatted(optimizerModule.getName(), totalBatchExecutions,
          initialUserSettingsSolution == null ? ""
              : " plus one initial-user-settings comparison batch", presetText,
          singlePassEstimates.describe()));

      try {
        final int maxProposals = Math.multiplyExact(totalBatchExecutions, PROPOSAL_BUDGET_MULTIPLIER);
        optimizer.run(new TaskStatusTerminationCondition(totalBatchExecutions, maxProposals,
            optimizationProblem::getSearchBatchExecutionCount, this::getStatus,
            stopSearchRequested::get));
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

      if (getStatus() != TaskStatus.PROCESSING) {
        return;
      }

      final NondominatedPopulation result = collectSearchResult(optimizationProblem);

      // log comparison: single-pass estimate versus the best optimizer result
      OptimizationResultLogger.logResults(singlePassSolution, singlePassEstimates,
          optimizationProblem.getEnabledMetrics());
      OptimizationResultLogger.logComparison(singlePassSolution, result,
          optimizationProblem.getEnabledMetrics());

      publishOutcome(new OptimizationOutcome(singlePassEstimates, singlePassSolution,
          initialUserSettingsSolution == null ? null : sequence.copy(), initialUserSettingsSolution,
          result, optimizationProblem), resultsController);
    } catch (RuntimeException exception) {
      if (!isCanceled()) {
        error("Parameter optimization failed: " + exception.getMessage(), exception);
      }
    } finally {
      try {
        // Completed evaluations remain usable after cancellation or an error, even when the
        // failure occurred before the algorithm entered its search loop.
        if (outcome == null && optimizationProblem.getCompletedBatchExecutionCount() > 0) {
          publishOutcome(new OptimizationOutcome(singlePassEstimates, singlePassSolution,
              initialUserSettingsSolution == null ? null : sequence.copy(), initialUserSettingsSolution,
              collectSearchResult(optimizationProblem), optimizationProblem), resultsController);
        }
      } finally {
        try {
          optimizationProblem.close();
        } finally {
          initialMemoryOption.enforceToMemoryMapping();
        }
      }
    }
  }

  /** Resolve task status before scheduling UI completion; AbstractTask preserves terminal states. */
  private void publishOutcome(final @NotNull OptimizationOutcome completed,
      final @NotNull AtomicReference<OptimizationResultsController> resultsController) {
    setStatus(TaskStatus.FINISHED);
    outcome = completed;
    completed.problem().setEvaluationListener(null);
    final OptimizationResultsController controller = resultsController.get();
    if (controller != null) {
      finishResultsView(controller, completed);
    }
  }

  private void finishResultsView(final @NotNull OptimizationResultsController controller,
      final @NotNull OptimizationOutcome completed) {
    if (getStatus() == TaskStatus.FINISHED) {
      controller.completeOptimization(completed.front());
    } else {
      controller.finishOptimization(completed.front(), getStatus());
    }
  }

  private void showLiveResultsWindow(@NotNull BatchWizardTab resultTab,
      @NotNull WizardOptimizationProblem optimizationProblem, @NotNull Solution singlePassSolution,
      @Nullable Solution initialUserSettingsSolution, final boolean showExtendedStatistics,
      @NotNull AtomicReference<OptimizationResultsController> resultsController) {
    FxThread.runLater(() -> {
      final OptimizationResultsController controller = OptimizationResultsController.showResultsWindow(
          resultTab, optimizationProblem, singlePassSolution, initialUserSettingsSolution,
          showExtendedStatistics, this::requestStopSearch, null);
      resultsController.set(controller);
      controller.refreshEvaluatedSolutions();
      final OptimizationOutcome alreadyCompleted = outcome;
      if (alreadyCompleted != null) {
        finishResultsView(controller, alreadyCompleted);
      }
    });
  }

  /**
   * Combines the algorithm's available result with every completed observation. A hard budget or
   * cancellation can happen after a batch completed but before the algorithm incorporated it.
   */
  private @NotNull NondominatedPopulation collectSearchResult(
      @NotNull WizardOptimizationProblem optimizationProblem) {
    final NondominatedPopulation optimizerResults = new NondominatedPopulation();
    final AbstractAlgorithm currentOptimizer = optimizer;
    if (currentOptimizer != null) {
      try {
        optimizerResults.addAll(currentOptimizer.getResult());
      } catch (NullPointerException e) {
        // Stopping during algorithm initialization can leave its result unavailable.
        logger.fine("Optimizer stopped before its initial population was complete.");
      }
    }
    return createSearchFront(optimizerResults, optimizationProblem.getEvaluatedSolutions());
  }

  static @NotNull NondominatedPopulation createSearchFront(
      @NotNull Iterable<@NotNull Solution> optimizerResults,
      @NotNull Iterable<@NotNull Solution> evaluatedSolutions) {
    final NondominatedPopulation result = new NondominatedPopulation();
    for (final Solution solution : optimizerResults) {
      result.add(solution);
    }
    for (final Solution solution : evaluatedSolutions) {
      result.add(solution);
    }
    return result;
  }

}
