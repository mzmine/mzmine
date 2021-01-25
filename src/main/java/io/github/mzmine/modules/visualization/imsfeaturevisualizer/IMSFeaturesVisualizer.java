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
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.ScanBPCProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.jfree.chart.plot.ValueMarker;

public class IMSFeaturesVisualizer extends BorderPane {

  private final SimpleXYChart<IonTimeSeriesToXYProvider> ticChart;
  private final Map<ModularFeature, SingleIMSFeatureVisualiserPane> featureVisualisersMap;
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

  public IMSFeaturesVisualizer() {
    super();
    getStylesheets().addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    ticChart = new SimpleXYChart<>();
    featureVisualisersMap = new LinkedHashMap<>();
    rawDataFiles = new ArrayList<>();

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    ticChart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    ticChart.setDomainAxisNumberFormatOverride(rtFormat);
    ticChart.setRangeAxisNumberFormatOverride(intensityFormat);
    ticChart.setRangeAxisLabel(unitFormat.format("Intensity", "counts"));
    ticChart.setShowCrosshair(false);

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
    selectedFeaturesPane.setMinWidth(400);
    setRight(selectedFeaturesPane);


  }

  public void setFeatures(Collection<ModularFeature> features) {
    featureVisualisersMap.clear();
    content.getChildren().clear();
    ticChart.removeAllDatasets();
    for (ModularFeature feature : features) {
      addFeatureToRightSide(feature);
    }
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
}
