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

package io.github.mzmine.modules.dataprocessing.adap_mcr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import dulab.adap.datamodel.BetterComponent;
import dulab.adap.datamodel.BetterPeak;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import javafx.scene.Cursor;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */

public class EICPlot extends EChartViewer {
  private enum PeakType {
    SIMPLE, MODEL
  };

  private static final Color[] COLORS = new Color[] {Color.BLUE, Color.CYAN, Color.GREEN,
      Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED};

  private final XYSeriesCollection xyDataset;
  private final List<Double> colorDataset;
  private final List<String> toolTips;
  private final List<Float> widths;

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
    widths = new ArrayList<>();

    int seriesID = 0;

    for (int i = 0; i < clusters.size(); ++i) {
      List<NavigableMap<Double, Double>> cluster = clusters.get(i);
      double color = colors.get(i);

      for (int j = 0; j < cluster.size(); ++j) {
        XYSeries series = new XYSeries(seriesID++);

        for (Entry<Double, Double> e : cluster.get(j).entrySet())
          series.add(e.getKey(), e.getValue());

        xyDataset.addSeries(series);
        colorDataset.add(color);
        toolTips.add(info.get(i).get(j));
      }
    }

    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer() {
      @Override
      public Paint getItemPaint(int row, int col) {
        String type = xyDataset.getSeries(row).getDescription();

        Paint color;

        if (type.equals(PeakType.MODEL.name()))
          color = COLORS[row % COLORS.length];
        else
          color = new Color(0, 0, 0, 50);

        return color;
      }

      @Override
      public Stroke getSeriesStroke(int series) {
        XYSeries s = xyDataset.getSeries(series);
        String type = s.getDescription();

        float width;
        if (type.equals((PeakType.MODEL.name())))
          width = 2.0f;
        else
          width = 1.0f;

        return new BasicStroke(width);
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

  // public void updateData(@NotNull List <List <NavigableMap <Double,
  // Double>>> clusters,
  // @NotNull List <Double> colors,
  // @NotNull List <List <String>> info,
  // @NotNull List <List<Boolean>> models)
  // {
  // final float DEFAULT_LINE_WIDTH = 1.0f;
  // final float THICK_LINE_WIDTH = 2.0f;
  //
  //
  //// for (int i = 0; i < xyDataset.getSeriesCount(); ++i)
  //// xyDataset.removeSeries(i);
  // xyDataset.removeAllSeries();
  // colorDataset.clear();
  // toolTips.clear();
  // widths.clear();
  //
  // int seriesID = 0;
  //
  // for (int i = 0; i < clusters.size(); ++i)
  // {
  // List <NavigableMap <Double, Double>> cluster = clusters.get(i);
  // double color = colors.get(i);
  //
  // for (int j = 0; j < cluster.size(); ++j)
  // {
  // XYSeries series = new XYSeries(seriesID++);
  //
  // for (Entry <Double, Double> e : cluster.get(j).entrySet())
  // series.add(e.getKey(), e.getValue());
  //
  // float width = DEFAULT_LINE_WIDTH;
  // if (models.get(i).get(j)) width = THICK_LINE_WIDTH;
  //
  // xyDataset.addSeries(series);
  // colorDataset.add(color);
  // toolTips.add(info.get(i).get(j));
  // widths.add(width);
  // }
  // }
  // }

  void updateData(@NotNull List<BetterPeak> peaks, @NotNull List<BetterComponent> modelPeaks) {
    xyDataset.removeAllSeries();
    xyDataset.setNotify(false);
    toolTips.clear();

    // Find retention-time range
    double startRetTime = Double.MAX_VALUE;
    double endRetTime = -Double.MAX_VALUE;
    for (BetterPeak peak : modelPeaks) {
      if (peak.getFirstRetTime() < startRetTime)
        startRetTime = peak.getFirstRetTime();
      if (peak.getLastRetTime() > endRetTime)
        endRetTime = peak.getLastRetTime();
    }

    if (endRetTime < startRetTime)
      return;

    int seriesID = 0;
    for (BetterPeak peak : peaks) {
      XYSeries series = new XYSeries(seriesID++);
      series.setDescription(PeakType.SIMPLE.name());

      for (int i = 0; i < peak.chromatogram.length; ++i) {
        double retTime = peak.chromatogram.getRetTime(i);
        if (startRetTime <= retTime && retTime <= endRetTime)
          series.add(peak.chromatogram.getRetTime(i), peak.chromatogram.getIntensity(i));
      }

      xyDataset.addSeries(series);
      toolTips.add(String.format("M/z: %.2f\nIntensity: %.0f", peak.getMZ(), peak.getIntensity()));
    }

    for (BetterPeak peak : modelPeaks) {
      XYSeries series = new XYSeries((seriesID++));
      series.setDescription(PeakType.MODEL.name());

      for (int i = 0; i < peak.chromatogram.length; ++i)
        series.add(peak.chromatogram.getRetTime(i), peak.chromatogram.getIntensity(i));

      xyDataset.addSeries(series);
      toolTips.add(String.format("Model peak\nM/z: %.2f\nIntensity: %.0f", peak.getMZ(),
          peak.getIntensity()));
    }

    xyDataset.setNotify(true);
  }

  void removeData() {
    xyDataset.removeAllSeries();
    toolTips.clear();
  }
}
