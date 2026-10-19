package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.gui.chartbasics.FxChartFactory;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.FxJFreeChart;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.components.factories.FxSplitPanes;
import io.github.mzmine.javafx.components.factories.TableColumns;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
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
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
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
public final class BatchResourceEstimationResultStage extends Stage {

  private final @NotNull ParameterSet parameters;
  private @NotNull EstimationSession session;
  private @NotNull BatchResourceModel model;
  private int files;
  private final ObservableList<StepResourcePrediction> stepPredictions =
      FXCollections.observableArrayList();
  private final VBox summary = FxLayout.newVBox();
  private final EstimationOutcomePane outcomePane = new EstimationOutcomePane(
      this::startLargerRun);
  private boolean largerRunRunning = false;
  private final Map<ResourceMetric, CategoryPlot> plots = new EnumMap<>(ResourceMetric.class);
  private final double maxHeapGB = Runtime.getRuntime().maxMemory() / (double) (1 << 30);
  // read once when the result is shown, never on spinner changes (may spin up a disk)
  private final double tempDirFreeGB = FileAndPathUtil.getTempDir().getUsableSpace() / 1e9;

  private BatchResourceEstimationResultStage(@NotNull final EstimationSession session,
      @NotNull final BatchResourceModel model, @NotNull final ParameterSet parameters) {
    this.session = session;
    this.model = model;
    this.parameters = parameters;
    this.files = model.totalFiles();
    setTitle("Batch resource estimation");
    getIcons().add(FxIconUtil.loadImageFromResources("mzmineIcon.png"));

    final IntegerComponent filesComponent = new IntegerComponent(100, 1, null);
    filesComponent.setText(String.valueOf(model.totalFiles()));
    filesComponent.setToolTipText("""
        Extrapolate to this number of files with the mean file size of the batch (%d files)"""
        .formatted(model.totalFiles()));
    // the value is committed on enter or focus loss
    filesComponent.valueProperty().addListener((_, _, n) -> {
      if (n != null && n > 0) {
        files = n;
        update();
      }
    });
    final VBox top = FxLayout.newVBox(outcomePane, FxLayout.newHBox(Pos.CENTER_LEFT,
        FxLabels.newBoldLabel("Extrapolate to files:"), filesComponent), summary);

    final BorderPane root = new BorderPane();
    root.setPadding(FxLayout.DEFAULT_PADDING_INSETS);
    root.setTop(top);
    root.setCenter(FxSplitPanes.newSplitPane(0.7, Orientation.VERTICAL, createCharts(),
        createTable()));

    final Scene scene = new Scene(root, 1400, 950);
    ConfigService.getConfiguration().getTheme().apply(scene.getStylesheets());
    setScene(scene);
    update();
  }

  /**
   * Opens the result in a new window. The verdict banner offers a further, larger run if needed.
   * Call on the FX thread.
   *
   * @param parameters the parameters of the estimation
   */
  public static void show(@NotNull final EstimationSession session,
      @NotNull final BatchResourceModel model, @NotNull final ParameterSet parameters) {
    new BatchResourceEstimationResultStage(session, model, parameters).show();
  }

  /**
   * Shows the result with an additional run.
   */
  private void setResult(@NotNull final EstimationSession session,
      @NotNull final BatchResourceModel model) {
    this.session = session;
    this.model = model;
    largerRunRunning = false;
    update();
    if (!isShowing()) {
      show();
    }
    toFront();
  }

  /**
   * decision: a further run is added to the current result instead of repeating all subsets. Only
   * the verdict banner offers it, a dialog would repeat the banner.
   */
  private void startLargerRun(final int runFiles) {
    if (largerRunRunning || !BatchResourceEstimationModule.confirmClearProject(
        ProjectService.getProject())) {
      return;
    }
    largerRunRunning = true;
    final BatchResourceExtensionTask task = new BatchResourceExtensionTask(Instant.now(),
        parameters, session, runFiles,
        (extended, extendedModel) -> FxThread.runLater(() -> setResult(extended, extendedModel)));
    // allow a new run after an error or cancel
    task.addTaskStatusListener((_, newStatus, _) -> {
      if (newStatus == TaskStatus.ERROR || newStatus == TaskStatus.CANCELED) {
        FxThread.runLater(() -> largerRunRunning = false);
      }
    });
    MZmineCore.getTaskController().addTask(task);
  }

  private void update() {
    final BatchResourcePrediction custom = model.predict(files);
    outcomePane.setOutcome(EstimationOutcome.of(model, custom, maxHeapGB, tempDirFreeGB));
    summary.getChildren().setAll(
        BatchResourceReport.summaryLines(model, custom, maxHeapGB, tempDirFreeGB)
            .stream()
            .map(Label::new).toList());
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
          "%d files (validation, extrapolated)".formatted(validation.predicted().files()));
    }
    for (final BatchResourcePrediction extension : model.extensions()) {
      series.add(extension);
      seriesLabels.add("%d files (additional run, measured)".formatted(extension.files()));
    }
    series.add(custom);
    seriesLabels.add("%d files (extrapolated)".formatted(files));

    plots.forEach((metric, plot) -> {
      final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
      for (int i = 0; i < series.size(); i++) {
        for (final StepResourcePrediction step : series.get(i).steps()) {
          dataset.addValue(metric.value(step), seriesLabels.get(i),
              "%d %s".formatted(step.step(), step.name()));
        }
      }
      plot.setDataset(dataset);
    });
  }

  private @NotNull GridPane createCharts() {
    final GridPane grid = new GridPane();
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

      final CategoryPlot plot = chart.getCategoryPlot();
      plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
      final BarRenderer renderer = (BarRenderer) plot.getRenderer();
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
    final NumberFormat gb = new DecimalFormat("0.00");
    table.getColumns().addAll(
        TableColumns.createColumn("Step", 40, NumberFormat.getIntegerInstance(), null,
            p -> new ReadOnlyObjectWrapper<>(p.step())),
        TableColumns.createColumn("Module", 200, p -> new ReadOnlyObjectWrapper<>(p.name())),
        TableColumns.createColumn("Scales with", 70,
            p -> new ReadOnlyObjectWrapper<>(p.scaling())),
        TableColumns.createColumn("Heap (GB)", 70, gb, null,
            p -> new ReadOnlyObjectWrapper<>(p.heapGB())),
        TableColumns.createColumn("Retained heap (GB)", 70, gb, null,
            p -> new ReadOnlyObjectWrapper<>(p.retainedHeapGB())),
        TableColumns.createColumn("Temp files (GB)", 70, gb, null,
            p -> new ReadOnlyObjectWrapper<>(p.liveTempGB())),
        TableColumns.createColumn("Time", 70,
            p -> new ReadOnlyObjectWrapper<>(BatchResourceReport.formatDuration(p.seconds()))));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    return table;
  }
}
