/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.dialogs;

import java.text.NumberFormat;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.CollectionUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * This class extends ParameterSetupDialog class, including a SpectraPlot. This is used to preview
 * how the selected mass detector and his parameters works over the raw data file.
 */
public abstract class ParameterSetupDialogWithScanPreview extends ParameterSetupDialogWithPreview {

  // Dialog components
  private final FlowPane pnlDataFile, pnlScanArrows, pnlScanNumber;
  private final VBox pnlControls;
  private final ComboBox<RawDataFile> comboDataFileName;
  private final ComboBox<Integer> comboScanNumber;
  // XYPlot
  private final SpectraPlot spectrumPlot;
  private RawDataFile[] dataFiles;
  private RawDataFile previewDataFile;

  /**
   * @param valueCheckRequired
   * @param parameters
   */
  public ParameterSetupDialogWithScanPreview(boolean valueCheckRequired, ParameterSet parameters) {

    super(valueCheckRequired, parameters);

    dataFiles = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

    // TODO: if (dataFiles.length == 0)
    // return;
    if (dataFiles.length == 0) {
      this.hide();
      MZmineCore.getDesktop()
          .displayMessage("Please load a raw data file before selecting a " + "mass detector.");
      throw new UnsupportedOperationException(
          "Please load a raw data file before selecting a mass detector.");
    }

    RawDataFile selectedFiles[] = MZmineCore.getDesktop().getSelectedDataFiles();

    if (selectedFiles.length > 0) {
      previewDataFile = selectedFiles[0];
    } else {
      previewDataFile = dataFiles[0];
    }

    // Elements of pnlLab
    pnlDataFile = new FlowPane();
    pnlDataFile.getChildren().add(new Label("Data file "));

    pnlScanNumber = new FlowPane();
    pnlScanNumber.getChildren().add(new Label("Scan number "));

    int scanNumbers[] = previewDataFile.getScanNumbers();
    ObservableList<Integer> scanNums =
        FXCollections.observableArrayList(CollectionUtils.toIntegerArray(scanNumbers));
    comboScanNumber = new ComboBox<Integer>(scanNums);
    comboScanNumber.getSelectionModel().select(0);
    comboScanNumber.getSelectionModel().selectedItemProperty().addListener((obs, old, newIndex) -> {
      parametersChanged();
    });

    comboDataFileName = new ComboBox<RawDataFile>(
        MZmineCore.getProjectManager().getCurrentProject().getRawDataFiles());
    comboDataFileName.getSelectionModel().select(previewDataFile);
    comboDataFileName.setOnAction(e -> {
      var previewDataFile = comboDataFileName.getSelectionModel().getSelectedItem();
      if (previewDataFile == null) {
        return;
      }
      int scanNumbers2[] = previewDataFile.getScanNumbers();
      ObservableList<Integer> scanNums2 =
          FXCollections.observableArrayList(CollectionUtils.toIntegerArray(scanNumbers2));

      comboScanNumber.setItems(scanNums2);
      comboScanNumber.getSelectionModel().select(0);
      parametersChanged();
    });

    pnlDataFile.getChildren().add(comboDataFileName);
    pnlDataFile.setAlignment(Pos.TOP_CENTER);

    pnlScanArrows = new FlowPane();
    final String leftArrow = new String(new char[] {'\u2190'});
    Button leftArrowButton = new Button(leftArrow);
    leftArrowButton.setOnAction(e -> {
      int ind = comboScanNumber.getSelectionModel().getSelectedIndex() - 1;
      if (ind >= 0) {
        comboScanNumber.getSelectionModel().select(ind);
      }
    });

    final String rightArrow = new String(new char[] {'\u2192'});
    Button rightArrowButton = new Button(rightArrow);
    rightArrowButton.setOnAction(e -> {
      int ind = comboScanNumber.getSelectionModel().getSelectedIndex() + 1;
      if (ind < (comboScanNumber.getItems().size() - 1)) {
        comboScanNumber.getSelectionModel().select(ind);
      }
    });

    pnlScanArrows.getChildren().addAll(leftArrowButton, comboScanNumber, rightArrowButton);
    pnlScanNumber.getChildren().add(pnlScanArrows);
    pnlScanNumber.setAlignment(Pos.TOP_CENTER);
    pnlScanArrows.setAlignment(Pos.TOP_CENTER);

    spectrumPlot = new SpectraPlot();
    spectrumPlot.setMinSize(400, 300);

    pnlControls = new VBox();
    pnlControls.setSpacing(5);

    // Put all together
    pnlControls.getChildren().add(pnlDataFile);
    pnlControls.getChildren().add(pnlScanNumber);
    pnlControls.setAlignment(Pos.TOP_CENTER);

    getPreviewWrapperPane().setCenter(spectrumPlot);
    getPreviewWrapperPane().setBottom(pnlControls);
    BorderPane.setAlignment(pnlControls, Pos.TOP_CENTER);
    setOnPreviewShown(() -> parametersChanged());
  }

  /**
   * This method must be overloaded by derived class to load all the preview data sets into the
   * spectrumPlot
   */
  protected abstract void loadPreview(SpectraPlot spectrumPlot, Scan previewScan);

  private void updateTitle(Scan currentScan) {

    // Formats
    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    // Set window and plot titles
    String title = "[" + previewDataFile.getName() + "] scan #" + currentScan.getScanNumber();

    String subTitle =
        "MS" + currentScan.getMSLevel() + ", RT " + rtFormat.format(currentScan.getRetentionTime());

    Double basePeakMz = currentScan.getBasePeakMz();
    Double basePeakIntensity = currentScan.getBasePeakIntensity();
    if (basePeakMz != null) {
      subTitle += ", base peak: " + mzFormat.format(basePeakMz) + " m/z ("
          + intensityFormat.format(basePeakIntensity) + ")";
    }
    spectrumPlot.setTitle(title, subTitle);

  }

  @Override
  protected void parametersChanged() {

    // Update preview as parameters have changed
    if ((comboScanNumber == null) || (!getPreviewCheckbox().isSelected())) {
      return;
    }

    Integer scanNumber = comboScanNumber.getSelectionModel().getSelectedItem();
    if (scanNumber == null) {
      return;
    }

    Scan currentScan = previewDataFile.getScan(scanNumber);
    updateParameterSetFromComponents();
    loadPreview(spectrumPlot, currentScan);
    updateTitle(currentScan);
  }
}
