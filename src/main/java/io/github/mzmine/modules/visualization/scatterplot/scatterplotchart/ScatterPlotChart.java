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

package io.github.mzmine.modules.visualization.scatterplot.scatterplotchart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleInsets;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramVisualizerModule;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.scatterplot.ScatterPlotAxisSelection;
import io.github.mzmine.modules.visualization.scatterplot.ScatterPlotTopPanel;
import io.github.mzmine.modules.visualization.scatterplot.ScatterPlotWindow;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.PeakUtils;
import io.github.mzmine.util.SearchDefinition;
import io.github.mzmine.util.components.ComponentToolTipManager;
import javafx.scene.control.ContextMenu;

public class ScatterPlotChart extends EChartViewer {

  // grid color
  private static final Color gridColor = Color.lightGray;

  // crosshair (selection) color
  private static final Color crossHairColor = Color.gray;

  // crosshair stroke
  private static final BasicStroke crossHairStroke =
      new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {5, 3}, 0);

  private JFreeChart chart;
  private XYPlot plot;

  // Renderers
  private ScatterPlotRenderer mainRenderer;
  private DiagonalLineRenderer diagonalLineRenderer;

  // Data sets
  private ScatterPlotDataSet mainDataSet;
  private DiagonalLineDataset diagonalLineDataset;

  private ScatterPlotTopPanel topPanel;
  private ComponentToolTipManager ttm;

  private ScatterPlotWindow window;
  private PeakList peakList;
  private ScatterPlotAxisSelection axisX, axisY;
  private int fold;

  public ScatterPlotChart(ScatterPlotWindow window, ScatterPlotTopPanel topPanel,
      PeakList peakList) {

    super(null);

    this.window = window;
    this.peakList = peakList;
    this.topPanel = topPanel;

    // initialize the chart by default time series chart from factory
    chart = ChartFactory.createXYLineChart("", // title
        "", // x-axis label
        "", // y-axis label
        null, // data set
        PlotOrientation.VERTICAL, // orientation
        false, // create legend
        false, // generate tooltips
        false // generate URLs
    );

    chart.setBackgroundPaint(Color.white);
    setChart(chart);

    // disable maximum size (we don't want scaling)
    // setMaximumDrawWidth(Integer.MAX_VALUE);
    // setMaximumDrawHeight(Integer.MAX_VALUE);

    // set the plot properties
    plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
    plot.setDomainGridlinePaint(gridColor);
    plot.setRangeGridlinePaint(gridColor);

    // Set the domain log axis
    LogAxis logAxisDomain = new LogAxis();
    logAxisDomain.setMinorTickCount(1);
    logAxisDomain.setNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    logAxisDomain.setAutoRange(true);
    plot.setDomainAxis(logAxisDomain);

    // Set the range log axis
    LogAxis logAxisRange = new LogAxis();
    logAxisRange.setMinorTickCount(1);
    logAxisRange.setNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    logAxisRange.setAutoRange(true);
    plot.setRangeAxis(logAxisRange);

    // Set crosshair properties
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);
    plot.setDomainCrosshairPaint(crossHairColor);
    plot.setRangeCrosshairPaint(crossHairColor);
    plot.setDomainCrosshairStroke(crossHairStroke);
    plot.setRangeCrosshairStroke(crossHairStroke);

    // Create data sets;
    mainDataSet = new ScatterPlotDataSet(peakList);
    plot.setDataset(0, mainDataSet);
    diagonalLineDataset = new DiagonalLineDataset();
    plot.setDataset(1, diagonalLineDataset);

    // Create renderers
    mainRenderer = new ScatterPlotRenderer();
    plot.setRenderer(0, mainRenderer);
    diagonalLineRenderer = new DiagonalLineRenderer();
    plot.setRenderer(1, diagonalLineRenderer);

    // Set tooltip properties
    // ttm = new ComponentToolTipManager();
    // ttm.registerComponent(this);
    // setDismissDelay(Integer.MAX_VALUE);
    // setInitialDelay(0);

    // add items to popup menu TODO: add other Show... items
    ContextMenu popupMenu = getContextMenu();
    // popupMenu.addSeparator();
    // GUIUtils.addMenuItem(popupMenu, "Show Chromatogram", this, "TIC");

    // reset zoom history
    ZoomHistory history = getZoomHistory();
    if (history != null)
      history.clear();
  }

  /*
   *
   * @Override public JComponent getCustomToolTipComponent(MouseEvent event) {
   *
   * String index = this.getToolTipText(event); if (index == null) { return null; } String
   * indexSplit[] = index.split(":");
   *
   * int series = Integer.parseInt(indexSplit[0]); int item = Integer.parseInt(indexSplit[1]);
   *
   * PeakListRow row = mainDataSet.getRow(series, item);
   *
   * PeakSummaryComponent newSummary = new PeakSummaryComponent(row, peakList.getRawDataFiles(),
   * true, true, true, true, false, ComponentToolTipManager.bg);
   *
   * double xValue = mainDataSet.getXValue(series, item); double yValue =
   * mainDataSet.getYValue(series, item); newSummary.setRatio(xValue, yValue);
   *
   * return newSummary;
   *
   *
   * }
   */

  /**
   * @see org.jfree.chart.event.ChartProgressListener#chartProgress(org.jfree.chart.event.ChartProgressEvent)
   */
  // @Override
  public void chartProgress(ChartProgressEvent event) {

    // Whenever chart is repainted (e.g. after crosshair position changed),
    // we update the selected item name
    if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {
      double valueX = plot.getDomainCrosshairValue();
      double valueY = plot.getRangeCrosshairValue();
      PeakListRow selectedRow = mainDataSet.getRow(valueX, valueY);
      topPanel.updateItemNameText(selectedRow);
    }
  }

  public XYPlot getPlot() {
    return plot;
  }

  public void actionPerformed(ActionEvent event) {



    String command = event.getActionCommand();


    if (command.equals("TIC")) {

      double valueX = plot.getDomainCrosshairValue();
      double valueY = plot.getRangeCrosshairValue();
      PeakListRow selectedRow = mainDataSet.getRow(valueX, valueY);

      if (selectedRow == null) {
        MZmineCore.getDesktop().displayErrorMessage("No peak is selected");
        return;
      }

      Feature[] peaks = selectedRow.getPeaks();
      Range<Double> rtRange = peakList.getRowsRTRange();
      Range<Double> mzRange = PeakUtils.findMZRange(peaks);

      // Label best peak with preferred identity.
      final Feature bestPeak = selectedRow.getBestPeak();
      final PeakIdentity peakIdentity = selectedRow.getPreferredPeakIdentity();
      final Map<Feature, String> labelMap = new HashMap<Feature, String>(1);
      if (bestPeak != null && peakIdentity != null) {

        labelMap.put(bestPeak, peakIdentity.getName());
      }

      ScanSelection scanSelection = new ScanSelection(rtRange, 1);

      ChromatogramVisualizerModule.showNewTICVisualizerWindow(
          peakList.getRawDataFiles().toArray(RawDataFile[]::new),
          Collections.singletonList(bestPeak), labelMap, scanSelection, TICPlotType.BASEPEAK,
          mzRange);
    }

  }

  public void setDisplayedAxes(ScatterPlotAxisSelection axisX, ScatterPlotAxisSelection axisY,
      int fold) {

    // Save values
    this.axisX = axisX;
    this.axisY = axisY;
    this.fold = fold;

    // Update axes
    plot.getDomainAxis().setLabel(axisX.toString());
    plot.getRangeAxis().setLabel(axisY.toString());

    // Update data sets
    mainDataSet.setDisplayedAxes(axisX, axisY);
    diagonalLineDataset.updateDiagonalData(mainDataSet, fold);

    topPanel.updateNumOfItemsText(peakList, mainDataSet, axisX, axisY, fold);
  }

  public void setItemLabels(boolean enabled) {
    mainRenderer.setSeriesItemLabelsVisible(1, enabled);
  }

  public void updateSearchDefinition(SearchDefinition newSearch) {
    mainDataSet.updateSearchDefinition(newSearch);
    topPanel.updateNumOfItemsText(peakList, mainDataSet, axisX, axisY, fold);
  }

}
