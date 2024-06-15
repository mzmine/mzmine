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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.pseudospectrumvisualizer.PseudoSpectrumVisualizerPane;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.ComboComponent;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;

public class SpectralDeconvolutionGCDialog extends ParameterSetupDialog {

  private final ParameterSet parameters;
  private final SplitPane paramPreviewSplit;
  private final SplitPane clusteringSelectedFeatureSplit;
  private final BorderPane previewWrapperPane;
  private final BorderPane pseudoSpectrumPaneWrapper;
  private final Button updateButton;
  private final ComboComponent<Feature> deconvolutedFeaturesComboBox;

  private FeatureList featureList;
  private SpectralDeconvolutionPreviewPlot scatterPlot;
  private SpectralDeconvolutionAlgorithm spectralDeconvolutionAlgorithm;
  private RTTolerance rtTolerance;
  private Integer minNumberOfSignals;
  private PseudoSpectrumVisualizerPane pseudoSpectrumVisualizerPane;

  public SpectralDeconvolutionGCDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);
    this.parameters = parameters;
    setMinWidth(800);
    setMinHeight(600);
    paramPreviewSplit = new SplitPane();
    paramPreviewSplit.getItems().add(getParamPane());
    paramPreviewSplit.setOrientation(Orientation.HORIZONTAL);
    paramPreviewSplit.setDividerPositions(0.3);
    mainPane.setCenter(paramPreviewSplit);
    previewWrapperPane = new BorderPane();
    pseudoSpectrumPaneWrapper = new BorderPane();
    updateButton = new Button("Update preview");
    updateButton.setOnAction(_ -> updatePreview());
    scatterPlot = new SpectralDeconvolutionPreviewPlot("Spectral Deconvolution", "Retention Time",
        "m/z");
    clusteringSelectedFeatureSplit = new SplitPane();
    clusteringSelectedFeatureSplit.getItems().add(scatterPlot);
    clusteringSelectedFeatureSplit.setOrientation(Orientation.HORIZONTAL);
    previewWrapperPane.setCenter(clusteringSelectedFeatureSplit);
    deconvolutedFeaturesComboBox = new ComboComponent<>(FXCollections.observableArrayList());
    HBox buttonBox = new HBox(updateButton);
    buttonBox.getChildren().add(deconvolutedFeaturesComboBox);
    deconvolutedFeaturesComboBox.setOnAction(_ -> {
      updateSelectedFeature();
    });
    previewWrapperPane.setBottom(buttonBox);
    paramPreviewSplit.getItems().add(previewWrapperPane);
    addMouseClickListenerToScatterPlot();
  }


  private void addMouseClickListenerToScatterPlot() {
    ChartViewer chartViewer = scatterPlot;
    chartViewer.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX chartMouseEventFX) {
        XYPlot plot = (XYPlot) chartViewer.getChart().getPlot();
        double crosshairValue = plot.getDomainCrosshairValue();
        handleCrosshairClick(crosshairValue);
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX chartMouseEventFX) {

      }
    });
  }

  private void handleCrosshairClick(double rtValue) {
    // Logic to find the closest feature based on rtValue and update the selection
    Feature closestFeature = findClosestFeature(rtValue);
    if (closestFeature != null) {
      deconvolutedFeaturesComboBox.setValue(closestFeature);
      updateSelectedFeature();
    }
  }

  private Feature findClosestFeature(double rtValue) {
    List<Feature> features = deconvolutedFeaturesComboBox.getItems();
    return features.stream()
        .min(Comparator.comparingDouble(feature -> Math.abs(feature.getRT() - rtValue)))
        .orElse(null);
  }

  private void updateSelectedFeature() {
    ModularFeature selectedFeature = (ModularFeature) deconvolutedFeaturesComboBox.getSelectionModel()
        .getSelectedItem();
    pseudoSpectrumVisualizerPane = new PseudoSpectrumVisualizerPane(selectedFeature);
    pseudoSpectrumPaneWrapper.setCenter(pseudoSpectrumVisualizerPane);
    scatterPlot.addIntervalMarker(selectedFeature.getRawDataPointsRTRange());
  }

  private void updatePreview() {
    if (parameters.getValue(SpectralDeconvolutionGCParameters.FEATURE_LISTS)
        .getMatchingFeatureLists().length > 0) {
      featureList = parameters.getParameter(SpectralDeconvolutionGCParameters.FEATURE_LISTS)
          .getValue().getMatchingFeatureLists()[0];
      List<ModularFeature> features = featureList.getFeatures(featureList.getRawDataFile(0));
      spectralDeconvolutionAlgorithm = parameters.getValue(
          SpectralDeconvolutionGCParameters.SPECTRAL_DECONVOLUTION_ALGORITHM);
      rtTolerance = parameters.getValue(SpectralDeconvolutionGCParameters.RT_TOLERANCE);
      minNumberOfSignals = parameters.getValue(
          SpectralDeconvolutionGCParameters.MIN_NUMBER_OF_SIGNALS);
      List<List<ModularFeature>> groupedFeatures = SpectralDeconvolutionTools.groupFeatures(
          spectralDeconvolutionAlgorithm, features, rtTolerance, minNumberOfSignals);
      scatterPlot.clearDatasets();
      Random random = new Random();
      for (List<ModularFeature> gF : groupedFeatures) {
        XYSeries series = new XYSeries(
            "Group " + MZmineCore.getConfiguration().getRTFormat().format(gF.getFirst().getRT()));
        for (ModularFeature feature : gF) {
          series.add(feature.getRT(), feature.getMZ());
        }
        // Generate a random color
        Color color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
        scatterPlot.addDataset(series, color);
      }
      List<FeatureListRow> featureListRows = SpectralDeconvolutionTools.generatePseudoSpectra(
          features, featureList, rtTolerance, minNumberOfSignals, spectralDeconvolutionAlgorithm);
      ObservableList<Feature> resultFeatures = featureListRows.stream()
          .map(row -> row.getFeature(featureList.getRawDataFile(0)))
          .filter(Objects::nonNull) // Ensure null features are not included
          .collect(Collectors.toCollection(FXCollections::observableArrayList));
      resultFeatures.sort(Comparator.comparingDouble(Feature::getRT));
      deconvolutedFeaturesComboBox.setItems(resultFeatures);
      deconvolutedFeaturesComboBox.setValue(resultFeatures.getFirst());
      PseudoSpectrumVisualizerPane pseudoSpectrumVisualizerPane = new PseudoSpectrumVisualizerPane(
          (ModularFeature) deconvolutedFeaturesComboBox.getSelectionModel().getSelectedItem());
      pseudoSpectrumPaneWrapper.setCenter(pseudoSpectrumVisualizerPane);
      clusteringSelectedFeatureSplit.getItems().add(pseudoSpectrumPaneWrapper);
      scatterPlot.addIntervalMarker(resultFeatures.getFirst().getRawDataPointsRTRange());
    } else {
      MZmineCore.getDesktop().displayMessage("No feature list selected. Cannot show preview");
    }
  }

  @Override
  protected void parametersChanged() {
    updateParameterSetFromComponents();
    super.parametersChanged();
  }
}
