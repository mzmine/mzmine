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

package io.github.mzmine.gui.chartbasics.gui.javafx.demo;

import java.util.Random;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class FXCombinedChartGestureDemo extends Application {

  @Override
  public void start(Stage stage) throws Exception {
    try {
      JFreeChart chart = createCombinedChart();
      EChartViewer canvas = new EChartViewer(chart);
      StackPane stackPane = new StackPane();
      stackPane.getChildren().add(canvas);
      stage.setScene(new Scene(stackPane));
      stage.setTitle("Chart gesture demo");
      stage.setWidth(700);
      stage.setHeight(390);
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private JFreeChart createCombinedChart() {
    // create subplot 1...
    final XYDataset data1 = createDataset();
    final XYItemRenderer renderer1 = new StandardXYItemRenderer();
    final NumberAxis rangeAxis1 = new NumberAxis("Range 1");
    final XYPlot subplot1 = new XYPlot(data1, null, rangeAxis1, renderer1);

    // create subplot 2...
    final XYDataset data2 = createDataset();
    final XYItemRenderer renderer2 = new StandardXYItemRenderer();
    final NumberAxis rangeAxis2 = new NumberAxis("Range 2");
    final XYPlot subplot2 = new XYPlot(data2, null, rangeAxis2, renderer2);

    // parent plot...
    final CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new NumberAxis("Domain"));
    plot.setGap(10.0);

    // add the subplots...
    plot.add(subplot1, 1);
    plot.add(subplot2, 1);
    plot.setOrientation(PlotOrientation.VERTICAL);

    // return a new chart containing the overlaid plot...
    return new JFreeChart("CombinedDomainXYPlot Demo", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
  }

  /**
   * Creates a dataset, consisting of two series of monthly data.
   *
   * @return the dataset.
   */
  private static XYDataset createDataset() {
    XYSeriesCollection data = new XYSeriesCollection();

    Random r = new Random(System.currentTimeMillis());

    for (int i = 0; i < 3; i++) {
      XYSeries s = new XYSeries("Series" + i);
      for (int x = 0; x < 100; x++) {
        double v = r.nextGaussian() * (i + 1);
        s.add(x, v);
      }
      data.addSeries(s);
    }
    return data;
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    launch(args);
  }

}
