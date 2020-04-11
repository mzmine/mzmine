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

package io.github.mzmine.modules.dataprocessing.align_ransac;

import java.util.ArrayList;
import java.util.Vector;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
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
  private final ComboBox<PeakList> peakListsComboX, peakListsComboY;
  private final Button alignmentPreviewButton;

  public RansacAlignerSetupDialog(boolean valueCheckRequired, RansacAlignerParameters parameters) {
    super(valueCheckRequired, parameters);

    var featureLists = MZmineCore.getProjectManager().getCurrentProject().getFeatureLists();

    PeakList selectedPeakLists[] = MZmineCore.getDesktop().getSelectedPeakLists();

    // Preview check box
    previewCheckBox = new CheckBox("Show preview of RANSAC alignment");

    peakListsPanel = new FlowPane();
    peakListsPanel.visibleProperty().bind(previewCheckBox.selectedProperty());
    // previewCheckBox.setHorizontalAlignment(SwingConstants.CENTER);

    paramsPane.add(new Separator(), 0, getNumberOfParameters() + 1);
    paramsPane.add(previewCheckBox, 0, getNumberOfParameters() + 2);

    // Panel for the combo boxes with the feature lists
    comboPanel = new GridPane();

    peakListsComboX = new ComboBox<PeakList>(featureLists);
    // peakListsComboX.addActionListener(this);
    peakListsComboY = new ComboBox<PeakList>(featureLists);
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
  private Vector<AlignStructMol> getVectorAlignment(PeakList peakListX, PeakList peakListY,
      RawDataFile file, RawDataFile file2) {

    Vector<AlignStructMol> alignMol = new Vector<AlignStructMol>();

    for (PeakListRow row : peakListX.getRows()) {

      // Calculate limits for a row with which the row can be aligned
      MZTolerance mzTolerance =
          super.parameterSet.getParameter(RansacAlignerParameters.MZTolerance).getValue();
      RTTolerance rtTolerance =
          super.parameterSet.getParameter(RansacAlignerParameters.RTToleranceBefore).getValue();
      Range<Double> mzRange = mzTolerance.getToleranceRange(row.getAverageMZ());
      Range<Double> rtRange = rtTolerance.getToleranceRange(row.getAverageRT());

      // Get all rows of the aligned peaklist within parameter limits
      PeakListRow candidateRows[] = peakListY.getRowsInsideScanAndMZRange(rtRange, mzRange);

      for (PeakListRow candidateRow : candidateRows) {
        if (file == null || file2 == null) {
          alignMol.addElement(new AlignStructMol(row, candidateRow));
        } else {
          if (candidateRow.getPeak(file2) != null) {
            alignMol.addElement(new AlignStructMol(row, candidateRow, file, file2));
          }
        }
      }
    }
    return alignMol;
  }

  private void updatePreview() {

    PeakList peakListX = peakListsComboX.getSelectionModel().getSelectedItem();
    PeakList peakListY = peakListsComboY.getSelectionModel().getSelectedItem();

    if ((peakListX == null) || (peakListY == null))
      return;

    // Select the rawDataFile which has more peaks in each peakList
    int numPeaks = 0;
    RawDataFile file = null;
    RawDataFile file2 = null;

    for (RawDataFile rfile : peakListX.getRawDataFiles()) {
      if (peakListX.getPeaks(rfile).size() > numPeaks) {
        numPeaks = peakListX.getPeaks(rfile).size();
        file = rfile;
      }
    }
    numPeaks = 0;
    for (RawDataFile rfile : peakListY.getRawDataFiles()) {
      if (peakListY.getPeaks(rfile).size() > numPeaks) {
        numPeaks = peakListY.getPeaks(rfile).size();
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
