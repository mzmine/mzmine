/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.equivalentcarbonnumberplot;

import io.github.mzmine.gui.chartbasics.FxChartFactory;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import java.util.Arrays;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class EquivalentCarbonNumberChart extends EChartViewer implements XYItemLabelGenerator,
    XYToolTipGenerator {

  private final double r2;

  public EquivalentCarbonNumberChart(String title, String xAxisLabel, String yAxisLabel,
      EquivalentCarbonNumberDataset dataset) {
    super(FxChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset,
        PlotOrientation.VERTICAL, false, true, true));
    setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

    // Calculate linear regression parameters
    double[] regressionParams = calculateLinearRegression(dataset, 0);

    // Add linear regression line to the plot
    getChart().getXYPlot().setDataset(1, createRegressionDataset(regressionParams,
        Arrays.stream(dataset.getXValues()).min().getAsDouble(),
        Arrays.stream(dataset.getXValues()).max().getAsDouble()));
    XYLineAndShapeRenderer regressionRenderer = new XYLineAndShapeRenderer(true, false);
    getChart().getXYPlot().setRenderer(1, regressionRenderer);
    regressionRenderer.setSeriesPaint(1,
        MZmineCore.getConfiguration().getDefaultColorPalette().getNeutralColorAWT());

    this.r2 = regressionParams[2];

    EStandardChartTheme defaultChartTheme = MZmineCore.getConfiguration().getDefaultChartTheme();
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
    renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));
    renderer.setDefaultItemLabelGenerator(this);
    renderer.setDefaultItemLabelsVisible(true);
    renderer.setSeriesLinesVisible(0, false);
    renderer.setDefaultToolTipGenerator(this);
    this.getChart().getXYPlot().setRenderer(renderer);
    defaultChartTheme.apply(this);
    renderer.setSeriesPaint(0,
        MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT());
    regressionRenderer.setSeriesPaint(0,
        MZmineCore.getConfiguration().getDefaultColorPalette().getNeutralColorAWT());
    setStickyZeroRangeAxis(false);
  }

  @Override
  public String generateToolTip(XYDataset dataset, int series, int item) {
    MatchedLipid matchedLipid = ((EquivalentCarbonNumberDataset) dataset).getMatchedLipid(item);
    return matchedLipid.getLipidAnnotation().getAnnotation();
  }

  @Override
  public String generateLabel(XYDataset dataset, int series, int item) {
    MatchedLipid matchedLipid = ((EquivalentCarbonNumberDataset) dataset).getMatchedLipid(item);

    if (matchedLipid == null) {
      return null;
    }
    double originalY = dataset.getYValue(0, item);

    //show only annotation with best MS/MS score if available
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      if (dataset.getYValue(0, i) == originalY && i != item) {
        if (((EquivalentCarbonNumberDataset) dataset).getMatchedLipid(item).getMsMsScore() != null
            && ((EquivalentCarbonNumberDataset) dataset).getMatchedLipid(item).getMsMsScore()
            < ((EquivalentCarbonNumberDataset) dataset).getMatchedLipid(i).getMsMsScore()) {
          return null;
        }
      }
    }

    return matchedLipid.getLipidAnnotation().getAnnotation();
  }

  private double[] calculateLinearRegression(XYDataset dataset, int series) {
    double sumX = 0.0;
    double sumY = 0.0;
    double sumXX = 0.0;
    double sumXY = 0.0;
    int itemCount = dataset.getItemCount(series);

    for (int i = 0; i < itemCount; i++) {
      double x = dataset.getXValue(series, i);
      double y = dataset.getYValue(series, i);
      sumX += x;
      sumY += y;
      sumXX += x * x;
      sumXY += x * y;
    }

    double slope = (itemCount * sumXY - sumX * sumY) / (itemCount * sumXX - sumX * sumX);
    double intercept = (sumY - slope * sumX) / itemCount;

    // Calculate R-squared
    double meanY = sumY / itemCount;
    double ssr = 0.0; // regression sum of squares
    double sse = 0.0; // error sum of squares

    for (int i = 0; i < itemCount; i++) {
      double x = dataset.getXValue(series, i);
      double y = dataset.getYValue(series, i);
      double predictedY = slope * x + intercept;
      ssr += (predictedY - meanY) * (predictedY - meanY);
      sse += (y - predictedY) * (y - predictedY);
    }

    double rSquared = ssr / (ssr + sse);

    return new double[]{slope, intercept, rSquared};
  }

  private XYDataset createRegressionDataset(double[] params, double minX, double maxX) {
    // Create a dataset for the linear regression line
    // This dataset will have two points (min and max X values) to draw the line
    XYSeries series = new XYSeries("Regression Line");
    series.add(minX, params[0] * minX + params[1]);
    series.add(maxX, params[0] * maxX + params[1]);

    XYSeriesCollection dataset = new XYSeriesCollection();
    dataset.addSeries(series);

    return dataset;
  }

  public double getR2() {
    return r2;
  }

}
