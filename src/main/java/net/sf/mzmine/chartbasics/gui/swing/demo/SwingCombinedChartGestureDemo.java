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

package net.sf.mzmine.chartbasics.gui.swing.demo;


import java.awt.BorderLayout;
import java.util.Random;
import javax.swing.JFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import javafx.scene.layout.StackPane;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;

public class SwingCombinedChartGestureDemo extends JFrame {

  public SwingCombinedChartGestureDemo() {
    setSize(800, 600);
    setTitle("Chart gesture test");
    JFreeChart chart = createCombinedChart();
    EChartPanel canvas = new EChartPanel(chart);
    StackPane stackPane = new StackPane();
    getContentPane().add(canvas, BorderLayout.CENTER);
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
    new SwingCombinedChartGestureDemo().setVisible(true);
  }

}
