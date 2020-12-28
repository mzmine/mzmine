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

package io.github.mzmine.modules.visualization.rawdataoverviewims;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleMobilogram;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing.PreviewMobilogram;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.CachedFrame;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.FrameHeatmapProvider;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.FrameSummedMobilogramProvider;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.FrameSummedSpectrumProvider;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.SingleSpectrumProvider;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.StorableFrame;
import io.github.mzmine.util.MobilogramUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Set;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;

public class IMSRawDataOverviewPane extends BorderPane {

  private final GridPane chartPanel;
  private final GridPane controlsPanel;
  private final SimpleXYChart<FrameSummedMobilogramProvider> summedMobilogramChart;
  private final SimpleXYChart<FrameSummedSpectrumProvider> summedSpectrumChart;
  private final SimpleXYChart<SingleSpectrumProvider> singleSpectrumChart;
  private final SimpleXYChart<PreviewMobilogram> extractedMobilogramChart;
  private final SimpleXYZScatterPlot<FrameHeatmapProvider> heatmapChart;

  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;
  private final UnitFormat unitFormat;

  private final MZTolerance mzTolerance;
  private final ObjectProperty<Frame> selectedFrame;
  private final DoubleProperty selectedMz;
  private final ObjectProperty<MobilityScan> selectedMobilityScan;
  private final Stroke markerStroke = new BasicStroke(1.0f);
  private CachedFrame cachedFrame;
  private Color markerColor;
  private IMSRawDataFile rawDataFile;

  /**
   * Creates a BorderPane layout.
   */
  public IMSRawDataOverviewPane() {
    super();
    chartPanel = new GridPane();
    controlsPanel = new GridPane();

    ColumnConstraints constraints = new ColumnConstraints(300, Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY, Priority.ALWAYS, HPos.CENTER, true);
    RowConstraints rowConstraints = new RowConstraints(300, Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY, Priority.ALWAYS, VPos.CENTER, true);

    chartPanel.getColumnConstraints()
        .addAll(constraints, constraints, new ColumnConstraints(), constraints);
    chartPanel.getRowConstraints().addAll(rowConstraints, rowConstraints);

    setCenter(chartPanel);

    initControlPanel();

    summedMobilogramChart = new SimpleXYChart<>("Total ion mobilogram");
    summedSpectrumChart = new SimpleXYChart<>("Summed frame spectrum");
    singleSpectrumChart = new SimpleXYChart<>("Mobility scan");
    extractedMobilogramChart = new SimpleXYChart<>("Extracted ion mobilogram");
    heatmapChart = new SimpleXYZScatterPlot<>("Frame heatmap");
    initCharts();

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    selectedFrame = new SimpleObjectProperty<>();
    selectedFrame.addListener((observable, oldValue, newValue) -> onSelectedFrameChanged());

    updateAxisLabels();
    chartPanel.add(new BorderPane(summedMobilogramChart), 0, 0);
    chartPanel.add(new BorderPane(heatmapChart), 1, 0);
    chartPanel.add(new Separator(Orientation.VERTICAL), 2, 0, 1, 2);
    chartPanel.add(new BorderPane(extractedMobilogramChart), 3, 0);
    chartPanel.add(new BorderPane(summedSpectrumChart), 1, 1);
    chartPanel.add(new BorderPane(singleSpectrumChart), 3, 1);
    chartPanel.add(controlsPanel, 0, 1);

    selectedMz = new SimpleDoubleProperty();
    selectedMobilityScan = new SimpleObjectProperty();
    mzTolerance = new MZTolerance(0.008, 10);
    markerColor = MZmineCore.getConfiguration().getDefaultColorPalette().getNeutralColorAWT();
    initChartListeners();
    initSelectedValueListeners();
  }

  private void onSelectedFrameChanged() {
    clearAllCharts();
    if (selectedFrame.get() == null) {
      return;
    }
    try {
      cachedFrame = new CachedFrame((StorableFrame) selectedFrame.get());
      heatmapChart.setDataset(new FrameHeatmapProvider(cachedFrame));
      summedMobilogramChart.addDataset(new FrameSummedMobilogramProvider(cachedFrame));
      summedSpectrumChart.addDataset(new FrameSummedSpectrumProvider(cachedFrame));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void updateAxisLabels() {
    String intensityLabel = unitFormat.format("Intensity", "cps");
    String mzLabel = "m/z";
    String mobilityLabel = (rawDataFile != null) ?
        unitFormat.format("Mobility (" + rawDataFile.getMobilityType().getAxisLabel() + ")",
            rawDataFile.getMobilityType().getUnit()) : "Mobility";
    summedMobilogramChart.setDomainAxisLabel(mobilityLabel);
    summedMobilogramChart.setDomainAxisNumberFormatOverride(mobilityFormat);
    summedMobilogramChart.setRangeAxisLabel(intensityLabel);
    summedMobilogramChart.setRangeAxisNumberFormatOverride(intensityFormat);
    extractedMobilogramChart.setDomainAxisLabel(mobilityLabel);
    extractedMobilogramChart.setDomainAxisNumberFormatOverride(mobilityFormat);
    extractedMobilogramChart.setRangeAxisLabel(intensityLabel);
    extractedMobilogramChart.setRangeAxisNumberFormatOverride(intensityFormat);
    summedSpectrumChart.setDomainAxisLabel(mzLabel);
    summedSpectrumChart.setDomainAxisNumberFormatOverride(mzFormat);
    summedSpectrumChart.setRangeAxisLabel(intensityLabel);
    summedSpectrumChart.setRangeAxisNumberFormatOverride(intensityFormat);
    singleSpectrumChart.setDomainAxisLabel(mzLabel);
    singleSpectrumChart.setDomainAxisNumberFormatOverride(mzFormat);
    singleSpectrumChart.setRangeAxisLabel(intensityLabel);
    singleSpectrumChart.setRangeAxisNumberFormatOverride(intensityFormat);
    heatmapChart.setDomainAxisLabel(mzLabel);
    heatmapChart.setDomainAxisNumberFormatOverride(mzFormat);
    heatmapChart.setRangeAxisLabel(mobilityLabel);
    heatmapChart.setRangeAxisNumberFormatOverride(mobilityFormat);
  }

  private void initCharts() {
    final ColoredXYBarRenderer summedSpectrumRenderer = new ColoredXYBarRenderer(false);
    summedSpectrumRenderer
        .setDefaultItemLabelGenerator(summedSpectrumChart.getXYPlot().getRenderer()
            .getDefaultItemLabelGenerator());
    summedSpectrumRenderer.setDefaultToolTipGenerator(summedSpectrumChart.getXYPlot().getRenderer()
        .getDefaultToolTipGenerator());
    summedSpectrumChart.setDefaultRenderer(summedSpectrumRenderer);
    summedSpectrumChart.setShowCrosshair(false);
    summedMobilogramChart.getXYPlot().setOrientation(PlotOrientation.HORIZONTAL);
    summedMobilogramChart.setShowCrosshair(false);

    final ColoredXYBarRenderer singleSpectrumRenderer = new ColoredXYBarRenderer(false);
    singleSpectrumRenderer
        .setDefaultItemLabelGenerator(singleSpectrumChart.getXYPlot().getRenderer()
            .getDefaultItemLabelGenerator());
    singleSpectrumRenderer.setDefaultToolTipGenerator(singleSpectrumChart.getXYPlot().getRenderer()
        .getDefaultToolTipGenerator());
    singleSpectrumChart.setDefaultRenderer(singleSpectrumRenderer);
    singleSpectrumChart.setShowCrosshair(false);
    extractedMobilogramChart.setShowCrosshair(false);
    heatmapChart.setShowCrosshair(false);
  }

  private void initControlPanel() {
    controlsPanel.add(new Label("Just a label"), 0, 0);
  }

  private void initChartListeners() {
    extractedMobilogramChart.cursorPositionProperty().addListener(((observable, oldValue,
        newValue) -> selectedMobilityScan
        .set(cachedFrame.getSortedMobilityScans().get(newValue.getValueIndex()))));
    summedMobilogramChart.cursorPositionProperty().addListener(((observable, oldValue,
        newValue) -> selectedMobilityScan
        .set(cachedFrame.getSortedMobilityScans().get(newValue.getValueIndex()))));
    singleSpectrumChart.cursorPositionProperty().addListener(((observable, oldValue, newValue) ->
        selectedMz.set(newValue.getDomainValue())));
    summedSpectrumChart.cursorPositionProperty().addListener(((observable, oldValue, newValue) ->
        selectedMz.set(newValue.getDomainValue())));
    heatmapChart.cursorPositionProperty().addListener(((observable, oldValue, newValue) -> {
      selectedMz.set(newValue.getDomainValue());
    }));
  }

  private void initSelectedValueListeners() {
    selectedMobilityScan.addListener(((observable, oldValue, newValue) -> {
      singleSpectrumChart.removeAllDatasets();
      singleSpectrumChart.addDataset(new SingleSpectrumProvider(selectedMobilityScan.get()));
      updateValueMarkers();
    }));

    selectedMz.addListener(((observable, oldValue, newValue) -> {
      SimpleMobilogram mobilogram = MobilogramUtils.buildMobilogramForMzRange(Set.of(cachedFrame),
          mzTolerance.getToleranceRange(selectedMz.getValue()));
      PreviewMobilogram prev = new PreviewMobilogram(mobilogram,
          "m/z" + mzFormat.format(selectedMz.getValue()));
      extractedMobilogramChart.removeAllDatasets();
      extractedMobilogramChart.addDataset(prev);
      updateValueMarkers();
    }));
  }

  private void updateValueMarkers() {
    extractedMobilogramChart.getXYPlot().clearDomainMarkers();
    extractedMobilogramChart.getXYPlot()
        .addDomainMarker(new ValueMarker(selectedMobilityScan.getValue().getMobility(),
            markerColor, markerStroke));
    summedMobilogramChart.getXYPlot().clearDomainMarkers();
    summedMobilogramChart.getXYPlot()
        .addDomainMarker(new ValueMarker(selectedMobilityScan.getValue().getMobility(),
            markerColor, markerStroke));
    summedSpectrumChart.getXYPlot().clearDomainMarkers();
    summedSpectrumChart.getXYPlot().addDomainMarker(new ValueMarker(selectedMz.get(),
        markerColor, markerStroke));
    singleSpectrumChart.getXYPlot().clearDomainMarkers();
    singleSpectrumChart.getXYPlot().addDomainMarker(new ValueMarker(selectedMz.get(),
        markerColor, markerStroke));
    heatmapChart.getXYPlot().clearDomainMarkers();
    heatmapChart.getXYPlot().clearRangeMarkers();
    heatmapChart.getXYPlot()
        .addDomainMarker(new ValueMarker(selectedMz.get(), markerColor, markerStroke));
    heatmapChart.getXYPlot().addRangeMarker(
        new ValueMarker(selectedMobilityScan.getValue().getMobility(), markerColor, markerStroke));
  }

  private void clearAllCharts() {
    summedMobilogramChart.removeAllDatasets();
    extractedMobilogramChart.removeAllDatasets();
    summedSpectrumChart.removeAllDatasets();
    heatmapChart.removeAllDatasets();
    singleSpectrumChart.removeAllDatasets();
  }

  public Frame getSelectedFrame() {
    return selectedFrame.get();
  }

  public void setSelectedFrame(Frame frame) {
    this.selectedFrame.set(frame);
  }

  public void setSelectedFrame(int frameId) {
    Frame frame = rawDataFile.getFrame(frameId);
    if (frame != null) {
      setSelectedFrame(frame);
    }
  }

  public ObjectProperty<Frame> selectedFrameProperty() {
    return selectedFrame;
  }

  public void setRawDataFile(RawDataFile rawDataFile) {
    if (!(rawDataFile instanceof IMSRawDataFile)) {
      return;
    }
    this.rawDataFile = (IMSRawDataFile) rawDataFile;
    updateAxisLabels();
    setSelectedFrame(1);
  }
}
