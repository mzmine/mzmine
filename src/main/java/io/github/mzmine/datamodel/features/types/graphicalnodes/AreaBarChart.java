/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
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
