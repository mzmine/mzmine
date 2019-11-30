package io.github.mzmine.datamodel.data.types.fx;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ZoomableChart extends BorderPane {

  private LineChart<Number, Number> chart;

  public ZoomableChart(LineChart<Number, Number> chart) {
    this.chart = chart;

    final StackPane chartContainer = new StackPane();
    chartContainer.getChildren().add(chart);

    final Rectangle zoomRect = new Rectangle();
    zoomRect.setManaged(false);
    zoomRect.setFill(Color.LIGHTSEAGREEN.deriveColor(0, 1, 1, 0.5));
    chartContainer.getChildren().add(zoomRect);

    setUpZooming(zoomRect, chart);

    final HBox controls = new HBox(10);
    controls.setPadding(new Insets(10));
    controls.setAlignment(Pos.CENTER);

    final Button zoomButton = new Button("Zoom");
    final Button resetButton = new Button("Reset");
    zoomButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        doZoom(zoomRect, chart);
      }
    });
    resetButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        final NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(1000);
        final NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(1000);

        zoomRect.setWidth(0);
        zoomRect.setHeight(0);
      }
    });
    final BooleanBinding disableControls =
        zoomRect.widthProperty().lessThan(5).or(zoomRect.heightProperty().lessThan(5));
    zoomButton.disableProperty().bind(disableControls);
    controls.getChildren().addAll(zoomButton, resetButton);

    this.setCenter(chartContainer);
    this.setBottom(controls);
  }



  private void setUpZooming(final Rectangle rect, final Node zoomingNode) {
    final ObjectProperty<Point2D> mouseAnchor = new SimpleObjectProperty<>();
    zoomingNode.setOnMousePressed(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent event) {
        mouseAnchor.set(new Point2D(event.getX(), event.getY()));
        rect.setWidth(0);
        rect.setHeight(0);
      }
    });
    zoomingNode.setOnMouseDragged(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        rect.setX(Math.min(x, mouseAnchor.get().getX()));
        rect.setY(Math.min(y, mouseAnchor.get().getY()));
        rect.setWidth(Math.abs(x - mouseAnchor.get().getX()));
        rect.setHeight(Math.abs(y - mouseAnchor.get().getY()));
      }
    });
  }

  private void doZoom(Rectangle zoomRect, LineChart<Number, Number> chart) {
    Point2D zoomTopLeft = new Point2D(zoomRect.getX(), zoomRect.getY());
    Point2D zoomBottomRight =
        new Point2D(zoomRect.getX() + zoomRect.getWidth(), zoomRect.getY() + zoomRect.getHeight());
    final NumberAxis yAxis = (NumberAxis) chart.getYAxis();
    final NumberAxis xAxis = (NumberAxis) chart.getXAxis();

    // final Bounds bb = chart.getBackground().sceneToLocal(zoomRect.getBoundsInLocal());
    //
    // final double minx = bb.getMinX();
    // final double maxx = bb.getMaxX();
    //
    // final double miny = bb.getMinY();
    // final double maxy = bb.getMaxY();
    //
    // doZoom(true, chart.getXAxis().getValueForDisplay(minx),
    // chart.getXAxis().getValueForDisplay(maxx));
    //
    // doZoom(false, chart.getYAxis().getValueForDisplay(miny),
    // chart.getYAxis().getValueForDisplay(maxy));
    //
    // rect.setWidth(0);
    // rect.setHeight(0);
    //
    // double xOffset = zoomTopLeft.getX() - yAxisInScene.getX();
    // double yOffset = zoomBottomRight.getY() - xAxisInScene.getY();
    // double xAxisScale = xAxis.getScale();
    // double yAxisScale = yAxis.getScale();
    // xAxis.setLowerBound(xAxis.getLowerBound() + xOffset / xAxisScale);
    // xAxis.setUpperBound(xAxis.getLowerBound() + zoomRect.getWidth() / xAxisScale);
    // yAxis.setLowerBound(yAxis.getLowerBound() + yOffset / yAxisScale);
    // yAxis.setUpperBound(yAxis.getLowerBound() - zoomRect.getHeight() / yAxisScale);
    // System.out.println(yAxis.getLowerBound() + " " + yAxis.getUpperBound());
    // zoomRect.setWidth(0);
    // zoomRect.setHeight(0);
  }
}
