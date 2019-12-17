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
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

/**
 * @author akshaj This class represents the component which shows Features in the parameter setup
 *         dialog of Fx3DVisualizer.
 */
public class FeaturesComponent extends BorderPane {

  public ObservableList<FeatureSelection> currentValue = FXCollections.observableArrayList();
  private ListView<FeatureSelection> featuresList = new ListView<>(currentValue);
  private final Button addButton = new Button("Add");;
  private final Button removeButton = new Button("Remove");
  private FlowPane buttonPane = new FlowPane(Orientation.VERTICAL);

  private Logger LOG = Logger.getLogger(this.getClass().getName());

  public FeaturesComponent() {
    // setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

    // JScrollPane scrollPane = new JScrollPane(jlist);
    // scrollPane.setPreferredSize(new Dimension(300, 60));
    setCenter(featuresList);

    buttonPane.getChildren().addAll(addButton, removeButton);
    setRight(buttonPane);

    addButton.setOnAction(e -> {
      currentValue.clear();
      LOG.finest("Add Button Clicked!");
      FeaturesSelectionDialog featuresSelectionDialog = new FeaturesSelectionDialog();
      featuresSelectionDialog.setModal(true);
      featuresSelectionDialog.setVisible(true);
      if (featuresSelectionDialog.getReturnState() == true) {
        // jlist.setVisible(true);
        PeakList selectedPeakList = featuresSelectionDialog.getSelectedPeakList();
        RawDataFile selectedRawDataFile = featuresSelectionDialog.getSelectedRawDataFile();
        LOG.finest("Selected PeakList is:" + selectedPeakList.getName());
        LOG.finest("Selected RawDataFile is:" + selectedRawDataFile.getName());
        currentValue.clear();
        for (Feature feature : featuresSelectionDialog.getSelectedFeatures()) {
          PeakListRow selectedRow = selectedPeakList.getPeakRow(feature);
          FeatureSelection featureSelection =
              new FeatureSelection(selectedPeakList, feature, selectedRow, selectedRawDataFile);
          currentValue.add(featureSelection);
        }
      }
    });

    removeButton.setOnAction(e -> {
      LOG.finest("Remove Button Clicked!");
      var sel = featuresList.getSelectionModel().getSelectedItems();
      currentValue.removeAll(sel);
    });

  }

  public void setValue(List<FeatureSelection> newValue) {
    currentValue = FXCollections.observableArrayList(newValue);
    featuresList.setItems(currentValue);
  }

  public List<FeatureSelection> getValue() {
    return currentValue;
  }

}
