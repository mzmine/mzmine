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
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This is used to preview
 * how the selected mass detector and his parameters works over the raw data file.
 */
public class RansacAlignerSetupDialog extends ParameterSetupDialog {

  // Dialog components
  private final BorderPane pnlPlotXY;
  private final GridPane comboPanel;
  private final FlowPane peakListsPanel;
  private final CheckBox previewCheckBox;
  private final AlignmentRansacPlot chart;
  private final ComboBox<FeatureList> peakListsComboX, peakListsComboY;
  private final Button alignmentPreviewButton;

  public RansacAlignerSetupDialog(boolean valueCheckRequired, RansacAlignerParameters parameters) {
    super(valueCheckRequired, parameters);

    var featureLists = FXCollections.observableArrayList(
        MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists());

    FeatureList[] selectedPeakLists = MZmineCore.getDesktop().getSelectedPeakLists();

    // Preview check box
    previewCheckBox = new CheckBox("Show preview of RANSAC alignment");

    peakListsPanel = new FlowPane();
    peakListsPanel.visibleProperty().bind(previewCheckBox.selectedProperty());
    // previewCheckBox.setHorizontalAlignment(SwingConstants.CENTER);

    paramsPane.add(new Separator(), 0, getNumberOfParameters() + 1);
    paramsPane.add(previewCheckBox, 0, getNumberOfParameters() + 2);

    // Panel for the combo boxes with the feature lists
    comboPanel = new GridPane();

    peakListsComboX = new ComboBox<>(featureLists);
    // peakListsComboX.addActionListener(this);
    peakListsComboY = new ComboBox<>(featureLists);
    // peakListsComboY.addActionListener(this);

    alignmentPreviewButton = new Button("Preview alignment");
    alignmentPreviewButton.setOnAction(e -> updatePreview());
    comboPanel.getChildren().addAll(peakListsComboX, peakListsComboY, alignmentPreviewButton);

    if (selectedPeakLists.length >= 2) {
      peakListsComboX.getSelectionModel().select(selectedPeakLists[0]);
      peakListsComboY.getSelectionModel().select(selectedPeakLists[1]);
    } else {
      peakListsComboX.getSelectionModel().select(featureLists.get(0));
      peakListsComboY.getSelectionModel().select(featureLists.get(1));
    }

    peakListsPanel.getChildren().add(comboPanel);

    // Panel for XYPlot
    pnlPlotXY = new BorderPane();
    // Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
    // Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
    // pnlPlotXY.setBorder(BorderFactory.createCompoundBorder(one, two));
    // pnlPlotXY.setBackground(Color.white);

    chart = new AlignmentRansacPlot();
    pnlPlotXY.setCenter(chart);

    paramsPane.add(peakListsPanel, 0, getNumberOfParameters() + 3);

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

    FeatureList peakListX = peakListsComboX.getSelectionModel().getSelectedItem();
    FeatureList peakListY = peakListsComboY.getSelectionModel().getSelectedItem();

    if ((peakListX == null) || (peakListY == null)) {
      return;
    }

    // Select the rawDataFile which has more peaks in each peakList
    int numPeaks = 0;
    RawDataFile file = null;
    RawDataFile file2 = null;

    for (RawDataFile rfile : peakListX.getRawDataFiles()) {
      if (peakListX.getFeatures(rfile).size() > numPeaks) {
        numPeaks = peakListX.getFeatures(rfile).size();
        file = rfile;
      }
    }
    numPeaks = 0;
    for (RawDataFile rfile : peakListY.getRawDataFiles()) {
      if (peakListY.getFeatures(rfile).size() > numPeaks) {
        numPeaks = peakListY.getFeatures(rfile).size();
        file2 = rfile;
      }
    }

    // Update the parameter set from dialog components
    updateParameterSetFromComponents();

    // Check the parameter values
    ArrayList<String> errorMessages = new ArrayList<String>();
    boolean parametersOK = super.parameterSet.checkParameterValues(errorMessages);
    if (!parametersOK) {
      StringBuilder message = new StringBuilder("Please check the parameter settings:\n\n");
      for (String m : errorMessages) {
        message.append(m);
        message.append("\n");
      }
      MZmineCore.getDesktop().displayMessage(null, message.toString());
      return;
    }

    // Ransac Alignment
    Vector<AlignStructMol> list = this.getVectorAlignment(peakListX, peakListY, file, file2);
    RANSAC ransac = new RANSAC(super.parameterSet);
    ransac.alignment(list);

    // Plot the result
    this.chart.removeSeries();
    this.chart.addSeries(list, peakListX.getName() + " vs " + peakListY.getName(),
        super.parameterSet.getParameter(RansacAlignerParameters.Linear).getValue());
    this.chart.printAlignmentChart(peakListX.getName() + " RT", peakListY.getName() + " RT");

  }

}
