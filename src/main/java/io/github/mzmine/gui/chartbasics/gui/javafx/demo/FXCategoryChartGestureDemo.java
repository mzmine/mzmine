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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class FXCategoryChartGestureDemo extends Application {

  @Override
  public void start(Stage stage) throws Exception {
    JFreeChart chart = ChartFactory.createBarChart("Random", "Category", "value", createDataset());
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
  private static CategoryDataset createDataset() {
    DefaultCategoryDataset data = new DefaultCategoryDataset();
    Random r = new Random(System.currentTimeMillis());
    for (int i = 0; i < 3; i++) {
      for (int t = 0; t < 2; t++) {

        for (int x = 0; x < 100; x++) {
          double v = r.nextGaussian() * (i + 1);
          data.addValue(v, "series" + i, "type" + t);
        }
      }
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
