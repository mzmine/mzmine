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

package io.github.mzmine.modules.dataprocessing.align_ransac;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;


/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This is used to preview
 * how the selected mass detector and his parameters works over the raw data file.
 */
public class RansacAlignerSetupDialog extends ParameterSetupDialogWithPreview {

  // Dialog components
  private GridPane comboPanel;

  private final FlowPane featureListsPanel;

  private ObservableList<FeatureList> featureLists;

  private ComboBox<FeatureList> featureListsComboX, featureListsComboY;

  private AlignmentRansacPlot plot;

  private RansacPreviewTask task;

  public RansacAlignerSetupDialog(boolean valueCheckRequired, RansacAlignerParameters parameters) {
    super(valueCheckRequired, parameters);

    featureLists = FXCollections.observableArrayList(
        MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists());

    featureListsPanel = new FlowPane();

    paramsPane.add(new Separator(), 0, getNumberOfParameters() + 1);
    // Panel for the combo boxes with the feature lists
    comboPanel = new GridPane();

    featureListsComboX = new ComboBox<>(featureLists);
    featureListsComboX.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.isEmpty()) {
        updatePreview();
      }
//      updatePreview();
    });
    comboPanel.add(featureListsComboX, 1, 1);
    featureListsComboY = new ComboBox<>(featureLists);
    comboPanel.add(featureListsComboY, 1, 2);
    featureListsComboY.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.isEmpty()) {
        updatePreview();
      }
    });

    Button refreshButton = new Button("Refresh preview");
    refreshButton.setOnAction(event -> updatePreview());
    comboPanel.add(refreshButton, 0, 3);

    featureListsPanel.getChildren().add(comboPanel);

//    featureListsPanel.getChildren().add(refreshButton);

    comboPanel.setHgap(5);
    comboPanel.setVgap(5);
    previewWrapperPane.setBottom(comboPanel);

    plot = new AlignmentRansacPlot();

    previewWrapperPane.setCenter(plot);
//    setOnPreviewShown(this::parametersChanged);

//    previewWrapperPane.visibleProperty().bind(FXCollections.observableArrayList(parameterSet.get));
//    previewWrapperPane.visibleProperty().addListener((c, o, n) ->
////    {
////      if (n) {
////        updateParameterSetFromComponents();
////      }
////    });
//        parametersChanged());
    previewWrapperPane.isResizable();
  }

  private void updatePreview() {
    // Update the parameter set from dialog components
    updateParameterSetFromComponents();

    //check updated values
    ArrayList<String> messages = new ArrayList<>();
    boolean allParametersOK = paramPane.getParameterSet().checkParameterValues(messages);

    if (!allParametersOK) {
      StringBuilder message = new StringBuilder("Please check the parameter settings:\n\n");
      for (String m : messages) {
        message.append(m);
        message.append("\n");
      }
      MZmineCore.getDesktop().displayMessage(null, message.toString());
      return;
    }

    FeatureList featureListX = featureListsComboX.getSelectionModel().getSelectedItem();
    FeatureList featureListY = featureListsComboY.getSelectionModel().getSelectedItem();

    if ((featureListX == null) || (featureListY == null)) {
      return;
    }

    logger.finest("Creating new thread for RANSAC preview update");
    task = new RansacPreviewTask(this.plot, featureListX, featureListY, super.parameterSet);
    MZmineCore.getTaskController().addTask(task);
  }

  @Override
  protected void showPreview(boolean show) {
    super.showPreview(show);
    if(show) {
      updatePreview();
    }
  }

  //using this method makes module too slow
//  protected void parametersChanged() {
//
////    Scan scan = lastChangedScan.getValue();
////    if (scan == null) {
////      return;
////    }
//
//    updateParameterSetFromComponents();
////    loadPreview(spectrumPlot, scan);
////    updateTitle(scan);
//    updatePreview();
//  }
}
