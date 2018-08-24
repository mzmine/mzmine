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

package net.sf.mzmine.chartbasics.gui.javafx.demo;


import java.util.Random;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import net.sf.mzmine.chartbasics.gui.javafx.EChartViewer;

public class FXChartGestureDemo extends Application {

  @Override
  public void start(Stage stage) throws Exception {
    XYDataset dataset = createDataset();
    JFreeChart chart = ChartFactory.createXYLineChart("Random", "i", "r", createDataset());
    EChartViewer canvas = new EChartViewer(chart);
    StackPane stackPane = new StackPane();
    stackPane.getChildren().add(canvas);
    stage.setScene(new Scene(stackPane));
    stage.setTitle("Chart gesture demo");
    stage.setWidth(700);
    stage.setHeight(390);
    stage.show();
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
