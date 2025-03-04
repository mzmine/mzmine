/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.project.ProjectService;
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
import org.jetbrains.annotations.Nullable;

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

      final List<FeatureList> featureLists = ProjectService.getProjectManager().getCurrentProject()
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
        dataFilesCombo.setItems(FXCollections.observableList(featureList.getRawDataFiles()));
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

  public void setValue(@Nullable List<Feature> newValue) {
    if (newValue == null) {
      newValue = List.of();
    }
    currentValue = FXCollections.observableArrayList(newValue);
    featuresList.setItems(currentValue);
  }

}
