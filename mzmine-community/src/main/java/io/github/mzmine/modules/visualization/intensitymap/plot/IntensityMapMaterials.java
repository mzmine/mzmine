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

import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Materials of the overlays. Textures are cached, so slider and color changes do not upload them to
 * the GPU again.
 */
final class IntensityMapMaterials {

  private final Map<SimpleColorPalette, WritableImage> textures = new HashMap<>();
  // intensity gradients of single overlay colors from the plot background, by color
  private final Map<Color, WritableImage> gradients = new HashMap<>();

  @NotNull WritableImage texture(@NotNull final SimpleColorPalette palette) {
    return textures.computeIfAbsent(palette, IntensityMapColors::texture);
  }

  /**
   * Gradients start at the plot background, so they are recreated when it changes.
   */
  void clearGradients() {
    gradients.clear();
  }

  /**
   * @param palette    paint scale if colored by intensity, null for overlay colors
   * @param gradient   overlay colors shade from the background by intensity instead of a flat
   *                   color
   * @param background plot background, where gradients start
   * @param opacity    opacity of the overlay
   */
  void apply(@NotNull final IntensityMapSeriesState state,
      @Nullable final SimpleColorPalette palette, final boolean gradient,
      @NotNull final Color background, final double opacity) {
    final Color overlayColor = state.colorProperty().get();
    final Color color = palette != null || gradient ? Color.WHITE : overlayColor;
    final PhongMaterial material = state.material();
    material.setDiffuseColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity));
    final WritableImage colors;
    if (palette != null) {
      colors = texture(palette);
    } else if (gradient) {
      // decision (user request): low intensities fade into the plot background; a transparent
      // base looked washed out and showed depth sorting errors
      colors = gradients.computeIfAbsent(overlayColor,
          c -> IntensityMapColors.gradient(c, background));
    } else {
      colors = null;
    }
    state.colorBarProperty().set(colors);
    material.setDiffuseMap(colors == null ? null : state.clippedTexture(colors));
    material.setSpecularColor(Color.color(0.85, 0.9, 1, opacity));
    material.setSpecularPower(64);
  }
}
