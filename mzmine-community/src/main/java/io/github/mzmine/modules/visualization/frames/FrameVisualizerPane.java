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

package io.github.mzmine.modules.visualization.frames;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.CachedFrame;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.FrameHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.FrameSummedMobilogramProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.FrameSummedSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.rawdataoverviewims.threads.BuildMultipleMobilogramRanges;
import io.github.mzmine.modules.visualization.rawdataoverviewims.threads.BuildSelectedRanges;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;

public class FrameVisualizerPane extends BorderPane {

  private static final Logger logger = Logger.getLogger(FrameVisualizerPane.class.getName());

  private final GridPane chartPanel = new GridPane();

  protected final SimpleXYChart<FrameSummedMobilogramProvider> mobilogramChart;
  protected final SimpleXYChart<FrameSummedSpectrumProvider> summedSpectrumChart;
  protected final SimpleXYZScatterPlot<FrameHeatmapProvider> heatmapChart;

  protected final Canvas heatmapLegendCanvas;
  protected final NumberFormat rtFormat;
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;
  private final Stroke markerStroke = new BasicStroke(1.0f);

  protected final ObjectProperty<Frame> selectedFrame;
  protected final ObjectProperty<MobilityScan> selectedMobilityScan = new SimpleObjectProperty<>();
  protected final ObjectProperty<Range<Double>> selectedMz = new SimpleObjectProperty<>();

  //not thread safe, so we need one for building the selected and one for building all the others
  protected BinningMobilogramDataAccess selectedBinningMobilogramDataAccess;
  protected BinningMobilogramDataAccess rangesBinningMobilogramDataAccess;

  protected MZTolerance mzTolerance;

  protected Frame cachedFrame;
  protected double frameNoiseLevel;
  protected double mobilityScanNoiseLevel;
  protected int binWidth;

  protected final Color markerColor;

  protected int selectedMobilogramDatasetIndex;
  private final ChartGroup mobilityGroup = new ChartGroup(false, false, false, true);
  private final ChartGroup mzGroup = new ChartGroup(false, false, true, false);
  private final double legendHeight;
  private ScanSelection scanSelection;
  private IMSRawDataFile rawDataFile;

  private final ObservableList<Range<Double>> mobilogramRangesList;

  public FrameVisualizerPane(@NotNull MZTolerance mzTolerance, double frameNoiseLevel,
      double mobilityScanNoiseLevel, int binWidth, final double legendHeight,
      IMSRawDataFile rawDataFile, ScanSelection scanSelection,
      @NotNull ObservableList<Range<Double>> mobilogramRangesList) {
    super();
    super.getStyleClass().add("region-match-chart-bg");
    getStylesheets().addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    this.rawDataFile = rawDataFile;
    this.scanSelection = scanSelection;
    this.mobilogramRangesList = mobilogramRangesList;
    selectedMobilogramDatasetIndex = -1;
    this.mzTolerance = mzTolerance;
    this.frameNoiseLevel = frameNoiseLevel;
    this.mobilityScanNoiseLevel = mobilityScanNoiseLevel;
    this.binWidth = binWidth;
    this.legendHeight = legendHeight;

    heatmapLegendCanvas = new Canvas();

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

    initCharts();
    initChartPanel();
    updateAxisLabels();
    initChartLegendPanels();
    chartPanel.add(new BorderPane(summedSpectrumChart), 1, 0);
    chartPanel.add(new BorderPane(mobilogramChart, null, null,
        new Rectangle(1, legendHeight, javafx.scene.paint.Color.TRANSPARENT), null), 0, 1);
    chartPanel.add(
        new BorderPane(heatmapChart, null, null, new CanvasPane(heatmapChart.getLegendCanvas()),
            null), 1, 1);

    markerColor = MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT();
    initChartListeners();
    initSelectedValueListeners();

    setCenter(chartPanel);
  }

  private void initChartPanel() {
    ColumnConstraints colConstraints = new ColumnConstraints();
    ColumnConstraints colConstraints2 = new ColumnConstraints();
    colConstraints.setMinWidth(100);
    colConstraints.setPrefWidth(200);
    colConstraints.setMaxWidth(300);
    colConstraints2.setHgrow(Priority.ALWAYS);

    RowConstraints rowConstraints = new RowConstraints();
    RowConstraints rowConstraints2 = new RowConstraints();
    rowConstraints.setPercentHeight(35);
    rowConstraints2.setPercentHeight(65);
    chartPanel.getColumnConstraints().addAll(colConstraints, colConstraints2);
    chartPanel.getRowConstraints().addAll(rowConstraints, rowConstraints2);
  }

  private void clearAllCharts() {
    mobilogramChart.removeAllDatasets();
    summedSpectrumChart.removeAllDatasets();
    heatmapChart.removeAllDatasets();
  }

  protected void onSelectedFrameChanged() {
    clearAllCharts();
    if (selectedFrame.get() == null) {
      return;
    }

    logger.finest(() -> "Selected frame changed to " + ScanUtils.scanToString(selectedFrame.get()));

    cachedFrame = new CachedFrame(selectedFrame.get(), frameNoiseLevel,
        mobilityScanNoiseLevel);//selectedFrame.get();//
    heatmapChart.setDataset(new FrameHeatmapProvider(cachedFrame));
    mobilogramChart.addDataset(new FrameSummedMobilogramProvider(cachedFrame, binWidth));
    summedSpectrumChart.addDataset(new FrameSummedSpectrumProvider(cachedFrame));
    if (selectedMobilityScan.get() != null) {
      selectedMobilityScan.set(
          cachedFrame.getMobilityScan(selectedMobilityScan.get().getMobilityScanNumber()));
    }

    MZmineCore.getTaskController().addTask(
        new BuildMultipleMobilogramRanges(mobilogramRangesList, Set.of(cachedFrame), rawDataFile,
            this::addMobilogramRangesToChart, rangesBinningMobilogramDataAccess, new Date()));

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
        (selectedFrame.get() != null) ? selectedFrame.get().getMobilityType().getAxisLabel()
            : "Mobility";
    mobilogramChart.setRangeAxisLabel(mobilityLabel);
    mobilogramChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    mobilogramChart.setDomainAxisLabel(intensityLabel);
    mobilogramChart.setDomainAxisNumberFormatOverride(intensityFormat);
    summedSpectrumChart.setDomainAxisLabel(mzLabel);
    summedSpectrumChart.setDomainAxisNumberFormatOverride(mzFormat);
    summedSpectrumChart.setRangeAxisLabel(intensityLabel);
    summedSpectrumChart.setRangeAxisNumberFormatOverride(intensityFormat);
    heatmapChart.setDomainAxisLabel(mzLabel);
    heatmapChart.setDomainAxisNumberFormatOverride(mzFormat);
    heatmapChart.setRangeAxisLabel(mobilityLabel);
    heatmapChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    heatmapChart.setLegendNumberFormatOverride(intensityFormat);
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

    heatmapChart.setShowCrosshair(false);
    heatmapChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    heatmapChart.setDefaultPaintscaleLocation(RectangleEdge.BOTTOM);

    mzGroup.add(new ChartViewWrapper(heatmapChart));
    mzGroup.add(new ChartViewWrapper(summedSpectrumChart));

    mobilityGroup.add(new ChartViewWrapper(heatmapChart));
    mobilityGroup.add(new ChartViewWrapper(mobilogramChart));
  }

  private void initChartLegendPanels() {
    heatmapLegendCanvas.setHeight(legendHeight);
    heatmapChart.setLegendCanvas(heatmapLegendCanvas);
  }

  private void initChartListeners() {
    mobilogramChart.cursorPositionProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue.getValueIndex() != -1) {
        selectedMobilityScan.set(
            cachedFrame.getSortedMobilityScans().get(newValue.getValueIndex() * binWidth));
      }
    }));
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
  }

  private void initSelectedValueListeners() {
    selectedMobilityScan.addListener(((observable, oldValue, newValue) -> {
      logger.finest(() -> "Selected mobility scan changed to " + ScanUtils.scanToString(newValue));
//      singleSpectrumChart.removeAllDatasets();
//      singleSpectrumChart.addDataset(new SingleMobilityScanProvider(selectedMobilityScan.get()));
      updateValueMarkers();
    }));

    selectedMz.addListener(((observable, oldValue, newValue) -> {
      logger.finest(() -> String.format("Selected m/z changed to %s", newValue));
      if (selectedMobilogramDatasetIndex != -1) {
        mobilogramChart.removeDataSet(selectedMobilogramDatasetIndex, false);
      }
//      controlsPanel.setRangeToMobilogramRangeComp(newValue);
      Thread mobilogramCalc = new Thread(
          new BuildSelectedRanges(selectedMz.get(), Set.of(cachedFrame),
              (IMSRawDataFile) cachedFrame.getDataFile(), scanSelection, 0f,
              selectedBinningMobilogramDataAccess, this::setSelectedMobilogram, null));
      mobilogramCalc.start();
//      float rt = selectedFrame.get().getRetentionTime();
//      ionTraceChart.setDataset(new IMSIonTraceHeatmapProvider(rawDataFile, selectedMz.get(),
//          Range.closed(Math.max(rawDataFile.getDataRTRange(1).lowerEndpoint(), rt - rtWidth / 2),
//              Math.min(rawDataFile.getDataRTRange(1).upperEndpoint(), rt + rtWidth / 2)),
//          mobilityScanNoiseLevel));
      updateValueMarkers();
    }));
  }

  public void addMobilogramRangesToChart(List<? extends ColoredXYDataset> previewMobilograms) {
    MZmineCore.runLater(() -> {
      mobilogramChart.addDatasets(previewMobilograms);
      updateValueMarkers();
    });
  }

  public void setSelectedMobilogram(ColoredXYDataset mobilogram) {
    MZmineCore.runLater(() -> {
      if (selectedMobilogramDatasetIndex != -1) {
        mobilogramChart.removeDataSet(selectedMobilogramDatasetIndex, false);
      }
      selectedMobilogramDatasetIndex = mobilogramChart.addDataset(mobilogram);
    });
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
    }
    if (selectedMz.getValue() != null) {
      summedSpectrumChart.getXYPlot().clearDomainMarkers();
      summedSpectrumChart.getXYPlot().addDomainMarker(
          new ValueMarker(RangeUtils.rangeCenter(selectedMz.get()), markerColor, markerStroke),
          Layer.FOREGROUND);
      heatmapChart.getXYPlot().clearDomainMarkers();
      heatmapChart.getXYPlot().addDomainMarker(
          new ValueMarker(RangeUtils.rangeCenter(selectedMz.get()), markerColor, markerStroke),
          Layer.FOREGROUND);
    }
    if (selectedFrame.get() != null) {
    }
  }

  public Frame getSelectedFrame() {
    return selectedFrame.get();
  }

  public void setSelectedFrame(Frame frame) {
    if (frame.getDataFile() != getRawDataFile()) {
      setRawDataFile(rawDataFile);
    }
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

  public void setBinWidth(int binWidth) {
    // check the bin width the pane was set to before, not the actual computed bin width.
    if (binWidth != this.binWidth && selectedFrame.get() != null) {
      this.binWidth = binWidth;
      rangesBinningMobilogramDataAccess = EfficientDataAccess.of(this.rawDataFile, binWidth);
      selectedBinningMobilogramDataAccess = EfficientDataAccess.of(this.rawDataFile, binWidth);
    }
  }

  public void setMzTolerance(MZTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
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
    rangesBinningMobilogramDataAccess = EfficientDataAccess.of(this.rawDataFile, binWidth);
    selectedBinningMobilogramDataAccess = EfficientDataAccess.of(this.rawDataFile, binWidth);
    updateAxisLabels();
    setSelectedFrame(((IMSRawDataFile) rawDataFile).getFrames().stream().findFirst().get());
  }
}
