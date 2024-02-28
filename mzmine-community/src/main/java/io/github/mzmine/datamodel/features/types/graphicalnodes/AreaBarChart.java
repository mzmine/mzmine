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

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.util.color.ColorsFX;
import java.util.Map.Entry;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

public class AreaBarChart extends StackPane {

  public AreaBarChart(@NotNull ModularFeatureListRow row, AtomicDouble progress) {
    XYChart.Series data = new XYChart.Series();
    int i = 1;
    int size = row.getFilesFeatures().size();
    for (Entry<RawDataFile, ModularFeature> entry : row.getFilesFeatures().entrySet()) {
      Float areaProperty = entry.getValue().getArea();
      // set bar color according to the raw data file
      Data newData =
          new XYChart.Data("" + i, areaProperty == null ? 0f : areaProperty);
      newData.nodeProperty().addListener((ChangeListener<Node>) (ov, oldNode, newNode) -> {
        if (newNode != null) {
          Node node = newData.getNode();
          Color fileColor = entry.getKey().getColor();
          if (fileColor == null) {
            fileColor = Color.DARKORANGE;
          }
          node.setStyle("-fx-bar-fill: " + ColorsFX.toHexString(fileColor) + ";");
        }
      });
      data.getData().add(newData);
      i++;
      if (progress != null)
        progress.addAndGet(1.0 / size);
    }

    final CategoryAxis xAxis = new CategoryAxis();
    final NumberAxis yAxis = new NumberAxis();
    final BarChart<String, Number> bc = new BarChart<String, Number>(xAxis, yAxis);
    bc.setLegendVisible(false);
    bc.setMinHeight(100);
    bc.setPrefHeight(100);
    bc.setMaxHeight(100);
    bc.setBarGap(3);
    bc.setCategoryGap(3);
    bc.setPrefWidth(150);

    bc.getData().addAll(data);
    this.getChildren().add(bc);
  }

}
