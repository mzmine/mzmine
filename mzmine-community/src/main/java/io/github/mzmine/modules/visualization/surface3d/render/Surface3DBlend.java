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

package io.github.mzmine.modules.visualization.surface3d.render;

import java.util.List;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

/**
 * Color mixing of up to three overlays in their own overlay colors, so that every sample keeps one
 * identifying color. Colors are mixed additively by intensity ratio and shown
 * at full brightness, so that the mix reflects the ratio and not the absolute intensity, which the
 * height already shows.
 */
public final class Surface3DBlend {

  /**
   * Blending is unambiguous for at most three channels, like RGB overlays of imaging software
   */
  public static final int MAX_CHANNELS = 3;
  // equal parts appear light gray instead of white, keeping contrast on light backgrounds
  private static final double BRIGHTNESS = 0.9;

  private Surface3DBlend() {
  }

  /**
   * @return suggested overlay colors that mix to distinct colors: magenta and green
   * (colorblind-safe, equal parts light gray), or red, green, and blue
   */
  public static @NotNull List<Color> mixingColors(final int count) {
    return switch (Math.min(count, Surface3DBlend.MAX_CHANNELS)) {
      case 0, 1 -> List.of(Color.rgb(230, 130, 0));
      case 2 -> List.of(Color.MAGENTA, Color.LIME);
      default -> List.of(Color.RED, Color.LIME, Color.BLUE);
    };
  }

  /**
   * @return square texture: u is the fraction of channel 1, v of channel 2, the remainder
   * channel 3. Texels beyond u + v = 1 repeat the nearest valid mix for bilinear filtering.
   */
  public static @NotNull WritableImage texture(@NotNull final List<Color> channels) {
    final int size = Surface3DMesh.MIX_SIZE;
    final WritableImage image = new WritableImage(size, size);
    final Color first = channels.getFirst();
    final Color second = channels.size() > 1 ? channels.get(1) : first;
    final Color third = channels.size() > 2 ? channels.get(2) : Color.BLACK;
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        double a = i / (double) (size - 1);
        double b = j / (double) (size - 1);
        if (a + b > 1) {
          final double sum = a + b;
          a /= sum;
          b /= sum;
        }
        final double c = Math.max(0, 1 - a - b);
        image.getPixelWriter().setColor(i, j, mix(first, second, third, a, b, c));
      }
    }
    return image;
  }

  public static @NotNull Color mix(@NotNull final Color first, @NotNull final Color second,
      @NotNull final Color third, final double a, final double b, final double c) {
    final double red = a * first.getRed() + b * second.getRed() + c * third.getRed();
    final double green = a * first.getGreen() + b * second.getGreen() + c * third.getGreen();
    final double blue = a * first.getBlue() + b * second.getBlue() + c * third.getBlue();
    final double max = Math.max(red, Math.max(green, blue));
    if (!(max > 0)) {
      return Color.gray(BRIGHTNESS);
    }
    return Color.color(Math.min(1, red / max * BRIGHTNESS), Math.min(1, green / max * BRIGHTNESS),
        Math.min(1, blue / max * BRIGHTNESS));
  }
}
