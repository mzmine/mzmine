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
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatisticsDashboardPane;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.BenchmarkFeatureLoader;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.FeatureRecord;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimationContext;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.ParameterEstimators;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameterSet;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.RawDataAnalysis;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.RawDataPreparation;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.BatchExecutionLimitReachedException;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.OptimizationSearchStoppedException;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.TaskStatusTerminationCondition;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblem;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.gui.OptimizationResultsController;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.ShapeScoreDiagnostic;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.MoeadOptimizerParameters;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OptimizerOptions;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OriginTaggingInitialization;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.PatternSearchAlgorithm;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.SolutionOrigin;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.WarmStartInitialization;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.WarmStartSampling;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moeaframework.algorithm.AbstractAlgorithm;
import org.moeaframework.algorithm.MOEAD;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.initialization.Initialization;
import org.moeaframework.core.population.NondominatedPopulation;

public class BatchOptimizationMainTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(BatchOptimizationMainTask.class.getName());

  /**
   * Initial population size for MOEA/D. One evaluation is a full batch
   * run, so the MOEA Framework default of 100 would spend the entire batch budget on initialization
   * and leave no generations for the actual search.
   */
  private static final int MOEAD_POPULATION_SIZE = 20;

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
  private volatile WizardOptimizationProblem problem;

  public BatchOptimizationMainTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull File[] files, @Nullable File metadata,
      @NotNull BatchWizardTab tab, @NotNull OptimizerParameters params) {
    this(storage, moduleCallDate, files, metadata, tab.snapshotSequence(), tab, params);
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
   * what a user would get.
   */
  public BatchOptimizationMainTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull File[] files, @Nullable File metadata,
      @NotNull WizardSequence sequence, @NotNull OptimizerParameters params, long randomSeed) {
    this(storage, moduleCallDate, files, metadata, sequence, null, params, randomSeed);
  }

  private BatchOptimizationMainTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull File[] files, @Nullable File metadata,
      @NotNull WizardSequence sequence, @Nullable BatchWizardTab tab,
      @NotNull OptimizerParameters params) {
    this(storage, moduleCallDate, files, metadata, sequence, tab, params, DEFAULT_RANDOM_SEED);
  }

  private BatchOptimizationMainTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull File[] files, @Nullable File metadata,
      @NotNull WizardSequence sequence, @Nullable BatchWizardTab tab,
      @NotNull OptimizerParameters params, long randomSeed) {
    super(storage, moduleCallDate);
    this.files = files;
    this.metadata = metadata;
    this.sequence = sequence;
    this.tab = tab;
    this.params = params;
    this.randomSeed = randomSeed;

    addTaskStatusListener((_, newStatus, _) -> {
      if (newStatus == TaskStatus.CANCELED) {
        if (optimizer != null) {
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
    return current == null ? 0 : (int) current.getEvaluatedSolutions().stream()
        .map(solution -> solution.getAttribute(WizardOptimizationProblem.ATTR_BATCH_EXECUTION_INDEX))
        .filter(java.util.Objects::nonNull).distinct().count();
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
    final List<FeatureRecord> benchmarkFeatures =
        params.getValue(OptimizerParameters.benchmarkFeaturesFile)
            ? BenchmarkFeatureLoader.fromFile(null,
            params.getEmbeddedParameterValue(OptimizerParameters.benchmarkFeaturesFile),
            params.getValue(OptimizerParameters.benchmarkFeatureTypes)) : List.of();

    final List<DataFileStatistics> stats = RawDataPreparation.computeFileStatistics(importedFiles,
        benchmarkFeatures, getMemoryMapStorage());
    stats.forEach(stat -> logger.info(stat.getMzToleranceForIsotopes().toString()));

    // set a specific seed to make the results deterministic, see DEFAULT_RANDOM_SEED
    PRNG.setSeed(randomSeed);

    totalBatchExecutions = Math.max(params.getValue(OptimizerParameters.iterations), 30);
    final RawDataAnalysis analysis = RawDataAnalysis.analyze(stats);
    final ParameterEstimationContext estimationContext = new ParameterEstimationContext(analysis,
        sequence);
    final PreparedParameterSet singlePassEstimates = PreparedParameterSet.prepare(
        estimationContext);
    final WizardOptimizationProblem optimizationProblem = new WizardOptimizationProblem(
        estimationContext, singlePassEstimates, params, externalStatus, totalBatchExecutions,
        stopSearchRequested::get);
    problem = optimizationProblem;
    if (getStatus() == TaskStatus.CANCELED) {
      optimizationProblem.cancel();
    }
    try {
      if (DesktopService.isGUI()) {
        final List<DataFileStatistics> dashboardStats = List.copyOf(stats);
        FxThread.runLater(() -> MZmineCore.getDesktop().addTab(new SimpleTab("Auto Param Statistics",
            new DataFileStatisticsDashboardPane(dashboardStats,
                ParameterEstimators.interSampleRtStatistics(analysis),
                estimationContext.massDetectorType()))));
      }
      final AtomicReference<OptimizationResultsController> resultsController = new AtomicReference<>();
      final AtomicReference<NondominatedPopulation> completedResult = new AtomicReference<>();

      final OptimizerOptions optimizerOption = params.getValue(OptimizerParameters.optimizers);
      final ParameterSet optimizerParameters = OptimizerParameters.getSelectedOptimizerParameters(
          params);
      optimizer = optimizerOption.getOptimizer(optimizationProblem);

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

      // decision: Current is scored through the batch evaluator directly, never through the search
      // problem, because user values outside the search domain must remain exactly as entered.
      final WizardSequence currentSequence = sequence;
      final Solution currentSolution;
      if (optimizationProblem.currentMatchesEstimate(currentSequence, singlePassSolution)) {
        currentSolution = skippedCurrentSolution(singlePassSolution);
      } else {
        currentSolution = optimizationProblem.evaluateCurrentSequence(currentSequence);
      }

      if (tab != null) {
        optimizationProblem.setEvaluationListener(_ -> {
          final OptimizationResultsController controller = resultsController.get();
          if (controller != null) {
            controller.refreshEvaluatedSolutions();
          }
        });
        showLiveResultsWindow(tab, optimizationProblem, singlePassSolution, resultsController,
            completedResult);
      }

      final List<Solution> injected = switch (optimizerOption) {
        // decision: starting at the estimate is intrinsic to local pattern search, not an optional
        // warm-start strategy.
        case PATTERN_SEARCH -> WarmStartInitialization.createSolutions(optimizationProblem,
            PatternSearchAlgorithm.INITIAL_DESIGN_SIZE,
                WarmStartSampling.GAUSSIAN);
        case MOEAD -> {
          if (!optimizerParameters.getValue(MoeadOptimizerParameters.rawDataInitialization)) {
            yield List.of();
          }
          final WarmStartSampling sampling = optimizerParameters.getEmbeddedParameterValue(
              MoeadOptimizerParameters.rawDataInitialization);
          yield WarmStartInitialization.createSolutions(optimizationProblem, MOEAD_POPULATION_SIZE,
              sampling);
        }
      };

      if (!injected.isEmpty()) {
        logger.info("Initialization for %s: injected %d solutions, batch budget %d".formatted(
            optimizer.getName(), injected.size(), totalBatchExecutions));
        NotificationService.show(NotificationType.INFO, "Starting optimizer", """
            Using %d attempts around raw-data based estimations and %d full batch executions.
            Estimates:
            %s""".formatted(injected.size(), totalBatchExecutions, singlePassEstimates.describe()));
      }

      configureOptimizer(optimizerOption, optimizer, optimizationProblem, injected);

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
      final NondominatedPopulation result = createSearchFront(optimizer.getResult(),
          optimizationProblem.getEvaluatedSolutions());

      // log comparison: single-pass estimate versus the best optimizer result
      OptimizationResultLogger.logResults(singlePassSolution, singlePassEstimates,
          optimizationProblem.getEnabledMetrics());
      OptimizationResultLogger.logComparison(singlePassSolution, result,
          optimizationProblem.getEnabledMetrics());

      outcome = new OptimizationOutcome(singlePassEstimates, singlePassSolution, currentSequence,
          currentSolution, result,
          optimizationProblem);
      completedResult.set(result);
      optimizationProblem.setEvaluationListener(null);
      final OptimizationResultsController controller = resultsController.get();
      if (controller != null) {
        controller.completeOptimization(result);
      }

      setStatus(TaskStatus.FINISHED);
    } finally {
      optimizationProblem.close();
    }
  }

  private void showLiveResultsWindow(@NotNull BatchWizardTab resultTab,
      @NotNull WizardOptimizationProblem optimizationProblem, @NotNull Solution singlePassSolution,
      @NotNull AtomicReference<OptimizationResultsController> resultsController,
      @NotNull AtomicReference<NondominatedPopulation> completedResult) {
    FxThread.runLater(() -> {
      final Stage stage = new Stage();
      final OptimizationResultsController controller = new OptimizationResultsController(resultTab,
          optimizationProblem, singlePassSolution, stage, this::requestStopSearch);
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
      stage.setWidth(Math.min(1400d, screenWidth * 0.9d));
      stage.setHeight(Math.min(900d, screenHeight * 0.85d));
      stage.centerOnScreen();
    });
  }

  static @NotNull Solution skippedCurrentSolution(@NotNull Solution estimateSolution) {
    final Solution current = estimateSolution.copy();
    SolutionOrigin.CURRENT.applyTo(current);
    current.setAttribute("Current baseline skipped", true);
    return current;
  }

  static @NotNull NondominatedPopulation createSearchFront(
      @NotNull Iterable<@NotNull Solution> optimizerResults,
      @NotNull Iterable<@NotNull Solution> evaluatedSolutions) {
    final NondominatedPopulation result = new NondominatedPopulation();
    for (final Solution solution : optimizerResults) {
      if (SolutionOrigin.of(solution) != SolutionOrigin.CURRENT) {
        result.add(solution);
      }
    }
    for (final Solution solution : evaluatedSolutions) {
      if (SolutionOrigin.of(solution) != SolutionOrigin.CURRENT) {
        result.add(solution);
      }
    }
    return result;
  }

  /**
   * Configures the two supported algorithms and injects raw data-derived start solutions.
   *
   * @param injected solutions to seed the search with. May be empty for MOEA/D, which then
   *                 initializes randomly.
   */
  private void configureOptimizer(@NotNull OptimizerOptions option,
      @NotNull AbstractAlgorithm algorithm,
      @NotNull WizardOptimizationProblem problem, @NotNull List<Solution> injected) {
    switch (option) {
      case MOEAD -> {
        final MOEAD moead = (MOEAD) algorithm;
        moead.setInitialPopulationSize(MOEAD_POPULATION_SIZE);
        final Initialization initialization = new OriginTaggingInitialization(problem, injected);
        moead.setInitialization(initialization);
        // at the MOEA/D default of 20 the neighborhood equals the population, so mating draws
        // from the whole population and the decomposition loses the locality it relies on.
        // assumption: the floor of 4 keeps the neighborhood above the differential evolution arity
        // of 4 minus 1, which MOEA/D requires now that the solution vector is all real-valued
        moead.setNeighborhoodSize(Math.max(4, MOEAD_POPULATION_SIZE / 5));
        // the variation operator is chosen in the MOEAD constructor from whether every variable is
        // real-valued, so log it - "de+pm" confirms MOEA/D-DE, "sbx+pm+hux+bf" means the vector
        // still contains a non-real variable and the decomposition fell back to SBX
        logger.info("MOEA/D using variation %s, neighborhood %d, population %d".formatted(
            moead.getVariation().getName(), moead.getNeighborhoodSize(), MOEAD_POPULATION_SIZE));
      }
      case PATTERN_SEARCH -> ((PatternSearchAlgorithm) algorithm).setInitialSolutions(injected);
    }
  }

}
