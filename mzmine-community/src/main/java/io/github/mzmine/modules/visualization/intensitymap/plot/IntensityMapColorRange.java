/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.intensitymap.plot;

import io.github.mzmine.javafx.components.factories.FxLabels;
import java.util.function.DoubleFunction;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.NotNull;

/**
 * Color bar of one overlay with two handles that clip its color range, like the intensity sliders
 * of imaging software (user request). Only the colors change, the data are not resampled.
 */
final class IntensityMapColorRange extends Region {

  private static final double BAR_HEIGHT = 8;
  private static final double HANDLE_WIDTH = 5;
  private static final double LABEL_GAP = 3;
  // assumption: a range below one percent of the scale is not useful and hard to grab
  private static final double MIN_SPAN = 0.01;

  private final IntensityMapSeriesState state;
  private final DoubleFunction<String> format;
  private final ImageView bar = new ImageView();
  private final Rectangle belowRange = new Rectangle();
  private final Rectangle aboveRange = new Rectangle();
  private final Rectangle lowHandle = handle();
  private final Rectangle highHandle = handle();
  private final Label lowLabel = FxLabels.newSmallLabel("");
  private final Label highLabel = FxLabels.newSmallLabel("");
  private boolean draggingLow;

  /**
   * @param format formats intensities for the range labels
   */
  IntensityMapColorRange(@NotNull final IntensityMapSeriesState state,
      @NotNull final DoubleFunction<String> format) {
    this.state = state;
    this.format = format;
    bar.setPreserveRatio(false);
    bar.setSmooth(true);
    bar.imageProperty().bind(state.colorBarProperty());
    for (final Rectangle shade : new Rectangle[]{belowRange, aboveRange}) {
      shade.setFill(Color.rgb(128, 128, 128, 0.55));
      shade.setMouseTransparent(true);
    }
    lowLabel.setOpacity(0.75);
    highLabel.setOpacity(0.75);
    getChildren().addAll(bar, belowRange, aboveRange, lowHandle, highHandle, lowLabel, highLabel);
    Tooltip.install(this,
        new Tooltip("Drag the handles to clip the color range, double-click to reset"));

    state.colorLowProperty().addListener((_, _, _) -> update());
    state.colorHighProperty().addListener((_, _, _) -> update());
    state.colorMaximumProperty().addListener((_, _, _) -> update());
    setOnMousePressed(this::press);
    setOnMouseDragged(this::drag);
    // clicks must not reach the overlay row, which toggles visibility on click
    setOnMouseClicked(event -> {
      if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
        state.colorLowProperty().set(0);
        state.colorHighProperty().set(1);
      }
      event.consume();
    });
    update();
  }

  private static @NotNull Rectangle handle() {
    final Rectangle handle = new Rectangle(HANDLE_WIDTH, BAR_HEIGHT + 6);
    handle.setArcWidth(2);
    handle.setArcHeight(2);
    handle.setFill(Color.WHITE);
    handle.setStroke(Color.rgb(51, 65, 85));
    handle.setStrokeWidth(1);
    handle.setMouseTransparent(true);
    return handle;
  }

  private void press(@NotNull final MouseEvent event) {
    if (event.getButton() != MouseButton.PRIMARY) {
      return;
    }
    // decision: the closer handle follows the mouse, so a click also moves a handle there
    final double position = position(event.getX());
    draggingLow = Math.abs(position - state.colorLowProperty().get()) <= Math.abs(
        position - state.colorHighProperty().get());
    move(position);
    event.consume();
  }

  private void drag(@NotNull final MouseEvent event) {
    if (event.isPrimaryButtonDown()) {
      move(position(event.getX()));
    }
    event.consume();
  }

  private void move(final double position) {
    if (draggingLow) {
      state.colorLowProperty()
          .set(Math.clamp(position, 0, state.colorHighProperty().get() - MIN_SPAN));
    } else {
      state.colorHighProperty()
          .set(Math.clamp(position, state.colorLowProperty().get() + MIN_SPAN, 1));
    }
  }

  /**
   * @return color scale position of a local x coordinate
   */
  private double position(final double x) {
    final double width = barWidth();
    return width > 0 ? Math.clamp((x - HANDLE_WIDTH / 2) / width, 0, 1) : 0;
  }

  private double barWidth() {
    return Math.max(0, getWidth() - HANDLE_WIDTH);
  }

  private void update() {
    final double maximum = state.colorMaximumProperty().get();
    lowLabel.setText(maximum > 0 ? text(state.colorLowProperty().get() * maximum) : "");
    highLabel.setText(maximum > 0 ? text(state.colorHighProperty().get() * maximum) : "");
    requestLayout();
  }

  private @NotNull String text(final double intensity) {
    // scientific formats write zero as 0E0
    return intensity == 0 ? "0" : format.apply(intensity);
  }

  @Override
  protected void layoutChildren() {
    final double left = HANDLE_WIDTH / 2;
    final double width = barWidth();
    final double low = left + state.colorLowProperty().get() * width;
    final double high = left + state.colorHighProperty().get() * width;
    final double top = 3;
    bar.relocate(left, top);
    bar.setFitWidth(width);
    bar.setFitHeight(BAR_HEIGHT);
    belowRange.relocate(left, top);
    belowRange.setWidth(Math.max(0, low - left));
    belowRange.setHeight(BAR_HEIGHT);
    aboveRange.relocate(high, top);
    aboveRange.setWidth(Math.max(0, left + width - high));
    aboveRange.setHeight(BAR_HEIGHT);
    lowHandle.relocate(low - HANDLE_WIDTH / 2, 0);
    highHandle.relocate(high - HANDLE_WIDTH / 2, 0);
    final double labelTop = BAR_HEIGHT + 6 + LABEL_GAP;
    lowLabel.autosize();
    highLabel.autosize();
    lowLabel.relocate(0, labelTop);
    highLabel.relocate(Math.max(lowLabel.getWidth() + 6, getWidth() - highLabel.getWidth()),
        labelTop);
  }

  @Override
  protected double computePrefHeight(final double width) {
    return BAR_HEIGHT + 6 + LABEL_GAP + lowLabel.prefHeight(-1);
  }

  @Override
  protected double computeMinWidth(final double height) {
    return 60;
  }

  @Override
  protected double computePrefWidth(final double height) {
    return 160;
  }
}
