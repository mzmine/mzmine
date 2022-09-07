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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.List;
import java.util.Vector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
      updatePreview();
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
    refreshButton.setOnAction(event -> {
      updatePreview();
    });
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
  }


  /**
   * Create the vector which contains all the possible aligned peaks.
   *
   * @return vector which contains all the possible aligned peaks.
   */
  private Vector<AlignStructMol> getVectorAlignment(FeatureList peakListX, FeatureList peakListY,
      RawDataFile file, RawDataFile file2) {

    Vector<AlignStructMol> alignMol = new Vector<AlignStructMol>();

    for (FeatureListRow row : peakListX.getRows()) {

      // Calculate limits for a row with which the row can be aligned
      MZTolerance mzTolerance = super.parameterSet.getParameter(RansacAlignerParameters.MZTolerance)
          .getValue();
      RTTolerance rtTolerance = super.parameterSet.getParameter(
          RansacAlignerParameters.RTToleranceBefore).getValue();
      Range<Double> mzRange = mzTolerance.getToleranceRange(row.getAverageMZ());
      Range<Float> rtRange = rtTolerance.getToleranceRange(row.getAverageRT());

      // Get all rows of the aligned peaklist within parameter limits
      List<FeatureListRow> candidateRows = peakListY.getRowsInsideScanAndMZRange(rtRange, mzRange);

      for (FeatureListRow candidateRow : candidateRows) {
        if (file == null || file2 == null) {
          alignMol.addElement(new AlignStructMol(row, candidateRow));
        } else {
          if (candidateRow.getFeature(file2) != null) {
            alignMol.addElement(new AlignStructMol(row, candidateRow, file, file2));
          }
        }
      }
    }
    return alignMol;
  }

  private void updatePreview() {

    this.plot.removeSeries();

    // Update the parameter set from dialog components
    updateParameterSetFromComponents();

    FeatureList featureListX = featureListsComboX.getSelectionModel().getSelectedItem();
    FeatureList featureListY = featureListsComboY.getSelectionModel().getSelectedItem();

    if ((featureListsComboX == null) || (featureListsComboY == null)) {
      return;
    }

    // Select the rawDataFile which has more peaks in each peakList
    int numPeaks = 0;
    RawDataFile file = null;
    RawDataFile file2 = null;

    for (RawDataFile rfile : featureListX.getRawDataFiles()) {
      if (featureListX.getFeatures(rfile).size() > numPeaks) {
        numPeaks = featureListX.getFeatures(rfile).size();
        file = rfile;
      }
    }
    numPeaks = 0;
    for (RawDataFile rfile : featureListY.getRawDataFiles()) {
      if (featureListY.getFeatures(rfile).size() > numPeaks) {
        numPeaks = featureListY.getFeatures(rfile).size();
        file2 = rfile;
      }
    }

    // Check the parameter values
//    ArrayList<String> errorMessages = new ArrayList<String>();
//    boolean parametersOK = super.parameterSet.checkParameterValues(errorMessages);
//    if (!parametersOK) {
//      StringBuilder message = new StringBuilder("Please check the parameter settings:\n\n");
//      for (String m : errorMessages) {
//        message.append(m);
//        message.append("\n");
//      }
//      MZmineCore.getDesktop().displayMessage(null, message.toString());
//      return;
//    }

    // Ransac Alignment
    Vector<AlignStructMol> list = this.getVectorAlignment(featureListX, featureListY, file, file2);
    RANSAC ransac = new RANSAC(super.parameterSet);
    ransac.alignment(list);

    // Plot the result
    this.plot.addSeries(list, featureListX.getName() + " vs " + featureListY.getName(),
        super.parameterSet.getParameter(RansacAlignerParameters.Linear).getValue());
    this.plot.printAlignmentChart(featureListX.getName() + " RT", featureListY.getName() + " RT");

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
