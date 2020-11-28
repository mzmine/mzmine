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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.Feature;
import io.github.mzmine.datamodel.data.FeatureList;
import io.github.mzmine.datamodel.data.FeatureListRow;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.awt.Font;
import java.util.Collections;
import java.util.Vector;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.jfree.chart.plot.XYPlot;

/**
 * MS/MS visualizer's bottom panel
 */
class MsMsBottomPanel extends HBox {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

  private ComboBox<FeatureList> featureListSelector;
  private ComboBox<FeatureThresholdMode> thresholdCombo;
  private TextField featureTextField;
  private FeatureThresholdParameter thresholdSettings;

  private MsMsVisualizerTab masterFrame;
  private RawDataFile dataFile;

  MsMsBottomPanel(MsMsVisualizerTab masterFrame, RawDataFile dataFile, ParameterSet parameters) {

    this.dataFile = dataFile;
    this.masterFrame = masterFrame;

    thresholdCombo = new ComboBox<>(FXCollections.observableArrayList(FeatureThresholdMode.values()));
    thresholdCombo.setOnAction(e -> {
      FeatureThresholdMode mode = thresholdCombo.getSelectionModel().getSelectedItem();

      switch (mode) {
        case ABOVE_INTENSITY_FEATURES:
          featureTextField.setText(String.valueOf(thresholdSettings.getIntensityThreshold()));
          featureTextField.setDisable(false);
          break;
        case ALL_FEATURES:
          featureTextField.setDisable(true);
          break;
        case TOP_FEATURES:
        case TOP_FEATURES_AREA:
          featureTextField.setText(String.valueOf(thresholdSettings.getTopFeaturesThreshold()));
          featureTextField.setDisable(false);
          break;
      }

      thresholdSettings.setMode(mode);
      FeatureList selectedFeatureList = getFeaturesInThreshold();
      if (selectedFeatureList != null) {
        masterFrame.getPlot().loadFeatureList(selectedFeatureList);
      }
    });

    featureTextField = new TextField();
    featureTextField.setOnAction(e -> {
      FeatureThresholdMode mode = thresholdCombo.getSelectionModel().getSelectedItem();
      String value = featureTextField.getText();
      switch (mode) {
        case ABOVE_INTENSITY_FEATURES:
          double topInt = Double.parseDouble(value);
          thresholdSettings.setIntensityThreshold(topInt);
          break;
        case TOP_FEATURES:
        case TOP_FEATURES_AREA:
          int topFeatures = Integer.parseInt(value);
          thresholdSettings.setTopFeaturesThreshold(topFeatures);
          break;
        default:
          break;
      }
      FeatureList selectedFeatureList = getFeaturesInThreshold();
      if (selectedFeatureList != null) {
        masterFrame.getPlot().loadFeatureList(selectedFeatureList);
      }

    });

    featureListSelector =
        new ComboBox<>(MZmineCore.getProjectManager().getCurrentProject().getFeatureLists());

    thresholdSettings = parameters.getParameter(MsMsParameters.featureThresholdSettings);

    thresholdCombo.getSelectionModel().select(thresholdSettings.getMode());
    setSpacing(10);
    getChildren().addAll( //
        new Label("Show: "), //
        thresholdCombo, //
        new Label("Value: "), //
        featureTextField, //
        new Label(" from feature list: ") //
    );

  }

  /**
   * Returns a feature list different features depending on the selected option of the "peak Threshold"
   * combo box
   */
  FeatureList getFeaturesInThreshold() {

    FeatureList selectedFeatureList = featureListSelector.getSelectionModel().getSelectedItem();
    FeatureThresholdMode mode = thresholdCombo.getSelectionModel().getSelectedItem();

    switch (mode) {
      case ABOVE_INTENSITY_FEATURES:
        double threshold = thresholdSettings.getIntensityThreshold();
        return getIntensityThresholdPeakList(threshold);

      case ALL_FEATURES:
        return selectedFeatureList;
      case TOP_FEATURES:
      case TOP_FEATURES_AREA:
        int topFeatures = thresholdSettings.getTopFeaturesThreshold();
        return getTopThresholdFeatureList(topFeatures);
    }

    return null;
  }

  /**
   * Returns a feature list with the peaks which intensity is above the parameter "intensity"
   */
  FeatureList getIntensityThresholdPeakList(double intensity) {
    FeatureList selectedFeatureList = featureListSelector.getSelectionModel().getSelectedItem();
    if (selectedFeatureList == null) {
      return null;
    }
    ModularFeatureList newList =
        new ModularFeatureList(selectedFeatureList.getName(), selectedFeatureList.getRawDataFiles());

    for (FeatureListRow featureListRow : selectedFeatureList.getRows()) {
      Feature feature = featureListRow.getFeature(dataFile);
      if (feature == null) {
        continue;
      }
      if (feature.getRawDataPointsIntensityRange().upperEndpoint() > intensity) {
        newList.addRow(featureListRow);
      }
    }
    return newList;
  }

  /**
   * Returns a feature list with the top peaks defined by the parameter "threshold"
   */
  FeatureList getTopThresholdFeatureList(int threshold) {

    FeatureList selectedFeatureList = featureListSelector.getSelectionModel().getSelectedItem();
    if (selectedFeatureList == null) {
      return null;
    }
    ModularFeatureList newList =
        new ModularFeatureList(selectedFeatureList.getName(), selectedFeatureList.getRawDataFiles());

    Vector<FeatureListRow> featureListRows = new Vector<FeatureListRow>();

    Range<Double> mzRange = selectedFeatureList.getRowsMZRange();
    Range<Float> rtRange = selectedFeatureList.getRowsRTRange();

    FeatureThresholdMode selectedFeatureOption = thresholdCombo.getSelectionModel().getSelectedItem();
    if (selectedFeatureOption == FeatureThresholdMode.TOP_FEATURES_AREA) {
      XYPlot xyPlot = masterFrame.getPlot().getXYPlot();
      org.jfree.data.Range yAxis = xyPlot.getRangeAxis().getRange();
      org.jfree.data.Range xAxis = xyPlot.getDomainAxis().getRange();
      rtRange = Range.closed((float) xAxis.getLowerBound(), (float) xAxis.getUpperBound());
      mzRange = Range.closed(yAxis.getLowerBound(), yAxis.getUpperBound());
    }

    for (FeatureListRow featureListRow : selectedFeatureList.getRows()) {
      if (mzRange.contains(featureListRow.getAverageMZ()) && rtRange.contains(featureListRow.getAverageRT())) {
        featureListRows.add(featureListRow);
      }
    }

    Collections.sort(featureListRows,
        new FeatureListRowSorter(SortingProperty.Intensity, SortingDirection.Descending));

    if (threshold > featureListRows.size()) {
      threshold = featureListRows.size();
    }
    for (int i = 0; i < threshold; i++) {
      newList.addRow(featureListRows.elementAt(i));
    }
    return newList;
  }

  /**
   * Returns selected feature list
   */
  FeatureList getSelectedFeatureList() {
    FeatureList selectedFeatureList = featureListSelector.getSelectionModel().getSelectedItem();
    return selectedFeatureList;
  }


}
