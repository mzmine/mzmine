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

package io.github.mzmine.modules.dataanalysis.projectionplots;

import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.parameters.ParameterSet;

public class ProjectionPlotPanel extends EChartViewer {

  private static final Color gridColor = Color.lightGray;
  private static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11);

  private static final float dataPointAlpha = 0.8f;

  private JFreeChart chart;
  private XYPlot plot;

  private ProjectionPlotItemLabelGenerator itemLabelGenerator;
  private ProjectionPlotRenderer spotRenderer;

  public ProjectionPlotPanel(ProjectionPlotWindow masterFrame, ProjectionPlotDataset dataset,
      ParameterSet parameters) {
    super(null);

    boolean createLegend = false;
    if ((dataset.getNumberOfGroups() > 1) && (dataset.getNumberOfGroups() < 20))
      createLegend = true;

    chart = ChartFactory.createXYAreaChart("", dataset.getXLabel(), dataset.getYLabel(), dataset,
        PlotOrientation.VERTICAL, createLegend, false, false);
    chart.setBackgroundPaint(Color.white);

    setChart(chart);

    // title

    TextTitle chartTitle = chart.getTitle();
    chartTitle.setMargin(5, 0, 0, 0);
    chartTitle.setFont(titleFont);
    chart.removeSubtitle(chartTitle);

    // disable maximum size (we don't want scaling)
    // setMaximumDrawWidth(Integer.MAX_VALUE);
    // setMaximumDrawHeight(Integer.MAX_VALUE);

    // set the plot properties
    plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

    // set grid properties
    plot.setDomainGridlinePaint(gridColor);
    plot.setRangeGridlinePaint(gridColor);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    plot.setForegroundAlpha(dataPointAlpha);

    NumberFormat numberFormat = NumberFormat.getNumberInstance();

    // set the X axis (component 1) properties
    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setNumberFormatOverride(numberFormat);

    // set the Y axis (component 2) properties
    NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
    yAxis.setNumberFormatOverride(numberFormat);

    plot.setDataset(dataset);

    spotRenderer = new ProjectionPlotRenderer(plot, dataset);
    itemLabelGenerator = new ProjectionPlotItemLabelGenerator(parameters);
    spotRenderer.setDefaultItemLabelGenerator(itemLabelGenerator);
    spotRenderer.setDefaultItemLabelsVisible(true);
    spotRenderer.setDefaultToolTipGenerator(new ProjectionPlotToolTipGenerator(parameters));
    plot.setRenderer(spotRenderer);

    // Setup legend
    if (createLegend) {
      LegendItemCollection legendItemsCollection = new LegendItemCollection();
      for (int groupNumber = 0; groupNumber < dataset.getNumberOfGroups(); groupNumber++) {
        Object paramValue = dataset.getGroupParameterValue(groupNumber);
        if (paramValue == null) {
          // No parameter value available: search for raw data files
          // within this group, and use their names as group's name
          String fileNames = new String();
          for (int itemNumber = 0; itemNumber < dataset.getItemCount(0); itemNumber++) {
            String rawDataFile = dataset.getRawDataFile(itemNumber);
            if (dataset.getGroupNumber(itemNumber) == groupNumber)
              fileNames = fileNames.concat(rawDataFile);
          }
          if (fileNames.length() == 0)
            fileNames = "Empty group";

          paramValue = fileNames;
        }
        Color nextColor = (Color) spotRenderer.getGroupPaint(groupNumber);
        Color groupColor = new Color(nextColor.getRed(), nextColor.getGreen(), nextColor.getBlue(),
            Math.round(255 * dataPointAlpha));
        legendItemsCollection.add(new LegendItem(paramValue.toString(), "-", null, null,
            spotRenderer.getDataPointsShape(), groupColor));
      }
      plot.setFixedLegendItems(legendItemsCollection);
    }

    // reset zoom history
    ZoomHistory history = getZoomHistory();
    if (history != null)
      history.clear();
  }

  protected void cycleItemLabelMode() {
    itemLabelGenerator.cycleLabelMode();
    spotRenderer.setDefaultItemLabelsVisible(true);
  }
}
