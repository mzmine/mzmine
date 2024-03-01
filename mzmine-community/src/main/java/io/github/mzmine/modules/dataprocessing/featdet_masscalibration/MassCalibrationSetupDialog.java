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

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.charts.ErrorDistributionChart;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.charts.ErrorVsMzChart;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.charts.MeasuredVsMatchedMzChart;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.ArrayList;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.apache.commons.text.WordUtils;

/**
 * This class extends ParameterSetupDialog class to include mass calibration plots. This is used to
 * preview how the chosen mass calibration setup will match peaks, estimate bias and calibrate the
 * mass spectra.
 */
public class MassCalibrationSetupDialog extends ParameterSetupDialog {

  private final ErrorDistributionChart errorDistributionChart;
  private final ErrorVsMzChart errorVsMzChart;
  private final MeasuredVsMatchedMzChart measuredVsMatchedMzChart;
  // Dialog components
  private final BorderPane pnlPreviewFields;
  private final FlowPane pnlDataFile;
  private final Pane chartsPane;
  private final CheckBox labelsCheckbox;
  private final VBox pnlControls;
  private final ToggleGroup chartChoice;
  private final RadioButton errorDistributionButton;
  private final RadioButton errorVsMzButton;
  private final RadioButton measuredVsMatchedMzButton;
  private final ComboBox<RawDataFile> comboDataFileName;
  private final CheckBox previewCheckBox;
  private final RawDataFile[] dataFiles;
  private final RawDataFile previewDataFile;

  protected MassCalibrationTask previewTask;
  protected final PauseTransition debounceTime = new PauseTransition(Duration.millis(500));

  protected static final String universalCalibrantsMessage =
      "Universal calibrants list disclaimer: "
          + "If you use universal calibrants matching mode, please cite suitable publication (source of universal "
          + "calibrants list) depending on which list you used. References are available in the help file.";

  public MassCalibrationSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {

    super(valueCheckRequired, parameters, universalCalibrantsMessage);

    dataFiles = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

    RawDataFile[] selectedFiles = MZmineCore.getDesktop().getSelectedDataFiles();

    if (selectedFiles.length > 0) {
      previewDataFile = selectedFiles[0];
    } else if (dataFiles.length > 0) {
      previewDataFile = dataFiles[0];
    } else {
      previewDataFile = null;
    }

    //TODO: Improve handling of ComboBox in case no raw files are loaded
    if (previewDataFile != null) {
      comboDataFileName = new ComboBox<>(FXCollections.observableList(
        MZmineCore.getProjectManager().getCurrentProject().getCurrentRawDataFiles()));
      comboDataFileName.setOnAction(e -> {
        parametersChanged(true);
      });
      comboDataFileName.getSelectionModel().select(previewDataFile);
    } else {
      comboDataFileName = new ComboBox<>();
    }

    previewCheckBox = new CheckBox("Show preview");

    paramsPane.add(new Separator(), 0, getNumberOfParameters() + 1);
    paramsPane.add(previewCheckBox, 0, getNumberOfParameters() + 2);

    // Elements of pnlLab
    pnlDataFile = new FlowPane();
    pnlDataFile.getChildren().add(new Label("Data file "));

    pnlDataFile.getChildren().add(comboDataFileName);

    errorDistributionChart = new ErrorDistributionChart();
    errorVsMzChart = new ErrorVsMzChart();
    measuredVsMatchedMzChart = new MeasuredVsMatchedMzChart();

    chartChoice = new ToggleGroup();
    errorDistributionButton = new RadioButton("Error distribution");
    errorDistributionButton.setToggleGroup(chartChoice);
    errorDistributionButton.setSelected(true);
    errorVsMzButton = new RadioButton("Error size vs mz ratio");
    errorVsMzButton.setToggleGroup(chartChoice);
    measuredVsMatchedMzButton = new RadioButton("Measured vs matched mz");
    measuredVsMatchedMzButton.setToggleGroup(chartChoice);
    // chartChoice.selectedToggleProperty().addListener(e -> loadPreview(false));
    chartChoice.selectedToggleProperty().addListener(e -> chartChoiceChange());

    FlowPane chartChoicePane = new FlowPane();
    chartChoicePane.getChildren().add(errorDistributionButton);
    chartChoicePane.getChildren().add(errorVsMzButton);
    chartChoicePane.getChildren().add(measuredVsMatchedMzButton);
    chartChoicePane.setHgap(5);

    labelsCheckbox = new CheckBox("Labels preview");
    labelsCheckbox.setTooltip(new Tooltip(WordUtils.wrap(
        "When selected, labels such as extraction range"
            + " and bias estimation value markers plus additional trend extraction details are displayed on the charts."
            + " Deselecting can come in handy when the charts get cluttered with overlapping labels.",
        90)));
    labelsCheckbox.setSelected(true);
    labelsCheckbox.setOnAction(event -> loadPreview(false));

    pnlControls = new VBox();
    pnlControls.setSpacing(5);
    BorderPane.setAlignment(pnlControls, Pos.CENTER);

    // Put all together
    pnlControls.getChildren().add(pnlDataFile);
    pnlControls.getChildren().add(chartChoicePane);
    pnlControls.getChildren().add(labelsCheckbox);
    pnlPreviewFields = new BorderPane();
    pnlPreviewFields.setCenter(pnlControls);
    pnlPreviewFields.visibleProperty().bind(previewCheckBox.selectedProperty());

    chartsPane = new StackPane();
    chartsPane.setMinSize(400, 300);
    chartsPane.setPrefSize(800, 600);
    chartsPane.getChildren().add(errorDistributionChart);

    chartsPane.visibleProperty().bind(previewCheckBox.selectedProperty());
    chartsPane.visibleProperty().addListener((c, o, n) -> {
      if (n) {
        mainPane.setCenter(chartsPane);
        mainPane.setLeft(getParamPane());
        mainPane.autosize();
        mainPane.getScene().getWindow().sizeToScene();
        parametersChanged(false);
      } else {
        mainPane.setLeft(null);
        mainPane.setCenter(getParamPane());
        mainPane.autosize();
        mainPane.getScene().getWindow().sizeToScene();
      }
    });

    paramsPane.add(pnlPreviewFields, 0, getNumberOfParameters() + 3, 1, 1);
    this.setOnCloseRequest(event -> cancelRunningPreviewTask());
  }

  @Override
  public void closeDialog(ExitCode exitCode) {
    super.closeDialog(exitCode);
    cancelRunningPreviewTask();
  }

  protected void cancelRunningPreviewTask() {
    if (previewTask != null && (previewTask.getStatus() == TaskStatus.PROCESSING
        || previewTask.getStatus() == TaskStatus.WAITING)) {
      previewTask.cancel();
    }
  }

  protected void loadPreview(boolean rerun) {
    ArrayList<String> errors = new ArrayList<>();
    boolean paramsOK = parameterSet.checkParameterValues(errors);
    if (!paramsOK) {
      return;
    }

    RawDataFile previewDataFile = comboDataFileName.getSelectionModel().getSelectedItem();
    if (previewDataFile == null) {
      return;
    }

    if (rerun || previewTask == null) {
      if (previewTask != null) {
        previewTask.cancel();
      }

      previewTask = new MassCalibrationTask(previewDataFile, parameterSet, null, true,
          Instant.now());

      previewTask.setAfterHook(() -> Platform.runLater(() -> updatePreviewAfterTaskRun(rerun)));
      MZmineCore.getTaskController().addTask(previewTask);
    } else {
      updatePreviewAfterTaskRun(rerun);
    }

  }

  protected void updatePreviewAfterTaskRun(boolean rerun) {
    if (previewTask.getStatus() != TaskStatus.FINISHED) {
      if (previewTask.getErrorMessage() != null) {
        MZmineCore.getDesktop()
            .displayMessage("Mass calibration error message", previewTask.getErrorMessage());
      }
      return;
    }

    if (rerun) {
      errorDistributionChart.cleanDistributionPlot();
      errorDistributionChart.updateDistributionPlot(previewTask.getMassPeakMatches(),
          previewTask.getErrors(), previewTask.getErrorRanges(), previewTask.getBiasEstimate());

      errorVsMzChart.cleanPlot();
      errorVsMzChart.updatePlot(previewTask.getMassPeakMatches(), previewTask.getErrorRanges(),
          previewTask.getBiasEstimate(), previewTask.getErrorVsMzTrend());

      measuredVsMatchedMzChart.cleanPlot();
      measuredVsMatchedMzChart.updatePlot(previewTask.getMassPeakMatches());
    }

    /*
     * if (labelsCheckbox.isSelected() == false) { errorDistributionChart.cleanPlotLabels();
     * errorVsMzChart.cleanPlotLabels(); }
     */
    errorDistributionChart.displayPlotLabels(labelsCheckbox.isSelected());
    errorVsMzChart.displayPlotLabels(labelsCheckbox.isSelected());

  }

  protected void chartChoiceChange() {
    Toggle choice = chartChoice.getSelectedToggle();
    chartsPane.getChildren().clear();
    if (choice == errorVsMzButton) {
      chartsPane.getChildren().add(errorVsMzChart);
    } else if (choice == measuredVsMatchedMzButton) {
      chartsPane.getChildren().add(measuredVsMatchedMzChart);
    } else {
      chartsPane.getChildren().add(errorDistributionChart);
    }
  }

  @Override
  protected void parametersChanged() {
    parametersChanged(true);
  }

  protected void parametersChanged(boolean debounce) {
    if (!previewCheckBox.isSelected()) {
      return;
    }

    updateParameterSetFromComponents();
    if (debounce) {
      debounceTime.setOnFinished(event -> loadPreview(true));
      debounceTime.playFromStart();
    } else {
      loadPreview(true);
    }
  }
}
