/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import java.awt.Color;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.MSLevel;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResultsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.IsotopesDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.PeakListDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.ContinuousRenderer;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.PeakRenderer;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraItemLabelGenerator;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;

/**
 *
 */
public class SpectraPlot extends EChartViewer {

  private JFreeChart chart;
  private XYPlot plot;

  // initially, plotMode is set to null, until we load first scan
  private MassSpectrumType plotMode = null;

  // peak labels color
  private static final Color labelsColor = Color.darkGray;

  // grid color
  private static final Color gridColor = Color.lightGray;

  // title font - moved to EStandardChartTheme ~SteffenHeu
  // private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
  // private static final Font subTitleFont = new Font("SansSerif", Font.PLAIN, 11);
  private TextTitle chartTitle, chartSubTitle;

  // legend - moved to EStandardChartTheme ~SteffenHeu
  // private static final Font legendFont = new Font("SansSerif", Font.PLAIN, 11);

  private boolean isotopesVisible = true, peaksVisible = true, itemLabelsVisible = true,
      dataPointsVisible = false;

  // We use our own counter, because plot.getDatasetCount() just keeps
  // increasing even when we remove old data sets
  private int numOfDataSets = 0;

  // Spectra processing
  protected DataPointProcessingController controller;
  private boolean processingAllowed;

  protected EStandardChartTheme theme;

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

    // setBackground(Color.white);
    setCursor(Cursor.CROSSHAIR);

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
    xAxis.setUpperMargin(0.001);
    xAxis.setLowerMargin(0.001);
    xAxis.setTickLabelInsets(new RectangleInsets(0, 0, 20, 20));

    // set the Y axis (intensity) properties
    NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
    yAxis.setNumberFormatOverride(intensityFormat);

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
     * if (masterPlot instanceof SpectraVisualizerWindow) {
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

  /**
   * This will set either centroid or continuous renderer to the first data set, assuming that
   * dataset with index 0 contains the raw data.
   */
  public void setPlotMode(MassSpectrumType plotMode) {

    this.plotMode = plotMode;

    XYDataset dataSet = plot.getDataset(0);
    if (!(dataSet instanceof ScanDataSet))
      return;

    XYItemRenderer newRenderer;
    if (plotMode == MassSpectrumType.CENTROIDED) {
      newRenderer = new PeakRenderer(SpectraVisualizerWindow.scanColor, false);
    } else {
      newRenderer = new ContinuousRenderer(SpectraVisualizerWindow.scanColor, false);
      ((ContinuousRenderer) newRenderer).setDefaultShapesVisible(dataPointsVisible);
    }

    // Add label generator for the dataset
    SpectraItemLabelGenerator labelGenerator = new SpectraItemLabelGenerator(this);
    newRenderer.setDefaultItemLabelGenerator(labelGenerator);
    newRenderer.setDefaultItemLabelsVisible(itemLabelsVisible);
    newRenderer.setDefaultItemLabelPaint(theme.getItemLabelPaint());

    plot.setRenderer(0, newRenderer);

  }

  public MassSpectrumType getPlotMode() {
    return plotMode;
  }

  public XYPlot getXYPlot() {
    return plot;
  }

  void switchItemLabelsVisible() {
    itemLabelsVisible = !itemLabelsVisible;
    for (int i = 0; i < plot.getDatasetCount(); i++) {
      XYItemRenderer renderer = plot.getRenderer(i);
      renderer.setDefaultItemLabelsVisible(itemLabelsVisible);
    }
  }

  void switchDataPointsVisible() {
    dataPointsVisible = !dataPointsVisible;
    for (int i = 0; i < plot.getDatasetCount(); i++) {
      XYItemRenderer renderer = plot.getRenderer(i);
      if (!(renderer instanceof ContinuousRenderer))
        continue;
      ContinuousRenderer contRend = (ContinuousRenderer) renderer;
      contRend.setDefaultShapesVisible(dataPointsVisible);
    }
  }

  void switchPickedPeaksVisible() {
    peaksVisible = !peaksVisible;
    for (int i = 0; i < plot.getDatasetCount(); i++) {
      XYDataset dataSet = plot.getDataset(i);
      if (!(dataSet instanceof PeakListDataSet))
        continue;
      XYItemRenderer renderer = plot.getRenderer(i);
      renderer.setDefaultSeriesVisible(peaksVisible);
    }
  }

  void switchIsotopePeaksVisible() {
    isotopesVisible = !isotopesVisible;
    for (int i = 0; i < plot.getDatasetCount(); i++) {
      XYDataset dataSet = plot.getDataset(i);
      if (!(dataSet instanceof IsotopesDataSet))
        continue;
      XYItemRenderer renderer = plot.getRenderer(i);
      renderer.setDefaultSeriesVisible(isotopesVisible);
    }
  }

  public void setTitle(String title, String subTitle) {
    if (title != null)
      chartTitle.setText(title);
    if (subTitle != null)
      chartSubTitle.setText(subTitle);
  }

  /*
   * public void mouseClicked(MouseEvent event) {
   *
   * // let the parent handle the event (selection etc.) super.mouseClicked(event);
   *
   * // request focus to receive key events requestFocus(); }
   */


  public synchronized void removeAllDataSets() {

    // if the data sets are removed, we have to cancel the tasks.
    if (controller != null)
      controller.cancelTasks();
    controller = null;

    for (int i = 0; i < plot.getDatasetCount(); i++) {
      plot.setDataset(i, null);
    }
    numOfDataSets = 0;
  }

  public synchronized void addDataSet(XYDataset dataSet, Color color, boolean transparency) {

    XYItemRenderer newRenderer;

    if (dataSet instanceof ScanDataSet) {
      ScanDataSet scanDataSet = (ScanDataSet) dataSet;
      Scan scan = scanDataSet.getScan();
      if (scan.getSpectrumType() == MassSpectrumType.CENTROIDED)
        newRenderer = new PeakRenderer(color, transparency);
      else {
        newRenderer = new ContinuousRenderer(color, transparency);
        ((ContinuousRenderer) newRenderer).setDefaultShapesVisible(dataPointsVisible);
      }

      // Add label generator for the dataset
      SpectraItemLabelGenerator labelGenerator = new SpectraItemLabelGenerator(this);
      newRenderer.setDefaultItemLabelGenerator(labelGenerator);
      newRenderer.setDefaultItemLabelsVisible(itemLabelsVisible);
      newRenderer.setDefaultItemLabelPaint(theme.getItemLabelPaint());

    } else {
      newRenderer = new PeakRenderer(color, transparency);
    }

    plot.setDataset(numOfDataSets, dataSet);
    plot.setRenderer(numOfDataSets, newRenderer);
    numOfDataSets++;

    if (dataSet instanceof ScanDataSet)
      checkAndRunController();
  }

  // add Dataset with label generator
  public synchronized void addDataSet(XYDataset dataSet, Color color, boolean transparency,
      XYItemLabelGenerator labelGenerator) {

    XYItemRenderer newRenderer;

    if (dataSet instanceof ScanDataSet) {
      ScanDataSet scanDataSet = (ScanDataSet) dataSet;
      Scan scan = scanDataSet.getScan();
      if (scan.getSpectrumType() == MassSpectrumType.CENTROIDED)
        newRenderer = new PeakRenderer(color, transparency);
      else {
        newRenderer = new ContinuousRenderer(color, transparency);
        ((ContinuousRenderer) newRenderer).setDefaultShapesVisible(dataPointsVisible);
      }

      // Add label generator for the dataset
      newRenderer.setDefaultItemLabelGenerator(labelGenerator);
      newRenderer.setDefaultItemLabelsVisible(itemLabelsVisible);
      newRenderer.setDefaultItemLabelPaint(theme.getItemLabelPaint());

    } else {
      newRenderer = new PeakRenderer(color, transparency);
      // Add label generator for the dataset
      newRenderer.setDefaultItemLabelGenerator(labelGenerator);
      newRenderer.setDefaultItemLabelsVisible(itemLabelsVisible);
      newRenderer.setDefaultItemLabelPaint(theme.getItemLabelPaint());
    }

    plot.setDataset(numOfDataSets, dataSet);
    plot.setRenderer(numOfDataSets, newRenderer);
    numOfDataSets++;

    if (dataSet instanceof ScanDataSet)
      checkAndRunController();
  }

  public synchronized void removePeakListDataSets() {
    for (int i = 0; i < plot.getDatasetCount(); i++) {
      XYDataset dataSet = plot.getDataset(i);
      if (dataSet instanceof PeakListDataSet) {
        plot.setDataset(i, null);
      }
    }
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

    if (!isProcessingAllowed() || !inst.isEnabled())
      return;

    if (controller != null)
      controller = null;

    // if a controller is re-run then delete previous results
    removeDataPointProcessingResultDataSets();

    // if enabled, do the data point processing as set up by the user
    XYDataset dataSet = getMainScanDataSet();
    if (dataSet instanceof ScanDataSet) {
      Scan scan = ((ScanDataSet) dataSet).getScan();
      MSLevel mslevel = inst.decideMSLevel(scan);
      controller = new DataPointProcessingController(inst.getProcessingQueue(mslevel), this,
          getMainScanDataSet().getDataPoints());
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
  }
}
