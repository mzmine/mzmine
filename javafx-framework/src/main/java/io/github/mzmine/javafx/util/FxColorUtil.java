/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.javafx.util;

import javafx.scene.paint.Color;

public class FxColorUtil {

  public static String colorToHex(final Color color) {
    return String.format("#%02X%02X%02X", (int) (color.getRed() * 255),
        (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
  }


  public static java.awt.Color fxColorToAWT(final Color color) {
    final int r = (int) (color.getRed() * 255d);
    final int g = (int) (color.getGreen() * 255d);
    final int b = (int) (color.getBlue() * 255d);
    final int opacity = (int) (color.getOpacity() * 255d);
    return new java.awt.Color(r, g, b, opacity);
  }

  public static Color awtColorToFX(final java.awt.Paint paint) {
    if (paint instanceof java.awt.Color color) {
      final double r = color.getRed() / 255d;
      final double g = color.getGreen() / 255d;
      final double b = color.getBlue() / 255d;
      final double opacity = color.getAlpha() / 255d;
      return Color.color(r, g, b, opacity);
    } else if (paint instanceof java.awt.GradientPaint c) {
      return awtColorToFX(c.getColor1());
    }
    throw new IllegalArgumentException("Unsupported paint type: " + paint.getClass().getName());
  }

}
