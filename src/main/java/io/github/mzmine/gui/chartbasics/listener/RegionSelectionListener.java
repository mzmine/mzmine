/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.gui.chartbasics.listener;

import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.input.MouseButton;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;

public class RegionSelectionListener implements ChartMouseListenerFX {

  private final ObjectProperty<java.awt.geom.Path2D> buildingPath;
  private final ListProperty<Point2D> points;
  private final EChartViewer chart;

  public RegionSelectionListener(EChartViewer chart) {
    this.chart = chart;
    points = new SimpleListProperty<>(FXCollections.observableArrayList());
    buildingPath = new SimpleObjectProperty<>();
    points.addListener((ListChangeListener<Point2D>) c -> {
      c.next();
      buildingPath.set(getShape());
    });
  }

  @Override
  public void chartMouseClicked(ChartMouseEventFX event) {
    event.getTrigger().consume();
    if (event.getTrigger().getButton() != MouseButton.PRIMARY) {
      return;
    }

    Point2D p = ChartLogicsFX
        .mouseXYToPlotXY(chart, event.getTrigger().getX(), event.getTrigger().getY());

    points.get().add(p);
//    buildingPath.set(getShape());
  }

  @Override
  public void chartMouseMoved(ChartMouseEventFX event) {

  }

  private Path2D getShape() {
    if (points.isEmpty()) {
      return null;
    }
    Path2D path = new Path2D.Double();
    path.moveTo(points.get(0).getX(), points.get(0).getY());

    for (int i = 1; i < points.size(); i++) {
      path.lineTo(points.get(i).getX(), points.get(i).getY());
    }
    path.closePath();
    return path;
  }

  public ObjectProperty<Path2D> buildingPathProperty() {
    return buildingPath;
  }

  public ListProperty<Point2D> buildingPointsProperty() {
    return points;
  }
}
