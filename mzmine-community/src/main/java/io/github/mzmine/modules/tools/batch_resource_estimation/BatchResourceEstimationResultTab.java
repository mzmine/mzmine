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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.gui.chartbasics.FxChartFactory;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.FxJFreeChart;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.components.factories.FxSplitPanes;
import io.github.mzmine.javafx.components.factories.TableColumns;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.parameters.parametertypes.IntegerComponent;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Result of the batch resource estimation: per step bar charts of the measured subsets and the
 * extrapolation to all files and to a number of files that the user enters, plus a summary and a
 * table of the extrapolation to the entered number of files.
 */
final class BatchResourceEstimationResultTab extends SimpleTab {

  private final @NotNull ParameterSet parameters;
  private @NotNull EstimationSession session;
  private @NotNull BatchResourceModel model;
  private int files;
  private final ObservableList<StepResourcePrediction> stepPredictions = FXCollections.observableArrayList();
  private final GridPane summary = new GridPane();
  private final Label threadsLabel = new Label();
  private final IntegerComponent additionalRunFiles;
  private final IntegerProperty largestMeasuredFiles = new SimpleIntegerProperty();
  private final BooleanProperty largerRunRunning = new SimpleBooleanProperty(false);
  private final EstimationOutcomePane outcomePane = new EstimationOutcomePane();
  private final Map<ResourceMetric, CategoryPlot> plots = new EnumMap<>(ResourceMetric.class);
  private final double maxHeapGB = Runtime.getRuntime().maxMemory() / (double) (1 << 30);
  // read once when the result is shown, never on spinner changes (may spin up a disk)
  private final double tempDirFreeGB = FileAndPathUtil.getTempDir().getUsableSpace() / 1e9;

  private BatchResourceEstimationResultTab(@NotNull final EstimationSession session,
      @NotNull final BatchResourceModel model, @NotNull final ParameterSet parameters) {
    super("Batch resource estimation");
    this.session = session;
    this.model = model;
    this.parameters = parameters;
    this.files = model.totalFiles();

    final IntegerComponent filesComponent = new IntegerComponent(100, 1, null);
    filesComponent.setPrefWrapLength(100);
    filesComponent.setMinWidth(Region.USE_PREF_SIZE);
    filesComponent.setMaxWidth(Region.USE_PREF_SIZE);
    filesComponent.setText(String.valueOf(model.totalFiles()));
    filesComponent.setToolTipText("""
        Estimate resources for this many files, using the average file size of the batch (%d files).""".formatted(
        model.totalFiles()));
    // the value is committed on enter or focus loss
    filesComponent.valueProperty().addListener((_, _, n) -> {
      if (n != null && n > 0) {
        files = n;
        update();
      }
    });
    additionalRunFiles = new IntegerComponent(100, 1, model.totalFiles());
    additionalRunFiles.setPrefWrapLength(100);
    additionalRunFiles.setMinWidth(Region.USE_PREF_SIZE);
    additionalRunFiles.setMaxWidth(Region.USE_PREF_SIZE);
    additionalRunFiles.disableProperty()
        .bind(largerRunRunning.or(largestMeasuredFiles.greaterThanOrEqualTo(model.totalFiles())));
    updateAdditionalRunFiles();
    final Button runButton = FxButtons.createButton("Run estimate", FxIcons.RELOAD,
        "Run a larger subset and add its measurements to this result",
        () -> startLargerRun(selectedRunFiles()));
    runButton.disableProperty().bind(Bindings.createBooleanBinding(
        () -> largerRunRunning.get() || selectedRunFiles() <= largestMeasuredFiles.get()
            || selectedRunFiles() > this.model.totalFiles(), largerRunRunning, largestMeasuredFiles,
        additionalRunFiles.getTextField().textProperty()));
    summary.setHgap(24);
    summary.setVgap(6);
    summary.setMinWidth(0);
    summary.setMaxWidth(Double.MAX_VALUE);
    for (int i = 0; i < 2; i++) {
      final ColumnConstraints column = new ColumnConstraints();
      column.setPercentWidth(50);
      column.setHgrow(Priority.ALWAYS);
      summary.getColumnConstraints().add(column);
    }
    final VBox top = FxLayout.newVBox(outcomePane,
        FxLayout.newHBox(Pos.CENTER_LEFT, FxLabels.newBoldLabel("Estimate for files:"),
            filesComponent, threadsLabel, FxLabels.newLabel("Additional run files:"),
            additionalRunFiles, runButton), summary);
    top.setFillWidth(true);
    top.setMinWidth(0);

    final BorderPane root = new BorderPane();
    root.setMinSize(0, 0);
    root.setPadding(FxLayout.DEFAULT_PADDING_INSETS);
    root.setTop(top);
    root.setCenter(
        FxSplitPanes.newSplitPane(0.7, Orientation.VERTICAL, createCharts(), createTable()));

    setContent(root);
    update();
  }

  /**
   * Opens the result in a main window tab. A larger run can be added regardless of the verdict.
   * Call on the FX thread.
   *
   * @param parameters the parameters of the estimation
   */
  static void show(@NotNull final EstimationSession session,
      @NotNull final BatchResourceModel model, @NotNull final ParameterSet parameters) {
    MZmineCore.getDesktop()
        .addTab(new BatchResourceEstimationResultTab(session, model, parameters));
  }

  /**
   * Shows the result with an additional run.
   */
  private void setResult(@NotNull final EstimationSession session,
      @NotNull final BatchResourceModel model) {
    this.session = session;
    this.model = model;
    updateAdditionalRunFiles();
    largerRunRunning.set(false);
    update();
    if (getTabPane() == null) {
      MZmineCore.getDesktop().addTab(this);
    } else {
      getTabPane().getSelectionModel().select(this);
    }
  }

  /**
   * A further run extends the current result instead of repeating all subsets. Runs must grow in
   * size because the model fits the final segment of the measured curve.
   */
  private void startLargerRun(final int runFiles) {
    if (largerRunRunning.get() || runFiles <= largestMeasuredFiles.get()
        || runFiles > model.totalFiles() || !BatchResourceEstimationModule.confirmClearProject(
        ProjectService.getProject())) {
      return;
    }
    largerRunRunning.set(true);
    final BatchResourceExtensionTask task = new BatchResourceExtensionTask(Instant.now(),
        parameters, session, runFiles,
        (extended, extendedModel) -> FxThread.runLater(() -> setResult(extended, extendedModel)));
    // allow a new run after an error or cancel
    task.addTaskStatusListener((_, newStatus, _) -> {
      if (newStatus == TaskStatus.ERROR || newStatus == TaskStatus.CANCELED) {
        FxThread.runLater(() -> largerRunRunning.set(false));
      }
    });
    MZmineCore.getTaskController().addTask(task);
  }

  private void updateAdditionalRunFiles() {
    final int largest = session.extensionRuns().isEmpty() ? session.validationRun().files()
        : session.extensionRuns().getLast().files();
    largestMeasuredFiles.set(largest);
    additionalRunFiles.setText(String.valueOf(Math.min(2L * largest, model.totalFiles())));
    additionalRunFiles.setToolTipText(
        largest >= model.totalFiles() ? "All files in the batch have already been measured"
            : "Run with more than %d and at most %d files".formatted(largest, model.totalFiles()));
  }

  private int selectedRunFiles() {
    try {
      return Integer.parseInt(additionalRunFiles.getText());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private void update() {
    final BatchResourcePrediction custom = model.predict(files);
    outcomePane.setOutcome(EstimationOutcome.of(model, custom, maxHeapGB, tempDirFreeGB));
    threadsLabel.setText("(%d threads)".formatted(model.numThreads()));
    summary.getChildren().clear();
    final List<String> statistics = BatchResourceReport.summaryLines(custom, maxHeapGB,
        tempDirFreeGB);
    for (int i = 0; i < statistics.size(); i++) {
      final Label label = new Label(statistics.get(i));
      label.setWrapText(true);
      label.setMinWidth(0);
      label.setMaxWidth(Double.MAX_VALUE);
      GridPane.setValignment(label, VPos.TOP);
      summary.add(label, i % 2, i / 2);
    }
    stepPredictions.setAll(custom.steps());

    // series: measured subsets, validation and the extrapolation to the entered number of files
    final List<BatchResourcePrediction> series = new ArrayList<>(model.measured());
    final List<String> seriesLabels = new ArrayList<>(
        model.measured().stream().map(m -> "%d files (measured)".formatted(m.files())).toList());
    final ModelValidation validation = model.validation();
    if (validation != null) {
      series.add(validation.measured());
      seriesLabels.add("%d files (validation, measured)".formatted(validation.measured().files()));
      series.add(validation.predicted());
      seriesLabels.add(
          "%d files (validation, estimated)".formatted(validation.predicted().files()));
    }
    for (final BatchResourcePrediction extension : model.extensions()) {
      series.add(extension);
      seriesLabels.add("%d files (additional run, measured)".formatted(extension.files()));
    }
    series.add(custom);
    seriesLabels.add("%d files (estimated)".formatted(files));

    plots.forEach((metric, plot) -> {
      final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
      for (int i = 0; i < series.size(); i++) {
        for (final StepResourcePrediction step : series.get(i).steps()) {
          dataset.addValue(metric.value(step), seriesLabels.get(i),
              "%d %s".formatted(step.step(), step.name()));
        }
      }
      if (metric == ResourceMetric.HEAP) {
        final RamReliabilityBarRenderer renderer = (RamReliabilityBarRenderer) plot.getRenderer();
        renderer.setRuns(series);
        renderer.setDefaultToolTipGenerator((data, row, column) -> {
          final StepResourcePrediction step = series.get(row).steps().get(column);
          return "%s, %s: %.3f GB; %s".formatted(data.getRowKey(row), data.getColumnKey(column),
              step.heapGB(), step.heapReliability());
        });
      }
      plot.setDataset(dataset);
    });
  }

  private @NotNull GridPane createCharts() {
    final GridPane grid = new GridPane();
    grid.setMinSize(0, 0);
    for (int i = 0; i < 2; i++) {
      final ColumnConstraints column = new ColumnConstraints();
      column.setPercentWidth(50);
      column.setHgrow(Priority.ALWAYS);
      grid.getColumnConstraints().add(column);
      final RowConstraints row = new RowConstraints();
      row.setPercentHeight(50);
      row.setVgrow(Priority.ALWAYS);
      grid.getRowConstraints().add(row);
    }

    final List<Color> colors = ConfigService.getDefaultColorPalette().getColorsAWT();
    final ResourceMetric[] metrics = ResourceMetric.values();
    for (int i = 0; i < metrics.length; i++) {
      final ResourceMetric metric = metrics[i];
      final FxJFreeChart chart = FxChartFactory.createBarChart(metric.title(), null,
          metric.axisLabel(), new DefaultCategoryDataset(), PlotOrientation.VERTICAL, true, true);
      final EChartViewer viewer = new EChartViewer(chart);
      viewer.setMinSize(0, 0);
      viewer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
      GridPane.setHgrow(viewer, Priority.ALWAYS);
      GridPane.setVgrow(viewer, Priority.ALWAYS);

      final CategoryPlot plot = chart.getCategoryPlot();
      plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
      final BarRenderer renderer;
      if (metric == ResourceMetric.HEAP) {
        renderer = new RamReliabilityBarRenderer();
        plot.setRenderer(renderer);
      } else {
        renderer = (BarRenderer) plot.getRenderer();
      }
      renderer.setItemMargin(0.02);
      for (int c = 0; c < colors.size(); c++) {
        renderer.setSeriesPaint(c, colors.get(c));
      }
      plots.put(metric, plot);
      grid.add(viewer, i % 2, i / 2);
    }
    return grid;
  }

  private @NotNull TableView<StepResourcePrediction> createTable() {
    final TableView<StepResourcePrediction> table = new TableView<>(stepPredictions);
    table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    final NumberFormat gb = new DecimalFormat("0.00");
    table.getColumns().addAll(
        TableColumns.createColumn("Step", 40, NumberFormat.getIntegerInstance(), null,
            p -> new ReadOnlyObjectWrapper<>(p.step())),
        TableColumns.createColumn("Module", 200, p -> new ReadOnlyObjectWrapper<>(p.name())),
        TableColumns.createColumn("Depends on", 70, p -> new ReadOnlyObjectWrapper<>(p.scaling())),
        TableColumns.createColumn("RAM (GB)", 70, gb, null,
            p -> new ReadOnlyObjectWrapper<>(p.heapGB())),
        TableColumns.createColumn("RAM estimate reliability", 160,
            p -> new ReadOnlyObjectWrapper<>(p.heapReliability())),
        TableColumns.createColumn("RAM kept (GB)", 90, gb, null,
            p -> new ReadOnlyObjectWrapper<>(p.retainedHeapGB())),
        TableColumns.createColumn("Temporary storage (GB)", 120, gb, null,
            p -> new ReadOnlyObjectWrapper<>(p.liveTempGB())), TableColumns.createColumn("Time", 70,
            p -> new ReadOnlyObjectWrapper<>(BatchResourceReport.formatDuration(p.seconds()))));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    final MenuItem copy = new MenuItem("Copy selected rows");
    copy.disableProperty().bind(Bindings.isEmpty(table.getSelectionModel().getSelectedItems()));
    copy.setOnAction(_ -> copySelectedRows(table));
    table.setContextMenu(new ContextMenu(copy));
    final KeyCombination copyShortcut = new KeyCodeCombination(KeyCode.C,
        KeyCombination.SHORTCUT_DOWN);
    table.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (copyShortcut.match(event)) {
        copySelectedRows(table);
        event.consume();
      }
    });
    return table;
  }

  private static void copySelectedRows(@NotNull final TableView<StepResourcePrediction> table) {
    if (table.getSelectionModel().getSelectedIndices().isEmpty()) {
      return;
    }
    final var columns = table.getVisibleLeafColumns();
    final StringBuilder text = new StringBuilder(
        columns.stream().map(column -> column.getText()).collect(Collectors.joining("\t")));
    for (final int row : table.getSelectionModel().getSelectedIndices().stream().sorted()
        .toList()) {
      text.append('\n').append(
          columns.stream().map(column -> Objects.toString(column.getCellData(row), ""))
              .collect(Collectors.joining("\t")));
    }
    final ClipboardContent content = new ClipboardContent();
    content.putString(text.toString());
    Clipboard.getSystemClipboard().setContent(content);
  }
}
