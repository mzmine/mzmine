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

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.simplechart.MultiDatasetXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYZValueProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.ScanBPCProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.Stroke;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
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
  private List<RawDataFile> rawDataFiles;

  private boolean isCtrlPressed = false;

  public IMSFeaturesVisualizer() {
    super();
    getStylesheets().addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    ticChart = new SimpleXYChart<>();
    heatmap = new MultiDatasetXYZScatterPlot<>();
    featureVisualisersMap = new LinkedHashMap<>();
    rawDataFiles = new ArrayList<>();

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    this.setOnKeyPressed(e -> {
      if(e.getCode() == KeyCode.CONTROL) {
        isCtrlPressed = true;
      }
    });
    this.setOnKeyReleased(e -> {
      if(e.getCode() == KeyCode.CONTROL) {
        isCtrlPressed = false;
      }
    });

    heatmap.setDomainAxisLabel("m/z");
    heatmap.setDomainAxisNumberFormatOverride(mzFormat);
    heatmap.setRangeAxisLabel("Mobility");
    heatmap.setRangeAxisNumberFormatOverride(mobilityFormat);
    heatmap.setLegendAxisLabel(unitFormat.format("Intensity", "counts"));
    heatmap.setLegendNumberFormatOverride(intensityFormat);
//    heatmap.getXYPlot().setBackgroundPaint(Color.BLACK);
    ticChart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    ticChart.setDomainAxisNumberFormatOverride(rtFormat);
    ticChart.setRangeAxisNumberFormatOverride(intensityFormat);
    ticChart.setRangeAxisLabel(unitFormat.format("Intensity", "counts"));
    ticChart.setShowCrosshair(false);
    ticChart.getChart().setTitle("Extracted ion chromatograms");


    selectedScan = new SimpleObjectProperty<>();
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

    selectedScan.addListener((observable, oldValue, newValue) -> updateValueMarkers(newValue));

    ticChart.setMinHeight(250);
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

    initHeatMapListener();
  }

  public void setFeatures(Collection<ModularFeature> features) {
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

  private void initHeatMapListener() {
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
      }
    }));
  }

  private void clearRightSide() {
    content.getChildren().clear();
    featureVisualisersMap.clear();
    ticChart.removeAllDatasets();
  }
}
