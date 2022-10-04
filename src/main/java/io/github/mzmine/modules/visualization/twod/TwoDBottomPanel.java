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

package io.github.mzmine.modules.visualization.twod;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.Collections;
import java.util.Vector;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * 2D visualizer's bottom panel
 */
class TwoDBottomPanel extends HBox {


  private final ComboBox<FeatureList> featureListSelector;
  private final ComboBox<FeatureThresholdMode> thresholdCombo;
  private final TextField featureTextField;
  private final Button loadButton;
  private FeatureThresholdParameter thresholdSettings;

  private TwoDVisualizerTab masterFrame;
  private RawDataFile dataFile;

  private final MemoryMapStorage flistStorage = MemoryMapStorage.forFeatureList();

  TwoDBottomPanel(TwoDVisualizerTab masterFrame, RawDataFile dataFile, ParameterSet parameters) {

    this.dataFile = dataFile;
    this.masterFrame = masterFrame;

    // setBackground(Color.white);
    // setBorder(new EmptyBorder(5, 5, 5, 0));

    //
    thresholdCombo = new ComboBox<>(
        FXCollections.observableArrayList(FeatureThresholdMode.values()));
    // thresholdCombo.setBackground(Color.white);
    // thresholdCombo.setFont(smallFont);

    featureTextField = new TextField();
    // peakTextField.setPreferredSize(new Dimension(50, 15));
    // peakTextField.setFont(smallFont);
    // peakTextField.addActionListener(this);

    featureListSelector = new ComboBox<FeatureList>(FXCollections.observableArrayList(
        MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists()));
    // peakListSelector.setBackground(Color.white);
    // peakListSelector.setFont(smallFont);

    loadButton = new Button("Load");

    thresholdSettings = parameters.getParameter(TwoDVisualizerParameters.featureThresholdSettings);

    thresholdCombo.getSelectionModel().select(thresholdSettings.getMode());

    getChildren().addAll(new Label("Show: "), thresholdCombo, new Label("Value: "),
        featureTextField,
        new Label(" from feature list: "), featureListSelector, loadButton);

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
    });

    loadButton.setOnAction(e -> {
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
      if (selectedFeatureList != null)
        masterFrame.getPlot().loadFeatureList(selectedFeatureList);

    });

  }

  /**
   * Returns a feature list different features depending on the selected option of the "feature Threshold"
   * combo box
   */
  FeatureList getFeaturesInThreshold() {

    FeatureList selectedFeatureList = featureListSelector.getSelectionModel().getSelectedItem();
    FeatureThresholdMode mode = thresholdCombo.getSelectionModel().getSelectedItem();

    switch (mode) {
      case ABOVE_INTENSITY_FEATURES:
        double threshold = thresholdSettings.getIntensityThreshold();
        return getIntensityThresholdFeatureList(threshold);

      case ALL_FEATURES:
        return selectedFeatureList;
      case TOP_FEATURES:
      case TOP_FEATURES_AREA:
        int topFeature = thresholdSettings.getTopFeaturesThreshold();
        return getTopThresholdFeatureList(topFeature);
    }

    return null;
  }

  /**
   * Returns a feature list with the features which intensity is above the parameter "intensity"
   */
  FeatureList getIntensityThresholdFeatureList(double intensity) {
    FeatureList selectedFeatureList = featureListSelector.getSelectionModel().getSelectedItem();
    if (selectedFeatureList == null)
      return null;
    ModularFeatureList newList =
        new ModularFeatureList(selectedFeatureList.getName(), flistStorage, selectedFeatureList.getRawDataFiles());

    for (FeatureListRow featureListRow : selectedFeatureList.getRows()) {
      Feature feature = featureListRow.getFeature(dataFile);
      if (feature == null)
        continue;
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
    if (selectedFeatureList == null)
      return null;
    ModularFeatureList newList =
        new ModularFeatureList(selectedFeatureList.getName(), flistStorage, selectedFeatureList.getRawDataFiles());

    Vector<FeatureListRow> featureRows = new Vector<FeatureListRow>();

    Range<Double> mzRange = selectedFeatureList.getRowsMZRange();
    Range<Float> rtRange = selectedFeatureList.getRowsRTRange();

    FeatureThresholdMode selectedFeatureOption = thresholdCombo.getSelectionModel().getSelectedItem();
    if (selectedFeatureOption == FeatureThresholdMode.TOP_FEATURES_AREA) {
      mzRange = masterFrame.getPlot().getXYPlot().getAxisRange();
      rtRange = RangeUtils.toFloatRange(masterFrame.getPlot().getXYPlot().getDomainRange());
    }

    for (FeatureListRow featureListRow : selectedFeatureList.getRows()) {
      if (mzRange.contains(featureListRow.getAverageMZ()) && rtRange.contains(featureListRow.getAverageRT())) {
        featureRows.add(featureListRow);
      }
    }

    Collections.sort(featureRows,
        new FeatureListRowSorter(SortingProperty.Intensity, SortingDirection.Descending));

    if (threshold > featureRows.size())
      threshold = featureRows.size();
    for (int i = 0; i < threshold; i++) {
      newList.addRow(featureRows.elementAt(i));
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
