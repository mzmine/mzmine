/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.mzmine.datamodel.data.types.fx;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ZoomManager<X, Y> {

  static <X, Y> ObservableList<XYChart.Series<X, Y>> deepCopySeries(
      final Collection<XYChart.Series<X, Y>> data) {
    final ObservableList<XYChart.Series<X, Y>> backup = FXCollections.observableArrayList();
    for (final Series<X, Y> s : data) {
      backup.add(deepCopySeries(s));
    }
    return backup;
  }

  static <X, Y> XYChart.Series<X, Y> deepCopySeries(final XYChart.Series<X, Y> series) {
    final XYChart.Series<X, Y> result = new XYChart.Series<>();
    result.setName(series.getName());
    result.setData(deepCopySeriesData(series.getData()));
    return result;
  }

  static <X, Y> ObservableList<XYChart.Data<X, Y>> deepCopySeriesData(
      final Collection<? extends XYChart.Data<X, Y>> data) {
    final ObservableList<XYChart.Data<X, Y>> result = FXCollections.observableArrayList();
    for (final Data<X, Y> i : data) {
      result.add(new Data<>(i.getXValue(), i.getYValue()));
    }
    return result;
  }

  static <X, Y> ObservableList<X> extractXValues(final ObservableList<Data<X, Y>> data) {
    final ObservableList<X> result = FXCollections.observableArrayList();
    for (final Data<X, Y> d : data) {
      result.add(d.getXValue());
    }
    return result;
  }

  static <X, Y> ObservableList<Y> extractYValues(final ObservableList<Data<X, Y>> data) {
    final ObservableList<Y> result = FXCollections.observableArrayList();
    for (final Data<X, Y> d : data) {
      result.add(d.getYValue());
    }
    return result;
  }

  static Object getObject(final Axis<?> axis, final double cooridnate) {
    final Object object = axis.getValueForDisplay(cooridnate);
    return object;
  }

  static Node getRootNode(final Node node) {
    Node n = node;
    while (n.getParent() != null) {
      n = n.getParent();
    }
    return n;
  }

  private final ObservableList<XYChart.Series<X, Y>> series;

  private final XYChart<X, Y> chart;

  private volatile boolean zoomed;

  /**
   *
   *
   * Creates a new {@code ZoomManager}.
   * </p>
   * <b> Due to a bug, {@code series} must not be added to the chart! </b> If you do so, the chart
   * will be emtpy.
   * </p>
   *
   *
   * @param chartParent the chart's parent {@link Pane}.
   * @param chart the {@link XYChart} to which zooming functionality is added
   * @param series collection of chart data to display
   * @throws IllegalArgumentException if chart data is empty or {@code null}
   */
  public ZoomManager(final Pane chartParent, final XYChart<X, Y> chart,
      final Collection<? extends Series<X, Y>> series) {
    super();
    this.chart = Objects.requireNonNull(chart);
    if (series == null || series.isEmpty()) {
      throw new IllegalArgumentException("No chart data given");
    }
    this.series = FXCollections.observableArrayList(series);
    restoreData();
    final Rectangle zoomRect = new Rectangle();
    zoomRect.setManaged(false);
    zoomRect.setFill(Color.LIGHTSEAGREEN.deriveColor(0, 1, 1, 0.5));
    chartParent.getChildren().add(zoomRect);
    setUpZooming(zoomRect, chart);

  }

  /**
   *
   *
   * Creates a new {@code ZoomManager}.
   * </p>
   * <b> Due to a bug, {@code series} must not be added to the chart! </b> If you do so, the chart
   * will be emtpy.
   * </p>
   *
   *
   * @param chartParent the chart's parent {@link Pane}.
   * @param chart the {@link XYChart} to which zooming functionality is added
   * @param series array of chart data to display
   * @throws IllegalArgumentException if chart data is empty or {@code null}
   */
  public ZoomManager(final Pane chartParent, final XYChart<X, Y> chart,
      final Series<X, Y>... series) {
    this(chartParent, chart, Arrays.asList(series));

  }

  private void doZoom(final boolean x, final Number n1, final Number n2) {
    final double min = Math.min(n1.doubleValue(), n2.doubleValue());
    final double max = Math.max(n1.doubleValue(), n2.doubleValue());
    if (max - min > 1) {
      zoomed = true;
      final Iterator<XYChart.Series<X, Y>> it = chart.getData().iterator();
      while (it.hasNext()) {
        final XYChart.Series<X, Y> s = it.next();
        final Iterator<XYChart.Data<X, Y>> it2 = s.getData().iterator();
        while (it2.hasNext()) {
          final XYChart.Data<X, Y> d = it2.next();
          final Object value;
          if (x) {
            value = d.getXValue();
          } else {
            value = d.getYValue();
          }
          if (value instanceof Number) {
            final Number n = (Number) value;
            final double dd = n.doubleValue();
            if (dd < min || dd > max) {
              it2.remove();
            } else {
            }
          }
          if (s.getData().isEmpty()) {
            it.remove();
          }
        }
      }
    } else {
      // System.out.println("Skip tiny zoom");
    }

  }

  private void doZoom(final boolean x, final Object o1, final Object o2) {
    if (o1 instanceof Number && o2 instanceof Number) {
      doZoom(x, (Number) o1, (Number) o2);
    } else if (o1 instanceof String || o2 instanceof String) {
      doZoom(x, (String) o1, (String) o2);
    } else {
      final int wait = 0;
    }
  }

  private void doZoom(final boolean x, String s1, String s2) {
    if (s1 == null && s2 == null) {
      return;
    }
    if (s1 == null) {
      s1 = s2;
    }
    if (s2 == null) {
      s2 = s1;
    }

    final Iterator<XYChart.Series<X, Y>> it = chart.getData().iterator();
    while (it.hasNext()) {
      final XYChart.Series<X, Y> s = it.next();
      final List<?> values;
      if (x) {
        values = extractXValues(s.getData());
      } else {
        values = extractYValues(s.getData());
      }
      final int index1 = values.indexOf(s1);
      final int index2 = values.indexOf(s2);
      final int lower = Math.min(index1, index2);
      final int upper = Math.max(index1, index2);
      final Iterator<Data<X, Y>> it2 = s.getData().iterator();
      while (it2.hasNext()) {
        final Data<X, Y> d = it2.next();
        final Object value;
        if (x) {
          value = d.getXValue();
        } else {
          value = d.getYValue();
        }
        final int index = values.indexOf(value);
        if (index != -1 && (index < lower || index > upper)) {
          it2.remove();
        }
      }
    }
  }

  private synchronized void restoreData() {
    // make a tmp variable of data, since we will modify it but need to be
    // able to restore
    final ObservableList<XYChart.Series<X, Y>> backup2 = deepCopySeries(series);
    chart.getData().setAll(backup2);

  }

  private void setUpZooming(final Rectangle rect, final XYChart<X, Y> chart) {

    setUpZoomingRectangle(rect);

  }

  /**
   * Displays a colored rectangle that will indicate zooming boundaries
   *
   * @param rect
   */
  private void setUpZoomingRectangle(final Rectangle rect) {

    /*
     * The chart background. Needed to get the correct rectangle boundaries.
     */
    final Node chartBackground = chart.lookup(".chart-plot-background");
    final ObjectProperty<Point2D> mouseAnchor = new SimpleObjectProperty<>();
    chart.setOnMousePressed(new EventHandler<MouseEvent>() {
      @Override
      public void handle(final MouseEvent event) {
        mouseAnchor.set(new Point2D(event.getX(), event.getY()));
      }
    });
    chart.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(final MouseEvent event) {
        if (event.getButton().equals(MouseButton.PRIMARY)) {
          if (zoomed && event.getClickCount() == 2) {
            restoreData();
            zoomed = false;
            event.consume();
          }
        }
      }
    });
    chart.setOnMouseDragged(new EventHandler<MouseEvent>() {
      @Override
      public void handle(final MouseEvent event) {
        final double x = event.getX();
        final double y = event.getY();
        rect.setX(Math.min(x, mouseAnchor.get().getX()));
        rect.setY(Math.min(y, mouseAnchor.get().getY()));
        rect.setWidth(Math.abs(x - mouseAnchor.get().getX()));
        rect.setHeight(Math.abs(y - mouseAnchor.get().getY()));
      }
    });
    chart.setOnMouseReleased(new EventHandler<MouseEvent>() {
      @Override
      public void handle(final MouseEvent event) {

        final Bounds bb = chartBackground.sceneToLocal(rect.getBoundsInLocal());

        final double minx = bb.getMinX();
        final double maxx = bb.getMaxX();

        final double miny = bb.getMinY();
        final double maxy = bb.getMaxY();

        doZoom(true, chart.getXAxis().getValueForDisplay(minx),
            chart.getXAxis().getValueForDisplay(maxx));

        doZoom(false, chart.getYAxis().getValueForDisplay(miny),
            chart.getYAxis().getValueForDisplay(maxy));

        rect.setWidth(0);
        rect.setHeight(0);
      }
    });
  }

}
