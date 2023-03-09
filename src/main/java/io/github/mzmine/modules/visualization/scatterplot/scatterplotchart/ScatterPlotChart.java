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

package io.github.mzmine.modules.visualization.scatterplot.scatterplotchart;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramVisualizerModule;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.scatterplot.ScatterPlotAxisSelection;
import io.github.mzmine.modules.visualization.scatterplot.ScatterPlotTab;
import io.github.mzmine.modules.visualization.scatterplot.ScatterPlotTopPanel;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.SearchDefinition;
import io.github.mzmine.util.components.ComponentToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.control.ContextMenu;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleInsets;

public class ScatterPlotChart extends EChartViewer {

  // grid color
  private static final Color gridColor = Color.lightGray;

  // crosshair (selection) color
  private static final Color crossHairColor = Color.gray;

  // crosshair stroke
  private static final BasicStroke crossHairStroke = new BasicStroke(1, BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_BEVEL, 1.0f, new float[]{5, 3}, 0);

  private final JFreeChart chart;
  private final XYPlot plot;

  // Renderers
  private final ScatterPlotRenderer mainRenderer;
  private final DiagonalLineRenderer diagonalLineRenderer;

  // Data sets
  private final ScatterPlotDataSet mainDataSet;
  private final DiagonalLineDataset diagonalLineDataset;

  private final ScatterPlotTopPanel topPanel;
  private ComponentToolTipManager ttm;

  private final ScatterPlotTab tab;
  private final FeatureList featureList;
  private ScatterPlotAxisSelection axisX, axisY;
  private int fold;

  public ScatterPlotChart(ScatterPlotTab tab, ScatterPlotTopPanel topPanel,
      FeatureList featureList) {

    super(null);

    this.tab = tab;
    this.featureList = featureList;
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
    mainDataSet = new ScatterPlotDataSet(featureList);
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
    if (history != null) {
      history.clear();
    }
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
      FeatureListRow selectedRow = mainDataSet.getRow(valueX, valueY);
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
      FeatureListRow selectedRow = mainDataSet.getRow(valueX, valueY);

      if (selectedRow == null) {
        MZmineCore.getDesktop().displayErrorMessage("No feature is selected");
        return;
      }

      Feature[] peaks = selectedRow.getFeatures().toArray(new Feature[0]);
      Range<Float> rtRange = featureList.getRowsRTRange();
      Range<Double> mzRange = FeatureUtils.findMZRange(peaks);

      // Label best peak with preferred identity.
      final Feature bestPeak = selectedRow.getBestFeature();
      final FeatureIdentity featureIdentity = selectedRow.getPreferredFeatureIdentity();
      final Map<Feature, String> labelMap = new HashMap<Feature, String>(1);
      if (bestPeak != null && featureIdentity != null) {

        labelMap.put(bestPeak, featureIdentity.getName());
      }

      ScanSelection scanSelection = new ScanSelection(1, rtRange);

      ChromatogramVisualizerModule.showNewTICVisualizerWindow(
          featureList.getRawDataFiles().toArray(RawDataFile[]::new),
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

    topPanel.updateNumOfItemsText(featureList, mainDataSet, axisX, axisY, fold);
  }

  public void setItemLabels(boolean enabled) {
    mainRenderer.setSeriesItemLabelsVisible(1, enabled);
  }

  public void updateSearchDefinition(SearchDefinition newSearch) {
    mainDataSet.updateSearchDefinition(newSearch);
    topPanel.updateNumOfItemsText(featureList, mainDataSet, axisX, axisY, fold);
  }

}
