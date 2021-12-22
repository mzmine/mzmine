/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.CheckListView;

/**
 * @author akshaj This class represents the component which shows Features in the parameter setup
 * dialog of Fx3DVisualizer.
 */
public class FeaturesComponent extends HBox {

  private final Button addButton = new Button("Add");
  private final Button removeButton = new Button("Remove");
  public ObservableList<Feature> currentValue = FXCollections.observableArrayList();
  ;
  private ListView<Feature> featuresList = new ListView<>(currentValue);
  private VBox buttonPane = new VBox();

  private Logger logger = Logger.getLogger(this.getClass().getName());

  public FeaturesComponent() {

    setSpacing(8.0);

    buttonPane.setSpacing(8.0);
    buttonPane.getChildren().addAll(addButton, removeButton);

    getChildren().addAll(featuresList, buttonPane);

    featuresList.setPrefHeight(100.0);

    addButton.setOnAction(e -> {
      logger.finest("Add button clicked!");

      final List<FeatureList> featureLists = MZmineCore.getProjectManager().getCurrentProject()
          .getCurrentFeatureLists();

      ComboParameter<FeatureList> featureListsParam = new ComboParameter<>("Feature list",
          "Feature list selection", FXCollections.observableList(featureLists));
      ComboParameter<RawDataFile> dataFilesParam = new ComboParameter<>("Raw data file",
          "Raw data file selection", FXCollections.observableArrayList());
      MultiChoiceParameter<Feature> featuresParam = new MultiChoiceParameter<>("Features",
          "Feature selection", new Feature[0]);
      SimpleParameterSet paramSet = new SimpleParameterSet(
          new Parameter[]{featureListsParam, dataFilesParam, featuresParam});

      ParameterSetupDialog dialog = new ParameterSetupDialog(true, paramSet);
      ComboBox<FeatureList> featureListsCombo = dialog.getComponentForParameter(featureListsParam);
      ComboBox<RawDataFile> dataFilesCombo = dialog.getComponentForParameter(dataFilesParam);
      CheckListView<Feature> featuresSelection = dialog.getComponentForParameter(featuresParam);
      featureListsCombo.setOnAction(e2 -> {
        FeatureList featureList = featureListsCombo.getSelectionModel().getSelectedItem();
        if (featureList == null) {
          return;
        }
        dataFilesCombo.setItems(featureList.getRawDataFiles());
        dataFilesCombo.getSelectionModel().selectFirst();
      });
      dataFilesCombo.setOnAction(e3 -> {
        FeatureList featureList = featureListsCombo.getSelectionModel().getSelectedItem();
        RawDataFile dataFile = dataFilesCombo.getSelectionModel().getSelectedItem();
        if (featureList == null || dataFile == null) {
          return;
        }
        var features = FXCollections.observableArrayList(featureList.getFeatures(dataFile));
        featuresSelection.setItems(
            (ObservableList<Feature>) (ObservableList<? extends Feature>) features);
      });
      featureListsCombo.getSelectionModel().selectFirst();
      dataFilesCombo.getSelectionModel().selectFirst();

      /*
       * Load features into featuresSelection. Events don't trigger automatically until the Stage is
       * shown.
       */
      featureListsCombo.fireEvent(new ActionEvent());
      dataFilesCombo.fireEvent(new ActionEvent());

      dialog.showAndWait();
      if (dialog.getExitCode() == ExitCode.OK) {
        currentValue.addAll(featuresSelection.getCheckModel().getCheckedItems());
      }
    });

    removeButton.setOnAction(e -> {
      logger.finest("Remove Button Clicked!");
      var sel = featuresList.getSelectionModel().getSelectedItems();
      currentValue.removeAll(sel);
    });

  }

  public List<Feature> getValue() {
    return currentValue;
  }

  public void setValue(List<Feature> newValue) {
    currentValue = FXCollections.observableArrayList(newValue);
    featuresList.setItems(currentValue);
  }

}
