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

package io.github.mzmine.modules.dataprocessing.adap_hierarchicalclustering;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import javafx.scene.Cursor;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */

public class EICPlot extends EChartViewer {
  private static java.util.logging.Logger logger = Logger.getLogger(EICPlot.class.getName());
  private final XYSeriesCollection xyDataset;
  private final List<Double> colorDataset;
  private final List<String> toolTips;

  public EICPlot() {
    this(new ArrayList<List<NavigableMap<Double, Double>>>(), new ArrayList<Double>(),
        new ArrayList<List<String>>(), null);
  }

  public EICPlot(List<List<NavigableMap<Double, Double>>> clusters, List<Double> colors,
      List<List<String>> info, List<NavigableMap<Double, Double>> modelPeaks) {
    super(null);

    // setBackground(Color.white);
    setCursor(Cursor.CROSSHAIR);

    NumberAxis xAxis = new NumberAxis("Retention Time");
    xAxis.setAutoRangeIncludesZero(false);
    xAxis.setUpperMargin(0);
    xAxis.setLowerMargin(0);

    NumberAxis yAxis = new NumberAxis("Intensity");
    yAxis.setAutoRangeIncludesZero(false);
    yAxis.setUpperMargin(0);
    yAxis.setLowerMargin(0);

    xyDataset = new XYSeriesCollection();
    colorDataset = new ArrayList<>();
    toolTips = new ArrayList<>();

    int seriesID = 0;

    for (int i = 0; i < clusters.size(); ++i) {
      List<NavigableMap<Double, Double>> cluster = clusters.get(i);
      double color = colors.get(i);

      for (int j = 0; j < cluster.size(); ++j) {
        XYSeries series = new XYSeries(seriesID++);

        for (Entry<Double, Double> e : cluster.get(j).entrySet())
          series.add(e.getKey(), e.getValue());

        colorDataset.add(color);
        toolTips.add(info.get(i).get(j));
        xyDataset.addSeries(series);
      }
    }

    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer() {
      @Override
      public Paint getItemPaint(int row, int col) {
        double c = colorDataset.get(row);
        return Color.getHSBColor((float) c, 1.0f, 1.0f);
      }
    };

    renderer.setDefaultShapesVisible(false);
    renderer.setDefaultToolTipGenerator(new XYToolTipGenerator() {
      @Override
      public String generateToolTip(XYDataset dataset, int series, int item) {
        try {
          return toolTips.get(series);
        } catch (NullPointerException | IndexOutOfBoundsException e) {
          return "";
        }
      }
    });

    XYPlot plot = new XYPlot(xyDataset, xAxis, yAxis, renderer);
    plot.setBackgroundPaint(Color.white);
    plot.setDomainGridlinesVisible(true);
    plot.setRangeGridlinesVisible(true);

    JFreeChart chart = new JFreeChart("", new Font("SansSerif", Font.BOLD, 12), plot, false);
    chart.setBackgroundPaint(Color.white);

    super.setChart(chart);

    // reset zoom history
    ZoomHistory history = getZoomHistory();
    if (history != null)
      history.clear();
  }

  public void updateData(List<List<NavigableMap<Double, Double>>> clusters, List<Double> colors,
      List<List<String>> info, List<NavigableMap<Double, Double>> modelPeaks) {
    // for (int i = 0; i < xyDataset.getSeriesCount(); ++i)
    // xyDataset.removeSeries(i);
    xyDataset.removeAllSeries();
    colorDataset.clear();
    toolTips.clear();

    int seriesID = 0;

    for (int i = 0; i < clusters.size(); ++i) {
      List<NavigableMap<Double, Double>> cluster = clusters.get(i);
      double color = colors.get(i);

      for (int j = 0; j < cluster.size(); ++j) {
        XYSeries series = new XYSeries(seriesID++);

        for (Entry<Double, Double> e : cluster.get(j).entrySet())
          series.add(e.getKey(), e.getValue());

        colorDataset.add(color);
        toolTips.add(info.get(i).get(j));
        try{
          xyDataset.addSeries(series);
        }
        catch(Exception e) {
          logger.log(Level.WARNING, e.getMessage(), e);
        }
      }
    }
  }
}
