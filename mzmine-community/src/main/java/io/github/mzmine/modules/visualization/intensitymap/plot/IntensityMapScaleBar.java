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

import io.github.mzmine.javafx.components.util.FxLayout;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.NotNull;

/**
 * Map style scale bar in the lower right corner of an image: a bar of a round length with the
 * length written above it. Only exact where one screen distance stands for the same physical
 * distance across the whole image, as in the 2D view.
 */
final class IntensityMapScaleBar {

  // decision: about a fifth of the visible image width, like the scale of a map
  private static final int PARTS = 5;
  // smaller images show no bar, it would cover a large part of them
  private static final double MIN_IMAGE_PIXELS = 100;
  // screen distance to the image border
  private static final double INSET = 8;
  private static final double BAR_HEIGHT = 4;

  private final Rectangle bar = new Rectangle(0, BAR_HEIGHT);
  private final Label text = new Label();
  private final VBox box = FxLayout.newVBox(Pos.CENTER, new Insets(2, 6, 4, 6), text, bar);

  IntensityMapScaleBar() {
    box.setSpacing(2);
    box.setManaged(false);
    box.setMouseTransparent(true);
    box.setVisible(false);
  }

  @NotNull Node node() {
    return box;
  }

  /**
   * @param textColor  css color of the bar and its text
   * @param background plot background, the box behind the bar is a translucent version of it
   */
  void setColors(@NotNull final String textColor, @NotNull final Color background) {
    bar.setFill(Color.web(textColor));
    // no padding: the box already pads the text
    text.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 11; -fx-padding: 0;");
    box.setBackground(new Background(
        new BackgroundFill(background.deriveColor(0, 1, 1, 0.8), new CornerRadii(3),
            Insets.EMPTY)));
  }

  void hide() {
    box.setVisible(false);
  }

  /**
   * @param image  screen bounds of the visible part of the image, in the pane of the bar
   * @param pixels screen length of the visible image width
   * @param length physical length of the visible image width in µm
   */
  void place(@NotNull final Bounds image, final double pixels, final double length) {
    if (!(length > 0) || pixels < MIN_IMAGE_PIXELS) {
      hide();
      return;
    }
    final double step = IntensityMapTicks.step(0, length, PARTS);
    bar.setWidth(step * pixels / length);
    text.setText(format(step));
    box.setVisible(true);
    box.applyCss();
    box.autosize();
    box.relocate(image.getMaxX() - INSET - box.getWidth(),
        image.getMaxY() - INSET - box.getHeight());
  }

  /**
   * @param micrometers a round length
   * @return the length in µm, or in mm from 1 mm on
   */
  static @NotNull String format(final double micrometers) {
    // decision: whole sections often span several mm, 5 mm reads better than 5000 µm
    if (micrometers >= 1000) {
      return IntensityMapTicks.format(micrometers / 1000, micrometers / 1000) + " mm";
    }
    return IntensityMapTicks.format(micrometers, micrometers) + " µm";
  }
}
