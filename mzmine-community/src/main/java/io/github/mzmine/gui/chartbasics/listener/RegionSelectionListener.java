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
