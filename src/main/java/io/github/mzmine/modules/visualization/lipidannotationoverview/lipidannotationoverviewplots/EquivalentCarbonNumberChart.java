package io.github.mzmine.modules.visualization.lipidannotationoverview.lipidannotationoverviewplots;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import java.util.Arrays;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class EquivalentCarbonNumberChart extends EChartViewer implements XYItemLabelGenerator {

  public EquivalentCarbonNumberChart(String title, String xAxisLabel, String yAxisLabel,
      EquivalentCarbonNumberDataset dataset) {
    super(ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset,
        PlotOrientation.VERTICAL, false, true, true));

    // Calculate linear regression parameters
    double[] regressionParams = calculateLinearRegression(dataset,
        0); // Use appropriate series index
    // Add linear regression line to the plot
    getChart().getXYPlot().setDataset(1, createRegressionDataset(regressionParams,
        Arrays.stream(dataset.getXValues()).min().getAsDouble(),
        Arrays.stream(dataset.getXValues()).max().getAsDouble()));
    getChart().getXYPlot().setRenderer(1, new XYLineAndShapeRenderer(true, false));

    EStandardChartTheme defaultChartTheme = MZmineCore.getConfiguration().getDefaultChartTheme();
    XYItemRenderer renderer = new XYLineAndShapeRenderer();
    renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));
    renderer.setSeriesPaint(0,
        MZmineCore.getConfiguration().getDefaultColorPalette().getNeutralColorAWT());
    renderer.setDefaultItemLabelGenerator(this);
    renderer.setDefaultItemLabelsVisible(true);
    ((XYLineAndShapeRenderer) renderer).setSeriesLinesVisible(0, false);
    this.getChart().getXYPlot().setRenderer(renderer);
    defaultChartTheme.apply(this);
  }

  @Override
  public String generateLabel(XYDataset dataset, int series, int item) {
    if (isLabelOverlap(dataset, series, item)) {
      return null; // Avoid label overlap by not displaying the label
    }

    MatchedLipid matchedLipid = ((EquivalentCarbonNumberDataset) dataset).getMatchedLipid(item);
    return matchedLipid.getLipidAnnotation().getAnnotation();
  }

  private boolean isLabelOverlap(XYDataset dataset, int series, int currentItem) {
    double xAxisLowerBound = getChart().getXYPlot().getDomainAxis().getLowerBound();
    double xAxisUpperBound = getChart().getXYPlot().getDomainAxis().getUpperBound();
    double xAxisRange = xAxisUpperBound - xAxisLowerBound;

    double yAxisLowerBound = getChart().getXYPlot().getRangeAxis().getLowerBound();
    double yAxisUpperBound = getChart().getXYPlot().getRangeAxis().getUpperBound();
    double yAxisRange = yAxisUpperBound - yAxisLowerBound;

    // Calculate the thresholds based on the zoom levels (adjust as needed)
    double xLabelOverlapThreshold = 0.01 * xAxisRange;
    double yLabelOverlapThreshold = 0.01 * yAxisRange;

    double currentX = dataset.getXValue(series, currentItem);
    double currentY = dataset.getYValue(series, currentItem);

    double closestXDistance = Double.MAX_VALUE;
    double closestYDistance = Double.MAX_VALUE;
    int closestXIndex = -1;
    int closestYIndex = -1;

    // Iterate through the existing items and find the closest data points in x and y
    for (int item = 0; item < dataset.getItemCount(series); item++) {
      if (item == currentItem) {
        continue; // Skip checking against itself
      }

      double existingX = dataset.getXValue(series, item);
      double existingY = dataset.getYValue(series, item);

      // Calculate distances in x and y directions
      double xDistance = Math.abs(existingX - currentX);
      double yDistance = Math.abs(existingY - currentY);

      // Update closest distances and indices if the current distances are smaller
      if (xDistance < closestXDistance) {
        closestXDistance = xDistance;
        closestXIndex = item;
      }
      if (yDistance < closestYDistance) {
        closestYDistance = yDistance;
        closestYIndex = item;
      }
    }

    // Check if either x or y closest distance is less than their respective thresholds
    return (closestXDistance < xLabelOverlapThreshold && closestXIndex != -1) || (
        closestYDistance < yLabelOverlapThreshold && closestYIndex != -1);
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

    return new double[]{slope, intercept};
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

}
