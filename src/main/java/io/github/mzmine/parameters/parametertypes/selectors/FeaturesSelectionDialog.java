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
package io.github.mzmine.parameters.parametertypes.selectors;

import java.util.List;
import org.controlsfx.control.CheckListView;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * @author akshaj Shows the dialog to select the features and add them to the parameter setup dialog
 *         of the Fx3DVisualizer.
 */
public class FeaturesSelectionDialog extends Stage {

  private CheckListView<Feature> featuresSelectionBox;
  private final ComboBox<RawDataFile> rawDataFileComboBox;
  private final ComboBox<PeakList> peakListComboBox;
  private final FlowPane buttonPane = new FlowPane();
  private Button btnOk;
  private Button btnCancel;
  private boolean returnState = true;
  private final GridPane mainPanel = new GridPane();;
  private BorderPane panel00 = new BorderPane();
  private BorderPane panel01 = new BorderPane();
  private BorderPane panel02 = new BorderPane();
  private BorderPane panel10 = new BorderPane();
  private BorderPane panel11 = new BorderPane();
  private BorderPane panel12 = new BorderPane();

  public FeaturesSelectionDialog() {

    // Main panel which holds all the components in a grid
    Scene scene = new Scene(mainPanel);

    // Use main CSS
    scene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(scene);

    mainPanel.add(panel00, 0, 0);
    mainPanel.add(panel01, 0, 1);
    mainPanel.add(panel02, 0, 2);
    mainPanel.add(panel10, 1, 0);
    mainPanel.add(panel11, 1, 1);
    mainPanel.add(panel12, 1, 2);

    peakListComboBox = new ComboBox<PeakList>(
        MZmineCore.getProjectManager().getCurrentProject().getFeatureLists());
    peakListComboBox.setTooltip(new Tooltip("Feature list selection"));

    Label peakListsLabel = new Label("Feature list");

    panel00.setCenter(peakListsLabel);
    panel10.setCenter(peakListComboBox);

    rawDataFileComboBox = new ComboBox<RawDataFile>();
    rawDataFileComboBox.setTooltip(new Tooltip("Raw data file selection"));

    peakListComboBox.setOnAction(e -> {
      PeakList peakList = peakListComboBox.getSelectionModel().getSelectedItem();
      if (peakList == null)
        return;
      RawDataFile[] rawDataFiles = peakList.getRawDataFiles();
      RawDataFile selectedFile = rawDataFileComboBox.getSelectionModel().getSelectedItem();
      rawDataFileComboBox.getItems().clear();
      rawDataFileComboBox.getItems().addAll(rawDataFiles);
      if (selectedFile != null) {
        if (rawDataFileComboBox.getItems().contains(selectedFile))
          rawDataFileComboBox.getSelectionModel().select(selectedFile);
        Feature[] features = peakList.getPeaks(selectedFile);
        featuresSelectionBox.setItems(FXCollections.observableArrayList(features));
      }
    });

    rawDataFileComboBox.setOnAction(e -> {
      RawDataFile dataFile = rawDataFileComboBox.getSelectionModel().getSelectedItem();
      PeakList peakList = peakListComboBox.getSelectionModel().getSelectedItem();
      if (dataFile == null || peakList == null)
        return;
      Feature[] features = peakList.getPeaks(dataFile);
      featuresSelectionBox.getItems().clear();
      featuresSelectionBox.getItems().addAll(features);
    });

    Label rawDataFilesLabel = new Label("Raw data file");
    panel01.setCenter(rawDataFilesLabel);
    panel11.setCenter(rawDataFileComboBox);

    RawDataFile datafile = MZmineCore.getProjectManager().getCurrentProject().getFeatureLists()
        .get(0).getRawDataFile(0);
    var features = FXCollections.observableArrayList(MZmineCore.getProjectManager()
        .getCurrentProject().getFeatureLists().get(0).getPeaks(datafile));
    featuresSelectionBox = new CheckListView<Feature>(features);
    featuresSelectionBox.setTooltip(new Tooltip("Features selection"));
    Label featuresLabel = new Label("Features");
    // featuresSelectionBox.setSize(50, 30);
    panel02.setLeft(featuresLabel);
    panel12.setCenter(featuresSelectionBox);

    btnOk = new Button("OK");
    btnOk.setOnAction(e -> {
      returnState = true;
      this.hide();
    });
    btnCancel = new Button("Cancel");
    btnOk.setOnAction(e -> {
      returnState = false;
      this.hide();
    });

    buttonPane.getChildren().addAll(btnOk, btnCancel);
    mainPanel.add(buttonPane, 0, 3);

    // this.pack();
    // this.setSize(670, 400);
    // this.setLocationRelativeTo(null);
  }

  public List<Feature> getSelectedFeatures() {
    return featuresSelectionBox.getSelectionModel().getSelectedItems();
  }

  public PeakList getSelectedPeakList() {
    return peakListComboBox.getSelectionModel().getSelectedItem();
  }

  public RawDataFile getSelectedRawDataFile() {
    return rawDataFileComboBox.getSelectionModel().getSelectedItem();
  }

  public boolean getReturnState() {
    return returnState;
  }

}
