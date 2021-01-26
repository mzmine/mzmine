/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.visualization.imsfeaturevisualizer;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.MultiDatasetXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYZValueProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.ScanBPCProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.FeaturesToMobilityMzHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.IonMobilityUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.plot.ValueMarker;

public class IMSFeaturesVisualizer extends BorderPane {

  private final SimpleXYChart<IonTimeSeriesToXYProvider> ticChart;
  private final Map<ModularFeature, SingleIMSFeatureVisualiserPane> featureVisualisersMap;
  private final MultiDatasetXYZScatterPlot<SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider> heatmap;
  private final ScrollPane scrollPane;
  private final VBox content;

  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;
  private final UnitFormat unitFormat;

  private final Stroke markerStroke = new BasicStroke(1f);
  private final Paint markerColor = MZmineCore.getConfiguration().getDefaultColorPalette()
      .getPositiveColorAWT();

  private final ObjectProperty<Scan> selectedScan;
  private final ObservableList<Path2D> finishedRegions;
  private final Stroke roiStroke = new BasicStroke(1f);
  private final Paint roiPaint = MZmineCore.getConfiguration().getDefaultColorPalette()
      .getNegativeColorAWT();
  private List<RawDataFile> rawDataFiles;
  private boolean isCtrlPressed = false;

  private Collection<ModularFeature> features;

  public IMSFeaturesVisualizer() {
    super();
    getStylesheets().addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    getStyleClass().add(".region-match-chart-bg");

    ticChart = new SimpleXYChart<>();
    heatmap = new MultiDatasetXYZScatterPlot<>();
    featureVisualisersMap = new LinkedHashMap<>();
    rawDataFiles = new ArrayList<>();
    finishedRegions = FXCollections.observableArrayList();
//    isDrawing = new SimpleBooleanProperty(false);

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    initCharts();
//    this.setOnKeyPressed(e -> {
//      if(e.getCode() == KeyCode.CONTROL) {
//        isCtrlPressed = true;
//      }
//    });
//    this.setOnKeyReleased(e -> {
//      if(e.getCode() == KeyCode.CONTROL) {
//        isCtrlPressed = false;
//      }
//    });

    selectedScan = new SimpleObjectProperty<>();
    selectedScan.addListener((observable, oldValue, newValue) -> updateValueMarkers(newValue));

    scrollPane = new ScrollPane();
    scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
    content = new VBox();
    content.maxWidthProperty().bind(scrollPane.widthProperty().subtract(10));
    content.minWidthProperty().bind(scrollPane.widthProperty().subtract(10));
    scrollPane.setContent(content);

    BorderPane selectedFeaturesPane = new BorderPane();
    selectedFeaturesPane.setTop(ticChart);
    selectedFeaturesPane.setCenter(scrollPane);
    selectedFeaturesPane.setMinWidth(500);
    setRight(selectedFeaturesPane);
    setCenter(heatmap);

    initChartListeners();
    initSelectionPane();
  }

  private void initCharts() {
    heatmap.setDomainAxisLabel("m/z");
    heatmap.setDomainAxisNumberFormatOverride(mzFormat);
    heatmap.setRangeAxisLabel("Mobility");
    heatmap.setRangeAxisNumberFormatOverride(mobilityFormat);
    heatmap.setLegendAxisLabel(unitFormat.format("Intensity", "counts"));
    heatmap.setLegendNumberFormatOverride(intensityFormat);
    heatmap.getXYPlot().setBackgroundPaint(Color.BLACK);
    heatmap.getXYPlot().setDomainCrosshairPaint(Color.LIGHT_GRAY);
    heatmap.getXYPlot().setRangeCrosshairPaint(Color.LIGHT_GRAY);
//    heatmap.getXYPlot().setBackgroundPaint(Color.BLACK);
    ticChart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    ticChart.setDomainAxisNumberFormatOverride(rtFormat);
    ticChart.setRangeAxisNumberFormatOverride(intensityFormat);
    ticChart.setRangeAxisLabel(unitFormat.format("Intensity", "counts"));
    ticChart.setShowCrosshair(false);
    ticChart.getChart().setTitle("Extracted ion chromatograms");
    ticChart.setMinHeight(250);
  }

  private void initSelectionPane() {
    final GridPane selectionControls = new GridPane();

    Button btnStartRegion = new Button("Start region");
    Button btnFinishRegion = new Button("Finish region");
    Button btnCancelRegion = new Button("Cancel region");
    Button btnClearRegions = new Button("Clear regions");
    Button btnExtractRegions = new Button("Extract regions");
    TextField tfSuffix = new TextField();
    tfSuffix.setPromptText("suffix");

    btnStartRegion.setOnAction(e -> heatmap.startRegion());

    btnFinishRegion.setOnAction(e -> {
      Path2D path = heatmap.finishRegion();
      finishedRegions.add(path);
    });

    btnCancelRegion.setOnAction(e -> heatmap.finishRegion());

    btnClearRegions.setOnAction(e -> {
      List<XYAnnotation> annotations = heatmap.getXYPlot().getAnnotations();
      new ArrayList<>(annotations).forEach(a -> heatmap.getXYPlot().removeAnnotation(a, true));
      finishedRegions.clear();
    });

    btnExtractRegions
        .setOnAction(e -> Platform.runLater(() -> MZmineCore.getProjectManager().getCurrentProject()
            .addFeatureList(IonMobilityUtils.extractRegionFromFeatureList(finishedRegions,
                (ModularFeatureList) features.stream().findFirst().get().getFeatureList(),
                Objects.requireNonNullElse(tfSuffix.getText(), "extracted")))));

    finishedRegions.addListener((ListChangeListener<Path2D>) c -> {
      c.next();
      if (c.wasRemoved()) {
        boolean disable = c.getList().isEmpty();
        btnClearRegions.setDisable(disable);
        btnExtractRegions.setDisable(disable);
      }
      if (c.wasAdded()) {
        heatmap.getXYPlot()
            .addAnnotation(new XYShapeAnnotation(c.getAddedSubList().get(0), roiStroke, roiPaint));
        btnClearRegions.setDisable(false);
        btnExtractRegions.setDisable(false);
      }
    });

    selectionControls.add(btnStartRegion, 0, 0);
    selectionControls.add(btnFinishRegion, 1, 0);
    selectionControls.add(btnCancelRegion, 2, 0);
    selectionControls.add(btnClearRegions, 3, 0);
    selectionControls.add(btnExtractRegions, 4, 0);
    selectionControls.add(tfSuffix, 5, 0);
    selectionControls.setHgap(5);
    selectionControls.getStyleClass().add(".region-match-chart-bg");
    selectionControls.setAlignment(Pos.TOP_CENTER);
    setBottom(selectionControls);
  }

  public void setFeatures(Collection<ModularFeature> features) {
    assert Platform.isFxApplicationThread();

    if (!features.isEmpty()) {
      RawDataFile file = features.stream().findFirst().get().getRawDataFile();
      if (!(file instanceof IMSRawDataFile)) {
        throw new IllegalArgumentException(
            "Cannot visualize non-ion mobility spectrometry files in an IMS visualizer");
      }
      heatmap.setRangeAxisLabel(((IMSRawDataFile) file).getMobilityType().getAxisLabel());
    }

    this.features = features;

    featureVisualisersMap.clear();
    content.getChildren().clear();
    ticChart.removeAllDatasets();

    CalculateDatasetsTask calc = new CalculateDatasetsTask(features);
    MZmineCore.getTaskController().addTask(calc);
    calc.addTaskStatusListener((task, newStatus, oldStatus) -> {
      if (newStatus == TaskStatus.FINISHED) {
        Platform.runLater(() -> {
          var datasetsRenderers = calc.getDatasetsRenderers();
          heatmap.addDatasetsAndRenderers(datasetsRenderers);
          heatmap.setLegendPaintScale(calc.getPaintScale());
        });
      }
    });
  }

  public void addFeatureToRightSide(ModularFeature feature) {
    if (!rawDataFiles.contains(feature.getRawDataFile())) {
      ticChart.addDataset(new FastColoredXYDataset(
          new ScanBPCProvider(feature.getRawDataFile().getScanNumbers(1))));
      rawDataFiles.add(feature.getRawDataFile());
    }
    ticChart.addDataset(new IonTimeSeriesToXYProvider(feature.getFeatureData(),
        FeatureUtils.featureToString(feature),
        new SimpleObjectProperty<>(
            MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor())));

    SingleIMSFeatureVisualiserPane featureVisualiserPane = new SingleIMSFeatureVisualiserPane(
        feature);

    featureVisualiserPane.getHeatmapChart().cursorPositionProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue.getDataset() instanceof ColoredXYDataset) {
            ColoredXYDataset dataset = (ColoredXYDataset) newValue.getDataset();
            if (dataset.getValueProvider() instanceof MassSpectrumProvider) {
              MassSpectrumProvider spectrumProvider = (MassSpectrumProvider) dataset
                  .getValueProvider();
              MassSpectrum spectrum = spectrumProvider.getSpectrum(newValue.getValueIndex());
              if (spectrum instanceof Scan) {
                selectedScan.set((Scan) spectrum);
              }
            }
          }
        });

    featureVisualiserPane.selectedMobilityScanProperty()
        .addListener((observable, oldValue, newValue) -> selectedScan.set(newValue.getFrame()));
    featureVisualisersMap.put(feature, featureVisualiserPane);
    content.getChildren().add(featureVisualiserPane);
    content.getChildren().add(new Separator(Orientation.HORIZONTAL));
  }

  private void updateValueMarkers(Scan newValue) {
    ticChart.getXYPlot().clearDomainMarkers();
    final ValueMarker newMarker = new ValueMarker(newValue.getRetentionTime(), markerColor,
        markerStroke);
    ticChart.getXYPlot().addDomainMarker(newMarker);
    featureVisualisersMap.values().forEach(vis -> {
      vis.getHeatmapChart().getXYPlot().clearDomainMarkers();
      vis.getHeatmapChart().getXYPlot().addDomainMarker(newMarker);
    });
  }

  private void initChartListeners() {
    heatmap.cursorPositionProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue.getDataset() instanceof ColoredXYZDataset) {
        XYZValueProvider prov = ((ColoredXYZDataset) newValue.getDataset()).getXyzValueProvider();
        if (prov instanceof SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider) {
          ModularFeature f = ((SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider) prov)
              .getSourceFeature();
          if (f != null) {
            if (!isCtrlPressed) {
              clearRightSide();
            }
            addFeatureToRightSide(f);
            ticChart.addDataset(new IonTimeSeriesToXYProvider(f));
          }
        }
        if (prov instanceof FeaturesToMobilityMzHeatmapProvider) {
          ModularFeature f = ((FeaturesToMobilityMzHeatmapProvider) prov).getSourceFeatures().get(
              newValue.getValueIndex());
          if (f != null) {
            if (!isCtrlPressed) {
              clearRightSide();
            }
            addFeatureToRightSide(f);
            ticChart.addDataset(new IonTimeSeriesToXYProvider(f));
          }
        }
      }
    }));

    ticChart.cursorPositionProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.getDataset() instanceof ColoredXYDataset) {
        ColoredXYDataset dataset = (ColoredXYDataset) newValue.getDataset();
        if (dataset.getValueProvider() instanceof MassSpectrumProvider) {
          MassSpectrumProvider spectrumProvider = (MassSpectrumProvider) dataset.getValueProvider();
          MassSpectrum spectrum = spectrumProvider.getSpectrum(newValue.getValueIndex());
          if (spectrum instanceof Scan) {
            selectedScan.set((Scan) spectrum);
          }
        }
      }
    });
  }

  private void clearRightSide() {
    content.getChildren().clear();
    featureVisualisersMap.clear();
    ticChart.removeAllDatasets();
  }
}
