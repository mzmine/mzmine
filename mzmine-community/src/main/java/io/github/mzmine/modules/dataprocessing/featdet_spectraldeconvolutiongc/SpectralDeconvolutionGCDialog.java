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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.pseudospectrumvisualizer.PseudoSpectrumVisualizerPane;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.ComboComponent;
import io.github.mzmine.taskcontrol.SimpleRunnableTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;

public class SpectralDeconvolutionGCDialog extends ParameterSetupDialog {

  private static final Color DOMAIN_MARKER_COLOR = new Color(200, 200, 255, 100);
  private static final Color TOLERANCE_MARKER_COLOR = new Color(255, 128, 0, 100);
  public static final NumberFormats GUI_FORMATS = ConfigService.getGuiFormats();

  private final ParameterSet parameters;
  private final SplitPane paramPreviewSplit;
  private final SplitPane clusteringSelectedFeatureSplit;
  private final BorderPane previewWrapperPane;
  private final BorderPane pseudoSpectrumPaneWrapper;
  private final BorderPane scatterPlotBorderPane;
  private final HBox scatterPlotLegend;
  private final ComboComponent<Feature> deconvolutedFeaturesComboBox;
  private final Label numberOfCompoundsLabel;
  private final Label selectedFeatureGroupLabel;
  private final Label preparingPreviewLabel;
  private final SpectralDeconvolutionPreviewPlot scatterPlot;
  private final Button updateButton;

  private FeatureList featureList;
  private SpectralDeconvolutionAlgorithm spectralDeconvolutionAlgorithm;
  private List<Range<Double>> mzValuesToIgnore;
  private PseudoSpectrumVisualizerPane pseudoSpectrumVisualizerPane;
  private List<ModularFeature> allFeatures;
  private List<List<ModularFeature>> groupedFeatures;
  private Feature closestFeatureGroup;

  public SpectralDeconvolutionGCDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);
    this.parameters = parameters;
    setMinWidth(1000);
    setMinHeight(800);

    paramPreviewSplit = new SplitPane();
    paramPreviewSplit.getItems().add(getParamPane());
    paramPreviewSplit.setOrientation(Orientation.HORIZONTAL);
    paramPreviewSplit.setDividerPositions(1.0);
    mainPane.setCenter(paramPreviewSplit);

    previewWrapperPane = new BorderPane();
    pseudoSpectrumPaneWrapper = new BorderPane();
    scatterPlot = new SpectralDeconvolutionPreviewPlot("Spectral Deconvolution",
        GUI_FORMATS.unit("Retention time", "min"), "m/z");
    NumberAxis domainAxis = (NumberAxis) scatterPlot.getChart().getXYPlot().getDomainAxis();
    domainAxis.setNumberFormatOverride(ConfigService.getGuiFormats().rtFormat());

    NumberAxis rangeAxis = (NumberAxis) scatterPlot.getChart().getXYPlot().getRangeAxis();
    rangeAxis.setNumberFormatOverride(ConfigService.getGuiFormats().mzFormat());

    scatterPlotBorderPane = new BorderPane();
    scatterPlotBorderPane.setCenter(scatterPlot);
    scatterPlotLegend = buildScatterPlotLegend();
    scatterPlotBorderPane.setBottom(scatterPlotLegend);
    clusteringSelectedFeatureSplit = new SplitPane();
    clusteringSelectedFeatureSplit.getItems().add(scatterPlotBorderPane);
    clusteringSelectedFeatureSplit.setOrientation(Orientation.VERTICAL);
    previewWrapperPane.setCenter(clusteringSelectedFeatureSplit);
    previewWrapperPane.setVisible(false); // Initially invisible

    deconvolutedFeaturesComboBox = new ComboComponent<>(FXCollections.observableArrayList());
    HBox buttonBox = FxLayout.newHBox(deconvolutedFeaturesComboBox);
    deconvolutedFeaturesComboBox.setOnAction(_ -> updateSelectedFeature());
    previewWrapperPane.setBottom(buttonBox);

    paramPreviewSplit.getItems().add(previewWrapperPane);
    numberOfCompoundsLabel = new Label("Number of compounds: ");
    selectedFeatureGroupLabel = new Label("Selected rt group: ");
    VBox labelVBox = FxLayout.newVBox(numberOfCompoundsLabel, selectedFeatureGroupLabel);
    buttonBox.getChildren().add(labelVBox);

    preparingPreviewLabel = new Label("Preparing preview");
    preparingPreviewLabel.setStyle("-fx-font-size: 24px;");
    preparingPreviewLabel.setVisible(false);
    previewWrapperPane.setTop(preparingPreviewLabel);

    addMouseClickListenerToScatterPlot();

    // Add the update button to the button bar of the parameter pane
    updateButton = new Button("Update preview");
    updateButton.setOnAction(_ -> updatePreview());
    getParamPane().getButtonBar().getButtons().add(updateButton);
  }

  private HBox buildScatterPlotLegend() {
    HBox legendBox = new HBox(20);

    // Create the legend item for the main feature RT range
    Label mainFeatureRtRangeLbl = new Label("Compound RT range");
    Rectangle mainFeatureRtRangeRect = new Rectangle(20, 20,
        FxColorUtil.awtColorToFX(DOMAIN_MARKER_COLOR));
    HBox mainFeatureLegendItem = FxLayout.newHBox(mainFeatureRtRangeRect, mainFeatureRtRangeLbl);

    // Create the legend item for the RT tolerance range
    Label rtToleranceLbl = new Label("RT tolerance range");
    Rectangle rtToleranceRect = new Rectangle(20, 20,
        FxColorUtil.awtColorToFX(TOLERANCE_MARKER_COLOR));
    HBox rtToleranceLegendItem = FxLayout.newHBox(rtToleranceRect, rtToleranceLbl);

    legendBox.getChildren().addAll(mainFeatureLegendItem, rtToleranceLegendItem);
    legendBox.setAlignment(Pos.CENTER);
    legendBox.setPadding(new Insets(10, 0, 10, 0)); // 10 pixels padding at top and bottom
    return legendBox;
  }

  private void addMouseClickListenerToScatterPlot() {
    ChartViewer chartViewer = scatterPlot;
    chartViewer.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        XYPlot plot = (XYPlot) chartViewer.getChart().getPlot();
        double crosshairXValue = plot.getDomainCrosshairValue();
        double crosshairYValue = plot.getRangeCrosshairValue();
        handleCrosshairClick(crosshairXValue, crosshairYValue);
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {
      }
    });
  }

  private void handleCrosshairClick(double rtValue, double mzValue) {
    Feature closestFeatureGroupNew = findClosestFeatureGroup(rtValue, mzValue);
    if (closestFeatureGroupNew != null && closestFeatureGroup != closestFeatureGroupNew) {
      closestFeatureGroup = closestFeatureGroupNew;
      deconvolutedFeaturesComboBox.getSelectionModel().select(closestFeatureGroup);
      updateSelectedFeature();
    }
  }

  private Feature findClosestFeatureGroup(double rtValue, double mzValue) {
    Feature closestFeature = allFeatures.stream().min(Comparator.comparingDouble(
        feature -> calculateEuclideanDistance(feature, rtValue, mzValue))).orElse(null);

    if (closestFeature != null) {
      List<Range<Double>> adjustedRanges = SpectralDeconvolutionUtils.getAdjustedRanges(
          mzValuesToIgnore);
      for (List<ModularFeature> features : groupedFeatures) {
        for (ModularFeature feature : features) {
          if (feature.equals(closestFeature)) {
            return SpectralDeconvolutionUtils.getMainFeature(features, adjustedRanges);
          }
        }
      }
    }
    return null;
  }

  private double calculateEuclideanDistance(Feature feature, double rtValue, double mzValue) {
    double rtDiff = feature.getRT() - rtValue;
    double mzDiff = feature.getMZ() - mzValue;
    return Math.sqrt(rtDiff * rtDiff + mzDiff * mzDiff);
  }

  private void updateSelectedFeature() {
    ModularFeature selectedFeature = (ModularFeature) deconvolutedFeaturesComboBox.getSelectionModel()
        .getSelectedItem();
    if (selectedFeature != null) {
      pseudoSpectrumVisualizerPane = new PseudoSpectrumVisualizerPane(selectedFeature);
      pseudoSpectrumPaneWrapper.setCenter(pseudoSpectrumVisualizerPane);
      scatterPlot.getChart().getXYPlot().clearDomainMarkers();
      scatterPlot.addIntervalMarker(selectedFeature.getRawDataPointsRTRange(), DOMAIN_MARKER_COLOR);
      scatterPlot.addIntervalMarker(spectralDeconvolutionAlgorithm.getRtTolerance()
          .getToleranceRange(selectedFeature.getRT()), TOLERANCE_MARKER_COLOR);
      selectedFeatureGroupLabel.setText(
          "Selected RT group: " + ConfigService.getGuiFormats().rtFormat()
              .format(selectedFeature.getRT()) + " min");
    }
  }

  private void updatePreview() {
    if (!checkParameterValues(true, false)) {
      return;
    }
    if (parameters.getValue(SpectralDeconvolutionGCParameters.FEATURE_LISTS)
            .getMatchingFeatureLists().length > 0) {

      initializeParameters();

      updateButton.setDisable(true);
      preparingPreviewLabel.setVisible(true);
      previewWrapperPane.setVisible(true);
      scatterPlot.setVisible(false);

      paramPreviewSplit.setDividerPositions(0.4); // Set divider position after button click

      var groupFeaturesTask = new SimpleRunnableTask(
          () -> groupedFeatures = spectralDeconvolutionAlgorithm.groupFeatures(allFeatures));

      TaskStatusListener taskStatusListener = new TaskStatusListener() {
        @Override
        public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
          if (newStatus.equals(TaskStatus.FINISHED)) {
            FxThread.runLater(() -> {
              populateScatterPlot();
              updateFeatureComboBox();
              updateSelectedFeature();
              updateButton.setDisable(false);
              preparingPreviewLabel.setVisible(false);
              scatterPlot.setVisible(true);
            });
          }
        }
      };
      groupFeaturesTask.addTaskStatusListener(taskStatusListener);
      TaskService.getController().addTask(groupFeaturesTask);
    } else {
      MZmineCore.getDesktop().displayMessage("No feature list selected. Cannot show preview");
    }
  }

  private void initializeParameters() {
    featureList = parameters.getParameter(SpectralDeconvolutionGCParameters.FEATURE_LISTS)
        .getValue().getMatchingFeatureLists()[0];
    allFeatures = featureList.getFeatures(featureList.getRawDataFile(0));
    var deconParams = parameters.getParameter(
            SpectralDeconvolutionGCParameters.SPECTRAL_DECONVOLUTION_ALGORITHM)
        .getValueWithParameters();
    spectralDeconvolutionAlgorithm = SpectralDeconvolutionAlgorithms.createOption(deconParams);
    if (parameters.getParameter(SpectralDeconvolutionGCParameters.MZ_VALUES_TO_IGNORE).getValue()) {
      mzValuesToIgnore = parameters.getParameter(
          SpectralDeconvolutionGCParameters.MZ_VALUES_TO_IGNORE).getEmbeddedParameter().getValue();
    } else {
      mzValuesToIgnore = null;
    }
  }

  private void populateScatterPlot() {
    FxThread.runLater(() -> {
      scatterPlot.clearDatasets();
      SimpleColorPalette colorPalette = ConfigService.getDefaultColorPalette();
      groupedFeatures.sort(Comparator.comparingDouble(group -> group.getFirst().getRT()));
      for (int i = 0; i < groupedFeatures.size(); i++) {
        List<ModularFeature> group = groupedFeatures.get(i);
        XYSeries series = new XYSeries("Group " + MZmineCore.getConfiguration().getRTFormat()
            .format(group.getFirst().getRT()));
        for (ModularFeature feature : group) {
          series.add(feature.getRT(), feature.getMZ());
        }
        Color color = FxColorUtil.fxColorToAWT(colorPalette.get(i % colorPalette.size()));
        scatterPlot.addDataset(series, color);
      }
    });
  }

  private void updateFeatureComboBox() {
    List<List<ModularFeature>> groupedFeatures = spectralDeconvolutionAlgorithm.groupFeatures(
        allFeatures);
    List<FeatureListRow> featureListRows = SpectralDeconvolutionUtils.generatePseudoSpectra(
        groupedFeatures, featureList, mzValuesToIgnore);

    ObservableList<Feature> resultFeatures = featureListRows.stream()
        .map(row -> row.getFeature(featureList.getRawDataFile(0))).filter(Objects::nonNull)
        .sorted(Comparator.comparingDouble(Feature::getRT))
        .collect(Collectors.toCollection(FXCollections::observableArrayList));
    deconvolutedFeaturesComboBox.setItems(resultFeatures);
    if (!resultFeatures.isEmpty()) {
      deconvolutedFeaturesComboBox.setValue(resultFeatures.getFirst());
    }
    if (!clusteringSelectedFeatureSplit.getItems().contains(pseudoSpectrumPaneWrapper)) {
      clusteringSelectedFeatureSplit.getItems().add(pseudoSpectrumPaneWrapper);
    }
    numberOfCompoundsLabel.setText("Number of compounds: " + resultFeatures.size());

  }

  @Override
  protected void parametersChanged() {
    updateParameterSetFromComponents();
    super.parametersChanged();
  }
}