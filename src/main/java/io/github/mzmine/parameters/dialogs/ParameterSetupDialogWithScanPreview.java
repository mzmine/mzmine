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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.scans.ScanUtils;
import java.text.NumberFormat;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * This class extends ParameterSetupDialog class, including a SpectraPlot. This is used to preview
 * how the selected mass detector and his parameters works over the raw data file.
 */
public abstract class ParameterSetupDialogWithScanPreview extends ParameterSetupDialogWithPreview {

  // Dialog components
  protected HBox pnlDataFile, pnlScanArrows, pnlScanNumber;
  protected VBox pnlControls;
  protected ComboBox<RawDataFile> comboDataFileName;
  protected ComboBox<Scan> comboScan;

  protected ComboBox<MobilityScan> mobilityScanComboBox = new ComboBox<>();
  protected HBox pnMobilityScans = new HBox();

  protected ObjectProperty<Scan> selectedScan = new SimpleObjectProperty<>();
  protected ObjectProperty<Scan> selectedMobilityScan = new SimpleObjectProperty<>();
  protected ObjectProperty<Scan> lastChangedScan = new SimpleObjectProperty<>();

  // XYPlot
  protected SpectraPlot spectrumPlot;

  /**
   */
  public ParameterSetupDialogWithScanPreview(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    RawDataFile[] dataFiles = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

    // if no data files, return the dialog without preview functions
    if (dataFiles.length == 0) {
      return;
    }
    //      this.hide();
    //      MZmineCore.getDesktop()
    //          .displayMessage("Please load a raw data file before selecting a " + "mass detector.");
    //      throw new UnsupportedOperationException(
    //          "Please load a raw data file before selecting a mass detector.");
    //    }

    final RawDataFile[] selectedFiles = MZmineCore.getDesktop().getSelectedDataFiles();
    final RawDataFile previewDataFile;

    if (selectedFiles.length > 0) {
      previewDataFile = selectedFiles[0];
    } else {
      previewDataFile = dataFiles[0];
    }

    // Elements of pnlLab
    pnlDataFile = new HBox();
    pnlDataFile.getChildren().add(new Label("Data file "));

    pnlScanNumber = new HBox();
    pnlScanNumber.getChildren().add(new Label("Scan number "));
    pnlScanNumber.setAlignment(Pos.TOP_CENTER);

    selectedScan.addListener(((observable, oldValue, newValue) -> {
      if (newValue == null) {
        mobilityScanComboBox.setValue(null);
        return;
      }
      int selected = mobilityScanComboBox.getSelectionModel().getSelectedIndex();
      pnMobilityScans.setDisable(!(newValue instanceof Frame));
      if (newValue instanceof Frame frame) {
        mobilityScanComboBox.setItems(FXCollections.observableArrayList(frame.getMobilityScans()));
        if (selected > 0 && selected < mobilityScanComboBox.getItems().size()) {
          mobilityScanComboBox.getSelectionModel().select(selected);
        }
      } else {
        mobilityScanComboBox.getItems().clear();
        mobilityScanComboBox.setValue(null);
      }
    }));
    initMobilityScanControlPanel();

    ObservableList<Scan> scanNumbers = previewDataFile.getScans();
    comboScan = new ComboBox<>(scanNumbers);
    comboScan.getSelectionModel().selectedItemProperty()
        .addListener((obs, old, newIndex) -> selectedScan.set(newIndex));
    comboScan.getSelectionModel().select(0);

    comboDataFileName = new ComboBox<>(FXCollections.observableList(
        MZmineCore.getProjectManager().getCurrentProject().getCurrentRawDataFiles()));
    comboDataFileName.getSelectionModel().select(previewDataFile);
    comboDataFileName.setOnAction(e -> {
      var newDataFile = comboDataFileName.getSelectionModel().getSelectedItem();
      if (newDataFile == null) {
        return;
      }
      ObservableList<Scan> scanNumbers2 = newDataFile.getScans();
      comboScan.setItems(scanNumbers2);
      comboScan.getSelectionModel().select(0);
    });

    pnlDataFile.getChildren().add(comboDataFileName);
    pnlDataFile.setAlignment(Pos.TOP_CENTER);

    pnlScanArrows = new HBox();
    pnlScanArrows.setPrefWidth(-1);
    pnlScanArrows.setAlignment(Pos.TOP_CENTER);
    final String leftArrow = String.valueOf('\u2190');
    Button leftArrowButton = new Button(leftArrow);
    leftArrowButton.setOnAction(e -> {
      int ind = comboScan.getSelectionModel().getSelectedIndex() - 1;
      if (ind >= 0) {
        comboScan.getSelectionModel().select(ind);
      }
    });

    final String rightArrow = String.valueOf('\u2192');
    Button rightArrowButton = new Button(rightArrow);
    rightArrowButton.setOnAction(e -> {
      int ind = comboScan.getSelectionModel().getSelectedIndex() + 1;
      if (ind < (comboScan.getItems().size() - 1)) {
        comboScan.getSelectionModel().select(ind);
      }
    });

    pnlScanArrows.getChildren().addAll(leftArrowButton, comboScan, rightArrowButton);
    pnlScanArrows.setAlignment(Pos.TOP_CENTER);
    pnlScanArrows.setSpacing(5d);
    pnlScanNumber.getChildren().add(pnlScanArrows);

    spectrumPlot = new SpectraPlot();
    spectrumPlot.setMinSize(400, 300);

    pnlControls = new VBox();
    pnlControls.setSpacing(5);

    // Put all together
    pnlControls.getChildren().add(pnlDataFile);
    pnlControls.getChildren().add(pnlScanNumber);
    pnlControls.getChildren().add(pnMobilityScans);
    pnlControls.setAlignment(Pos.TOP_CENTER);

    getPreviewWrapperPane().setCenter(spectrumPlot);
    getPreviewWrapperPane().setBottom(pnlControls);
    BorderPane.setAlignment(pnlControls, Pos.TOP_CENTER);
    setOnPreviewShown(this::parametersChanged);

    selectedMobilityScan.addListener(
        ((observable, oldValue, newValue) -> lastChangedScan.setValue(newValue)));
    selectedScan.addListener(
        ((observable, oldValue, newValue) -> lastChangedScan.setValue(newValue)));
    lastChangedScan.addListener((observable, oldValue, newValue) -> parametersChanged());
  }

  /**
   * This method must be overloaded by derived class to load all the preview data sets into the
   * spectrumPlot
   */
  protected abstract void loadPreview(SpectraPlot spectrumPlot, Scan previewScan);

  private void updateTitle(Scan currentScan) {

    // Formats
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    // Set window and plot titles
    String title =
        "[" + currentScan.getDataFile().getName() + "] scan #" + currentScan.getScanNumber();

    String subTitle = ScanUtils.scanToString(lastChangedScan.get());

    Double basePeakMz = currentScan.getBasePeakMz();
    Double basePeakIntensity = currentScan.getBasePeakIntensity();
    if (basePeakMz != null) {
      subTitle += ", base peak: " + mzFormat.format(basePeakMz) + " m/z (" + intensityFormat.format(
          basePeakIntensity) + ")";
    }
    spectrumPlot.setTitle(title, subTitle);
  }

  @Override
  protected void parametersChanged() {

    // Update preview as parameters have changed
    if ((comboScan == null) || (!getPreviewCheckbox().isSelected())) {
      return;
    }

    Scan scan = lastChangedScan.getValue();
    if (scan == null) {
      return;
    }

    updateParameterSetFromComponents();
    loadPreview(spectrumPlot, scan);
    updateTitle(scan);
  }

  private void initMobilityScanControlPanel() {
    final String leftArrow = String.valueOf('\u2190');
    Button leftArrowButton = new Button(leftArrow);
    leftArrowButton.setOnAction(e -> {
      int ind = mobilityScanComboBox.getSelectionModel().getSelectedIndex() - 1;
      if (ind >= 0) {
        mobilityScanComboBox.getSelectionModel().select(ind);
      }
    });
    final String rightArrow = String.valueOf('\u2192');
    Button rightArrowButton = new Button(rightArrow);
    rightArrowButton.setOnAction(e -> {
      int ind = mobilityScanComboBox.getSelectionModel().getSelectedIndex() + 1;
      if (ind < (mobilityScanComboBox.getItems().size() - 1)) {
        mobilityScanComboBox.getSelectionModel().select(ind);
      }
    });

    pnMobilityScans.getChildren()
        .addAll(new Label("Mobility Scan "), leftArrowButton, mobilityScanComboBox,
            rightArrowButton);

    leftArrowButton.disableProperty().bind(pnMobilityScans.disabledProperty());
    rightArrowButton.disableProperty().bind(pnMobilityScans.disabledProperty());
    mobilityScanComboBox.disableProperty().bind(pnMobilityScans.disabledProperty());

    mobilityScanComboBox.valueProperty()
        .addListener(((observable, oldValue, newValue) -> selectedMobilityScan.set(newValue)));
    pnMobilityScans.setAlignment(Pos.CENTER);
    pnMobilityScans.setSpacing(5);
  }
}
