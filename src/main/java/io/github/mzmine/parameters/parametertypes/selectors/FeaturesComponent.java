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
import java.util.logging.Logger;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * @author akshaj This class represents the component which shows Features in the parameter setup
 *         dialog of Fx3DVisualizer.
 */
public class FeaturesComponent extends HBox {

  public ObservableList<Feature> currentValue = FXCollections.observableArrayList();
  private ListView<Feature> featuresList = new ListView<>(currentValue);
  private final Button addButton = new Button("Add");;
  private final Button removeButton = new Button("Remove");
  private VBox buttonPane = new VBox();

  private Logger logger = Logger.getLogger(this.getClass().getName());

  public FeaturesComponent() {

    setSpacing(8.0);

    buttonPane.setSpacing(8.0);
    buttonPane.getChildren().addAll(addButton, removeButton);

    getChildren().addAll(featuresList, buttonPane);

    featuresList.setPrefHeight(120.0);

    addButton.setOnAction(e -> {
      currentValue.clear();
      logger.finest("Add Button Clicked!");
      FeaturesSelectionDialog featuresSelectionDialog = new FeaturesSelectionDialog();
      featuresSelectionDialog.showAndWait();
      if (featuresSelectionDialog.getReturnState() == true) {
        // jlist.setVisible(true);
        PeakList selectedPeakList = featuresSelectionDialog.getSelectedPeakList();
        RawDataFile selectedRawDataFile = featuresSelectionDialog.getSelectedRawDataFile();
        logger.finest("Selected PeakList is:" + selectedPeakList.getName());
        logger.finest("Selected RawDataFile is:" + selectedRawDataFile.getName());
        // currentValue.clear();
        currentValue.addAll(featuresSelectionDialog.getSelectedFeatures());
      }
    });

    removeButton.setOnAction(e -> {
      logger.finest("Remove Button Clicked!");
      var sel = featuresList.getSelectionModel().getSelectedItems();
      currentValue.removeAll(sel);
    });

  }

  public void setValue(List<Feature> newValue) {
    currentValue = FXCollections.observableArrayList(newValue);
    featuresList.setItems(currentValue);
  }

  public List<Feature> getValue() {
    return currentValue;
  }

}
