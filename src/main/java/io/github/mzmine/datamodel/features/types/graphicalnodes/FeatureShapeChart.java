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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.util.color.ColorsFX;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class FeatureShapeChart extends StackPane {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public FeatureShapeChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {
    try {
      final NumberAxis xAxis = new NumberAxis();
      final NumberAxis yAxis = new NumberAxis();
      final LineChart<Number, Number> bc = new LineChart<>(xAxis, yAxis);

      // DataPoint max = null;
      double minRT = Double.MAX_VALUE, maxRT = 0;
      int size = row.getFilesFeatures().size();
      int fi = 0;
      for (Feature f : row.getFeatures()) {
        XYChart.Series<Number, Number> data = new XYChart.Series<>();
        List<Integer> scans = f.getScanNumbers();
        List<DataPoint> dps = f.getDataPoints();
        RawDataFile raw = f.getRawDataFile();
        // add data points retention time -> intensity
        for (int i = 0; i < scans.size(); i++) {
          DataPoint dp = dps.get(i);
          double retentionTime = raw.getScan(scans.get(i)).getRetentionTime();
          double intensity = dp == null ? 0 : dp.getIntensity();
          data.getData().add(new XYChart.Data<>(retentionTime, intensity));
          /*
           * if (dp != null && (max == null || max.getIntensity() < dp.getIntensity())) { max = dp;
           * }
           */
          if (retentionTime > maxRT) {
            maxRT = retentionTime;
          }
          if (retentionTime < minRT) {
            minRT = retentionTime;
          }
          if (progress != null)
            progress.addAndGet(1.0 / size / scans.size());
        }
        fi++;
        bc.getData().add(data);

        // set series color according to the rawDataFile color
        Node line = data.getNode().lookup(".chart-series-line");
        Color fileColor = raw.getColor();
        if (fileColor == null) {
          fileColor = Color.DARKORANGE;
        }
        line.setStyle("-fx-stroke: " + ColorsFX.toHexString(fileColor) + ";");

        if (progress != null)
          progress.set((double) fi / size);
      }

      bc.setLegendVisible(false);
      bc.setMinHeight(100);
      bc.setPrefHeight(100);
      bc.setMaxHeight(100);
      bc.setPrefWidth(150);
      bc.setCreateSymbols(false);

      // do not add data to chart
      xAxis.setAutoRanging(false);
      xAxis.setUpperBound(maxRT);
      xAxis.setLowerBound(minRT == Double.MAX_VALUE ? 0 : minRT);

      bc.setOnScroll(new EventHandler<>() {
        @Override
        public void handle(ScrollEvent event) {
          NumberAxis axis = xAxis;
          final double minX = xAxis.getLowerBound();
          final double maxX = xAxis.getUpperBound();
          double d = maxX - minX;
          double x = event.getX();
          double direction = event.getDeltaY();
          if (direction > 0) {
            if (d > 0.3) {
              axis.setLowerBound(minX + 0.1);
              axis.setUpperBound(maxX - 0.1);
            }
          } else {
            axis.setLowerBound(minX - 0.1);
            axis.setUpperBound(maxX + 0.1);
          }
          event.consume();
        }
      });

      this.getChildren().add(bc);
    } catch (Exception ex) {
      logger.log(Level.WARNING, "error in DP", ex);
    }
  }
}
