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

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.util.color.SimpleColorPalette;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

/**
 * Color textures of the plot. Meshes store the linear color position of every value as texture
 * coordinate, so the textures alone define how intensities are colored.
 */
final class IntensityMapColors {

  static final int TEXTURE_SIZE = 1024;

  private IntensityMapColors() {
  }

  static boolean isDark(@NotNull final Color color) {
    return luminance(color) < 0.5;
  }

  static double luminance(@NotNull final Color color) {
    return 0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue();
  }

  static @NotNull WritableImage texture(@NotNull final SimpleColorPalette palette) {
    final var paint = palette.toPaintScale(PaintScaleTransform.LINEAR, Range.closed(0d, 1d));
    final WritableImage image = new WritableImage(TEXTURE_SIZE, 2);
    final PixelWriter writer = image.getPixelWriter();
    for (int i = 0; i < TEXTURE_SIZE; i++) {
      final Color fx = FxColorUtil.awtColorToFX(paint.getPaint(i / (TEXTURE_SIZE - 1d)));
      writer.setColor(i, 0, fx);
      writer.setColor(i, 1, fx);
    }
    return image;
  }

  /**
   * @return gradient from the background to the overlay color, like the single color heatmaps of
   * mzmine (e.g. overlaid ion mobility traces), which fade into their black background
   */
  static @NotNull WritableImage gradient(@NotNull final Color color,
      @NotNull final Color background) {
    // decision: colors too close to the background, e.g. yellow on white, end on a shade further
    // from it, so all gradients keep low intensities at the background
    final double contrast = Math.abs(luminance(color) - luminance(background));
    final Color end = contrast < 0.3 ? color.interpolate(isDark(background) ? Color.WHITE
        : Color.BLACK, 0.5 - contrast) : color;
    return texture(new SimpleColorPalette(background, end));
  }

  /**
   * @return position in the unclipped color scale that colors a position of the clipped scale
   */
  static double clipped(final double position, final double low, final double high) {
    return Math.clamp((position - low) / Math.max(1e-6, high - low), 0, 1);
  }

  /**
   * Clips the colors to a range like the intensity sliders of imaging software: positions below
   * the low end get the first color, above the high end the last color, and the full color scale
   * spans the range in between.
   *
   * @param low  lowest color position in [0, 1]
   * @param high highest color position in [0, 1], above low
   */
  static @NotNull Image clip(@NotNull final Image texture, final double low, final double high) {
    if (low <= 0 && high >= 1) {
      return texture;
    }
    final int width = (int) texture.getWidth();
    final PixelReader reader = texture.getPixelReader();
    final WritableImage image = new WritableImage(width, 2);
    final PixelWriter writer = image.getPixelWriter();
    for (int i = 0; i < width; i++) {
      final double position = clipped(i / (width - 1d), low, high);
      final int source = (int) Math.round(position * (width - 1));
      final Color color = reader.getColor(source, 0);
      writer.setColor(i, 0, color);
      writer.setColor(i, 1, color);
    }
    return image;
  }
}
