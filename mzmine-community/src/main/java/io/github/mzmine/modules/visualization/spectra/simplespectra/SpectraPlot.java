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
package io.github.mzmine.modules.visualization.spectra.simplespectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.chartbasics.ChartLogics;
import io.github.mzmine.gui.chartbasics.JFreeChartUtils;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartthemes.LabelColorMatch;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.MSLevel;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResultsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.IsotopesDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.MassListDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.PeakListDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.ContinuousRenderer;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.PeakRenderer;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraItemLabelGenerator;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraMassListRenderer;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.awt.Color;
import java.awt.Paint;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.util.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.Layer;
import org.jfree.data.xy.XYDataset;

/**
 *
 */
public class SpectraPlot extends EChartViewer implements LabelColorMatch {

  private static final Logger logger = Logger.getLogger(SpectraPlot.class.getName());
  // initially, plotMode is set to null, until we load first scan
  private final ObjectProperty<SpectrumPlotType> plotMode;
  /**
   * Contains coordinated of labels for each dataset. It is supposed to be updated by
   * {@link SpectraItemLabelGenerator}.
   */
  private final Map<XYDataset, List<Pair<Double, Double>>> datasetToLabelsCoords = new HashMap<>();
  private final JFreeChart chart;
  private final XYPlot plot;
  private final TextTitle chartTitle;
  private final TextTitle chartSubTitle;
  /**
   * If true, the labels of the data set will have the same color as the data set itself
   */
  protected BooleanProperty matchLabelColors;
  protected ObjectProperty<SpectrumCursorPosition> cursorPosition;
  private final ObjectProperty<MZTolerance> mzToleranceProperty = new SimpleObjectProperty<>();
  private final ObjectProperty<Range<Double>> selectedMzRangeProperty = new SimpleObjectProperty<>();

  // Spectra processing
  protected DataPointProcessingController controller;
  protected EStandardChartTheme theme;
  private Marker mzMarker;
  private boolean showCursor = false;
  private boolean isotopesVisible = true, peaksVisible = true, itemLabelsVisible = true, dataPointsVisible = false;
  private boolean processingAllowed;

  public SpectraPlot() {
    this(false);
  }

  public SpectraPlot(boolean processingAllowed) {
    this(processingAllowed, true);
  }

  public SpectraPlot(boolean processingAllowed, boolean showLegend) {

    super(ChartFactory.createXYLineChart("", // title
        "m/z", // x-axis label
        "Intensity", // y-axis label
        null, // data set
        PlotOrientation.VERTICAL, // orientation
        true, // isotopeFlag, // create legend?
        true, // generate tooltips?
        false // generate URLs?
    ));

    plotMode = new SimpleObjectProperty<>(SpectrumPlotType.AUTO);
    addPlotModeListener();

    // setBackground(Color.white);
    setCursor(Cursor.CROSSHAIR);

    cursorPosition = new SimpleObjectProperty<>();
    cursorPosition.addListener(((observable, oldValue, newValue) -> updateDomainMarker(newValue)));

    ObjectBinding<Range<Double>> selectedMzBinding = Bindings.createObjectBinding(
        () -> getMzRange(cursorPositionProperty().getValue(), mzToleranceProperty.getValue()),
        cursorPosition, mzToleranceProperty);
    selectedMzRangeProperty.bind(selectedMzBinding);

    initializeSpectrumMouseListener();
    matchLabelColors = new SimpleBooleanProperty(false);
    addMatchLabelColorsListener();

    // initialize the chart by default time series chart from factory
    chart = getChart();
    chart.setBackgroundPaint(Color.white);

    plot = chart.getXYPlot();
    theme = MZmineCore.getConfiguration().getDefaultChartTheme();

    // title
    chartTitle = chart.getTitle();
    chartSubTitle = new TextTitle();
    chart.addSubtitle(chartSubTitle);

    // disable maximum size (we don't want scaling)
    // setMaximumDrawWidth(Integer.MAX_VALUE);
    // setMaximumDrawHeight(Integer.MAX_VALUE);
    // setMinimumDrawHeight(0);

    // set rendering order
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    // set the X axis (retention time) properties
    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setNumberFormatOverride(mzFormat);
    xAxis.setUpperMargin(0.01); // have some margin so m/z labels are not cut off
    xAxis.setLowerMargin(0.01);

    // set the Y axis (intensity) properties
    NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
    yAxis.setNumberFormatOverride(intensityFormat);
    yAxis.setUpperMargin(0.1); // some margin for m/z labels
    // only allow positive values for the axes
    ChartLogics.setAxesTypesPositive(chart);

    // reset zoom history
    ZoomHistory history = getZoomHistory();
    if (history != null) {
      history.clear();
    }

    theme.apply(this);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    getChart().getLegend().setVisible(showLegend);

    setMinHeight(50);

    // set processingAllowed
    setProcessingAllowed(processingAllowed);

    // If the plot is changed then clear the map containing coordinates of labels. New values will be
    // added by the SpectraItemLabelGenerator
    getChart().getXYPlot().addChangeListener(event -> datasetToLabelsCoords.clear());
  }

  public void setLegendVisible(boolean state) {
    final LegendTitle legend = getChart().getLegend();
    legend.setVisible(state);
    theme.setShowLegend(state);
    //theme.apply paints first series in white/black which is wrong
  }

  public MZTolerance getMzTolerance() {
    return mzToleranceProperty.get();
  }

  public ObjectProperty<MZTolerance> mzToleranceProperty() {
    return mzToleranceProperty;
  }

  public ObjectProperty<Range<Double>> selectedMzRangeProperty() {
    return selectedMzRangeProperty;
  }

  private Range<Double> getMzRange(SpectrumCursorPosition pos, MZTolerance tolerance) {
    if (pos == null) {
      return null;
    }
    if (tolerance == null) {
      return Range.singleton(pos.mz);
    } else {
      return tolerance.getToleranceRange(pos.mz);
    }
  }

  public void setShowCursor(boolean showCursor) {
    this.showCursor = showCursor;
    updateDomainMarker(cursorPosition.getValue());
  }

  public SpectrumPlotType getPlotMode() {
    return plotMode.getValue();
  }

  /**
   * This will set either centroid or continuous renderer to the first data set, assuming that
   * dataset with index 0 contains the raw data.
   */
  public void setPlotMode(SpectrumPlotType plotMode) {
    this.plotMode.setValue(plotMode);
  }

  public ObjectProperty<SpectrumPlotType> plotModeProperty() {
    return plotMode;
  }

  private void addPlotModeListener() {
    assert plotMode != null;

    plotMode.addListener(((observable, oldValue, newValue) -> {
      int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
      for (int i = 0; i < numDatasets; i++) {
        XYDataset dataset = plot.getDataset(i);
        if (!(dataset instanceof ScanDataSet)) {
          continue;
        }

        XYItemRenderer oldRenderer = plot.getRendererForDataset(dataset);
        Paint clr = oldRenderer.getDefaultPaint();

        // if getPlotMode() == AUTO then we use the scan's type, if not we use getPlotMode()
        SpectrumPlotType typeForDataSet =
            (getPlotMode() == SpectrumPlotType.AUTO) ? SpectrumPlotType.fromScan(
                ((ScanDataSet) dataset).getScan()) : getPlotMode();

        XYItemRenderer newRenderer;
        if (typeForDataSet == SpectrumPlotType.CENTROID) {
          newRenderer = new PeakRenderer((Color) clr, false);
          ((PeakRenderer) newRenderer).setBarPainter(new StandardXYBarPainter());
        } else {
          newRenderer = new ContinuousRenderer((Color) clr, false);
          ((ContinuousRenderer) newRenderer).setDefaultShapesVisible(dataPointsVisible);
        }

        // Add label generator for the dataset
        newRenderer.setDefaultItemLabelGenerator(oldRenderer.getDefaultItemLabelGenerator());
        newRenderer.setDefaultItemLabelsVisible(itemLabelsVisible);
        newRenderer.setDefaultItemLabelPaint(oldRenderer.getDefaultItemLabelPaint());

        plot.setRenderer(i, newRenderer);
      }
    }));
  }

  public XYPlot getXYPlot() {
    return plot;
  }

  void switchItemLabelsVisible() {
    itemLabelsVisible = !itemLabelsVisible;
    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int i = 0; i < numDatasets; i++) {
      XYItemRenderer renderer = plot.getRenderer(i);
      renderer.setDefaultItemLabelsVisible(itemLabelsVisible, false);
    }
    if (isNotifyChange()) {
      fireChangeEvent();
    }
  }

  void switchDataPointsVisible() {
    applyWithNotifyChanges(false, () -> {

      dataPointsVisible = !dataPointsVisible;
      int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
      for (int i = 0; i < numDatasets; i++) {
        XYItemRenderer renderer = plot.getRenderer(i);
        if (!(renderer instanceof ContinuousRenderer contRend)) {
          continue;
        }
        contRend.setDefaultShapesVisible(dataPointsVisible);
      }
    });
  }

  void switchPickedPeaksVisible() {
    peaksVisible = !peaksVisible;
    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int i = 0; i < numDatasets; i++) {
      XYDataset dataSet = plot.getDataset(i);
      if (!(dataSet instanceof PeakListDataSet)) {
        continue;
      }
      XYItemRenderer renderer = plot.getRenderer(i);
      renderer.setDefaultSeriesVisible(peaksVisible, false);
    }
    if (isNotifyChange()) {
      fireChangeEvent();
    }
  }

  void switchIsotopePeaksVisible() {
    isotopesVisible = !isotopesVisible;
    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int i = 0; i < numDatasets; i++) {
      XYDataset dataSet = plot.getDataset(i);
      if (!(dataSet instanceof IsotopesDataSet)) {
        continue;
      }
      XYItemRenderer renderer = plot.getRenderer(i);
      renderer.setDefaultSeriesVisible(isotopesVisible, false);
    }
    if (isNotifyChange()) {
      fireChangeEvent();
    }
  }

  public void setTitle(String title, String subTitle) {
    if (title != null) {
      chartTitle.setText(title);
    }
    if (subTitle != null) {
      chartSubTitle.setText(subTitle);
    }
  }


  /*
   * public void mouseClicked(MouseEvent event) {
   *
   * // let the parent handle the event (selection etc.) super.mouseClicked(event);
   *
   * // request focus to receive key events requestFocus(); }
   */

  public synchronized void removeAllDataSets() {
    applyWithNotifyChanges(false, () -> {
      // if the data sets are removed, we have to cancel the tasks.
      if (controller != null) {
        controller.cancelTasks();
      }
      controller = null;

      int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
      for (int i = 0; i < numDatasets; i++) {
        plot.setDataset(i, null);
      }
      plot.clearDomainMarkers();
      this.getDatasetToLabelsCoords().clear();
    });
  }

  public synchronized void addDataSet(XYDataset dataSet, Color color, boolean transparency,
      boolean notifyChange) {
    addDataSet(dataSet, color, transparency, true, notifyChange);
  }

  public synchronized void addDataSet(XYDataset dataSet, Color color, boolean transparency,
      boolean addPrecursorMarkers, boolean notifyChange) {
    SpectraItemLabelGenerator labelGenerator = new SpectraItemLabelGenerator(this);
    addDataSet(dataSet, color, transparency, labelGenerator, addPrecursorMarkers, notifyChange);
  }

  // add Dataset with label generator
  public synchronized void addDataSet(XYDataset dataSet, Color color, boolean transparency,
      XYItemLabelGenerator labelGenerator, boolean notifyChange) {
    addDataSet(dataSet, color, transparency, labelGenerator, true, notifyChange);
  }

  public synchronized void addDataSet(XYDataset dataSet, Color color, boolean transparency,
      XYItemLabelGenerator labelGenerator, boolean addPrecursorMarkers, boolean notifyChange) {

    XYItemRenderer newRenderer;

    if (dataSet instanceof ScanDataSet scanDataSet) {
      Scan scan = scanDataSet.getScan();

      // if getPlotMode() == AUTO then we use the scan's type, if not we use getPlotMode()
      SpectrumPlotType typeForDataSet =
          (getPlotMode() == SpectrumPlotType.AUTO) ? SpectrumPlotType.fromScan(scan)
              : getPlotMode();

      if (typeForDataSet == SpectrumPlotType.CENTROID) {
        newRenderer = new PeakRenderer(color, transparency);
      } else {
        newRenderer = new ContinuousRenderer(color, transparency);
        ((ContinuousRenderer) newRenderer).setDefaultShapesVisible(dataPointsVisible);
      }
    } else if (dataSet instanceof MassListDataSet) {
      newRenderer = new SpectraMassListRenderer(color);
    } else {
      newRenderer = new PeakRenderer(color, transparency);
    }

    addDataSet(dataSet, color, transparency, newRenderer, labelGenerator, addPrecursorMarkers,
        notifyChange);
  }

  public synchronized void addDataSet(XYDataset dataSet, Color color, boolean transparency,
      XYItemRenderer newRenderer, boolean addPrecursorMarkers, boolean notifyChange) {
    addDataSet(dataSet, color, transparency, newRenderer, new SpectraItemLabelGenerator(this),
        addPrecursorMarkers, notifyChange);
  }

  public synchronized void addDataSet(XYDataset dataSet, Color color, boolean transparency,
      XYItemRenderer newRenderer, XYItemLabelGenerator labelGenerator, boolean addPrecursorMarkers,
      boolean notifyChange) {
    applyWithNotifyChanges(notifyChange, () -> {

      if (addPrecursorMarkers && dataSet instanceof ScanDataSet scanDataSet) {
        Scan scan = scanDataSet.getScan();
        // add all precursors for MS>=2
        if (scan != null && scan.getMSLevel() > 1) {
          addPrecursorMarkers(scan);
        }
      }

      // Add label generator for the dataset
      newRenderer.setDefaultItemLabelGenerator(labelGenerator);
      newRenderer.setDefaultItemLabelsVisible(itemLabelsVisible);
      if (matchLabelColors.get()) {
        newRenderer.setDefaultItemLabelPaint(color);
      } else {
        if (MZmineCore.getConfiguration().isDarkMode()) {
          newRenderer.setDefaultItemLabelPaint(Color.white);
        } else {
          newRenderer.setDefaultItemLabelPaint(Color.black);
        }
      }
      ((AbstractRenderer) newRenderer).setItemLabelAnchorOffset(1.3d);

      int nextDatasetId = JFreeChartUtils.getNextDatasetIndex(plot);
      plot.setDataset(nextDatasetId, dataSet);
      plot.setRenderer(nextDatasetId, newRenderer);

      if (dataSet instanceof ScanDataSet) {
        checkAndRunController();
      }
    });
  }

  public void addPrecursorMarkers(Scan scan) {
    addPrecursorMarkers(scan, Color.GRAY, 0.5f);
  }

  public void addPrecursorMarkers(Scan scan, Color color, float alpha) {
    boolean showPrecursorWindow = MZmineCore.getConfiguration().getPreferences()
        .getValue(MZminePreferences.showPrecursorWindow);
    if (scan.getMSLevel() == 2) {
      final Double prmz = scan.getPrecursorMz();
      if (prmz != null) {
        final MsMsInfo info = scan.getMsMsInfo();
        if (showPrecursorWindow && info != null && info.getIsolationWindow() != null) {
          addDomainMarker(info.getIsolationWindow(), color, alpha);
        } else {
          addDomainMarker(prmz, color, alpha);
        }
      }
    } else if (scan.getMSLevel() > 2) {
      // add all parent precursors
      if (scan.getMsMsInfo() instanceof MSnInfoImpl msn) {
        for (var info : msn.getPrecursors()) {
          if (showPrecursorWindow && info.getIsolationWindow() != null) {
            addDomainMarker(info.getIsolationWindow(), color, alpha);
          } else {
            addDomainMarker(info.getIsolationMz(), color, alpha);
          }
        }
      }
    }
  }


  /**
   * Add marker to background
   *
   * @param pos position to mark in spectrum by mz
   */
  private void updateDomainMarker(SpectrumCursorPosition pos) {
    if (pos == null || !showCursor) {
      if (mzMarker == null) {
        getXYPlot().removeDomainMarker(mzMarker, Layer.BACKGROUND);
      }
      return;
    }
    if (mzMarker == null) {
      Color color = MZmineCore.getConfiguration().getDefaultColorPalette().getNeutralColorAWT();
      mzMarker = addDomainMarker(pos.getMz(), color, 0.7f);
      getXYPlot().addDomainMarker(mzMarker);
    } else {
      if (mzMarker instanceof ValueMarker valueMarker) {
        valueMarker.setValue(pos.getMz());
        if (!getChart().getXYPlot().getDomainMarkers(Layer.BACKGROUND).contains(mzMarker)) {
          getXYPlot().addDomainMarker(mzMarker, Layer.BACKGROUND);
        }
      }
    }
  }


  public synchronized void removePeakListDataSets() {
    applyWithNotifyChanges(false, () -> {

      int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
      for (int i = 0; i < numDatasets; i++) {
        XYDataset dataSet = plot.getDataset(i);
        if (dataSet instanceof PeakListDataSet) {
          plot.setDataset(i, null);
        }
      }
    });
  }

  public ScanDataSet getMainScanDataSet() {
    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int i = 0; i < numDatasets; i++) {
      XYDataset dataSet = plot.getDataset(i);
      if (dataSet instanceof ScanDataSet) {
        return (ScanDataSet) dataSet;
      }
    }
    return null;
  }

  /**
   * Checks if the spectra processing is enabled & allowed and executes the controller if it is.
   * Processing is forbidden for instances of ParameterSetupDialogWithScanPreviews
   */
  public void checkAndRunController() {

    // if controller != null, processing on the current spectra has already
    // been executed. When
    // loading a new spectrum, the controller is set to null in
    // removeAllDataSets()
    DataPointProcessingManager inst = DataPointProcessingManager.getInst();

    if (!isProcessingAllowed() || !inst.isEnabled()) {
      return;
    }

    if (controller != null) {
      controller = null;
    }

    // if a controller is re-run then delete previous results
    removeDataPointProcessingResultDataSets();

    // if enabled, do the data point processing as set up by the user
    ScanDataSet dataSet = getMainScanDataSet();
    if (dataSet != null) {
      Scan scan = dataSet.getScan();
      MSLevel mslevel = inst.decideMSLevel(scan);
      controller = new DataPointProcessingController(inst.getProcessingQueue(mslevel), this, scan);
      inst.addController(controller);
    }
  }

  public boolean isProcessingAllowed() {
    return processingAllowed;
  }

  public void setProcessingAllowed(boolean processingAllowed) {
    this.processingAllowed = processingAllowed;
  }

  public synchronized void removeDataPointProcessingResultDataSets() {
    applyWithNotifyChanges(false, () -> {

      int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
      for (int i = 0; i < numDatasets; i++) {
        XYDataset dataSet = plot.getDataset(i);
        if (dataSet instanceof DPPResultsDataSet) {
          plot.setDataset(i, null);
        }
      }
      // when adding DPPResultDataSet the label generator is overwritten,
      // revert here
      SpectraItemLabelGenerator labelGenerator = new SpectraItemLabelGenerator(this);
      plot.getRenderer().setDefaultItemLabelGenerator(labelGenerator);
    });
  }

  @Override
  public void setLabelColorMatch(boolean matchColor) {
    matchLabelColors.set(matchColor);
  }

  private void addMatchLabelColorsListener() {
    matchLabelColors.addListener(((observable, oldValue, newValue) -> {
      if (newValue) {
        int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
        for (int i = 0; i < numDatasets; i++) {
          XYDataset dataset = getXYPlot().getDataset();
          if (!(dataset instanceof ScanDataSet)) {
            continue;
          }
          XYItemRenderer renderer = getXYPlot().getRendererForDataset(dataset);
          renderer.setDefaultItemLabelPaint(
              ((ScanDataSet) dataset).getScan().getDataFile().getColorAWT());
        }
      } else {
        int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
        for (int i = 0; i < numDatasets; i++) {
          XYDataset dataset = getXYPlot().getDataset();
          if (dataset == null) {
            continue;
          }
          XYItemRenderer renderer = getXYPlot().getRendererForDataset(dataset);
          renderer.setDefaultItemLabelPaint(theme.getItemLabelPaint());
        }
      }
    }));
  }

  /**
   * Listens to clicks in the Spectrum plot and updates the selected position accordingly.
   */
  private void initializeSpectrumMouseListener() {
    getMouseAdapter().addGestureHandler(
        new ChartGestureHandler(new ChartGesture(Entity.PLOT, Event.CLICK, GestureButton.BUTTON1),
            e -> {
              SpectrumCursorPosition pos = updateCursorPosition();
              if (pos != null) {
                setCursorPosition(pos);
              }
            }));
    getMouseAdapter().addGestureHandler(new ChartGestureHandler(
        new ChartGesture(Entity.XY_ITEM, Event.CLICK, GestureButton.BUTTON1), e -> {
      SpectrumCursorPosition pos = updateCursorPosition();
      if (pos != null) {
        setCursorPosition(pos);
      }
    }));

  }

  private SpectrumCursorPosition updateCursorPosition() {
    double selectedMZ = getXYPlot().getDomainCrosshairValue();
    double selectedIntensity = getXYPlot().getRangeCrosshairValue();
    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int i = 0; i < numDatasets; i++) {
      XYDataset ds = getXYPlot().getDataset(i);

      if (!(ds instanceof ScanDataSet scanDataSet)) {
        continue;
      }
      int index = scanDataSet.getIndex(selectedMZ, selectedIntensity);
      if (index >= 0) {
        return new SpectrumCursorPosition(selectedIntensity, selectedMZ, scanDataSet.getScan());
      }
    }
    return null;
  }

  public SpectrumCursorPosition getCursorPosition() {
    return cursorPosition.get();
  }

  public void setCursorPosition(SpectrumCursorPosition cursorPosition) {
    this.cursorPosition.set(cursorPosition);
  }

  public ObjectProperty<SpectrumCursorPosition> cursorPositionProperty() {
    return cursorPosition;
  }

  public Map<XYDataset, List<Pair<Double, Double>>> getDatasetToLabelsCoords() {
    return datasetToLabelsCoords;
  }
}
