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

package io.github.mzmine.modules.visualization.rawdataoverviewims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gestures.SimpleDataDragGestureHandler;
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
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.frames.CanvasPane;
import io.github.mzmine.modules.visualization.rawdataoverviewims.threads.BuildMultipleMobilogramRanges;
import io.github.mzmine.modules.visualization.rawdataoverviewims.threads.BuildMultipleTICRanges;
import io.github.mzmine.modules.visualization.rawdataoverviewims.threads.BuildSelectedRanges;
import io.github.mzmine.modules.visualization.rawdataoverviewims.threads.MergeFrameThread;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter.Options;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.javafx.MZmineIconUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;
import org.kordamp.ikonli.javafx.FontIcon;

public class IMSRawDataOverviewPane extends BorderPane {

  private static final Logger logger = Logger.getLogger(IMSRawDataOverviewPane.class.getName());

  private static final int HEATMAP_LEGEND_HEIGHT = 50;

  private final GridPane chartPanel;
  private final IMSRawDataOverviewControlPanel controlsPanel;
  private final SimpleXYChart<FrameSummedMobilogramProvider> mobilogramChart;
  private final SimpleXYChart<FrameSummedSpectrumProvider> summedSpectrumChart;
  private final SimpleXYChart<SingleMobilityScanProvider> singleSpectrumChart;
  private final SimpleXYZScatterPlot<FrameHeatmapProvider> heatmapChart;
  private final SimpleXYZScatterPlot<IMSIonTraceHeatmapProvider> ionTraceChart;
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
  private final ObjectProperty<Range<Double>> selectedMz;
  private final Stroke markerStroke = new BasicStroke(1.0f);
  private final Color markerColor;
  private final Set<Integer> mzRangeTicDatasetIndices;
  private final GridPane massDetectionPane;
  // not thread safe, so we need one for building the selected and one for building all the others
  private BinningMobilogramDataAccess selectedBinningMobilogramDataAccess;
  private BinningMobilogramDataAccess rangesBinningMobilogramDataAccess;
  private MZTolerance mzTolerance;
  private MsLevelFilter msLevelFilter;
  private Frame cachedFrame;
  private double frameNoiseLevel;
  private double mobilityScanNoiseLevel;
  private int binWidth;
  private Float rtWidth;

  private IMSRawDataFile rawDataFile;
  private int selectedMobilogramDatasetIndex;
  private int selectedChromatogramDatasetIndex;

  private FontIcon massDetectionScanIcon;
  private FontIcon massDetectionFrameIcon;
  private final Label binWidthLabel = new Label("");

  /**
   * Creates a BorderPane layout.
   */
  public IMSRawDataOverviewPane() {
    this(0, 0, new MZTolerance(0.008, 10), new MsLevelFilter(Options.MS1), 2f, 1);
  }

  public IMSRawDataOverviewPane(final double frameNoiseLevel, final double mobilityScanNoiseLevel,
      final MZTolerance mzTolerance, final MsLevelFilter msLevelFilter, final Float rtWidth,
      final Integer binWidth) {
    super();
    super.getStyleClass().add("region-match-chart-bg");
    getStylesheets().addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    chartPanel = new GridPane();
    selectedMobilogramDatasetIndex = -1;
    selectedChromatogramDatasetIndex = -1;
    mzRangeTicDatasetIndices = new HashSet<>();
    selectedMz = new SimpleObjectProperty<>();
    selectedMobilityScan = new SimpleObjectProperty<>();
    this.mzTolerance = mzTolerance;
    this.msLevelFilter = msLevelFilter;
    this.rtWidth = rtWidth;
    this.frameNoiseLevel = frameNoiseLevel;
    this.mobilityScanNoiseLevel = mobilityScanNoiseLevel;
    this.binWidth = binWidth;

    controlsPanel = new IMSRawDataOverviewControlPanel(this, frameNoiseLevel,
        mobilityScanNoiseLevel, mzTolerance, msLevelFilter, rtWidth, binWidth);
    controlsPanel.addSelectedRangeListener((obs, old, newVal) -> selectedMz.set(newVal));
    initChartPanel();

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    setCenter(chartPanel);

    massDetectionPane = new GridPane();
    massDetectionPane.setPadding(new Insets(5, 5, 5, 5));
    massDetectionScanIcon = new FontIcon();
    Label massDetectionScanLabel = new Label("Masses detected in all mobility scans");
    massDetectionScanLabel.setTooltip(new Tooltip(
        "Indication if the mass detection was " + "performed successfully in all mobility scans"));
    massDetectionPane.add(massDetectionScanIcon, 1, 1);
    massDetectionPane.add(massDetectionScanLabel, 0, 1);

    massDetectionFrameIcon = new FontIcon();
    Label massDetectionFrameLabel = new Label("Masses detected in selected frame");
    massDetectionFrameLabel.setTooltip(new Tooltip(
        "Indication if the mass detection was " + "performed successfully in the selected frame"));
    massDetectionPane.add(massDetectionFrameIcon, 1, 2);
    massDetectionPane.add(massDetectionFrameLabel, 0, 2);
    final Label binWidthDesc = new Label("Default mobility bin width:");
    Tooltip.install(binWidthDesc, new Tooltip(
        "The automatically determined bin width for this dataset. Optimising this manually and setting it in the\n"
            + "IMS expander step may improve processing results."));
    massDetectionPane.add(binWidthDesc, 0, 3);
    massDetectionPane.add(this.binWidthLabel, 1, 3);
    chartPanel.add(massDetectionPane, 0, 0);

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
    chartPanel.add(new BorderPane(mobilogramChart, null, null,
        new Rectangle(1, HEATMAP_LEGEND_HEIGHT, javafx.scene.paint.Color.TRANSPARENT), null), 0, 1);
    chartPanel.add(
        new BorderPane(heatmapChart, null, null, new CanvasPane(heatmapChart.getLegendCanvas()),
            null), 1, 1);
    chartPanel.add(
        new BorderPane(ionTraceChart, null, null, new CanvasPane(ionTraceChart.getLegendCanvas()),
            null), 2, 1, 1, 1);
    chartPanel.add(controlsPanel, 3, 1);

    markerColor = MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT();
    initChartListeners();
    initSelectedValueListeners();
  }

  protected void onSelectedFrameChanged() {
    clearAllCharts();
    if (selectedFrame.get() == null) {
      return;
    }
    // ticChart.removeDatasets(mzRangeTicDatasetIndices);

    massDetectionPane.getChildren().remove(massDetectionFrameIcon);
    massDetectionFrameIcon =
        selectedFrame.get().getMassList() != null ? MZmineIconUtils.getCheckedIcon()
            : MZmineIconUtils.getUncheckedIcon();
    massDetectionPane.add(massDetectionFrameIcon, 1, 2);

    massDetectionPane.getChildren().remove(massDetectionScanIcon);
    massDetectionScanIcon =
        selectedFrame.get().getMobilityScans().stream().anyMatch(s -> s.getMassList() != null)
            ? MZmineIconUtils.getCheckedIcon() : MZmineIconUtils.getUncheckedIcon();
    massDetectionPane.add(massDetectionScanIcon, 1, 1);

    mzRangeTicDatasetIndices.clear();
    cachedFrame = new CachedFrame(selectedFrame.get(), frameNoiseLevel,
        mobilityScanNoiseLevel);//selectedFrame.get();//
    heatmapChart.setDataset(new FrameHeatmapProvider(cachedFrame));
    mobilogramChart.addDataset(new FrameSummedMobilogramProvider(cachedFrame, binWidth));
    summedSpectrumChart.addDataset(new FrameSummedSpectrumProvider(cachedFrame));
    if (selectedMobilityScan.get() != null) {
      singleSpectrumChart.addDataset(new SingleMobilityScanProvider(cachedFrame.getMobilityScan(
          Math.min(selectedMobilityScan.get().getMobilityScanNumber(),
              selectedFrame.get().getNumberOfMobilityScans() - 1))));
    }
    MZmineCore.getTaskController().addTask(
        new BuildMultipleMobilogramRanges(controlsPanel.getMobilogramRangesList(),
            Set.of(cachedFrame), rawDataFile, this::addMobilogramRangesToChart,
            rangesBinningMobilogramDataAccess, new Date()));
    if (!RangeUtils.isGuavaRangeEnclosingJFreeRange(
        heatmapChart.getXYPlot().getRangeAxis().getRange(),
        selectedFrame.get().getMobilityRange())) {
      Range<Double> mobilityRange = selectedFrame.get().getMobilityRange();
      if (mobilityRange != null && !mobilityRange.isEmpty()) {
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

    final Color boxClr = MZmineCore.getConfiguration().getDefaultColorPalette()
        .getNegativeColorAWT();
    final Color transparent = new Color(0.5f, 0f, 0f, 0.5f);
    for (IonMobilityMsMsInfo info : selectedFrame.get().getImsMsMsInfos()) {
      final double mobLow = selectedFrame.get()
          .getMobilityForMobilityScanNumber(info.getSpectrumNumberRange().lowerEndpoint());
      final double mobHigh = selectedFrame.get()
          .getMobilityForMobilityScanNumber(info.getSpectrumNumberRange().upperEndpoint());
      final var mzRange = info.getIsolationWindow();
      if (mzRange == null) {
        continue;
      }

      var rect = new Rectangle2D.Double(mzRange.lowerEndpoint(), Math.min(mobLow, mobHigh),
          RangeUtils.rangeLength(mzRange), Math.abs(mobHigh - mobLow));
      final XYShapeAnnotation precursorIso = new XYShapeAnnotation(rect, new BasicStroke(1f),
          Color.red, null);
      heatmapChart.getXYPlot().addAnnotation(precursorIso);
    }
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
    ticChart.getMouseAdapter().addGestureHandler(new SimpleDataDragGestureHandler((start, end) -> {
      final Range<Double> rtRange = Range.closed(start.getX(), end.getX());
      final ScanSelection selection = new ScanSelection(msLevelFilter).cloneWithNewRtRange(rtRange);
      MZmineCore.getTaskController().addTask(
          new MergeFrameThread(rawDataFile, selection, binWidth, mobilityScanNoiseLevel,
              f -> FxThread.runLater(() -> setSelectedFrame(f))));
    }));

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
          new BuildSelectedRanges(selectedMz.get(), Set.of(cachedFrame), rawDataFile,
              new ScanSelection(msLevelFilter), rtWidth, selectedBinningMobilogramDataAccess,
              this::setSelectedMobilogram, c -> this.setSelectedChromatogram(c,
              MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT())));
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
    Platform.runLater(() -> {
      mobilogramChart.addDatasets(previewMobilograms);
      updateValueMarkers();
    });
  }

  public void setTICRangesToChart(List<TICDataSet> ticDataSets, List<Color> ticDatasetColors) {
    assert ticDatasetColors.size() == ticDataSets.size();
    ticChart.getChart().setNotify(false);
    ticChart.removeDatasets(mzRangeTicDatasetIndices);
    for (int i = 0; i < ticDataSets.size(); i++) {
      mzRangeTicDatasetIndices.add(
          ticChart.addTICDataSet(ticDataSets.get(i), ticDatasetColors.get(i)));
    }
    ticChart.getChart().setNotify(true);
    ticChart.getChart().fireChartChanged();
  }

  public void setSelectedRangesToChart(ColoredXYDataset dataset, TICDataSet ticDataSet,
      Color ticDatasetColor) {
    if (selectedMobilogramDatasetIndex != -1) {
      mobilogramChart.removeDataSet(selectedMobilogramDatasetIndex, false);
    }
    if (selectedChromatogramDatasetIndex != -1) {
      ticChart.removeDataSet(selectedChromatogramDatasetIndex);
    }
    selectedMobilogramDatasetIndex = mobilogramChart.addDataset(dataset);
    selectedChromatogramDatasetIndex = ticChart.addTICDataSet(ticDataSet, ticDatasetColor);
  }

  public void setSelectedMobilogram(ColoredXYDataset mobilogram) {
    FxThread.runLater(() -> {
      if (selectedMobilogramDatasetIndex != -1) {
        mobilogramChart.removeDataSet(selectedMobilogramDatasetIndex, false);
      }
      selectedMobilogramDatasetIndex = mobilogramChart.addDataset(mobilogram);
    });
  }

  public void setSelectedChromatogram(TICDataSet dataset, Color color) {
    FxThread.runLater(() -> {
      if (selectedChromatogramDatasetIndex != -1) {
        ticChart.removeDataSet(selectedChromatogramDatasetIndex);
      }
      selectedChromatogramDatasetIndex = ticChart.addTICDataSet(dataset, color);
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

  protected void updateTicPlot() {
    ticChart.removeAllDataSets();
    mzRangeTicDatasetIndices.clear();
    final ScanSelection scanSel = new ScanSelection(msLevelFilter);
    Thread thread = new Thread(
        new BuildMultipleTICRanges(controlsPanel.getMobilogramRangesList(), rawDataFile, scanSel,
            this));
    thread.start();
    TICDataSet dataSet = new TICDataSet(rawDataFile,
        new ScanSelection(msLevelFilter).getMatchingScans(rawDataFile),
        rawDataFile.getDataMZRange(), null);
    if (RangeUtils.isDefaultJFreeRange(ticChart.getXYPlot().getDomainAxis().getRange())
        || !RangeUtils.isJFreeRangeConnectedToGuavaRange(
        ticChart.getXYPlot().getDomainAxis().getRange(), rawDataFile.getDataRTRange(1))) {
      ticChart.getXYPlot().getDomainAxis().setRange(RangeUtils.guavaToJFree(
          RangeUtils.getPositiveRange(rawDataFile.getDataRTRange(), 0.001f)));
    }
    // add tic dataset after setting the range, so autoscale on the y axis uses the correct range.
    ticChart.addTICDataSet(dataSet, rawDataFile.getColorAWT());
  }

  public void addRanges(List<Range<Double>> ranges) {
    controlsPanel.addRanges(ranges);
    updateTicPlot();
  }

  private void clearAllCharts() {
    mobilogramChart.removeAllDatasets();
    summedSpectrumChart.removeAllDatasets();
    heatmapChart.removeAllDatasets();
    heatmapChart.getXYPlot().clearAnnotations();
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

  @Nullable
  public RawDataFile getRawDataFile() {
    return rawDataFile;
  }

  public void setRawDataFile(RawDataFile rawDataFile) {
    if (!(rawDataFile instanceof IMSRawDataFile)) {
      return;
    }
    this.rawDataFile = (IMSRawDataFile) rawDataFile;
    binWidthLabel.setText(
        "%d".formatted(BinningMobilogramDataAccess.getRecommendedBinWidth(this.rawDataFile)));
    rangesBinningMobilogramDataAccess = EfficientDataAccess.of(this.rawDataFile, binWidth);
    selectedBinningMobilogramDataAccess = EfficientDataAccess.of(this.rawDataFile, binWidth);
    updateTicPlot();
    updateAxisLabels();
    ionTraceChart.removeAllDatasets();
    setSelectedFrame(((IMSRawDataFile) rawDataFile).getFrames().stream().findFirst().get());
  }

  public void setMzTolerance(MZTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
  }

  public void setMsLevelFilter(MsLevelFilter msLevelFilter) {
    this.msLevelFilter = msLevelFilter;
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
}
