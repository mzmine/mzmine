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
