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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.gui;

import com.opencsv.ICSVWriter;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxFileChooser;
import io.github.mzmine.javafx.util.FxFileChooser.FileSelectionType;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchModeParameters;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.tools.batchwizard.BatchWizardTab;
import io.github.mzmine.modules.tools.batchwizard.WizardParameterChanges.Source;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.FeatureRecord;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.OrdinalIntegerVariable;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblem;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.IsotopeRatioConsistencyScore;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.SolutionOrigin;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moeaframework.core.Solution;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.variable.RealVariable;

public class OptimizationResultsController extends FxController<OptimizationResultModel> {

  /**
   * Replaced by a new wizard tab if the original one was closed, see {@link #showWizardTab()}.
   */
  private @NotNull BatchWizardTab wizardTab;
  private final WizardOptimizationProblem optimization;
  @Nullable
  private final Stage stage;
  @Nullable
  private final Runnable stopSearchAction;

  /**
   * @param singlePassSolution     the evaluated raw data estimate. Shown as the first row of the
   *                               results table so it can always be compared against the optimized
   *                               solutions, regardless of whether it was used to warm-start the
   *                               optimizer.
   * @param showExtendedStatistics show every evaluated solution with all diagnostic attributes
   *                               instead of only the estimate and the current front
   */
  public OptimizationResultsController(@NotNull BatchWizardTab wizardTab,
      @NotNull WizardOptimizationProblem optimization, @Nullable final Solution singlePassSolution,
      final boolean showExtendedStatistics, @Nullable final Stage stage,
      @Nullable final Runnable stopSearchAction) {
    super(new OptimizationResultModel(showExtendedStatistics));
    this.wizardTab = wizardTab;
    this.optimization = optimization;
    model.getParameters().setAll(optimization.getIndexedParameters());
    this.stage = stage;
    this.stopSearchAction = stopSearchAction;
    model.preferredSortObjectiveIndexProperty().set(preferredSortObjectiveIndex());
    model.singlePassSolutionProperty().set(singlePassSolution);
    rebuildDisplayedSolutions();
  }

  /**
   * Refreshes the table and chart from all completed evaluations. Safe to call from the optimizer
   * task thread.
   */
  public void refreshEvaluatedSolutions() {
    onGuiThread(this::rebuildDisplayedSolutions);
  }

  /**
   * Publishes the final non-dominated population and enables actions that consume a selected
   * solution. Safe to call from the optimizer task thread.
   */
  public void completeOptimization(@NotNull NondominatedPopulation result) {
    onGuiThread(() -> completeModel(result));
  }

  private void completeModel(@NotNull NondominatedPopulation result) {
    model.resultProperty().set(result);
    model.stopSearchRequestedProperty().set(false);
    rebuildDisplayedSolutions();
    final Solution preferred = FrontSolutionRanker.selectBestAverageRank(result.asList(),
        model.getPreferredSortObjectiveIndex());
    model.preferredFrontSolutionProperty().set(preferred);
    model.selectedSolutionProperty().set(preferred);
    model.optimizationRunningProperty().set(false);
    if (stage != null) {
      stage.setTitle("Optimization Results");
      final String selectionMessage = preferred == null ? "" : preferred.getNumberOfObjectives() > 1
          ? " The solution with the best average rank across all scores was selected."
          : " The highest ranked solution was selected.";
      DialogLoggerUtil.showDialog(AlertType.INFORMATION, stage, "Optimization finished",
          "Parameter optimization has finished." + selectionMessage, true);
      stage.requestFocus();
    }
  }

  private int preferredSortObjectiveIndex() {
    final List<SweepMetric> metrics = optimization.getEnabledMetrics();
    for (int i = 0; i < metrics.size(); i++) {
      if (metrics.get(i) instanceof IsotopeRatioConsistencyScore) {
        return i;
      }
    }
    return 0;
  }

  /**
   * @return the non-dominated solutions among the given evaluations, respecting constraints.
   */
  private static @NotNull List<Solution> currentFront(@NotNull List<Solution> evaluated) {
    final NondominatedPopulation front = new NondominatedPopulation();
    front.addAll(evaluated);
    return front.asList();
  }

  private void rebuildDisplayedSolutions() {
    final NondominatedPopulation result = model.getResult();
    final Solution singlePassSolution = model.getSinglePassSolution();
    final List<Solution> evaluatedSolutions = optimization.getEvaluatedSolutions();

    // decision: while the search is running, derive the current front from all completed
    // evaluations, so the compact table can already show the best solutions found so far
    final List<Solution> front =
        result != null ? result.asList() : currentFront(evaluatedSolutions);
    model.getFrontSolutions().clear();
    model.getFrontSolutions().addAll(front);

    // identity based, because the estimate is also an evaluated solution and may be on the front
    final Set<Solution> alreadyShown = Collections.newSetFromMap(new IdentityHashMap<>());
    final List<Solution> displayed = new ArrayList<>();
    // the raw data estimate is intentionally kept outside the non-dominated population, which
    // would reject it whenever an optimized solution dominates it
    if (singlePassSolution != null && alreadyShown.add(singlePassSolution)) {
      displayed.add(singlePassSolution);
    }
    for (final Solution solution : front) {
      if (alreadyShown.add(solution)) {
        displayed.add(solution);
      }
    }

    // every remaining evaluated solution, so the table shows the whole search and not just the
    // front - dominated and infeasible candidates carry the diagnostics needed to judge where the
    // batch budget went, while cache hits remain visible as cheap proposals
    if (model.isShowExtendedStatistics()) {
      for (final Solution evaluated : evaluatedSolutions) {
        if (alreadyShown.add(evaluated)) {
          displayed.add(evaluated);
        }
      }
    }

    model.getDisplayedSolutions().setAll(displayed);
    model.getEvaluatedSolutions().setAll(evaluatedSolutions);
  }

  @Override
  protected @NotNull FxViewBuilder<OptimizationResultModel> getViewBuilder() {
    return new OptimizationResultsViewBuilder(this.model, this::applyToWizardSequence,
        this::openInBatch, this::exportSolutions, this::runBatchFilterResults,
        stopSearchAction == null ? null : this::requestStopSearch, stage);
  }

  private void requestStopSearch() {
    if (!model.isOptimizationRunning() || model.isStopSearchRequested()
        || stopSearchAction == null) {
      return;
    }
    model.stopSearchRequestedProperty().set(true);
    stopSearchAction.run();
  }

  public void applyToWizardSequence() {

    final ButtonType choice = ((MZmineGUI) DesktopService.getDesktop()).displayConfirmation(
        "Information", """
            This will replace the current wizard parameters.
            Continue?""", ButtonType.YES, ButtonType.NO);
    if (choice != ButtonType.YES) {
      return;
    }

    applySelectedSolutionToWizard();
  }

  /**
   * Applies only the estimated and optimized parameter values of the selected solution to the
   * wizard, keeping all other current wizard values.
   */
  private void applySelectedSolutionToWizard() {
    final Solution solution = Objects.requireNonNull(model.getSelectedSolution(),
        "No solution selected");
    showWizardTab().applyParameterValues(
        sequence -> optimization.applySolutionToWizard(solution, sequence), Source.OPTIMIZATION);
  }

  /**
   * Selects the wizard tab of the optimization. If it was closed, opens a new wizard tab with the
   * sequence the optimization started from, so the solution is applied to the same presets it was
   * evaluated with.
   */
  private @NotNull BatchWizardTab showWizardTab() {
    if (wizardTab.getTabPane() == null) {
      final BatchWizardTab reopened = new BatchWizardTab();
      // runs immediately on the JavaFX thread, so the tab pane is set afterward
      MZmineCore.getDesktop().addTab(reopened);
      reopened.applyPartialSequence(optimization.getInitialSequence());
      wizardTab = reopened;
    } else {
      wizardTab.getTabPane().getSelectionModel().select(wizardTab);
    }
    return wizardTab;
  }

  public void openInBatch() {
    final BatchQueue q = createOptimizedBatch();
    if (q == null) {
      return;
    }

    BatchModeParameters batchModeParameters = (BatchModeParameters) ConfigService.getConfiguration()
        .getModuleParameters(BatchModeModule.class);
    batchModeParameters.getParameter(BatchModeParameters.batchQueue).setValue(q);

    if (batchModeParameters.showSetupDialog(false) == ExitCode.OK) {
      MZmineCore.runMZmineModule(BatchModeModule.class, batchModeParameters.cloneParameterSet());
    }
  }

  private @Nullable BatchQueue createOptimizedBatch() {
    applySelectedSolutionToWizard();

    final WizardSequence sequenceSteps = wizardTab.getSequence();

    final Optional<WizardStepParameters> workflow = sequenceSteps.get(WizardPart.WORKFLOW);
    if (workflow.isEmpty()) {
      DialogLoggerUtil.showErrorDialog("Cannot create batch",
          "A workflow must be selected to create a batch.");
      return null;
    }

    BatchQueue q;
    try {
      q = ((WorkflowWizardParameterFactory) workflow.get().getFactory()).getBatchBuilder(
          sequenceSteps).createQueue();
    } catch (Exception e) {
      DialogLoggerUtil.showErrorDialog("Cannot create batch", e.getMessage());
      q = null;
    }
    return q;
  }

  private void runBatchFilterResults() {
    final BatchQueue q = createOptimizedBatch();
    final BatchModeParameters batchModeParameters = (BatchModeParameters) MZmineCore.getConfiguration()
        .getModuleParameters(BatchModeModule.class);
    batchModeParameters.getParameter(BatchModeParameters.batchQueue).setValue(q);

    final BatchTask batchTask = new BatchTask(ProjectService.getProject(), batchModeParameters,
        Instant.now(), null);
    TaskService.getController().addTask(batchTask);
    new AllTasksFinishedListener(List.of(batchTask), _ -> {
      final List<FeatureList> latestFlists = batchTask.getLatestCreatedFeatureLists();
      if (latestFlists.size() != 1) {
        throw new IllegalStateException(
            "More or less than 1 feature list as final result. Cannot annotate.");
      }
      final FeatureList flist = latestFlists.getFirst();
      final List<FeatureListRow> mzSortedRows = flist.stream()
          .sorted(FeatureListRowSorter.MZ_ASCENDING).toList();

      for (final FeatureRecord target : optimization.getFileOnlyBenchmarkFeatures()) {
        final FeatureListRow bestMatch = target.getBestMatch(mzSortedRows);
        if (bestMatch == null) {
          continue;
        }
        final SimpleCompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();
        annotation.put(CompoundNameType.class, "target feature");
        bestMatch.addCompoundAnnotation(annotation);
      }

      final ModularFeatureList copy = FeatureListUtils.createCopy(flist, null, " target", null,
          false, flist.getRawDataFiles(), false, null, null);
      for (final FeatureRecord target : optimization.getAllTargets()) {
        final FeatureListRow bestMatch = target.getBestMatch(mzSortedRows);
        if (bestMatch == null) {
          continue;
        }
        final SimpleCompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();
        annotation.put(CompoundNameType.class, "benchmark feature");
        final ModularFeatureListRow annotatedRow = new ModularFeatureListRow(copy,
            (ModularFeatureListRow) bestMatch, true);
        annotatedRow.addCompoundAnnotation(annotation);
        copy.addRow(annotatedRow);
      }
      FxThread.runLater(() -> ProjectService.getProject().addFeatureList(copy));
    });
  }

  private void exportSolutions() {

    // export exactly what the table shows, which includes the raw data estimate row
    final List<Solution> solutions = List.copyOf(model.getDisplayedSolutions());
    if (solutions.isEmpty()) {
      return;
    }

    FxThread.runLater(() -> {
      final File file = FxFileChooser.openSelectDialog(FileSelectionType.SAVE,
          List.of(ExtensionFilters.CSV), null, "Export solutions");
      if (file == null) {
        return;
      }
      try (final ICSVWriter writer = CSVParsingUtils.createDefaultWriter(file, ',',
          WriterOptions.REPLACE)) {
        writeSolutions(writer, solutions);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

  }

  /**
   * Writes the displayed solutions as csv.
   * <p>
   * decision: written column by column instead of via {@code Population.asTabularData()}, which
   * emits the raw variable values. {@link OrdinalIntegerVariable} is backed by a real value, so the
   * raw export would show e.g. {@code 1.514916} for an m/z tolerance index that the batch actually
   * ran as {@code 2}, and would not match the results table.
   */
  private void writeSolutions(@NotNull ICSVWriter writer, @NotNull List<Solution> solutions) {
    final Solution template = solutions.getFirst();

    final boolean showOrigin = model.isAttributeShown(SolutionOrigin.ATTRIBUTE);

    final List<String> header = new ArrayList<>();
    header.add("Source");
    // kept next to Source and out of the sorted attribute block below, so the two columns that
    // classify a row stay side by side, exactly as in the results table
    if (showOrigin) {
      header.add(SolutionOrigin.ATTRIBUTE);
    }
    for (int i = 0; i < template.getNumberOfVariables(); i++) {
      header.add(template.getVariable(i).getName());
    }
    for (int i = 0; i < template.getNumberOfObjectives(); i++) {
      header.add(template.getObjective(i).getName());
    }
    // the diagnostic values live in attributes, so the csv has to carry the same ones the results
    // table shows - an export without them cannot be analysed
    final List<String> attributes = template.getAttributes().keySet().stream()
        .filter(model::isAttributeShown).filter(a -> !a.equals(SolutionOrigin.ATTRIBUTE)).sorted()
        .toList();
    header.addAll(attributes);
    writer.writeNext(header.toArray(String[]::new));

    final Solution singlePass = model.getSinglePassSolution();
    for (final Solution solution : solutions) {
      final List<String> row = new ArrayList<>(header.size());
      row.add(solution == singlePass ? "Raw data estimate"
          : model.isOnFront(solution) ? "Front" : "Evaluated");
      if (showOrigin) {
        row.add(Objects.toString(solution.getAttribute(SolutionOrigin.ATTRIBUTE), ""));
      }
      for (int i = 0; i < solution.getNumberOfVariables(); i++) {
        // the effective value, so the csv matches what the batch was actually run with
        row.add(solution.getVariable(i) instanceof OrdinalIntegerVariable ? Integer.toString(
            OrdinalIntegerVariable.getInt(solution, i))
            : Double.toString(RealVariable.getReal(solution.getVariable(i))));
      }
      for (int i = 0; i < solution.getNumberOfObjectives(); i++) {
        row.add(Double.toString(solution.getObjectiveValue(i)));
      }
      for (final String attribute : attributes) {
        row.add(Objects.toString(solution.getAttribute(attribute), ""));
      }
      writer.writeNext(row.toArray(String[]::new));
    }
  }
}
