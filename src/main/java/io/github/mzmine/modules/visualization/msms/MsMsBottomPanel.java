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

package io.github.mzmine.modules.visualization.msms;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Vector;
import java.util.logging.Logger;
import org.jfree.chart.plot.XYPlot;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.PeakListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * MS/MS visualizer's bottom panel
 */
class MsMsBottomPanel extends HBox {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

  private ComboBox<PeakList> peakListSelector;
  private ComboBox<PeakThresholdMode> thresholdCombo;
  private TextField peakTextField;
  private PeakThresholdParameter thresholdSettings;

  private MsMsVisualizerWindow masterFrame;
  private RawDataFile dataFile;

  MsMsBottomPanel(MsMsVisualizerWindow masterFrame, RawDataFile dataFile, ParameterSet parameters) {

    this.dataFile = dataFile;
    this.masterFrame = masterFrame;

    // setBackground(Color.white);
    // setBorder(new EmptyBorder(5, 5, 5, 0));

    // add(Box.createHorizontalGlue());


    thresholdCombo = new ComboBox<>(FXCollections.observableArrayList(PeakThresholdMode.values()));
    // thresholdCombo.getSelectionModel().select(PeakThresholdMode.NONE);
    // thresholdCombo.setBackground(Color.white);
    // thresholdCombo.setFont(smallFont);
    thresholdCombo.addActionListener(this);


    peakTextField = new TextField();
    // peakTextField.setPreferredSize(new Dimension(50, 15));
    // peakTextField.setFont(smallFont);
    peakTextField.addActionListener(this);


    peakListSelector =
        new ComboBox<>(MZmineCore.getProjectManager().getCurrentProject().getFeatureLists());
    // peakListSelector.setBackground(Color.white);
    // peakListSelector.setFont(smallFont);
    // peakListSelector.setActionCommand("PEAKLIST_CHANGE");

    thresholdSettings = parameters.getParameter(MsMsParameters.peakThresholdSettings);

    thresholdCombo.getSelectionModel().select(thresholdSettings.getMode());

    // add(Box.createHorizontalStrut(10));

    // add(Box.createHorizontalGlue());
    getChildren().addAll( //
        new Label("Show: "), //
        thresholdCombo, //
        new Label("Value: "), //
        peakTextField, //
        new Label(" from feature list: ") //
    );

  }

  /**
   * Returns a feature list different peaks depending on the selected option of the "peak Threshold"
   * combo box
   */
  PeakList getPeaksInThreshold() {

    PeakList selectedPeakList = peakListSelector.getSelectionModel().getSelectedItem();
    PeakThresholdMode mode = thresholdCombo.getSelectionModel().getSelectedItem();

    switch (mode) {
      case ABOVE_INTENSITY_PEAKS:
        double threshold = thresholdSettings.getIntensityThreshold();
        return getIntensityThresholdPeakList(threshold);

      case ALL_PEAKS:
        return selectedPeakList;
      case TOP_PEAKS:
      case TOP_PEAKS_AREA:
        int topPeaks = thresholdSettings.getTopPeaksThreshold();
        return getTopThresholdPeakList(topPeaks);
    }

    return null;
  }

  /**
   * Returns a feature list with the peaks which intensity is above the parameter "intensity"
   */
  PeakList getIntensityThresholdPeakList(double intensity) {
    PeakList selectedPeakList = peakListSelector.getSelectionModel().getSelectedItem();
    if (selectedPeakList == null)
      return null;
    SimplePeakList newList =
        new SimplePeakList(selectedPeakList.getName(), selectedPeakList.getRawDataFiles());

    for (PeakListRow peakRow : selectedPeakList.getRows()) {
      Feature peak = peakRow.getPeak(dataFile);
      if (peak == null)
        continue;
      if (peak.getRawDataPointsIntensityRange().upperEndpoint() > intensity) {
        newList.addRow(peakRow);
      }
    }
    return newList;
  }

  /**
   * Returns a feature list with the top peaks defined by the parameter "threshold"
   */
  PeakList getTopThresholdPeakList(int threshold) {

    PeakList selectedPeakList = peakListSelector.getSelectionModel().getSelectedItem();
    if (selectedPeakList == null)
      return null;
    SimplePeakList newList =
        new SimplePeakList(selectedPeakList.getName(), selectedPeakList.getRawDataFiles());

    Vector<PeakListRow> peakRows = new Vector<PeakListRow>();

    Range<Double> mzRange = selectedPeakList.getRowsMZRange();
    Range<Double> rtRange = selectedPeakList.getRowsRTRange();

    PeakThresholdMode selectedPeakOption = thresholdCombo.getSelectionModel().getSelectedItem();
    if (selectedPeakOption == PeakThresholdMode.TOP_PEAKS_AREA) {
      XYPlot xyPlot = masterFrame.getPlot().getXYPlot();
      org.jfree.data.Range yAxis = xyPlot.getRangeAxis().getRange();
      org.jfree.data.Range xAxis = xyPlot.getDomainAxis().getRange();
      rtRange = Range.closed(xAxis.getLowerBound(), xAxis.getUpperBound());
      mzRange = Range.closed(yAxis.getLowerBound(), yAxis.getUpperBound());
    }

    for (PeakListRow peakRow : selectedPeakList.getRows()) {
      if (mzRange.contains(peakRow.getAverageMZ()) && rtRange.contains(peakRow.getAverageRT())) {
        peakRows.add(peakRow);
      }
    }

    Collections.sort(peakRows,
        new PeakListRowSorter(SortingProperty.Intensity, SortingDirection.Descending));

    if (threshold > peakRows.size())
      threshold = peakRows.size();
    for (int i = 0; i < threshold; i++) {
      newList.addRow(peakRows.elementAt(i));
    }
    return newList;
  }

  /**
   * Returns selected feature list
   */
  PeakList getSelectedPeakList() {
    PeakList selectedPeakList = peakListSelector.getSelectionModel().getSelectedItem();
    return selectedPeakList;
  }



  @Override
  public void actionPerformed(ActionEvent e) {

    Object src = e.getSource();

    if (src == thresholdCombo) {

      PeakThresholdMode mode = thresholdCombo.getSelectionModel().getSelectedItem();

      switch (mode) {
        case ABOVE_INTENSITY_PEAKS:
          peakTextField.setText(String.valueOf(thresholdSettings.getIntensityThreshold()));
          peakTextField.setDisable(false);
          break;
        case ALL_PEAKS:
          peakTextField.setDisable(true);
          break;
        case TOP_PEAKS:
        case TOP_PEAKS_AREA:
          peakTextField.setText(String.valueOf(thresholdSettings.getTopPeaksThreshold()));
          peakTextField.setDisable(false);
          break;
      }

      thresholdSettings.setMode(mode);

    }

    if (src == peakTextField) {
      PeakThresholdMode mode = thresholdCombo.getSelectionModel().getSelectedItem();
      String value = peakTextField.getText();
      switch (mode) {
        case ABOVE_INTENSITY_PEAKS:
          double topInt = Double.parseDouble(value);
          thresholdSettings.setIntensityThreshold(topInt);
          break;
        case TOP_PEAKS:
        case TOP_PEAKS_AREA:
          int topPeaks = Integer.parseInt(value);
          thresholdSettings.setTopPeaksThreshold(topPeaks);
          break;
        default:
          break;
      }
    }

    PeakList selectedPeakList = getPeaksInThreshold();
    if (selectedPeakList != null)
      masterFrame.getPlot().loadPeakList(selectedPeakList);

  }



}
