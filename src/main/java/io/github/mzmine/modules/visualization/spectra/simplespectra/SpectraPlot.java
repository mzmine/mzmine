/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.chartbasics.ChartLogics;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartthemes.LabelColorMatch;
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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.util.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;

/**
 *
 */
public class SpectraPlot extends EChartViewer implements LabelColorMatch {

  // peak labels color
  private static final Color labelsColor = Color.darkGray;
  // grid color
  private static final Color gridColor = Color.lightGray;
  // initially, plotMode is set to null, until we load first scan
  private final ObjectProperty<SpectrumPlotType> plotMode;
  /**
   * Contains coordinated of labels for each dataset. It is supposed to be updated by {@link
   * SpectraItemLabelGenerator}.
   */
  private final Map<XYDataset, List<Pair<Double, Double>>> datasetToLabelsCoords = new HashMap<>();
  /**
   * If true, the labels of the data set will have the same color as the data set itself
   */
  protected BooleanProperty matchLabelColors;
  protected ObjectProperty<SpectrumCursorPosition> cursorPosition;

  // legend - moved to EStandardChartTheme ~SteffenHeu
  // private static final Font legendFont = new Font("SansSerif", Font.PLAIN, 11);
  // Spectra processing
  protected DataPointProcessingController controller;
  protected EStandardChartTheme theme;
  private JFreeChart chart;
  private XYPlot plot;
  // title font - moved to EStandardChartTheme ~SteffenHeu
  // private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
  // private static final Font subTitleFont = new Font("SansSerif", Font.PLAIN, 11);
  private TextTitle chartTitle, chartSubTitle;
  private boolean isotopesVisible = true, peaksVisible = true, itemLabelsVisible = true, dataPointsVisible = false;
  // We use our own counter, because plot.getDatasetCount() just keeps
  // increasing even when we remove old data sets
  private int numOfDataSets = 0;
  private boolean processingAllowed;

  public SpectraPlot() {
    this(false);
  }

  public SpectraPlot(boolean processingAllowed) {

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

    // set grid properties - TODO: do we want gridlines in spectra?
    plot.setDomainGridlinePaint(gridColor);
    plot.setRangeGridlinePaint(gridColor);

    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    // set the X axis (retention time) properties
    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setNumberFormatOverride(mzFormat);
    xAxis.setUpperMargin(0.01); // have some margin so m/z labels are not cut off
    xAxis.setLowerMargin(0.01);
    xAxis.setTickLabelInsets(new RectangleInsets(0, 0, 20, 20));

    // set the Y axis (intensity) properties
    NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
    yAxis.setNumberFormatOverride(intensityFormat);
    yAxis.setUpperMargin(0.1); // some margin for m/z labels

    // only allow positive values for the axes
    ChartLogics.setAxesTypesPositive(chart);

    // set focusable state to receive key events
    // setFocusable(true);

    // register key handlers
    // FxMenuUtil.registerKeyHandler(this, KeyStroke.getKeyStroke("LEFT"), masterPlot,
    // "PREVIOUS_SCAN");
    // FxMenuUtil.registerKeyHandler(this, KeyStroke.getKeyStroke("RIGHT"), masterPlot,
    // "NEXT_SCAN");
    // FxMenuUtil.registerKeyHandler(this, KeyStroke.getKeyStroke('+'), this, "ZOOM_IN");
    // FxMenuUtil.registerKeyHandler(this, KeyStroke.getKeyStroke('-'), this, "ZOOM_OUT");

    ContextMenu popupMenu = getContextMenu();

    // add items to popup menu
    /*
     * if (masterPlot instanceof SpectraVisualizerTab) {
     *
     * FxMenuUtil.addMenuItem(popupMenu, "Export spectra to spectra file", masterPlot,
     * "EXPORT_SPECTRA"); FxMenuUtil.addMenuItem(popupMenu, "Create spectral library entry",
     * masterPlot, "CREATE_LIBRARY_ENTRY");
     *
     * popupMenu.addSeparator();
     *
     * FxMenuUtil.addMenuItem(popupMenu, "Toggle centroid/continuous mode", masterPlot,
     * "TOGGLE_PLOT_MODE"); FxMenuUtil.addMenuItem(popupMenu,
     * "Toggle displaying of data points in continuous mode", masterPlot, "SHOW_DATA_POINTS");
     * FxMenuUtil.addMenuItem(popupMenu, "Toggle displaying of peak values", masterPlot,
     * "SHOW_ANNOTATIONS"); FxMenuUtil.addMenuItem(popupMenu, "Toggle displaying of picked peaks",
     * masterPlot, "SHOW_PICKED_PEAKS");
     *
     * FxMenuUtil.addMenuItem(popupMenu, "Reset removed titles to visible", this,
     * "SHOW_REMOVED_TITLES");
     *
     * popupMenu.addSeparator();
     *
     * FxMenuUtil.addMenuItem(popupMenu, "Set axes range", masterPlot, "SETUP_AXES");
     *
     * FxMenuUtil.addMenuItem(popupMenu, "Set same range to all windows", masterPlot,
     * "SET_SAME_RANGE");
     *
     * popupMenu.addSeparator();
     *
     * FxMenuUtil.addMenuItem(popupMenu, "Add isotope pattern", masterPlot, "ADD_ISOTOPE_PATTERN");
     * }
     */

    // reset zoom history
    ZoomHistory history = getZoomHistory();
    if (history != null) {
      history.clear();
    }

    theme.apply(chart);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    // set processingAllowed
    setProcessingAllowed(processingAllowed);

    // If the plot is changed then clear the map containing coordinates of labels. New values will be
    // added by the SpectraItemLabelGenerator
    getChart().getXYPlot().addChangeListener(event -> datasetToLabelsCoords.clear());
  }



  /*
   * public void actionPerformed(final ActionEvent event) {
   *
   * final String command = event.getActionCommand(); if ("SHOW_REMOVED_TITLES".equals(command)) {
   * for (int i = 0; i < getChart().getSubtitleCount(); i++) {
   * getChart().getSubtitle(i).setVisible(true); } getChart().getTitle().setVisible(true); }
   *
   *
   * } }
   */

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
      for (int i = 0; i < numOfDataSets; i++) {
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
    for (int i = 0; i < plot.getDatasetCount(); i++) {
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
      for (int i = 0; i < plot.getDatasetCount(); i++) {
        XYItemRenderer renderer = plot.getRenderer(i);
        if (!(renderer instanceof ContinuousRenderer)) {
          continue;
        }
        ContinuousRenderer contRend = (ContinuousRenderer) renderer;
        contRend.setDefaultShapesVisible(dataPointsVisible);
      }
    });
  }

  void switchPickedPeaksVisible() {
    peaksVisible = !peaksVisible;
    for (int i = 0; i < plot.getDatasetCount(); i++) {
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
    for (int i = 0; i < plot.getDatasetCount(); i++) {
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

      for (int i = 0; i < plot.getDatasetCount(); i++) {
        plot.setDataset(i, null);
      }
      numOfDataSets = 0;
      plot.clearDomainMarkers();
    });
  }

  public synchronized int getNumOfDataSets() {
    return numOfDataSets;
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
      }
      ((AbstractRenderer) newRenderer).setItemLabelAnchorOffset(1.3d);

      plot.setDataset(numOfDataSets, dataSet);
      plot.setRenderer(numOfDataSets, newRenderer);
      numOfDataSets++;

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
          addPrecursorMarker(info.getIsolationWindow(), color, alpha);
        } else {
          addPrecursorMarker(prmz, color, alpha);
        }
      }
    } else if (scan.getMSLevel() > 2) {
      // add all parent precursors
      if (scan.getMsMsInfo() instanceof MSnInfoImpl msn) {
        for (var info : msn.getPrecursors()) {
          if (showPrecursorWindow && info.getIsolationWindow() != null) {
            addPrecursorMarker(info.getIsolationWindow(), color, alpha);
          } else {
            addPrecursorMarker(info.getIsolationMz(), color, alpha);
          }
        }
      }
    }
  }

  private void addPrecursorMarker(Range<Double> mzRange, Color color, float alpha) {
    addPrecursorMarker(mzRange.lowerEndpoint(), mzRange.upperEndpoint(), color, alpha);
  }

  private void addPrecursorMarker(double lowerMZ, double upperMZ, Color color, float alpha) {
    final IntervalMarker marker = new IntervalMarker(lowerMZ, upperMZ);
    marker.setStroke(
        new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{7}, 0f));
    marker.setPaint(color);
    marker.setAlpha(alpha);
    plot.addDomainMarker(marker);
  }

  private void addPrecursorMarker(double precursorMz, Color color, float alpha) {
    final ValueMarker marker = new ValueMarker(precursorMz);
    marker.setStroke(
        new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{7}, 0f));
    marker.setPaint(color);
    marker.setAlpha(alpha);
    plot.addDomainMarker(marker);
  }

  public synchronized void removePeakListDataSets() {
    applyWithNotifyChanges(false, () -> {

      for (int i = 0; i < plot.getDatasetCount(); i++) {
        XYDataset dataSet = plot.getDataset(i);
        if (dataSet instanceof PeakListDataSet) {
          plot.setDataset(i, null);
        }
      }
    });
  }

  public ScanDataSet getMainScanDataSet() {
    for (int i = 0; i < plot.getDatasetCount(); i++) {
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
    XYDataset dataSet = getMainScanDataSet();
    if (dataSet instanceof ScanDataSet) {
      Scan scan = ((ScanDataSet) dataSet).getScan();
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

      for (int i = 0; i < plot.getDatasetCount(); i++) {
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
      if (newValue == true) {
        for (int i = 0; i < getXYPlot().getDatasetCount(); i++) {
          XYDataset dataset = getXYPlot().getDataset();
          if (dataset == null || !(dataset instanceof ScanDataSet)) {
            continue;
          }
          XYItemRenderer renderer = getXYPlot().getRendererForDataset(dataset);
          renderer.setDefaultItemLabelPaint(
              ((ScanDataSet) dataset).getScan().getDataFile().getColorAWT());
        }
      } else {
        for (int i = 0; i < getXYPlot().getDatasetCount(); i++) {
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
    getCanvas().addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        SpectrumCursorPosition pos = updateCursorPosition();
        if (pos != null) {
          setCursorPosition(pos);
        }
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {
        // currently not in use
      }
    });
  }

  private SpectrumCursorPosition updateCursorPosition() {
    double selectedMZ = getXYPlot().getDomainCrosshairValue();
    double selectedIntensity = getXYPlot().getRangeCrosshairValue();

    for (int i = 0; i < numOfDataSets; i++) {
      XYDataset ds = getXYPlot().getDataset(i);

      if (ds == null || !(ds instanceof ScanDataSet)) {
        continue;
      }
      ScanDataSet scanDataSet = (ScanDataSet) ds;
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

  public XYItemRenderer getLastRenderer() {
    final XYPlot plot = getXYPlot();
    if (plot == null) {
      return null;
    }
    final int renderer = plot.getRendererCount();
    return renderer <= 0 ? null : plot.getRenderer(renderer - 1);
  }

  public XYDataset getLastDataset() {
    final XYPlot plot = getXYPlot();
    if (plot == null) {
      return null;
    }
    final int datasets = plot.getDatasetCount();
    return datasets <= 0 ? null : plot.getDataset(datasets - 1);
  }
}
