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

package io.github.mzmine.gui.chartbasics.gui.swing.demo;

import java.awt.BorderLayout;
import java.util.Random;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import io.github.mzmine.gui.chartbasics.gui.swing.EChartPanel;
import javafx.scene.layout.StackPane;

public class SwingChartGestureDemo extends JFrame {

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    new SwingChartGestureDemo().setVisible(true);
  }

  public SwingChartGestureDemo() {
    setSize(800, 600);
    setTitle("Chart gesture test");
    XYDataset dataset = createDataset();
    JFreeChart chart = ChartFactory.createXYLineChart("Random", "i", "r", createDataset());
    EChartPanel canvas = new EChartPanel(chart);
    StackPane stackPane = new StackPane();
    getContentPane().add(canvas, BorderLayout.CENTER);
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

}
