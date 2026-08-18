/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.rawdataoverview;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.graphicsexport.GraphicsExportModule;
import io.github.mzmine.gui.chartbasics.graphicsexport.GraphicsExportParameters;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchModule;
import io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters;
import io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchTask;
import io.github.mzmine.modules.dataprocessing.id_nist.NistMatchUtils;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogramandspectra.ChromatogramAndSpectraVisualizer;
import io.github.mzmine.project.impl.ImagingRawDataFileImpl;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import java.io.IOException;
import java.awt.BasicStroke;
import java.awt.Font;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.control.SplitPane;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.input.ContextMenuEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ui.TextAnchor;

/*
 * Raw data overview window controller class
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class RawDataOverviewWindowController {

  public static final Logger logger = Logger.getLogger(
      RawDataOverviewWindowController.class.getName());

  private boolean initialized = false;

  private final ObservableMap<RawDataFile, RawDataFileInfoPaneController> rawDataFilesAndControllers = FXCollections.observableMap(
      new HashMap<>());
  private final ObservableMap<RawDataFile, Tab> rawDataFilesAndTabs = FXCollections.observableMap(
      new HashMap<>());
  private final List<RenderedNistMatch> renderedNistMatches = new ArrayList<>();
  private boolean showNistMatchLabels = false;
  private int minimumLabelFmf = 0;
  private int minimumLabelRmf = 0;
  private int nistLabelFontSize = 13;
  private Label nistLabelFilterStatus;
  private CheckMenuItem showNistLabelsMenuItem;
  private CheckBox showNistLabelsCheckBox;
  private RawDataFile nistLabelRawFile;
  private RawDataFile selectedChromatogramRawFile;
  private double selectedChromatogramRt = Double.NaN;
  private double contextMenuRetentionTime = Double.NaN;


  @FXML
  private ChromatogramAndSpectraVisualizer visualizer;

  @FXML
  private TabPane tpRawDataInfo;

  @FXML
  private BorderPane pnMaster;

  @FXML
  private SplitPane pnMain;

  public void initialize() {

    // Selecting a scan already updates the spectrum below. The automatic filled EIC obscures the
    // raw chromatogram in this overview and can be mistaken for converted source data.
    visualizer.setAutomaticEicOverlayEnabled(false);
    installNistMatchLabelToggle();
    installChromatogramQuickControls();
    installExplicitNistSearch();
    addChromatogramSelectedScanListener();

    initialized = true;
  }

  private void installExplicitNistSearch() {
    visualizer.getChromPlot().addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
      final Point2D local = visualizer.getChromPlot().sceneToLocal(event.getSceneX(),
          event.getSceneY());
      final java.awt.geom.Point2D plotPoint = ChartLogicsFX.mouseXYToPlotXY(
          visualizer.getChromPlot(),
          local.getX(), local.getY());
      contextMenuRetentionTime = plotPoint == null ? Double.NaN : plotPoint.getX();
      NistChartLabelState.register(this);
    });

    final MenuItem explicitSearch = new MenuItem("Run NIST search at clicked peak apex");
    explicitSearch.setOnAction(event -> runNistSearchAtContextRetentionTime());
    visualizer.getChromPlot().getContextMenu().getItems().add(explicitSearch);
  }

  private void runNistSearchAtContextRetentionTime() {
    final RawDataFile rawDataFile = selectedChromatogramRawFile != null
        ? selectedChromatogramRawFile : visualizer.getSelectedRawDataFile();
    // A context-menu search is defined by the mouse click, not by a potentially stale dashed
    // cursor. Snap that click to the highest chromatogram point within the local peak window.
    final double initialRetentionTime = Double.isFinite(contextMenuRetentionTime)
        ? contextMenuRetentionTime : selectedChromatogramRt;
    final ChromatogramPoint clickedPeak = rawDataFile == null
        || !Double.isFinite(initialRetentionTime) ? null
        : findChromatogramPeak(rawDataFile, initialRetentionTime);
    final double requestedRetentionTime = clickedPeak == null ? initialRetentionTime
        : clickedPeak.retentionTime();
    logger.info(() -> "Explicit NIST request: file=%s, selected RT=%s, clicked RT=%s"
        .formatted(rawDataFile == null ? "none" : rawDataFile.getName(), selectedChromatogramRt,
            contextMenuRetentionTime));
    if (rawDataFile == null || !Double.isFinite(requestedRetentionTime)) {
      logger.warning("Explicit NIST request rejected because no raw-data scan/RT is selected");
      MZmineCore.getDesktop().displayErrorMessage(
          "Select a scan in the raw-data chromatogram, then right-click and run the NIST search.");
      return;
    }
    logger.info(() -> "Explicit NIST click snapped to local peak apex: clicked RT=%.3f, apex RT=%.3f"
        .formatted(initialRetentionTime, requestedRetentionTime));

    final Scan scan = rawDataFile.binarySearchClosestScan((float) requestedRetentionTime, 1);
    final NistRowTarget target = findClosestFeatureRow(rawDataFile, requestedRetentionTime);
    if (scan == null || target == null || target.distanceMinutes() > 0.15d) {
      logger.warning(() -> "Explicit NIST request rejected at RT %.3f: closest feature distance=%s"
          .formatted(requestedRetentionTime,
              target == null ? "none" : "%.4f min".formatted(target.distanceMinutes())));
      MZmineCore.getDesktop().displayErrorMessage(
          "No processed feature row was found within 0.15 min of RT %.3f. The explicit search needs a feature row to store its NIST candidates."
              .formatted(requestedRetentionTime));
      return;
    }

    final ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(NistMsSearchModule.class).cloneParameterSet();
    final List<String> errors = new ArrayList<>();
    // The row and its list are already resolved above; only validate settings needed to execute
    // this one search. Generic batch validation must not demand another feature-list selection.
    if (!((NistMsSearchParameters) parameters).checkParameterValuesForExplicitSearch(errors)) {
      MZmineCore.getDesktop().displayErrorMessage(
          "NIST search settings are invalid:\n" + String.join("\n", errors));
      return;
    }

    final NistMsSearchTask task = new NistMsSearchTask(target.row(), target.featureList(),
        parameters, Instant.now(), scan);
    logger.info(() -> "Starting explicit NIST search: file=%s, scan=%d, RT=%.3f, feature list=%s, minimum FMF=%d"
        .formatted(rawDataFile.getName(), scan.getScanNumber(), scan.getRetentionTime(),
            target.featureList().getName(), task.getMinimumMatchFactor()));
    MZmineCore.getDesktop().setStatusBarText(
        "Queued explicit NIST search at RT %.3f min (minimum FMF %d)"
            .formatted(scan.getRetentionTime(), task.getMinimumMatchFactor()));
    task.addTaskStatusListener((changedTask, newStatus, oldStatus) -> {
      if (newStatus == TaskStatus.FINISHED) {
        javafx.application.Platform.runLater(() -> {
          refreshNistMatchLabels(rawDataFile, true);
          MZmineGUI.setNistMatchSelection(rawDataFile, scan.getRetentionTime());
          MZmineGUI.refreshNistMatches();
          final String result = task.getAddedHitCount() == 0
              ? "No candidates passed the minimum FMF of %d at RT %.3f min."
                  .formatted(task.getMinimumMatchFactor(), scan.getRetentionTime())
              : "Stored %d candidates at RT %.3f min. Check the NIST matches dropdown."
                  .formatted(task.getAddedHitCount(), scan.getRetentionTime());
          MZmineCore.getDesktop().displayMessage("NIST search complete", result);
        });
      } else if (newStatus == TaskStatus.ERROR) {
        javafx.application.Platform.runLater(() -> MZmineCore.getDesktop().displayErrorMessage(
            "NIST search failed: " + changedTask.getErrorMessage()));
      }
    });
    final Thread explicitSearchThread = new Thread(task,
        "Explicit NIST search %.3f".formatted(scan.getRetentionTime()));
    explicitSearchThread.setDaemon(true);
    explicitSearchThread.start();
  }

  private NistRowTarget findClosestFeatureRow(RawDataFile rawDataFile, double retentionTime) {
    NistRowTarget closest = null;
    for (FeatureList featureList : ProjectService.getProjectManager().getCurrentProject()
        .getCurrentFeatureLists()) {
      for (FeatureListRow row : featureList.getRows()) {
        final var feature = row.getFeature(rawDataFile);
        if (feature == null || feature.getRT() == null) {
          continue;
        }
        final double distance = Math.abs(feature.getRT() - retentionTime);
        if (closest == null || distance < closest.distanceMinutes()) {
          closest = new NistRowTarget(featureList, row, distance);
        }
      }
    }
    return closest;
  }

  private void installNistMatchLabelToggle() {
    showNistLabelsMenuItem = new CheckMenuItem("Show NIST match labels");
    showNistLabelsMenuItem.setSelected(false);
    showNistLabelsMenuItem.setOnAction(
        event -> setShowNistMatchLabels(showNistLabelsMenuItem.isSelected()));
    visualizer.getChromPlot().getContextMenu().getItems().add(new SeparatorMenuItem());
    visualizer.getChromPlot().getContextMenu().getItems().add(showNistLabelsMenuItem);
  }

  private void setShowNistMatchLabels(boolean visible) {
      NistChartLabelState.register(this);
      showNistMatchLabels = visible;
      if (showNistLabelsMenuItem != null && showNistLabelsMenuItem.isSelected() != visible) {
        showNistLabelsMenuItem.setSelected(visible);
      }
      if (showNistLabelsCheckBox != null && showNistLabelsCheckBox.isSelected() != visible) {
        showNistLabelsCheckBox.setSelected(visible);
      }
      final var currentPosition = visualizer.getChromPosition();
      if (currentPosition != null && currentPosition.getDataFile() != null) {
        selectedChromatogramRawFile = currentPosition.getDataFile();
        selectedChromatogramRt = currentPosition.getRetentionTime();
        nistLabelRawFile = selectedChromatogramRawFile;
        MZmineGUI.setNistMatchSelection(selectedChromatogramRawFile, selectedChromatogramRt);
      } else if (nistLabelRawFile == null) {
        // Raw Data Overview may already show a file before the user has clicked a scan. Resolve
        // that visible file now so enabling labels does not depend on a later selection event.
        nistLabelRawFile = selectedChromatogramRawFile != null ? selectedChromatogramRawFile
            : visualizer.getRawDataFiles().stream().findFirst().orElse(null);
      }
      if (visualizer.getChromPlot().getXYPlot().getRangeAxis() instanceof NumberAxis axis) {
        axis.setUpperMargin(showNistMatchLabels ? 0.40d : 0.05d);
        axis.setAutoRange(true);
      }
      refreshNistMatchLabels(nistLabelRawFile, true);
      highlightNistMatch(selectedChromatogramRt);
      // Changing the axis margin schedules a JavaFX chart redraw. Re-apply annotations on the
      // following pulse and draw the canvas explicitly so the user does not need to click the
      // chart before newly enabled labels become visible.
      Platform.runLater(() -> {
        refreshNistMatchLabels(nistLabelRawFile, true);
        highlightNistMatch(selectedChromatogramRt);
        visualizer.getChromPlot().getCanvas().draw();
        visualizer.getChromPlot().requestLayout();
      });
      // The accordion and newly visible label headroom can complete layout on the next pulse.
      // Repaint once more after that layout instead of waiting for a chart click to trigger it.
      final PauseTransition repaintAfterLayout = new PauseTransition(Duration.millis(75));
      repaintAfterLayout.setOnFinished(event -> {
        refreshNistMatchLabels(nistLabelRawFile, true);
        visualizer.getChromPlot().getChart().fireChartChanged();
        visualizer.getChromPlot().getCanvas().draw();
      });
      repaintAfterLayout.play();
  }

  private void installChromatogramQuickControls() {
    final ParameterSet nistParameters = MZmineCore.getConfiguration()
        .getModuleParameters(NistMsSearchModule.class);
    minimumLabelFmf = nistParameters.getValue(NistMsSearchParameters.MIN_MATCH_FACTOR);
    minimumLabelRmf = minimumLabelFmf;

    showNistLabelsCheckBox = new CheckBox("NIST labels");
    showNistLabelsCheckBox.setTooltip(new Tooltip("Show the best stored NIST match at each RT."));
    showNistLabelsCheckBox.setOnAction(
        event -> setShowNistMatchLabels(showNistLabelsCheckBox.isSelected()));

    final ToggleButton orientation = new ToggleButton("NIST: Vertical");
    orientation.setTooltip(
        new Tooltip("Switch the default orientation of NIST labels on this chart."));
    orientation.setSelected(NistChartLabelState.isDefaultHorizontal());
    orientation.setOnAction(event -> {
      final boolean horizontal = orientation.isSelected();
      orientation.setText(horizontal ? "NIST: Horizontal" : "NIST: Vertical");
      // Repaints every open Raw Data Overview window, not just this one.
      NistChartLabelState.setDefaultHorizontal(horizontal);
    });

    final Spinner<Integer> fmf = new Spinner<>(0, 999, minimumLabelFmf, 25);
    fmf.setEditable(true);
    fmf.setPrefWidth(78);
    fmf.setTooltip(new Tooltip(
        "Minimum forward match factor shown on this chart. Type a value and press Enter."));
    installSpinnerCommit(fmf);
    fmf.valueProperty().addListener((obs, old, value) -> {
      minimumLabelFmf = value;
      refreshNistMatchLabels(nistLabelRawFile, true);
    });

    final Spinner<Integer> rmf = new Spinner<>(0, 999, minimumLabelRmf, 25);
    rmf.setEditable(true);
    rmf.setPrefWidth(78);
    rmf.setTooltip(new Tooltip(
        "Minimum reverse match factor shown on this chart. Type a value and press Enter."));
    installSpinnerCommit(rmf);
    rmf.valueProperty().addListener((obs, old, value) -> {
      minimumLabelRmf = value;
      refreshNistMatchLabels(nistLabelRawFile, true);
    });

    final ChoiceBox<TICPlotType> display = new ChoiceBox<>(
        FXCollections.observableArrayList(TICPlotType.values()));
    display.setValue(visualizer.getPlotType());
    display.setTooltip(new Tooltip("Choose base-peak or total-ion chromatogram. XIC stays on the right."));
    display.valueProperty().addListener((obs, old, value) -> {
      if (value != null) {
        visualizer.setPlotType(value);
      }
    });

    final ColorPicker lineColor = new ColorPicker(javafx.scene.paint.Color.DODGERBLUE);
    lineColor.setPrefWidth(48);
    lineColor.setTooltip(new Tooltip("Set the visible chromatogram line color."));

    final Spinner<Double> lineWidth = new Spinner<>(0.5d, 8d, 1d, 0.5d);
    lineWidth.setEditable(true);
    lineWidth.setPrefWidth(72);
    lineWidth.setTooltip(new Tooltip("Chromatogram line width."));

    final Spinner<Integer> labelSize = new Spinner<>(8, 30, nistLabelFontSize, 1);
    labelSize.setEditable(true);
    labelSize.setPrefWidth(68);
    labelSize.setTooltip(new Tooltip("Font size for NIST names and numeric apex labels."));

    final Runnable applyChartStyle = () -> {
      final javafx.scene.paint.Color fx = lineColor.getValue();
      final java.awt.Color awt = new java.awt.Color((float) fx.getRed(), (float) fx.getGreen(),
          (float) fx.getBlue(), (float) fx.getOpacity());
      visualizer.getChromPlot().setTicLineStyle(awt, lineWidth.getValue());
      nistLabelFontSize = labelSize.getValue();
      visualizer.getChromPlot().setTicItemLabelFontSize(nistLabelFontSize);
      refreshNistMatchLabels(nistLabelRawFile, true);
    };
    lineColor.valueProperty().addListener((obs, old, value) -> applyChartStyle.run());
    lineWidth.valueProperty().addListener((obs, old, value) -> applyChartStyle.run());
    labelSize.valueProperty().addListener((obs, old, value) -> applyChartStyle.run());

    final Button reset = new Button("Fit");
    reset.setTooltip(new Tooltip("Reset zoom and fit the complete chromatogram from zero."));
    reset.setOnAction(event -> visualizer.getChromPlot().resetZoomToData());

    final Button export = new Button("Export");
    export.setTooltip(new Tooltip("Open graphics export for this chromatogram."));
    export.setOnAction(event -> {
      final GraphicsExportParameters params = (GraphicsExportParameters) MZmineCore
          .getConfiguration().getModuleParameters(GraphicsExportModule.class);
      MZmineCore.getModuleInstance(GraphicsExportModule.class)
          .openDialog(visualizer.getChromPlot().getChart(), params);
    });

    nistLabelFilterStatus = new Label("NIST: off");
    nistLabelFilterStatus.setTooltip(
        new Tooltip("Number of stored NIST peak labels passing the FMF/RMF filters."));

    final FlowPane controls = new FlowPane(8, 5, showNistLabelsCheckBox, orientation,
        new Label("FMF"), fmf, new Label("RMF"), rmf, new Label("View"), display,
        new Label("Line"), lineColor, new Label("Width"), lineWidth, new Label("Labels"),
        labelSize, nistLabelFilterStatus, reset, export);
    controls.setAlignment(Pos.CENTER_LEFT);
    controls.setPadding(new Insets(4, 0, 4, 6));
    visualizer.setChromatogramToolbarLeft(controls);
  }

  private static void installSpinnerCommit(Spinner<Integer> spinner) {
    final Runnable commit = () -> {
      try {
        final int parsed = Integer.parseInt(spinner.getEditor().getText().trim());
        spinner.getValueFactory().setValue(Math.max(0, Math.min(999, parsed)));
      } catch (NumberFormatException ignored) {
        spinner.getEditor().setText(String.valueOf(spinner.getValue()));
      }
    };
    spinner.getEditor().setOnAction(event -> commit.run());
    spinner.getEditor().focusedProperty().addListener((obs, wasFocused, focused) -> {
      if (!focused) {
        commit.run();
      }
    });
  }

  private void refreshNistMatchLabels(RawDataFile rawDataFile, boolean force) {
    if (!force && rawDataFile == nistLabelRawFile) {
      return;
    }
    nistLabelRawFile = rawDataFile;
    final var plot = visualizer.getChromPlot().getXYPlot();
    final boolean notify = plot.isNotify();
    plot.setNotify(false);
    renderedNistMatches.forEach(rendered -> plot.removeAnnotation(rendered.annotation(), false));
    renderedNistMatches.clear();
    int visibleLabelCount = 0;

    if (showNistMatchLabels && rawDataFile != null) {
      final List<LabeledNistPeak> labeledPeaks = NistMatchUtils.findBestMatches(rawDataFile).stream()
          .map(result -> {
            final NistMatchUtils.NistMatch selected = getSelectedNistChartMatch(rawDataFile,
                result.retentionTime());
            return selected == null ? result : selected;
          })
          .filter(result -> result.forwardMatchFactor() >= minimumLabelFmf
              && result.reverseMatchFactor() >= minimumLabelRmf)
          .filter(result -> isNistMatchLabelVisible(rawDataFile, result.retentionTime()))
          .map(result -> new LabeledNistPeak(result,
              findChromatogramPeak(rawDataFile, result.retentionTime()))).toList();
      visibleLabelCount = labeledPeaks.size();
      // Lift each name clear of the trace so it does not sit on top of MZmine's numeric apex label.
      final double labelOffset = plot.getRangeAxis().getRange().getLength() * 0.035d;

      for (LabeledNistPeak labeled : labeledPeaks) {
        final var result = labeled.result();
        final ChromatogramPoint peak = labeled.peak();
        final boolean horizontal = isNistMatchLabelHorizontal(rawDataFile,
            result.retentionTime());
        final XYTextAnnotation annotation = new XYTextAnnotation(result.compoundName(),
            peak.retentionTime(), peak.intensity() + labelOffset);
        annotation.setRotationAngle(horizontal ? 0d : -Math.PI / 2d);
        // A vertical label grows upward from its anchor instead of straddling the plot line.
        final TextAnchor anchor = horizontal ? TextAnchor.BOTTOM_CENTER : TextAnchor.CENTER_LEFT;
        annotation.setTextAnchor(anchor);
        annotation.setRotationAnchor(anchor);
        annotation.setPaint(rawDataFile.getColorAWT());
        annotation.setFont(new Font(Font.SANS_SERIF, Font.BOLD, nistLabelFontSize));
        plot.addAnnotation(annotation, false);
        renderedNistMatches.add(new RenderedNistMatch(result.retentionTime(), annotation));
      }
    }
    if (nistLabelFilterStatus != null) {
      nistLabelFilterStatus.setText(showNistMatchLabels ? "NIST: " + visibleLabelCount : "NIST: off");
    }
    plot.setNotify(notify);
    if (notify) {
      visualizer.getChromPlot().getChart().fireChartChanged();
    }
  }

  private ChromatogramPoint findChromatogramPeak(RawDataFile rawDataFile, double retentionTime) {
    final var plot = visualizer.getChromPlot().getXYPlot();
    double nearestDistance = Double.POSITIVE_INFINITY;
    double nearestRt = retentionTime;
    double nearestIntensity = plot.getRangeAxis().getRange().getUpperBound() * 0.1d;
    for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {
      if (!(plot.getDataset(datasetIndex) instanceof TICDataSet dataset)
          || !rawDataFile.equals(dataset.getDataFile())) {
        continue;
      }
      for (int item = 0; item < dataset.getItemCount(0); item++) {
        final double itemRt = dataset.getXValue(0, item);
        final double itemIntensity = dataset.getYValue(0, item);
        final double distance = Math.abs(itemRt - retentionTime);
        if (distance < nearestDistance) {
          nearestDistance = distance;
          nearestRt = itemRt;
          nearestIntensity = itemIntensity;
        }
      }
    }
    // The NIST result already carries the sample-specific feature RT. Do not search for the
    // tallest point in a broad surrounding window: that can cross a valley and move two nearby
    // compounds onto the same peak.
    return new ChromatogramPoint(nearestRt, nearestIntensity);
  }

  private void highlightNistMatch(double retentionTime) {
    if (!showNistMatchLabels || !Double.isFinite(retentionTime)) {
      return;
    }
    RenderedNistMatch closest = renderedNistMatches.stream().min(java.util.Comparator.comparingDouble(
        rendered -> Math.abs(rendered.retentionTime() - retentionTime))).orElse(null);
    if (closest != null && Math.abs(closest.retentionTime() - retentionTime) > 0.15d) {
      closest = null;
    }
    for (RenderedNistMatch rendered : renderedNistMatches) {
      final boolean selected = rendered == closest;
      rendered.annotation().setFont(
          new Font(Font.SANS_SERIF, Font.BOLD,
              selected ? nistLabelFontSize + 3 : nistLabelFontSize));
      rendered.annotation().setBackgroundPaint(
          selected ? new java.awt.Color(255, 245, 160, 220) : null);
      rendered.annotation().setOutlineVisible(selected);
      rendered.annotation().setOutlinePaint(selected ? java.awt.Color.DARK_GRAY : null);
      rendered.annotation().setOutlineStroke(new BasicStroke(1f));
    }
    visualizer.getChromPlot().getChart().fireChartChanged();
  }

  /**
   * Repaints this window's labels if it currently shows {@code rawDataFile}. Called by
   * {@link NistChartLabelState} after any shared-state change, so every open Raw Data Overview
   * window stays in sync rather than only the one that happened to register last.
   *
   * @param rawDataFile the file whose state changed, or {@code null} for a global change
   */
  void repaintNistLabelsFor(@Nullable RawDataFile rawDataFile) {
    if (nistLabelRawFile == null || (rawDataFile != null && !rawDataFile.equals(nistLabelRawFile))) {
      return;
    }
    refreshNistMatchLabels(nistLabelRawFile, true);
    highlightNistMatch(selectedChromatogramRt);
  }

  public boolean isNistMatchLabelVisible(RawDataFile rawDataFile, double retentionTime) {
    return NistChartLabelState.isLabelVisible(rawDataFile, retentionTime);
  }

  public void setNistMatchLabelVisible(RawDataFile rawDataFile, double retentionTime,
      boolean visible) {
    NistChartLabelState.setLabelVisible(rawDataFile, retentionTime, visible);
  }

  public boolean isNistMatchLabelHorizontal(RawDataFile rawDataFile, double retentionTime) {
    return NistChartLabelState.isLabelHorizontal(rawDataFile, retentionTime);
  }

  public void setNistMatchLabelHorizontal(RawDataFile rawDataFile, double retentionTime,
      boolean horizontal) {
    NistChartLabelState.setLabelHorizontal(rawDataFile, retentionTime, horizontal);
  }

  public NistMatchUtils.NistMatch getSelectedNistChartMatch(RawDataFile rawDataFile,
      double retentionTime) {
    return NistChartLabelState.getSelectedMatch(rawDataFile, retentionTime);
  }

  public void setSelectedNistChartMatch(RawDataFile rawDataFile, double retentionTime,
      NistMatchUtils.NistMatch match) {
    NistChartLabelState.setSelectedMatch(rawDataFile, retentionTime, match);
  }

  private record ChromatogramPoint(double retentionTime, double intensity) {
  }

  private record LabeledNistPeak(NistMatchUtils.NistMatch result, ChromatogramPoint peak) {
  }

  private record RenderedNistMatch(double retentionTime, XYTextAnnotation annotation) {
  }

  private record NistRowTarget(FeatureList featureList, FeatureListRow row,
                               double distanceMinutes) {
  }

  /**
   * Sets the raw data files to be displayed. Already present files are not removed to optimise
   * performance. This should be called over
   * {@link RawDataOverviewWindowController#addRawDataFileTab} if possible.
   * <p>
   * Only add LC-MS data sets, exclude imaging
   *
   * @param rawDataFiles
   */
  public void setRawDataFiles(Collection<RawDataFile> rawDataFiles) {
    if(rawDataFiles.size()>25) {
      boolean result = DialogLoggerUtil.showDialogYesNo("Raw data overview",
          "Visualizing %d data files at once might slow down MZmine, continue?".formatted(
              rawDataFiles.size()));

      if(!result) {
        // just visualize the first file if user selected false
        rawDataFiles = rawDataFiles.stream().findFirst().map(List::of).orElse(List.of());
      }
    }

    // remove files first
    List<RawDataFile> filesToProcess = new ArrayList<>();
    for (RawDataFile rawDataFile : rawDataFilesAndTabs.keySet()) {
      if (!rawDataFiles.contains(rawDataFile)) {
        filesToProcess.add(rawDataFile);
      }
    }
    filesToProcess.forEach(this::removeRawDataFile);

    // presence of file is checked in the add method
    rawDataFiles.forEach(r -> {
      if (!(r instanceof ImagingRawDataFileImpl)) {
        addRawDataFileTab(r);
      }
    });
    visualizer.setRawDataFiles(rawDataFiles);
  }

  /**
   * Adds a raw data file table to the tab.
   *
   * @param raw The raw dataFile
   */
  public void addRawDataFileTab(RawDataFile raw) {

    if (!initialized) {
      initialize();
    }
    if (rawDataFilesAndControllers.containsKey(raw)) {
      return;
    }

    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("RawDataFileInfoPane.fxml"));
      BorderPane pane = loader.load();
      rawDataFilesAndControllers.put(raw, loader.getController());
      RawDataFileInfoPaneController con = rawDataFilesAndControllers.get(raw);
      con.getRawDataTableView().getSelectionModel().selectedItemProperty()
          // TODO: this clears the spectrum plot, somehow bind to mouse input, currenty it is just
          // slower than the thread
          .addListener(((obs, old, newValue) -> {
            if (newValue == null) {
              // this is the case it the table was not populated before.
              // in that case we just select the table.
              return;
            }
            visualizer.setFocusedScan(raw, newValue);
          }));

      Tab rawDataFileTab = new Tab(raw.getName());
      rawDataFileTab.setContent(pane);
      tpRawDataInfo.getTabs().add(rawDataFileTab);

      rawDataFileTab.selectedProperty().addListener((obs, o, n) -> {
        if (n) {
          con.populate(raw);
        }
      });

      rawDataFileTab.setOnClosed((e) -> {
        logger.fine("Removing raw data file " + raw.getName());
        removeRawDataFile(raw);
      });

      if (rawDataFileTab.selectedProperty().getValue()) {
        con.populate(raw);
      }

      rawDataFilesAndTabs.put(raw, rawDataFileTab);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not load RawDataFileInfoPane.fxml", e);
    }

    logger.fine("Added raw data file tab for " + raw.getName());
  }

  public void removeRawDataFile(RawDataFile raw) {
    visualizer.removeRawDataFile(raw);
    rawDataFilesAndControllers.remove(raw);
    Tab tab = rawDataFilesAndTabs.remove(raw);
    tpRawDataInfo.getTabs().remove(tab);
  }

  // plot update methods

  /**
   * Updates the selected row in the raw data table if the user clicks in the chromatogram plot.
   */
  private void addChromatogramSelectedScanListener() {

    visualizer.chromPositionProperty().addListener((observable, oldValue, pos) -> {
      NistChartLabelState.register(this);
      RawDataFile selectedRawDataFile = pos.getDataFile();
      if (selectedRawDataFile == null || selectedRawDataFile instanceof ImagingRawDataFileImpl) {
        return;
      }
      selectedChromatogramRt = pos.getRetentionTime();
      selectedChromatogramRawFile = selectedRawDataFile;
      MZmineGUI.setNistMatchSelection(selectedRawDataFile, selectedChromatogramRt);
      refreshNistMatchLabels(selectedRawDataFile, false);
      highlightNistMatch(selectedChromatogramRt);
      RawDataFileInfoPaneController con = rawDataFilesAndControllers.get(selectedRawDataFile);
      if (con == null) {
        logger.info("Cannot find controller for raw data file " + selectedRawDataFile.getName());
        return;
      }

      TableView<Scan> rawDataTableView = con.getRawDataTableView();
      tpRawDataInfo.getSelectionModel().select(rawDataFilesAndTabs.get(selectedRawDataFile));

      if (rawDataTableView.getItems() != null) {
        try {
          Scan scan = pos.getScan();
          rawDataTableView.getItems().stream().filter(item -> item.equals(scan)).findFirst()
              .ifPresent(item -> {
                rawDataTableView.getSelectionModel().select(item);
                rawDataTableView.getSelectionModel().focus(rawDataTableView.getItems().indexOf(item));
                if (!con.getVisibleRange().contains(rawDataTableView.getItems().indexOf(item))) {
                  rawDataTableView.scrollTo(item);
                }
              });
        } catch (Exception e) {
          e.getStackTrace();
        }
      }
    });

  }

  public RawDataFile getSelectedRawDataFile() {
    return visualizer.getSelectedRawDataFile();
  }

  @NotNull
  public Collection<RawDataFile> getRawDataFiles() {
    return visualizer.getRawDataFiles();
  }

}
