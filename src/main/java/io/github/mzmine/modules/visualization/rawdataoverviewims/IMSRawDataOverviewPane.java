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
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.rawdataoverviewims.Threads.BuildMultipleRanges;
import io.github.mzmine.modules.visualization.rawdataoverviewims.Threads.BuildSelectedRanges;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.CachedFrame;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.FrameHeatmapProvider;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.FrameSummedMobilogramProvider;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.FrameSummedSpectrumProvider;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.IonTraceProvider;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.SingleSpectrumProvider;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.StorableFrame;
import io.github.mzmine.util.RangeUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.Rectangle;
import javax.annotation.Nullable;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;

public class IMSRawDataOverviewPane extends BorderPane {

  private static final int HEATMAP_LEGEND_HEIGHT = 40;

  private final GridPane chartPanel;
  private final IMSRawDataOverviewControlPanel controlsPanel;
  private final SimpleXYChart<FrameSummedMobilogramProvider> mobilogramChart;
  private final SimpleXYChart<FrameSummedSpectrumProvider> summedSpectrumChart;
  private final SimpleXYChart<SingleSpectrumProvider> singleSpectrumChart;
  private final SimpleXYZScatterPlot<FrameHeatmapProvider> heatmapChart;
  private final SimpleXYZScatterPlot<IonTraceProvider> ionTraceChart;
  private final TICPlot ticChart;
  private final Canvas heatmapLegendCanvas;
  private final Canvas ionTraceLegendCanvas;

  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;
  private final UnitFormat unitFormat;

  private final ObjectProperty<Frame> selectedFrame;
  private final ObjectProperty<MobilityScan> selectedMobilityScan;
  private final DoubleProperty selectedMz;
  private final Stroke markerStroke = new BasicStroke(1.0f);

  private MZTolerance mzTolerance;
  private ScanSelection scanSelection;
  private CachedFrame cachedFrame;
  private double frameNoiseLevel;
  private double mobilityScanNoiseLevel;
  private Float rtWidth;

  private Color markerColor;
  private IMSRawDataFile rawDataFile;
  private int selectedMobilogramDatasetIndex;
  private int selectedChromatogramDatasetIndex;
  private Set<Integer> mzRangeTicDatasetIndices;

  /**
   * Creates a BorderPane layout.
   */
  public IMSRawDataOverviewPane() {
    this(0, 0, new MZTolerance(0.008, 10), new ScanSelection(1), 2f);
  }

  public IMSRawDataOverviewPane(final double frameNoiseLevel, final double mobilityScanNoiseLevel
      , final MZTolerance mzTolerance, final ScanSelection scanSelection, final Float rtWidth) {
    super();
    super.getStyleClass().add("region-match-chart-bg");
    getStylesheets().addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    chartPanel = new GridPane();
    selectedMobilogramDatasetIndex = -1;
    selectedChromatogramDatasetIndex = -1;
    mzRangeTicDatasetIndices = new HashSet<>();
    this.mzTolerance = mzTolerance;
    this.scanSelection = scanSelection;
    this.rtWidth = rtWidth;
    this.frameNoiseLevel = frameNoiseLevel;
    this.mobilityScanNoiseLevel = mobilityScanNoiseLevel;

    controlsPanel = new IMSRawDataOverviewControlPanel(this, frameNoiseLevel,
        mobilityScanNoiseLevel, mzTolerance, scanSelection, rtWidth);
    initChartPanel();

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    setCenter(chartPanel);

    selectedFrame = new SimpleObjectProperty<>();
    selectedFrame.addListener((observable, oldValue, newValue) -> onSelectedFrameChanged());

    mobilogramChart = new SimpleXYChart<>("Mobilogram chart");
    summedSpectrumChart = new SimpleXYChart<>("Summed frame spectrum");
    singleSpectrumChart = new SimpleXYChart<>("Mobility scan");
    heatmapChart = new SimpleXYZScatterPlot<>("Frame heatmap");
    ionTraceChart = new SimpleXYZScatterPlot<>("Ion trace chart");
    ticChart = new TICPlot();
    heatmapLegendCanvas = new Canvas();
    ionTraceLegendCanvas = new Canvas();
    initCharts();

    updateAxisLabels();
    initChartLegendPanels();
    chartPanel.add(new BorderPane(summedSpectrumChart), 1, 0);
    chartPanel.add(new BorderPane(ticChart), 2, 0);
    chartPanel.add(new BorderPane(singleSpectrumChart), 3, 0);
    chartPanel.add(new BorderPane(mobilogramChart, null, null, new Rectangle(1,
        HEATMAP_LEGEND_HEIGHT, javafx.scene.paint.Color.TRANSPARENT), null), 0, 1);
    chartPanel.add(new BorderPane(heatmapChart, null, null, heatmapChart.getLegendCanvas(), null)
        , 1, 1);
    chartPanel.add(new BorderPane(ionTraceChart, null, null, ionTraceChart.getLegendCanvas(),
        null), 2, 1, 1, 1);
    chartPanel.add(controlsPanel, 3, 1);

    selectedMz = new SimpleDoubleProperty();
    selectedMobilityScan = new SimpleObjectProperty<>();
    markerColor = MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT();
    initChartListeners();
    initSelectedValueListeners();
  }

  protected void onSelectedFrameChanged() {
    clearAllCharts();
    if (selectedFrame.get() == null) {
      return;
    }
    try {
      ticChart.removeDatasets(mzRangeTicDatasetIndices);
      mzRangeTicDatasetIndices.clear();
      cachedFrame = new CachedFrame((StorableFrame) selectedFrame.get(), frameNoiseLevel,
          mobilityScanNoiseLevel);
      heatmapChart.setDataset(new FrameHeatmapProvider(cachedFrame));
      mobilogramChart.addDataset(new FrameSummedMobilogramProvider(cachedFrame));
      summedSpectrumChart.addDataset(new FrameSummedSpectrumProvider(cachedFrame));
      if (selectedMobilityScan.get() != null) {
        singleSpectrumChart.addDataset(new SingleSpectrumProvider(
            cachedFrame.getMobilityScan(selectedMobilityScan.get().getMobilityScamNumber())));
      }
      ticChart.getXYPlot().clearDomainMarkers();
      ticChart.getXYPlot().addDomainMarker(
          new ValueMarker(selectedFrame.get().getRetentionTime(), markerColor, markerStroke),
          Layer.FOREGROUND);
      MZmineCore.getTaskController()
          .addTask(new BuildMultipleRanges(controlsPanel.getMobilogramRangesList(),
              Set.of(cachedFrame), rawDataFile, scanSelection, this, rtWidth));
      if (!RangeUtils
          .isJFreeRangeConnectedToGoogleRange(heatmapChart.getXYPlot().getRangeAxis().getRange(),
              selectedFrame.get().getMobilityRange())) {
        heatmapChart.getXYPlot().getRangeAxis()
            .setRange(selectedFrame.get().getMobilityRange().lowerEndpoint(),
                selectedFrame.get().getMobilityRange().upperEndpoint());
      }
      if (!RangeUtils
          .isJFreeRangeConnectedToGoogleRange(heatmapChart.getXYPlot().getDomainAxis().getRange(),
              selectedFrame.get().getDataPointMZRange())) {
        heatmapChart.getXYPlot().getDomainAxis()
            .setRange(selectedFrame.get().getDataPointMZRange().lowerEndpoint(),
                selectedFrame.get().getDataPointMZRange().upperEndpoint());
      }
//      updateValueMarkers();
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
    mobilogramChart.setDomainAxisLabel(mobilityLabel);
    mobilogramChart.setDomainAxisNumberFormatOverride(mobilityFormat);
    mobilogramChart.setRangeAxisLabel(intensityLabel);
    mobilogramChart.setRangeAxisNumberFormatOverride(intensityFormat);
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
    ionTraceChart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    ionTraceChart.setRangeAxisLabel(mobilityLabel);
    ionTraceChart.setDomainAxisNumberFormatOverride(rtFormat);
    ionTraceChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    ionTraceChart.setLegendNumberFormatOverride(intensityFormat);
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

    mobilogramChart.getXYPlot().setOrientation(PlotOrientation.HORIZONTAL);
    mobilogramChart.getXYPlot().getRangeAxis().setInverted(true);
    mobilogramChart.setShowCrosshair(false);
    mobilogramChart.switchLegendVisible();

    final ColoredXYBarRenderer singleSpectrumRenderer = new ColoredXYBarRenderer(false);
    singleSpectrumRenderer
        .setDefaultItemLabelGenerator(singleSpectrumChart.getXYPlot().getRenderer()
            .getDefaultItemLabelGenerator());
    singleSpectrumRenderer.setDefaultToolTipGenerator(singleSpectrumChart.getXYPlot().getRenderer()
        .getDefaultToolTipGenerator());
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

    heatmapChart.getXYPlot().getRangeAxis()
        .addChangeListener((axisChangeEvent) -> {
          org.jfree.data.Range range = ((ValueAxis) axisChangeEvent.getAxis()).getRange();
          if (!range.equals(mobilogramChart.getXYPlot().getDomainAxis().getRange())) {
            mobilogramChart.getXYPlot().getDomainAxis().setRange(range);
          }
        });
    mobilogramChart.getXYPlot().getDomainAxis()
        .addChangeListener((axisChangeEvent) -> {
          org.jfree.data.Range range = ((ValueAxis) axisChangeEvent.getAxis()).getRange();
          if (!range.equals(heatmapChart.getXYPlot().getRangeAxis().getRange())) {
            heatmapChart.getXYPlot().getRangeAxis().setRange(range);
          }
        });
    ChartGroup mobilityGroup = new ChartGroup(false, false, false, true);
    mobilityGroup.add(new ChartViewWrapper(heatmapChart));
    mobilityGroup.add(new ChartViewWrapper(ionTraceChart));
  }

  private void initChartPanel() {
    ColumnConstraints colConstraints = new ColumnConstraints();
    ColumnConstraints colConstraints2 = new ColumnConstraints();
    colConstraints.setPercentWidth(15);
    colConstraints2.setPercentWidth((100d - 15d) / 3d);

    RowConstraints rowConstraints = new RowConstraints();
    RowConstraints rowConstraints2 = new RowConstraints();
    rowConstraints.setPercentHeight(35);
    rowConstraints2.setPercentHeight(65);
    chartPanel.getColumnConstraints()
        .addAll(colConstraints, colConstraints2, colConstraints2, colConstraints2);
    chartPanel.getRowConstraints().addAll(rowConstraints, rowConstraints2);
  }

  private void initChartLegendPanels() {
    heatmapLegendCanvas.widthProperty().bind(heatmapChart.widthProperty());
    heatmapLegendCanvas.setHeight(HEATMAP_LEGEND_HEIGHT);
    ionTraceLegendCanvas.widthProperty().bind(ionTraceChart.widthProperty());
    ionTraceLegendCanvas.setHeight(HEATMAP_LEGEND_HEIGHT);
    heatmapChart.setLegendCanvas(heatmapLegendCanvas);
    ionTraceChart.setLegendCanvas(ionTraceLegendCanvas);
  }

  private void initChartListeners() {
    mobilogramChart.cursorPositionProperty().addListener(((observable, oldValue,
        newValue) -> selectedMobilityScan
        .set(cachedFrame.getSortedMobilityScans().get(newValue.getValueIndex()))));
    singleSpectrumChart.cursorPositionProperty().addListener(((observable, oldValue, newValue) ->
        selectedMz.set(newValue.getDomainValue())));
    summedSpectrumChart.cursorPositionProperty().addListener(((observable, oldValue, newValue) ->
        selectedMz.set(newValue.getDomainValue())));
    heatmapChart.cursorPositionProperty().addListener(
        ((observable, oldValue, newValue) -> {
          selectedMz.set(newValue.getDomainValue());
          selectedMobilityScan
              .set(((FrameHeatmapProvider) ((ColoredXYZDataset) newValue.getDataset())
                  .getXyzValueProvider()).getMobilityScanAtValueIndex(newValue.getValueIndex()));
        }));
    ticChart.cursorPositionProperty()
        .addListener(((observable, oldValue, newValue) -> setSelectedFrame(rawDataFile.getFrame(
            newValue.getScanNumber()))));
  }

  private void initSelectedValueListeners() {
    selectedMobilityScan.addListener(((observable, oldValue, newValue) -> {
      singleSpectrumChart.removeAllDatasets();
      singleSpectrumChart.addDataset(new SingleSpectrumProvider(selectedMobilityScan.get()));
      updateValueMarkers();
    }));

    selectedMz.addListener(((observable, oldValue, newValue) -> {
      if (selectedMobilogramDatasetIndex != -1) {
        mobilogramChart.removeDataSet(selectedMobilogramDatasetIndex);
      }
      Thread mobilogramCalc =
          new Thread(
              new BuildSelectedRanges(mzTolerance.getToleranceRange(selectedMz.get()),
                  Set.of(cachedFrame), rawDataFile, scanSelection, this, rtWidth));
      mobilogramCalc.start();
      ionTraceChart.setDataset(new IonTraceProvider(rawDataFile,
          mzTolerance.getToleranceRange(selectedMz.get()), rawDataFile.getDataRTRange(1),
          mobilityScanNoiseLevel));
      updateValueMarkers();
    }));
  }

  public void addRangesToChart(List<? extends ColoredXYDataset> previewMobilograms,
      List<TICDataSet> ticDataSets, List<Color> ticDatasetColors) {
    mobilogramChart.addDatasets(previewMobilograms);

    assert ticDatasetColors.size() == ticDataSets.size();
    ticChart.getChart().setNotify(false);
    for (int i = 0; i < ticDataSets.size(); i++) {
      mzRangeTicDatasetIndices.add(ticChart.addTICDataSet(ticDataSets.get(i),
          ticDatasetColors.get(i)));
    }
    ticChart.getChart().setNotify(true);
    ticChart.getChart().fireChartChanged();

    updateValueMarkers();
  }

  public void setSelectedRangesToChart(ColoredXYDataset dataset, TICDataSet ticDataSet,
      Color ticDatasetColor) {
    if (selectedMobilogramDatasetIndex != -1) {
      mobilogramChart.removeDataSet(selectedMobilogramDatasetIndex);
    }
    if (selectedChromatogramDatasetIndex != -1) {
      ticChart.removeDataSet(selectedChromatogramDatasetIndex);
    }
    selectedMobilogramDatasetIndex = mobilogramChart.addDataset(dataset);
    selectedChromatogramDatasetIndex = ticChart.addTICDataSet(ticDataSet, ticDatasetColor);
  }

  private void updateValueMarkers() {
    if (selectedMobilityScan.get() != null) {
      mobilogramChart.getXYPlot().clearDomainMarkers();
      mobilogramChart.getXYPlot()
          .addDomainMarker(new ValueMarker(selectedMobilityScan.getValue().getMobility(),
              markerColor, markerStroke), Layer.FOREGROUND);
      heatmapChart.getXYPlot().clearRangeMarkers();
      heatmapChart.getXYPlot().addRangeMarker(
          new ValueMarker(selectedMobilityScan.getValue().getMobility(), markerColor,
              markerStroke), Layer.FOREGROUND);
    }
    if (selectedMz.getValue() != null) {
      summedSpectrumChart.getXYPlot().clearDomainMarkers();
      summedSpectrumChart.getXYPlot().addDomainMarker(new ValueMarker(selectedMz.get(),
          markerColor, markerStroke), Layer.FOREGROUND);
      singleSpectrumChart.getXYPlot().clearDomainMarkers();
      singleSpectrumChart.getXYPlot().addDomainMarker(new ValueMarker(selectedMz.get(),
          markerColor, markerStroke), Layer.FOREGROUND);
      heatmapChart.getXYPlot().clearDomainMarkers();
      heatmapChart.getXYPlot()
          .addDomainMarker(new ValueMarker(selectedMz.get(), markerColor, markerStroke),
              Layer.FOREGROUND);
    }
  }

  private void clearAllCharts() {
    mobilogramChart.removeAllDatasets();
    summedSpectrumChart.removeAllDatasets();
    heatmapChart.removeAllDatasets();
    singleSpectrumChart.removeAllDatasets();
//    ticChart.removeAllDataSets(true);
//    ionTraceChart.removeAllDatasets();
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

  @Nullable
  public RawDataFile getRawDataFile() {
    return rawDataFile;
  }

  public void setRawDataFile(RawDataFile rawDataFile) {
    if (!(rawDataFile instanceof IMSRawDataFile)) {
      return;
    }
    this.rawDataFile = (IMSRawDataFile) rawDataFile;
    updateTicPlot();
    updateAxisLabels();
    setSelectedFrame(1);
  }

  protected void updateTicPlot() {
    ticChart.removeAllDataSets();
    TICDataSet dataSet = new TICDataSet(rawDataFile, scanSelection.getMatchingScans(rawDataFile),
        rawDataFile.getDataMZRange(), null);
    ticChart.addTICDataSet(dataSet, rawDataFile.getColorAWT());
    ticChart.getChart().setTitle(new TextTitle("TIC - " + rawDataFile.getName()));
    if (!RangeUtils.isJFreeRangeConnectedToGoogleRange(
        ticChart.getXYPlot().getDomainAxis().getRange(), rawDataFile.getDataRTRange(1))) {
      ticChart.getXYPlot().getDomainAxis().setRange(rawDataFile.getDataRTRange().lowerEndpoint(),
          rawDataFile.getDataRTRange().upperEndpoint());
    }
  }

  public void setMzTolerance(MZTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
  }

  public void setScanSelection(ScanSelection scanSelection) {
    this.scanSelection = scanSelection;
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
}