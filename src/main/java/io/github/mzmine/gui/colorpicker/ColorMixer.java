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

package io.github.mzmine.gui.colorpicker;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * Box for interactively selecting custom color.
 */
public class ColorMixer extends VBox {

  private final Region colorRectIndicator;
  private final DoubleProperty hue = new SimpleDoubleProperty();
  private final DoubleProperty sat = new SimpleDoubleProperty();
  private final DoubleProperty bright = new SimpleDoubleProperty();
  ObjectProperty<Color> selectedColor = new SimpleObjectProperty<>();

  public ColorMixer() {
    // hue picker
    Pane colorBar = new Pane();
    colorBar.getStyleClass().add("color-bar");
    colorBar.setBackground(new Background(new BackgroundFill(createHueGradient(),
        CornerRadii.EMPTY, Insets.EMPTY)));

    EventHandler<MouseEvent> barMouseHandler = event -> {
      final double x = event.getX();
      hue.set(clamp(x / colorBar.getWidth()) * 360);
    };

    colorBar.setOnMouseDragged(barMouseHandler);
    colorBar.setOnMousePressed(barMouseHandler);

    // hue picker indicator
    Region colorBarIndicator = new Region();
    colorBarIndicator.setId("color-bar-indicator");
    colorBarIndicator.setMouseTransparent(true);
    colorBarIndicator.setCache(true);

    colorBarIndicator.layoutXProperty().bind(
        hue.divide(360).multiply(colorBar.widthProperty()));

    colorBar.getChildren().setAll(colorBarIndicator);

    // saturation-value picker made of multiple overlaid layers
    final Pane customColorPane = new StackPane();
    customColorPane.getStyleClass().add("color-sv-picker");
    VBox.setVgrow(customColorPane, Priority.SOMETIMES);

    // overlay hue - fill solid color based on selected hue value
    Pane colorRectHue = new Pane();
    colorRectHue.backgroundProperty().bind(Bindings.createObjectBinding(() -> {
      return new Background(new BackgroundFill(
          Color.hsb(hue.getValue(), 1.0, 1.0),
          CornerRadii.EMPTY, Insets.EMPTY)
      );
    }, hue));

    // overlay - 1st gradient overlay
    Pane colorRectOverlayOne = new Pane();
    colorRectOverlayOne.getStyleClass().add("color-rect");
    colorRectOverlayOne.setBackground(new Background(new BackgroundFill(
        new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 255, 255, 1)),
            new Stop(1, Color.rgb(255, 255, 255, 0))),
        CornerRadii.EMPTY, Insets.EMPTY)));

    // overlay - 2nd gradient overlay
    Pane colorRectOverlayTwo = new Pane();
    colorRectOverlayTwo.getStyleClass().addAll("color-rect");
    colorRectOverlayTwo.setBackground(new Background(new BackgroundFill(
        new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(0, 0, 0, 0)), new Stop(1, Color.rgb(0, 0, 0, 1))),
        CornerRadii.EMPTY, Insets.EMPTY)));

    // mouse events
    EventHandler<MouseEvent> rectMouseHandler = event -> {
      final double x = event.getX();
      final double y = event.getY();
      sat.set(clamp(x / customColorPane.getWidth()) * 100);
      bright.set(100 - (clamp(y / customColorPane.getHeight()) * 100));
    };
    customColorPane.setOnMouseDragged(rectMouseHandler);
    customColorPane.setOnMousePressed(rectMouseHandler);

    // S-V selection indicator
    colorRectIndicator = new Region();
    colorRectIndicator.setId("color-rect-indicator");
    colorRectIndicator.setManaged(false);
    colorRectIndicator.setMouseTransparent(true);
    colorRectIndicator.setCache(true);

    colorRectIndicator.layoutXProperty().bind(
        sat.divide(100).multiply(customColorPane.widthProperty()));
    colorRectIndicator.layoutYProperty().bind(
        Bindings.subtract(1, bright.divide(100)).multiply(customColorPane.heightProperty()));

    customColorPane.getChildren()
        .setAll(colorRectHue, colorRectOverlayOne, colorRectOverlayTwo, colorRectIndicator);

    // result visualisation
    var resVis = new Region();
    resVis.getStyleClass().add("color-result");
    resVis.backgroundProperty().bind(Bindings.createObjectBinding(() -> {
      return new Background(new BackgroundFill(
          selectedColor.get(),
          CornerRadii.EMPTY, Insets.EMPTY
      ));
    }, selectedColor));

    selectedColor.bind(Bindings.createObjectBinding(() -> {
      return Color.hsb(hue.get(), clamp(sat.get() / 100), clamp(bright.get() / 100));
    }, hue, sat, bright));

    getChildren().addAll(colorBar, customColorPane, resVis);
  }

  private static LinearGradient createHueGradient() {
    Stop[] stops = new Stop[255];
    for (int x = 0; x < 255; x++) {
      double offset = (1.0 / 255) * x;
      int h = (int) ((x / 255.0) * 360);
      stops[x] = new Stop(offset, Color.hsb(h, 1.0, 1.0));
    }
    return new LinearGradient(0f, 0f, 1f, 0f, true, CycleMethod.NO_CYCLE, stops);
  }

  private static double clamp(double value) {
    return value < 0 ? 0 : value > 1 ? 1 : value;
  }

  @Override
  protected void layoutChildren() {
    super.layoutChildren();
    colorRectIndicator.autosize();
  }
}
