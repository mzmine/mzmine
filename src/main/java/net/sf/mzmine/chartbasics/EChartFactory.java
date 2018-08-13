/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */


package net.sf.mzmine.chartbasics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleFunction;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.util.maths.Precision;

public class EChartFactory {
  private static final Logger logger = LoggerFactory.getLogger(EChartFactory.class);

  private static GaussianCurveFitter fitter = GaussianCurveFitter.create().withMaxIterations(10000);

  /**
   * Performs Gaussian fit on XYSeries
   * 
   * @param data the data
   * @param gMin lower bound of Gaussian fit
   * @param gMax upper bound of Gaussian fit
   * @param sigDigits number of significant digits
   * @return double[] {normFactor, mean, sigma} as a result of
   *         GaussianCurveFitter.create().fit(obs.toList())
   */
  public static double[] gaussianFit(List<DataPoint> data, double gMin, double gMax) {
    // gaussian fit
    WeightedObservedPoints obs = new WeightedObservedPoints();

    for (int i = 0; i < data.size(); i++) {
      double x = data.get(i).getMZ();
      if (x >= gMin && x <= gMax)
        obs.add(x, data.get(i).getIntensity());
    }
    try {
      return fitter.fit(obs.toList());
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("Cannot fit Gaussian from {} to {}", gMin, gMax, e);
      return null;
    }
  }

  /**
   * Performs Gaussian fit on XYSeries
   * 
   * @param series the data
   * @param gMin lower bound of Gaussian fit
   * @param gMax upper bound of Gaussian fit
   * @param sigDigits number of significant digits
   * @return double[] {normFactor, mean, sigma} as a result of
   *         GaussianCurveFitter.create().fit(obs.toList())
   */
  public static double[] gaussianFit(XYSeries series, double gMin, double gMax) {
    // gaussian fit
    WeightedObservedPoints obs = new WeightedObservedPoints();

    for (int i = 0; i < series.getItemCount(); i++) {
      double x = series.getX(i).doubleValue();
      if (x >= gMin && x <= gMax)
        obs.add(x, series.getY(i).doubleValue());
    }

    return fitter.fit(obs.toList());
  }

  /**
   * Performs Gaussian fit on XYSeries
   * 
   * @param data the data
   * @param series the series index
   * @param gMin lower bound of Gaussian fit
   * @param gMax upper bound of Gaussian fit
   * @param sigDigits number of significant digits
   * @return double[] {normFactor, mean, sigma} as a result of
   *         GaussianCurveFitter.create().fit(obs.toList())
   */
  public static double[] gaussianFit(XYDataset data, int series, double gMin, double gMax) {
    // gaussian fit
    WeightedObservedPoints obs = new WeightedObservedPoints();

    for (int i = 0; i < data.getItemCount(series); i++) {
      double x = data.getXValue(series, i);
      if (x >= gMin && x <= gMax)
        obs.add(x, data.getYValue(series, i));
    }
    return fitter.fit(obs.toList());
  }

  /**
   * Adds a Gaussian curve to the plot
   * 
   * @param plot
   * @param series the data
   * @param gMin lower bound of Gaussian fit
   * @param gMax upper bound of Gaussian fit
   * @param sigDigits number of significant digits
   * @return
   */
  public static double[] addGaussianFit(XYPlot plot, XYSeries series, double gMin, double gMax,
      int sigDigits, boolean annotations) {
    double[] fit = gaussianFit(series, gMin, gMax);
    double minval = series.getX(0).doubleValue();
    double maxval = series.getX(series.getItemCount() - 1).doubleValue();
    return addGaussianFit(plot, fit, minval, maxval, gMin, gMax, sigDigits, annotations);
  }

  /**
   * Adds a Gaussian curve to the plot
   * 
   * @param plot
   * @param data the data
   * @param series the series index
   * @param gMin lower bound of Gaussian fit
   * @param gMax upper bound of Gaussian fit
   * @param sigDigits number of significant digits
   * @return
   */
  public static double[] addGaussianFit(XYPlot plot, XYDataset data, int series, double gMin,
      double gMax, int sigDigits, boolean annotations) {
    double[] fit = gaussianFit(data, series, gMin, gMax);
    double minval = data.getX(series, 0).doubleValue();
    double maxval = data.getX(series, data.getItemCount(series) - 1).doubleValue();
    return addGaussianFit(plot, fit, minval, maxval, gMin, gMax, sigDigits, annotations);
  }

  /**
   * Adds a Gaussian curve to the plot
   * 
   * @param plot
   * @param fit double[] {normFactor, mean, sigma}
   * @param drawStart start of curve
   * @param drawEnd end of curve
   * @param sigDigits number of significant digits
   * @return
   */
  public static double[] addGaussianFit(XYPlot plot, double[] fit, double drawStart, double drawEnd,
      int sigDigits, boolean annotations) {
    return addGaussianFit(plot, fit, drawStart, drawEnd, drawStart, drawEnd, sigDigits,
        annotations);
  }

  /**
   * Adds a Gaussian curve to the plot
   * 
   * @param plot
   * @param fit double[] {normFactor, mean, sigma}
   * @param drawStart start of curve
   * @param drawEnd end of curve
   * @param gMin lower bound of Gaussian fit
   * @param gMax upper bound of Gaussian fit
   * @param sigDigits number of significant digits
   * @return
   */
  public static double[] addGaussianFit(XYPlot plot, double[] fit, double drawStart, double drawEnd,
      double gMin, double gMax, int sigDigits, boolean annotations) {
    double gWidth = gMax - gMin;

    Gaussian g = new Gaussian(fit[0], fit[1], fit[2]);

    // create xy series for gaussian
    String mean = Precision.toString(fit[1], sigDigits, 7);
    String sigma = Precision.toString(fit[2], sigDigits, 7);
    String norm = Precision.toString(fit[0], sigDigits, 7);
    XYSeries gs = new XYSeries("Gaussian: " + mean + " \u00B1 " + sigma + " [" + norm
        + "] (mean \u00B1 sigma [normalisation])");
    // add lower dp number out of gaussian fit range
    int steps = 100;
    if (gMin > drawStart) {
      for (int i = 0; i <= steps; i++) {
        double x = drawStart + ((gMin - drawStart) / steps) * i;
        double y = g.value(x);
        gs.add(x, y);
      }
    }
    // add high resolution in gaussian fit area
    steps = 1000;
    for (int i = 0; i <= steps; i++) {
      double x = gMin + (gWidth / steps) * i;
      double y = g.value(x);
      gs.add(x, y);
    }
    // add lower dp number out of gaussian fit range
    steps = 100;
    if (gMax < drawEnd) {
      for (int i = 0; i <= steps; i++) {
        double x = gMax + ((drawEnd - gMax) / steps) * i;
        double y = g.value(x);
        gs.add(x, y);
      }
    }
    // add gaussian
    XYSeriesCollection gsdata = new XYSeriesCollection(gs);
    int index = plot.getDatasetCount();
    plot.setDataset(index, gsdata);
    plot.setRenderer(index, new XYLineAndShapeRenderer(true, false));

    if (annotations)
      addGaussianFitAnnotations(plot, fit);

    return fit;
  }


  /**
   * Adds annotations to the Gaussian fit parameters
   * 
   * @param plot
   * @param fit Gaussian fit {normalisation factor, mean, sigma}
   */
  public static void addGaussianFitAnnotations(XYPlot plot, double[] fit) {
    Paint c = plot.getDomainCrosshairPaint();
    BasicStroke s = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1,
        new float[] {5f, 2.5f}, 0);

    plot.addDomainMarker(new ValueMarker(fit[1], c, s));
    plot.addDomainMarker(new ValueMarker(fit[1] - fit[2], c, s));
    plot.addDomainMarker(new ValueMarker(fit[1] + fit[2], c, s));
  }


  public static JFreeChart createHistogram(double[] data, double binwidth) {
    return createHistogram(data, binwidth, null);
  }

  public static JFreeChart createHistogram(double[] data, double binwidth, String yAxisLabel) {
    Range r = getBounds(data);
    return createHistogram(data, binwidth, yAxisLabel, r.getLowerBound(), r.getUpperBound(), null);
  }

  public static JFreeChart createHistogram(double[] data, double binwidth, String yAxisLabel,
      double min, double max, DoubleFunction<Double> function) {
    if (data != null && data.length > 0) {
      double datawidth = (max - min);
      int cbin = (int) Math.ceil(datawidth / binwidth);
      int[] bins = new int[cbin + 1];

      XYSeries series = createHistoSeries(data, binwidth, min, max, function);
      double barwidth = binwidth;

      // calc new barwidth if a transformation function is defined
      if (function != null) {
        int sum = Arrays.stream(bins).sum();
        // see when 98% of the data is displayed
        int sum2 = 0;
        for (int i = 0; i < bins.length; i++) {
          if (bins[i] > 0) {
            sum2 += bins[i];
            if ((sum2 / (double) sum) >= 0.99) {
              barwidth = function.apply(min + (binwidth / 2.0) + i * binwidth).doubleValue()
                  - function.apply(min + (binwidth / 2.0) + (i - 1) * binwidth).doubleValue();
            }
          }
        }
      }
      return createHistogram(series, barwidth, yAxisLabel);
    } else
      return null;
  }


  public static JFreeChart createHistogram(XYSeries series, double barwidth, String yAxisLabel) {
    XYSeriesCollection xydata = new XYSeriesCollection(series);
    XYBarDataset dataset = new XYBarDataset(xydata, barwidth);
    JFreeChart chart = ChartFactory.createXYBarChart("", yAxisLabel, false, "n", dataset,
        PlotOrientation.VERTICAL, true, true, false);

    XYPlot xyplot = chart.getXYPlot();
    chart.setBackgroundPaint(new Color(230, 230, 230));
    chart.getLegend().setVisible(false);
    xyplot.setForegroundAlpha(0.7F);
    xyplot.setBackgroundPaint(Color.WHITE);
    xyplot.setDomainGridlinePaint(new Color(150, 150, 150));
    xyplot.setRangeGridlinePaint(new Color(150, 150, 150));
    xyplot.getDomainAxis().setVisible(true);
    xyplot.getRangeAxis().setVisible(yAxisLabel != null);
    XYBarRenderer xybarrenderer = (XYBarRenderer) xyplot.getRenderer();
    xybarrenderer.setShadowVisible(false);
    xybarrenderer.setBarPainter(new StandardXYBarPainter());
    xybarrenderer.setDrawBarOutline(false);
    return chart;
  }


  /**
   * Converts from double array to histogram array
   * 
   * @param data
   * @param binwidth
   * @return A histogram array with length = datawidth/binwidth +1 (datawidth = max-min)
   */
  public static XYSeries createHistoSeries(double[] data, double binwidth) {
    Range r = getBounds(data);
    return createHistoSeries(data, binwidth, r.getLowerBound(), r.getUpperBound(), null);
  }

  /**
   * Converts from double array to histogram array
   * 
   * @param data
   * @param binwidth
   * @param min real minimum of data
   * @param max real maximum of data
   * @param function function to transform data axis
   * @return A histogram array with length = datawidth/binwidth +1 (datawidth = max-min)
   */
  public static XYSeries createHistoSeries(double[] data, double binwidth, double min, double max,
      DoubleFunction<Double> function) {
    double datawidth = (max - min);
    int cbin = (int) Math.ceil(datawidth / binwidth);
    int[] bins = new int[cbin + 1];

    // count intensities in bins
    // if value>bin.upper put in next
    for (double v : data) {
      int i = (int) Math.ceil((v - min) / binwidth) - 1;
      if (i < 0) // does only happen if min>than minimum value of data
        i = 0;
      if (i >= bins.length)
        i = bins.length - 1;
      bins[i]++;
    }

    // add zeros around data
    boolean peakStarted = false;
    XYSeries series = new XYSeries("histo", true, true);
    for (int i = 0; i < bins.length; i++) {
      // start peak and add data if>0
      if (bins[i] > 0) {
        // add previous zero once
        if (!peakStarted && i > 0)
          addDPToSeries(series, bins, i - 1, binwidth, min, max, function);

        // add data
        addDPToSeries(series, bins, i, binwidth, min, max, function);

        peakStarted = true;
      } else {
        // add trailing zero
        addDPToSeries(series, bins, i, binwidth, min, max, function);
        peakStarted = false;
      }
    }
    return series;
  }

  /**
   * Converts from double array to histogram array
   * 
   * @param data
   * @param binwidth
   * @param min real minimum of data
   * @param max real maximum of data
   * @param function function to transform data axis
   * @return A histogram array with length = datawidth/binwidth +1 (datawidth = max-min)
   */
  public static XYSeries createHistoSeries(DoubleArrayList data, double binwidth, double min,
      double max, DoubleFunction<Double> function) {
    double datawidth = (max - min);
    int cbin = (int) Math.ceil(datawidth / binwidth);
    int[] bins = new int[cbin + 1];

    // count intensities in bins
    // if value>bin.upper put in next
    for (double v : data) {
      int i = (int) Math.ceil((v - min) / binwidth) - 1;
      if (i < 0) // does only happen if min>than minimum value of data
        i = 0;
      if (i >= bins.length)
        i = bins.length - 1;
      bins[i]++;
    }

    // add zeros around data
    boolean peakStarted = false;
    XYSeries series = new XYSeries("histo", true, true);
    for (int i = 0; i < bins.length; i++) {
      // start peak and add data if>0
      if (bins[i] > 0) {
        // add previous zero once
        if (!peakStarted && i > 0)
          addDPToSeries(series, bins, i - 1, binwidth, min, max, function);

        // add data
        addDPToSeries(series, bins, i, binwidth, min, max, function);

        peakStarted = true;
      } else {
        // add trailing zero
        addDPToSeries(series, bins, i, binwidth, min, max, function);
        peakStarted = false;
      }
    }
    return series;
  }

  private static void addDPToSeries(XYSeries series, int[] bins, int i, double binwidth, double min,
      double max, DoubleFunction<Double> function) {
    // adds a data point to the series
    double x = min + (binwidth / 2.0) + i * binwidth;
    if (function != null)
      x = function.apply(x);
    series.add(x, bins[i]);
  }

  /**
   * Converts from double array to histogram array
   * 
   * @param data
   * @param binwidth
   * @param min real minimum of data
   * @param max real maximum of data
   * @param function function to transform data axis
   * @return A histogram array with length = datawidth/binwidth +1 (datawidth = max-min)
   */
  public static List<DataPoint> createHistoList(DoubleArrayList data, double binwidth, double min,
      double max, DoubleFunction<Double> function) {
    double datawidth = (max - min);
    int cbin = (int) Math.ceil(datawidth / binwidth);
    int[] bins = new int[cbin + 1];

    // count intensities in bins
    // if value>bin.upper put in next
    for (double v : data) {
      int i = (int) Math.ceil((v - min) / binwidth) - 1;
      if (i < 0) // does only happen if min>than minimum value of data
        i = 0;
      if (i >= bins.length)
        i = bins.length - 1;
      bins[i]++;
    }

    // add zeros around data
    List<DataPoint> result = new ArrayList<>();
    boolean peakStarted = false;
    for (int i = 0; i < bins.length; i++) {
      // start peak and add data if>0
      if (bins[i] > 0) {
        // add previous zero once
        if (!peakStarted && i > 0)
          addDPToList(result, bins, i - 1, binwidth, min, max, function);

        // add data
        addDPToList(result, bins, i, binwidth, min, max, function);

        peakStarted = true;
      } else {
        // add trailing zero
        addDPToList(result, bins, i, binwidth, min, max, function);
        peakStarted = false;
      }
    }
    return result;
  }

  private static void addDPToList(List<DataPoint> list, int[] bins, int i, double binwidth,
      double min, double max, DoubleFunction<Double> function) {
    // adds a data point to the series
    double x = min + (binwidth / 2.0) + i * binwidth;
    if (function != null)
      x = function.apply(x);
    list.add(new SimpleDataPoint(x, bins[i]));
  }


  /**
   * Adds a value to a Histogram array. bins array should have length = datawidth/binwidth +1
   * (datawidth = max-min)
   * 
   * @param bins
   * @param value
   * @param binwidth
   * @param min minimum of
   */
  public static void addValueToHistoArray(int[] bins, double value, double binwidth, double min) {
    int i = (int) Math.ceil((value - min) / binwidth) - 1;
    if (i < 0) // does only happen if min>than minimum value of data
      i = 0;
    if (i >= bins.length)
      i = bins.length - 1;
    bins[i]++;
  }



  public static JFreeChart createHistogramOld(double[] data, int bin, String yAxisLabel, double min,
      double max) {
    if (data != null && data.length > 0) {
      HistogramDataset dataset = new HistogramDataset();
      dataset.addSeries("histo", data, bin, min, max);

      JFreeChart chart = ChartFactory.createHistogram("", yAxisLabel, "n", dataset,
          PlotOrientation.VERTICAL, true, false, false);

      chart.setBackgroundPaint(new Color(230, 230, 230));
      chart.getLegend().setVisible(false);
      XYPlot xyplot = chart.getXYPlot();
      xyplot.setForegroundAlpha(0.7F);
      xyplot.setBackgroundPaint(Color.WHITE);
      xyplot.setDomainGridlinePaint(new Color(150, 150, 150));
      xyplot.setRangeGridlinePaint(new Color(150, 150, 150));
      xyplot.getDomainAxis().setVisible(true);
      xyplot.getRangeAxis().setVisible(yAxisLabel != null);
      XYBarRenderer xybarrenderer = (XYBarRenderer) xyplot.getRenderer();
      xybarrenderer.setShadowVisible(false);
      xybarrenderer.setBarPainter(new StandardXYBarPainter());
      // xybarrenderer.setDrawBarOutline(false);
      return chart;
    } else
      return null;
  }

  public static JFreeChart createHistogram(double[] data) {
    double bin = Math.sqrt(data.length);
    Range r = getBounds(data);
    return createHistogram(data, r.getLength() / bin);
  }

  public static JFreeChart createHistogram(double[] data, String yAxisLabel) {
    Range range = getBounds(data);
    return createHistogram(data, yAxisLabel, range.getLowerBound(), range.getUpperBound());
  }

  public static JFreeChart createHistogram(double[] data, String yAxisLabel, double min,
      double max) {
    return createHistogram(data, yAxisLabel, min, max, val -> val);
  }

  public static JFreeChart createHistogram(double[] data, String yAxisLabel, double min, double max,
      DoubleFunction<Double> function) {
    int bin = (int) Math.sqrt(data.length);
    Range r = getBounds(data);
    return createHistogram(data, r.getLength() / bin, yAxisLabel, min, max, function);
  }

  /**
   * 
   * @param data
   * @param yAxisLabel
   * @param width automatic width if parameter is <=0
   * @return
   */
  public static JFreeChart createHistogram(double[] data, String yAxisLabel, double width) {
    Range range = getBounds(data);
    return createHistogram(data, yAxisLabel, width, range.getLowerBound(), range.getUpperBound());
  }

  /**
   * 
   * @param data
   * @param yAxisLabel
   * @param width automatic width if parameter is <=0
   * @return
   */
  public static JFreeChart createHistogram(double[] data, String yAxisLabel, double width,
      double min, double max) {
    if (width <= 0)
      return createHistogram(data, yAxisLabel, min, max);
    else {
      return createHistogram(data, width, yAxisLabel, min, max, val -> val);
    }
  }

  /**
   * 
   * @param data
   * @param yAxisLabel
   * @param width automatic width if parameter is <=0
   * @param function transform the data axis after binning
   * @return
   */
  public static JFreeChart createHistogram(double[] data, String yAxisLabel, double width,
      double min, double max, DoubleFunction<Double> function) {
    if (width <= 0)
      return createHistogram(data, yAxisLabel, min, max, function);
    else {
      return createHistogram(data, width, yAxisLabel, min, max, function);
    }
  }

  public static double getMin(double[] data) {
    double min = Double.MAX_VALUE;
    for (double d : data)
      if (d < min)
        min = d;
    return min;
  }

  public static double getMax(double[] data) {
    double max = Double.NEGATIVE_INFINITY;
    for (double d : data)
      if (d > max)
        max = d;
    return max;
  }

  public static Range getBounds(double[] data) {
    double min = Double.MAX_VALUE;
    double max = Double.NEGATIVE_INFINITY;
    for (double d : data) {
      if (d < min)
        min = d;
      if (d > max)
        max = d;
    }
    return new Range(min, max);
  }
}
