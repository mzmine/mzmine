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

import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.SimpleXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxSplitPanes;
import io.github.mzmine.javafx.components.factories.TableColumns;
import io.github.mzmine.javafx.components.factories.TableColumns.ColumnAlignment;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.ChoiceSearchDomain;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.OrdinalIntegerVariable;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.IndexedParameter;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblem;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.SolutionOrigin;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.BasicStroke;
import java.awt.geom.Ellipse2D;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.NumberAxis;
import org.moeaframework.core.Solution;
import org.moeaframework.core.objective.Minimize;
import org.moeaframework.core.objective.Objective;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.core.variable.Variable;

public class OptimizationResultsViewBuilder extends FxViewBuilder<OptimizationResultModel> {

  private static final String SOURCE_ESTIMATE = "Raw data estimate";
  private static final String SOURCE_FRONT = "Front";
  private static final String SOURCE_EVALUATED = "Evaluated";
  /**
   * Table height without extended statistics: header, about two rows and a horizontal scroll bar.
   */
  private static final double COMPACT_TABLE_HEIGHT = 100d;
  /**
   * Extra width so a vertical scroll bar in the compact table does not cover the last column.
   */
  private static final double VERTICAL_SCROLL_BAR_ALLOWANCE = 20d;

  private final NumberFormat threeDecimals = new DecimalFormat("0.###");
  private final NumberFormat noDecimals = new DecimalFormat("0");

  private final Runnable onAcceptPressed;
  private final Runnable openInBatch;
  private final Runnable onExportPressed;
  private final Runnable quickRun;
  @Nullable
  private final Runnable stopSearch;
  @Nullable
  private final Stage stage;

  protected OptimizationResultsViewBuilder(@NotNull OptimizationResultModel model,
      @NotNull Runnable onAcceptPressed, @NotNull Runnable openInBatch,
      @NotNull Runnable onExportPressed, @NotNull Runnable quickRun, @Nullable Runnable stopSearch,
      @Nullable final Stage stage) {
    super(model);
    this.onAcceptPressed = onAcceptPressed;
    this.openInBatch = openInBatch;
    this.onExportPressed = onExportPressed;
    this.quickRun = quickRun;
    this.stopSearch = stopSearch;
    this.stage = stage;
  }

  /**
   * Sets the stage width to the summed effective preferred column widths and the height to the same
   * value, both limited to the screen size.
   */
  private static void fitStageWidthToColumns(@NotNull Stage stage,
      @NotNull TableView<Solution> table) {
    // TableColumns only sets the min width, which exceeds the default pref width of 80 px
    final double columnsWidth = table.getVisibleLeafColumns().stream()
        .mapToDouble(c -> Math.max(c.getPrefWidth(), c.getMinWidth())).sum();
    final Scene scene = stage.getScene();
    final double decorationWidth = scene == null ? 0d : stage.getWidth() - scene.getWidth();
    final double width = columnsWidth + table.snappedLeftInset() + table.snappedRightInset()
        + VERTICAL_SCROLL_BAR_ALLOWANCE + decorationWidth;
    final Rectangle2D screen = Screen.getPrimary().getVisualBounds();
    final double fittedWidth = Math.min(width, screen.getWidth() * 0.9d);
    stage.setWidth(fittedWidth);
    stage.setHeight(Math.min(fittedWidth * 0.6, screen.getHeight() * 0.6d));
  }

  @Override
  public @NotNull Region build() {
    final TableView<Solution> solutionTable = new TableView<>();
    solutionTable.setRowFactory(_ -> new OptimizationSolutionTableRow(model));
    // decision: bind the model's single observable list instead of replacing the items list, so
    // listeners and bindings on it are not discarded
    solutionTable.setItems(model.getDisplayedSolutions());
    createColumns(solutionTable);

    solutionTable.getSelectionModel().selectedItemProperty()
        .subscribe(s -> model.selectedSolutionProperty().set(s));
    model.preferredFrontSolutionProperty().subscribe(preferred -> {
      solutionTable.refresh();
      focusSolution(solutionTable, preferred);
    });
    focusSolution(solutionTable, model.getPreferredFrontSolution());

    final SimpleXYChart<PlotXYDataProvider> progressChart = createProgressChart();
    // decision: the chart plots every evaluation, also when the table only shows the front
    model.getEvaluatedSolutions()
        .addListener((ListChangeListener<Solution>) _ -> updateProgressChart(progressChart));
    updateProgressChart(progressChart);

    final BorderPane borderPane = new BorderPane();
    if (model.isShowExtendedStatistics()) {
      final SplitPane content = FxSplitPanes.newSplitPane(0.45, Orientation.VERTICAL, progressChart,
          solutionTable);
      borderPane.setCenter(content);
    } else {
      // decision: the compact table only holds the estimate and the front, so a fixed height of
      // roughly two rows plus header and scroll bar leaves the space to the chart
      solutionTable.setPrefHeight(COMPACT_TABLE_HEIGHT);
      solutionTable.setMinHeight(COMPACT_TABLE_HEIGHT);
      final BorderPane content = new BorderPane(progressChart);
      content.setBottom(solutionTable);
      borderPane.setCenter(content);
      if (stage != null) {
        // decision: sized once the window is shown, when the table insets and the window
        // decoration are known
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN,
            _ -> fitStageWidthToColumns(stage, solutionTable));
      }
    }

    final Button acceptButton = FxButtons.createButton("Apply to wizard", FxIcons.CHECK_CIRCLE,
        null, onAcceptPressed);
    final Button batchButton = FxButtons.createButton("Open in batch", FxIcons.BATCH, null,
        openInBatch);
    final Button exportButton = FxButtons.createButton("Export to .csv", FxIcons.FILE, null,
        onExportPressed);
    final Button quickRunButton = FxButtons.createButton("Quick run & annotate", FxIcons.BATCH,
        null, quickRun);

    final BooleanBinding noUsableSelection = model.optimizationRunningProperty()
        .or(model.selectedSolutionProperty().isNull());
    acceptButton.disableProperty().bind(noUsableSelection);
    batchButton.disableProperty().bind(noUsableSelection);
    quickRunButton.disableProperty().bind(noUsableSelection);
    exportButton.disableProperty().bind(Bindings.isEmpty(model.getDisplayedSolutions()));

    final ButtonBar buttonBar = new ButtonBar();
    buttonBar.getButtons().addAll(exportButton, batchButton, quickRunButton, acceptButton);
    borderPane.setBottom(buttonBar);

    if (stopSearch != null) {
      final Button stopButton = FxButtons.createButton("Stop search", FxIcons.STOP,
          "Finish after the current batch and keep all completed results", stopSearch);
      stopButton.disableProperty()
          .bind(model.optimizationRunningProperty().not().or(model.stopSearchRequestedProperty()));
      stopButton.textProperty().bind(
          Bindings.when(model.stopSearchRequestedProperty()).then("Stopping...")
              .otherwise("Stop search"));
      ButtonBar.setButtonData(stopButton, ButtonBar.ButtonData.LEFT);
      buttonBar.getButtons().add(0, stopButton);
    }

    if (stage != null) {
      final Runnable close = () -> {
        if (stopSearch != null) {
          stopSearch.run();
        }
        stage.hide();
      };
      final Button closeButton = FxButtons.createButton("Close", FxIcons.CANCEL, null, close);
      buttonBar.getButtons().add(closeButton);
      // the window close button must also stop the search, otherwise it continues hidden
      stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, _ -> close.run());
    }

    return borderPane;
  }

  private void focusSolution(@NotNull TableView<Solution> solutionTable,
      @Nullable Solution solution) {
    if (solution == null) {
      return;
    }
    final int row = solutionTable.getItems().indexOf(solution);
    if (row < 0) {
      return;
    }
    solutionTable.getSelectionModel().select(row);
    solutionTable.getFocusModel().focus(row);
    // decision: the compact table is only about two rows high, scrolling to the selected front
    // solution would hide the raw data estimate in the first row, which is kept for comparison
    if (model.isShowExtendedStatistics()) {
      solutionTable.scrollTo(row);
    }
    solutionTable.requestFocus();
  }

  private @NotNull SimpleXYChart<PlotXYDataProvider> createProgressChart() {
    final Solution template = model.getDisplayedSolutions().getFirst();
    final String objectiveName = template.getObjective(0).getName();
    final SimpleXYChart<PlotXYDataProvider> chart = new SimpleXYChart<>(
        "Candidate evaluations and best-so-far", "Evaluation number", objectiveName);
    chart.setItemLabelsVisible(false);
    chart.setShowCrosshair(true);
    chart.setMinHeight(260d);
    final NumberAxis evaluationAxis = (NumberAxis) chart.getXYPlot().getDomainAxis();
    evaluationAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    return chart;
  }

  private void updateProgressChart(@NotNull SimpleXYChart<PlotXYDataProvider> chart) {
    final List<Solution> solutions = List.copyOf(model.getEvaluatedSolutions());
    if (solutions.isEmpty()) {
      chart.removeAllDatasets();
      return;
    }

    // assumption: the first objective is the score shown in the live chart. Multi-objective runs
    // retain all objective columns in the table; the chart label states which one is plotted.
    final OptimizationProgressData progress = OptimizationProgressData.create(solutions,
        model.getSinglePassSolution(), 0);
    chart.setRangeAxisLabel(progress.objectiveName());

    final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();
    final List<DatasetAndRenderer> datasets = new ArrayList<>(3);

    final SimpleXYProvider candidates = new SimpleXYProvider("Candidate score",
        palette.getPositiveColorAWT(), progress.evaluations(), progress.scores(), noDecimals,
        threeDecimals);
    final ColoredXYShapeRenderer candidateRenderer = new ColoredXYShapeRenderer(false,
        new Ellipse2D.Double(-3.5d, -3.5d, 7d, 7d), true);
    datasets.add(new DatasetAndRenderer(new ColoredXYDataset(candidates, RunOption.THIS_THREAD),
        candidateRenderer));

    final SimpleXYProvider best = new SimpleXYProvider("Best so far", palette.getAWT(1),
        progress.bestEvaluations(), progress.bestScores(), noDecimals, threeDecimals);
    final ColoredXYLineRenderer bestRenderer = new ColoredXYLineRenderer();
    bestRenderer.setDefaultStroke(new BasicStroke(2.2f));
    datasets.add(
        new DatasetAndRenderer(new ColoredXYDataset(best, RunOption.THIS_THREAD), bestRenderer));

    if (Double.isFinite(progress.estimateScore()) && progress.evaluations().length > 0) {
      final double firstEvaluation = progress.evaluations()[0];
      final double lastEvaluation = progress.evaluations()[progress.evaluations().length - 1];
      final double start =
          firstEvaluation == lastEvaluation ? firstEvaluation - 0.5d : firstEvaluation;
      final double end = firstEvaluation == lastEvaluation ? lastEvaluation + 0.5d : lastEvaluation;
      final SimpleXYProvider estimate = new SimpleXYProvider("Raw data estimate",
          palette.getNeutralColorAWT(), new double[]{start, end},
          new double[]{progress.estimateScore(), progress.estimateScore()}, noDecimals,
          threeDecimals);
      final ColoredXYLineRenderer estimateRenderer = new ColoredXYLineRenderer();
      estimateRenderer.setDefaultStroke(
          new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
              new float[]{6f, 4f}, 0f));
      datasets.add(new DatasetAndRenderer(new ColoredXYDataset(estimate, RunOption.THIS_THREAD),
          estimateRenderer));
    }

    chart.setDatasetsAndRenderers(datasets);
  }

  /**
   * Derives the table columns from the first displayed solution. The raw data estimate is always
   * the first row, so it remains a valid template even when the optimizer returned no solutions,
   * for example after an early cancel.
   */
  private void createColumns(@NotNull TableView<Solution> solutionTable) {
    final ObservableList<Solution> solutions = model.getDisplayedSolutions();
    if (solutions.isEmpty()) {
      return;
    }
    final Solution template = solutions.getFirst();

    final TableColumn<Solution, String> sourceCol = TableColumns.createColumn("Source", 130,
        s -> new ReadOnlyStringWrapper(s == model.getSinglePassSolution() ? SOURCE_ESTIMATE
            : model.isOnFront(s) ? SOURCE_FRONT : SOURCE_EVALUATED));
    solutionTable.getColumns().add(sourceCol);

    // origin and evaluation index are diagnostics of the search, not of the solution itself
    if (model.isShowExtendedStatistics()) {
      // decision: pinned next to Source instead of left to the generic attribute loop below, which
      // would scatter it among the diagnostics. Source says how a row got into the table, Origin
      // says which phase of the run produced its parameters.
      final TableColumn<Solution, String> originCol = TableColumns.createColumn(
          SolutionOrigin.ATTRIBUTE, 100, s -> new ReadOnlyStringWrapper(
              Objects.requireNonNullElse(s.getAttribute(SolutionOrigin.ATTRIBUTE), "").toString()));
      solutionTable.getColumns().add(originCol);

      final TableColumn<Solution, Number> indexCol = TableColumns.createColumn("Evaluation", 80,
          s -> new ReadOnlyIntegerWrapper(evaluationIndex(s)));
      solutionTable.getColumns().add(indexCol);
    }

    for (int i = 0; i < template.getNumberOfVariables(); i++) {
      final Variable variable = template.getVariable(i);
      final int finalI = i;
      switch (variable) {
        // decision: the ordinal case must precede RealVariable, which it extends
        case OrdinalIntegerVariable v -> {
          final IndexedParameter<?> parameter = model.getParameters().get(i);
          if (parameter.parameter().searchDomain() instanceof ChoiceSearchDomain<?>) {
            final TableColumn<Solution, String> col = TableColumns.createColumn(v.getName(), 120,
                200, ColumnAlignment.RIGHT, String::compareTo, s -> new ReadOnlyStringWrapper(
                    parameter.value(s).toString()));
            solutionTable.getColumns().add(col);
          } else {
            final TableColumn<Solution, Number> col = TableColumns.createColumn(v.getName(), 120,
                noDecimals, ColumnAlignment.RIGHT,
                s -> new ReadOnlyIntegerWrapper(OrdinalIntegerVariable.getInt(s, finalI)));
            solutionTable.getColumns().add(col);
          }
        }
        case RealVariable v -> {
          final TableColumn<Solution, Number> col = TableColumns.createColumn(v.getName(), 120,
              threeDecimals, ColumnAlignment.RIGHT,
              s -> new ReadOnlyDoubleWrapper(((RealVariable) s.getVariable(finalI)).getValue()));
          solutionTable.getColumns().add(col);
        }
        default -> {

        }
      }
    }

    TableColumn<Solution, Number> preferredSortColumn = null;
    for (int i = 0; i < template.getNumberOfObjectives(); i++) {
      final TableColumn<Solution, Number> col = createObjectiveColumn(template.getObjective(i), i);
      solutionTable.getColumns().add(col);
      if (i == model.getPreferredSortObjectiveIndex()) {
        preferredSortColumn = col;
      }
    }
    if (preferredSortColumn != null) {
      final int objectiveIndex = model.getPreferredSortObjectiveIndex();
      preferredSortColumn.setSortType(
          template.getObjective(objectiveIndex) instanceof Minimize ? TableColumn.SortType.ASCENDING
              : TableColumn.SortType.DESCENDING);
      solutionTable.getSortOrder().setAll(preferredSortColumn);
    }

    for (Entry<String, Serializable> attributeEntry : template.getAttributes().entrySet()) {
      final String attribute = attributeEntry.getKey();
      // Skip hidden attributes and the origin and proposal index, which already have their own
      // columns next to Source
      if (!model.isAttributeShown(attribute) || attribute.equals(SolutionOrigin.ATTRIBUTE)
          || attribute.equals(WizardOptimizationProblem.ATTR_PROPOSAL_INDEX)) {
        continue;
      }
      final TableColumn<Solution, String> col = TableColumns.createColumn(attribute, 120,
          s -> new ReadOnlyStringWrapper(
              Objects.requireNonNullElse(s.getAttribute(attribute), "").toString()));
      solutionTable.getColumns().add(col);
    }
  }

  /**
   * A score column with a bar, colored by the direction of the objective.
   */
  private @NotNull TableColumn<Solution, Number> createObjectiveColumn(@NotNull Objective objective,
      int index) {
    final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();
    final Color color =
        objective instanceof Minimize ? palette.getNegativeColor() : palette.getPositiveColor();
    final TableColumn<Solution, Number> column = TableColumns.createColumn(objective.getName(), 140,
        threeDecimals, ColumnAlignment.RIGHT,
        s -> new ReadOnlyDoubleWrapper(s.getObjectiveValue(index)));
    column.setCellFactory(_ -> new BarTableCell(color, threeDecimals));
    return column;
  }

  private int evaluationIndex(@NotNull Solution solution) {
    final Object index = solution.getAttribute(WizardOptimizationProblem.ATTR_PROPOSAL_INDEX);
    return index instanceof Number number ? number.intValue()
        : model.getDisplayedSolutions().indexOf(solution) + 1;
  }
}
