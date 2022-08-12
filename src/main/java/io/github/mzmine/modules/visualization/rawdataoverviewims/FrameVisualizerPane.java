/*
 *  Copyright 2006-2022 The MZmine Development Team
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
 *//*


package io.github.mzmine.modules.visualization.rawdataoverviewims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IMSIonTraceHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.CachedFrame;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.FrameHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.FrameSummedMobilogramProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.FrameSummedSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.SingleMobilityScanProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.rawdataoverviewims.threads.BuildMultipleMobilogramRanges;
import io.github.mzmine.modules.visualization.rawdataoverviewims.threads.BuildSelectedRanges;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.RangeUtils;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;

public class FrameVisualizerPane extends BorderPane {

  protected final SimpleXYChart<FrameSummedMobilogramProvider> mobilogramChart;
  protected final SimpleXYChart<FrameSummedSpectrumProvider> summedSpectrumChart;
  protected final SimpleXYZScatterPlot<FrameHeatmapProvider> heatmapChart;

  protected final Canvas heatmapLegendCanvas;
  protected final NumberFormat rtFormat;
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;

  protected final ObjectProperty<Frame> selectedFrame;
  protected final ObjectProperty<MobilityScan> selectedMobilityScan;
  protected final ObjectProperty<Range<Double>> selectedMz;

  //not thread safe, so we need one for building the selected and one for building all the others
  protected BinningMobilogramDataAccess selectedBinningMobilogramDataAccess;
  protected BinningMobilogramDataAccess rangesBinningMobilogramDataAccess;

  protected MZTolerance mzTolerance;

  protected Frame cachedFrame;
  protected double frameNoiseLevel;
  protected double mobilityScanNoiseLevel;
  protected int binWidth;

  protected Color markerColor;

  protected int selectedMobilogramDatasetIndex;

  public FrameVisualizerPane() {
    selectedMobilogramDatasetIndex = -1;
    selectedMz = new SimpleObjectProperty<>();
    selectedMobilityScan = new SimpleObjectProperty<>();
    this.mzTolerance = mzTolerance;
    this.frameNoiseLevel = frameNoiseLevel;
    this.mobilityScanNoiseLevel = mobilityScanNoiseLevel;
    this.binWidth = binWidth;

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    selectedFrame = new SimpleObjectProperty<>();
    selectedFrame.addListener((observable, oldValue, newValue) -> onSelectedFrameChanged());

    mobilogramChart = new SimpleXYChart<>("Mobilogram chart");
    summedSpectrumChart = new SimpleXYChart<>("Summed frame spectrum");
    heatmapChart = new SimpleXYZScatterPlot<>("Frame heatmap");
    heatmapLegendCanvas = new Canvas();
  }

  protected void onSelectedFrameChanged() {
    clearAllCharts();
    if (selectedFrame.get() == null) {
      return;
    }
    // ticChart.removeDatasets(mzRangeTicDatasetIndices);
    mzRangeTicDatasetIndices.clear();
    cachedFrame = new CachedFrame(selectedFrame.get(), frameNoiseLevel,
        mobilityScanNoiseLevel);//selectedFrame.get();//
    heatmapChart.setDataset(new FrameHeatmapProvider(cachedFrame));
    mobilogramChart.addDataset(new FrameSummedMobilogramProvider(cachedFrame, binWidth));
    summedSpectrumChart.addDataset(new FrameSummedSpectrumProvider(cachedFrame));
    if (selectedMobilityScan.get() != null) {
      singleSpectrumChart.addDataset(new SingleMobilityScanProvider(
          cachedFrame.getMobilityScan(selectedMobilityScan.get().getMobilityScanNumber())));
    }
    MZmineCore.getTaskController().addTask(
        new BuildMultipleMobilogramRanges(controlsPanel.getMobilogramRangesList(),
            Set.of(cachedFrame), rawDataFile, this, rangesBinningMobilogramDataAccess, new Date()));
    if (!RangeUtils.isGuavaRangeEnclosingJFreeRange(
        heatmapChart.getXYPlot().getRangeAxis().getRange(),
        selectedFrame.get().getMobilityRange())) {
      Range<Double> mobilityRange = selectedFrame.get().getMobilityRange();
      if (mobilityRange != null) {
        heatmapChart.getXYPlot().getRangeAxis()
            .setRange(mobilityRange.lowerEndpoint(), mobilityRange.upperEndpoint());
      }
    }
    if (!RangeUtils.isGuavaRangeEnclosingJFreeRange(
        heatmapChart.getXYPlot().getDomainAxis().getRange(),
        selectedFrame.get().getDataPointMZRange())) {
      Range<Double> mzRange = selectedFrame.get().getDataPointMZRange();
      if (mzRange != null) {
        heatmapChart.getXYPlot().getDomainAxis()
            .setRange(mzRange.lowerEndpoint(), mzRange.upperEndpoint());
      }
    }
    updateValueMarkers();

  }

  private void updateAxisLabels() {
    String intensityLabel = unitFormat.format("Intensity", "a.u.");
    String mzLabel = "m/z";
    String mobilityLabel =
        (rawDataFile != null) ? rawDataFile.getMobilityType().getAxisLabel() : "Mobility";
    mobilogramChart.setRangeAxisLabel(mobilityLabel);
    mobilogramChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    mobilogramChart.setDomainAxisLabel(intensityLabel);
    mobilogramChart.setDomainAxisNumberFormatOverride(intensityFormat);
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
    heatmapChart.setLegendNumberFormatOverride(intensityFormat);
    ionTraceChart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    ionTraceChart.setRangeAxisLabel(mobilityLabel);
    ionTraceChart.setDomainAxisNumberFormatOverride(rtFormat);
    ionTraceChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    ionTraceChart.setLegendNumberFormatOverride(intensityFormat);
  }

  private void initCharts() {
    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    final ColoredXYBarRenderer summedSpectrumRenderer = new ColoredXYBarRenderer(false);
    summedSpectrumRenderer.setDefaultItemLabelPaint(theme.getItemLabelPaint());

    summedSpectrumRenderer.setDefaultItemLabelGenerator(
        summedSpectrumChart.getXYPlot().getRenderer().getDefaultItemLabelGenerator());
    summedSpectrumRenderer.setDefaultToolTipGenerator(
        summedSpectrumChart.getXYPlot().getRenderer().getDefaultToolTipGenerator());
    summedSpectrumChart.setDefaultRenderer(summedSpectrumRenderer);
    summedSpectrumChart.setShowCrosshair(false);

    // mobilogramChart.getXYPlot().setOrientation(PlotOrientation.HORIZONTAL);
    mobilogramChart.getXYPlot().getDomainAxis().setInverted(true);
    mobilogramChart.setShowCrosshair(false);
    mobilogramChart.setLegendItemsVisible(false);
    NumberAxis axis = (NumberAxis) mobilogramChart.getXYPlot().getRangeAxis();
    axis.setAutoRangeMinimumSize(0.2);
    axis.setAutoRangeIncludesZero(false);
    axis.setAutoRangeStickyZero(false);

    final ColoredXYBarRenderer singleSpectrumRenderer = new ColoredXYBarRenderer(false);
    singleSpectrumRenderer.setDefaultItemLabelPaint(theme.getItemLabelPaint());
    singleSpectrumRenderer.setDefaultItemLabelGenerator(
        singleSpectrumChart.getXYPlot().getRenderer().getDefaultItemLabelGenerator());
    singleSpectrumRenderer.setDefaultToolTipGenerator(
        singleSpectrumChart.getXYPlot().getRenderer().getDefaultToolTipGenerator());
    singleSpectrumChart.setDefaultRenderer(singleSpectrumRenderer);
    singleSpectrumChart.setShowCrosshair(false);

    ionTraceChart.setShowCrosshair(false);
    ionTraceChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    ionTraceChart.setDefaultPaintscaleLocation(RectangleEdge.BOTTOM);
    heatmapChart.setShowCrosshair(false);
    heatmapChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    heatmapChart.setDefaultPaintscaleLocation(RectangleEdge.BOTTOM);
    ticChart.getXYPlot().setDomainCrosshairVisible(false);
    ticChart.getXYPlot().setRangeCrosshairVisible(false);
    ticChart.switchDataPointsVisible();
    ticChart.setMinHeight(150);

    ChartGroup rtGroup = new ChartGroup(false, false, true, false);
    rtGroup.add(new ChartViewWrapper(ticChart));
    rtGroup.add(new ChartViewWrapper(ionTraceChart));

    ChartGroup mzGroup = new ChartGroup(false, false, true, false);
    mzGroup.add(new ChartViewWrapper(heatmapChart));
    mzGroup.add(new ChartViewWrapper(summedSpectrumChart));

    ChartGroup mobilityGroup = new ChartGroup(false, false, false, true);
    mobilityGroup.add(new ChartViewWrapper(heatmapChart));
    mobilityGroup.add(new ChartViewWrapper(ionTraceChart));
    mobilityGroup.add(new ChartViewWrapper(mobilogramChart));
  }

  private void initChartLegendPanels() {
    heatmapLegendCanvas.setHeight(HEATMAP_LEGEND_HEIGHT);
    ionTraceLegendCanvas.setHeight(HEATMAP_LEGEND_HEIGHT);
    heatmapChart.setLegendCanvas(heatmapLegendCanvas);
    ionTraceChart.setLegendCanvas(ionTraceLegendCanvas);
  }

  private void initChartListeners() {
    mobilogramChart.cursorPositionProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue.getValueIndex() != -1) {
        selectedMobilityScan.set(
            cachedFrame.getSortedMobilityScans().get(newValue.getValueIndex() * binWidth));
      }
    }));
    singleSpectrumChart.cursorPositionProperty().addListener(
        ((observable, oldValue, newValue) -> selectedMz.set(
            mzTolerance.getToleranceRange(newValue.getDomainValue()))));
    summedSpectrumChart.cursorPositionProperty().addListener(
        ((observable, oldValue, newValue) -> selectedMz.set(
            mzTolerance.getToleranceRange(newValue.getDomainValue()))));
    heatmapChart.cursorPositionProperty().addListener(((observable, oldValue, newValue) -> {
      selectedMz.set(mzTolerance.getToleranceRange(newValue.getDomainValue()));
      if (newValue.getDataset() != null) {
        selectedMobilityScan.set(
            ((FrameHeatmapProvider) ((ColoredXYZDataset) newValue.getDataset()).getXyzValueProvider()).getMobilityScanAtValueIndex(
                newValue.getValueIndex()));
      }
    }));
    ticChart.cursorPositionProperty().addListener(
        ((observable, oldValue, newValue) -> setSelectedFrame((Frame) newValue.getScan())));
    ionTraceChart.cursorPositionProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue.getDataset() == null || newValue.getValueIndex() == -1) {
        return;
      }
      MobilityScan selectedScan = ((IMSIonTraceHeatmapProvider) ((ColoredXYZDataset) newValue.getDataset()).getXyzValueProvider()).getSpectrum(
          newValue.getValueIndex());
      if (selectedScan != null) {
        setSelectedFrame(selectedScan.getFrame());
        selectedMobilityScan.set(selectedScan);
      }
    }));
  }

  private void initSelectedValueListeners() {
    selectedMobilityScan.addListener(((observable, oldValue, newValue) -> {
      singleSpectrumChart.removeAllDatasets();
      singleSpectrumChart.addDataset(new SingleMobilityScanProvider(selectedMobilityScan.get()));
      updateValueMarkers();
    }));

    selectedMz.addListener(((observable, oldValue, newValue) -> {
      if (selectedMobilogramDatasetIndex != -1) {
        mobilogramChart.removeDataSet(selectedMobilogramDatasetIndex, false);
      }
      controlsPanel.setRangeToMobilogramRangeComp(newValue);
      Thread mobilogramCalc = new Thread(
          new BuildSelectedRanges(selectedMz.get(), Set.of(cachedFrame), rawDataFile, scanSelection,
              this, rtWidth, selectedBinningMobilogramDataAccess));
      mobilogramCalc.start();
      float rt = selectedFrame.get().getRetentionTime();
      ionTraceChart.setDataset(new IMSIonTraceHeatmapProvider(rawDataFile, selectedMz.get(),
          Range.closed(Math.max(rawDataFile.getDataRTRange(1).lowerEndpoint(), rt - rtWidth / 2),
              Math.min(rawDataFile.getDataRTRange(1).upperEndpoint(), rt + rtWidth / 2)),
          mobilityScanNoiseLevel));
      updateValueMarkers();
    }));
  }

  public void addMobilogramRangesToChart(List<? extends ColoredXYDataset> previewMobilograms) {
    mobilogramChart.addDatasets(previewMobilograms);
    updateValueMarkers();
  }

  private void updateValueMarkers() {
    if (selectedMobilityScan.get() != null) {
      mobilogramChart.getXYPlot().clearRangeMarkers();
      mobilogramChart.getXYPlot().addRangeMarker(
          new ValueMarker(selectedMobilityScan.getValue().getMobility(), markerColor, markerStroke),
          Layer.FOREGROUND);
      heatmapChart.getXYPlot().clearRangeMarkers();
      heatmapChart.getXYPlot().addRangeMarker(
          new ValueMarker(selectedMobilityScan.getValue().getMobility(), markerColor, markerStroke),
          Layer.FOREGROUND);
      ionTraceChart.getXYPlot().clearRangeMarkers();
      ionTraceChart.getXYPlot().addRangeMarker(
          new ValueMarker(selectedMobilityScan.get().getMobility(), markerColor, markerStroke),
          Layer.FOREGROUND);
    }
    if (selectedMz.getValue() != null) {
      summedSpectrumChart.getXYPlot().clearDomainMarkers();
      summedSpectrumChart.getXYPlot().addDomainMarker(
          new ValueMarker(RangeUtils.rangeCenter(selectedMz.get()), markerColor, markerStroke),
          Layer.FOREGROUND);
      singleSpectrumChart.getXYPlot().clearDomainMarkers();
      singleSpectrumChart.getXYPlot().addDomainMarker(
          new ValueMarker(RangeUtils.rangeCenter(selectedMz.get()), markerColor, markerStroke),
          Layer.FOREGROUND);
      heatmapChart.getXYPlot().clearDomainMarkers();
      heatmapChart.getXYPlot().addDomainMarker(
          new ValueMarker(RangeUtils.rangeCenter(selectedMz.get()), markerColor, markerStroke),
          Layer.FOREGROUND);
    }
    if (selectedFrame.get() != null) {
      ticChart.getXYPlot().clearDomainMarkers();
      ticChart.getXYPlot().addDomainMarker(
          new ValueMarker(selectedFrame.get().getRetentionTime(), markerColor, markerStroke),
          Layer.FOREGROUND);
      ionTraceChart.getXYPlot().clearDomainMarkers();
      ionTraceChart.getXYPlot().addDomainMarker(
          new ValueMarker(selectedFrame.get().getRetentionTime(), markerColor, markerStroke),
          Layer.FOREGROUND);
    }
  }

  public Frame getSelectedFrame() {
    return selectedFrame.get();
  }

  public void setSelectedFrame(Frame frame) {
    this.selectedFrame.set(frame);
  }

  public ObjectProperty<Frame> selectedFrameProperty() {
    return selectedFrame;
  }

  public void setFrameNoiseLevel(double frameNoiseLevel) {
    this.frameNoiseLevel = frameNoiseLevel;
  }

  public void setMobilityScanNoiseLevel(double mobilityScanNoiseLevel) {
    this.mobilityScanNoiseLevel = mobilityScanNoiseLevel;
  }

  public void setRtWidth(Float rtWidth) {
    this.rtWidth = rtWidth;
  }

  public void setBinWidth(int binWidth) {
    // check the bin width the pane was set to before, not the actual computed bin width.
    if (binWidth != this.binWidth) {
      this.binWidth = binWidth;
      rangesBinningMobilogramDataAccess = EfficientDataAccess.of(this.rawDataFile, binWidth);
      selectedBinningMobilogramDataAccess = EfficientDataAccess.of(this.rawDataFile, binWidth);
    }
  }

  public void setMzTolerance(MZTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
  }
}
*/
