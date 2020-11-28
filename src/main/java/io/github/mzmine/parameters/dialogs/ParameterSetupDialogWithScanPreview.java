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

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.CollectionUtils;
import java.text.NumberFormat;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * This class extends ParameterSetupDialog class, including a SpectraPlot. This is used to preview
 * how the selected mass detector and his parameters works over the raw data file.
 */
public abstract class ParameterSetupDialogWithScanPreview extends ParameterSetupDialog {

  private RawDataFile[] dataFiles;
  private RawDataFile previewDataFile;

  // Dialog components
  private final BorderPane pnlPreviewFields;
  private final FlowPane pnlDataFile, pnlScanArrows, pnlScanNumber;
  private final VBox pnlControls;
  private final ComboBox<RawDataFile> comboDataFileName;
  private final ComboBox<Integer> comboScanNumber;
  private final CheckBox previewCheckBox;

  // XYPlot
  private final SpectraPlot spectrumPlot;

  /**
   * @param parameters
   * @param massDetectorTypeNumber
   */
  public ParameterSetupDialogWithScanPreview(boolean valueCheckRequired, ParameterSet parameters) {

    super(valueCheckRequired, parameters);

    dataFiles = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

    // TODO: if (dataFiles.length == 0)
    // return;

    RawDataFile selectedFiles[] = MZmineCore.getDesktop().getSelectedDataFiles();

    if (selectedFiles.length > 0) {
      previewDataFile = selectedFiles[0];
    } else {
      previewDataFile = dataFiles[0];
    }

    previewCheckBox = new CheckBox("Show preview");

    paramsPane.add(new Separator(), 0, getNumberOfParameters() + 1);
    paramsPane.add(previewCheckBox, 0, getNumberOfParameters() + 2);

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

    pnlScanArrows = new FlowPane();
    final String leftArrow = new String(new char[]{'\u2190'});
    Button leftArrowButton = new Button(leftArrow);
    leftArrowButton.setOnAction(e -> {
      int ind = comboScanNumber.getSelectionModel().getSelectedIndex() - 1;
      if (ind >= 0) {
        comboScanNumber.getSelectionModel().select(ind);
      }
    });

    final String rightArrow = new String(new char[]{'\u2192'});
    Button rightArrowButton = new Button(rightArrow);
    rightArrowButton.setOnAction(e -> {
      int ind = comboScanNumber.getSelectionModel().getSelectedIndex() + 1;
      if (ind < (comboScanNumber.getItems().size() - 1)) {
        comboScanNumber.getSelectionModel().select(ind);
      }
    });

    pnlScanArrows.getChildren().addAll(leftArrowButton, comboScanNumber, rightArrowButton);
    pnlScanNumber.getChildren().add(pnlScanArrows);

    spectrumPlot = new SpectraPlot();
    spectrumPlot.setMinSize(400, 300);

    pnlControls = new VBox();
    pnlControls.setSpacing(5);
    BorderPane.setAlignment(pnlControls, Pos.CENTER);

    // Put all together
    pnlControls.getChildren().add(pnlDataFile);
    pnlControls.getChildren().add(pnlScanNumber);
    pnlPreviewFields = new BorderPane();
    pnlPreviewFields.setCenter(pnlControls);
    pnlPreviewFields.visibleProperty().bind(previewCheckBox.selectedProperty());
    spectrumPlot.visibleProperty().bind(previewCheckBox.selectedProperty());
    spectrumPlot.visibleProperty().addListener((c, o, n) -> {
      if (n == true) {
        mainPane.setCenter(spectrumPlot);
        mainPane.setLeft(mainScrollPane);
        mainPane.autosize();
        mainPane.getScene().getWindow().sizeToScene();
        parametersChanged();
      } else {
        mainPane.setLeft(null);
        mainPane.setCenter(mainScrollPane);
        mainPane.autosize();
        mainPane.getScene().getWindow().sizeToScene();
      }
    });

    paramsPane.add(pnlPreviewFields, 0, getNumberOfParameters() + 3, 2, 1);
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

    DataPoint basePeak = currentScan.getHighestDataPoint();
    if (basePeak != null) {
      subTitle += ", base peak: " + mzFormat.format(basePeak.getMZ()) + " m/z ("
          + intensityFormat.format(basePeak.getIntensity()) + ")";
    }
    spectrumPlot.setTitle(title, subTitle);

  }

  @Override
  protected void parametersChanged() {

    // Update preview as parameters have changed
    if ((comboScanNumber == null) || (!previewCheckBox.isSelected())) {
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
