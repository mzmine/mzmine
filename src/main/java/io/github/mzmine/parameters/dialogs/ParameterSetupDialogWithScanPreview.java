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
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.CollectionUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

/**
 * This class extends ParameterSetupDialog class, including a SpectraPlot. This is used to preview
 * how the selected mass detector and his parameters works over the raw data file.
 */
public abstract class ParameterSetupDialogWithScanPreview extends ParameterSetupDialog {

  private RawDataFile[] dataFiles;
  private RawDataFile previewDataFile;

  // Dialog components
  private final BorderPane pnlPreviewFields;
  private final FlowPane pnlLab, pnlScanArrows, pnlFlds;
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

    if (selectedFiles.length > 0)
      previewDataFile = selectedFiles[0];
    else
      previewDataFile = dataFiles[0];

    previewCheckBox = new CheckBox("Show preview");
    // previewCheckBox.setHorizontalAlignment(SwingConstants.CENTER);

    paramsPane.add(new Separator(), 0, getNumberOfParameters() + 1);
    paramsPane.add(previewCheckBox, 0, getNumberOfParameters() + 2);

    // Elements of pnlLab
    pnlLab = new FlowPane(Orientation.VERTICAL);
    // pnlLab.setLayout(new BoxLayout(pnlLab, BoxLayout.Y_AXIS));
    // pnlLab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    pnlLab.getChildren().add(new Label("Data file "));
    // pnlLab.add(Box.createVerticalStrut(25));
    pnlLab.getChildren().add(new Label("Scan number "));

    // Elements of pnlFlds
    pnlFlds = new FlowPane(Orientation.VERTICAL);
    // pnlFlds.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    int scanNumbers[] = previewDataFile.getScanNumbers();
    ObservableList<Integer> scanNums =
        FXCollections.observableArrayList(CollectionUtils.toIntegerArray(scanNumbers));
    comboScanNumber = new ComboBox<Integer>(scanNums);
    comboScanNumber.getSelectionModel().select(0);


    comboDataFileName = new ComboBox<RawDataFile>(
        MZmineCore.getProjectManager().getCurrentProject().getRawDataFiles());
    comboDataFileName.getSelectionModel().select(previewDataFile);
    comboDataFileName.setOnAction(e -> {
      var previewDataFile = comboDataFileName.getSelectionModel().getSelectedItem();
      if (previewDataFile == null)
        return;
      int scanNumbers2[] = previewDataFile.getScanNumbers();
      ObservableList<Integer> scanNums2 =
          FXCollections.observableArrayList(CollectionUtils.toIntegerArray(scanNumbers2));

      comboScanNumber.setItems(scanNums2);
      comboScanNumber.getSelectionModel().select(0);
      parametersChanged();
    });


    // comboScanNumber.addActionListener(this);

    pnlFlds.getChildren().add(comboDataFileName);
    // pnlFlds.add(Box.createVerticalStrut(10));

    // --> Elements of pnlScanArrows

    pnlScanArrows = new FlowPane();
    // pnlScanArrows.setLayout(new BoxLayout(pnlScanArrows, BoxLayout.X_AXIS));
    final String leftArrow = new String(new char[] {'\u2190'});
    Button leftArrowButton = new Button(leftArrow);
    leftArrowButton.setOnAction(e -> {
      int ind = comboScanNumber.getSelectionModel().getSelectedIndex() - 1;
      if (ind >= 0)
        comboScanNumber.getSelectionModel().select(ind);
    });

    // pnlScanArrows.add(Box.createHorizontalStrut(5));
    // pnlScanArrows.add(comboScanNumber);
    // pnlScanArrows.add(Box.createHorizontalStrut(5));

    final String rightArrow = new String(new char[] {'\u2192'});
    Button rightArrowButton = new Button(rightArrow);
    rightArrowButton.setOnAction(e -> {
      int ind = comboScanNumber.getSelectionModel().getSelectedIndex() + 1;
      if (ind < (comboScanNumber.getItems().size() - 1))
        comboScanNumber.getSelectionModel().select(ind);
    });

    pnlScanArrows.getChildren().addAll(leftArrowButton, comboScanNumber, rightArrowButton);
    pnlFlds.getChildren().add(pnlScanArrows);

    // Put all together
    pnlPreviewFields = new BorderPane();

    pnlPreviewFields.setLeft(pnlLab);
    pnlPreviewFields.setCenter(pnlFlds);
    pnlPreviewFields.visibleProperty().bind(previewCheckBox.selectedProperty());

    spectrumPlot = new SpectraPlot();
    // spectrumPlot.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
    // spectrumPlot.setMinimumSize(new Dimension(400, 300));

    paramsPane.add(pnlPreviewFields, 0, getNumberOfParameters() + 3);
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
    if ((comboScanNumber == null) || (!previewCheckBox.isSelected()))
      return;

    Integer scanNumber = comboScanNumber.getSelectionModel().getSelectedItem();
    if (scanNumber == null)
      return;
    Scan currentScan = previewDataFile.getScan(scanNumber);

    updateParameterSetFromComponents();

    loadPreview(spectrumPlot, currentScan);

    updateTitle(currentScan);

  }



}
