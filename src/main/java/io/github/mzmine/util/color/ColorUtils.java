/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.util.color;

import io.github.mzmine.util.javafx.FxColorUtil;
import java.util.logging.Logger;
import javafx.scene.paint.Color;

public class ColorUtils {

  private static final Logger logger = Logger.getLogger(ColorUtils.class.getName());

  /**
   * Basic value to use as minimum. The value is arbitrary now and not tested. might change in the
   * future.
   */
  public static double MIN_REDMEAN_COLOR_DIFF = 65;

  /**
   * Returns tinted version of the given color.
   *
   * @param color  input color
   * @param factor tint factor, must be from [0, 1], higher value is, brighter output color is
   * @return new color
   */
  public static Color tintColor(Color color, double factor) {
    return new Color(color.getRed() + (1d - color.getRed()) * factor,
        color.getGreen() + (1d - color.getGreen()) * factor,
        color.getBlue() + (1d - color.getBlue()) * factor, color.getOpacity());
  }

  /**
   * Returns an absolute distance of rgb values between two colors. Can be treated as a metric on
   * colors.
   */
  public static double getColorDistance(Color color1, Color color2) {
    return Math.abs(color1.getRed() - color2.getRed()) + Math.abs(
        color1.getGreen() - color2.getGreen()) + Math.abs(color1.getBlue() - color2.getBlue());
  }

  /**
   * Returns either positive or negative color from the given palette depending on which one is more
   * contrast to the given color.
   */
  public static Color getContrastPaletteColor(Color color, SimpleColorPalette palette) {
    Color positiveColor = palette.getPositiveColor();
    Color negativeColor = palette.getNegativeColor();

    return getColorDistance(color, positiveColor) > getColorDistance(color, negativeColor)
        ? positiveColor : negativeColor;
  }

  public static Color getContrastPaletteColor(java.awt.Color color, SimpleColorPalette palette) {
    return getContrastPaletteColor(FxColorUtil.awtColorToFX(color), palette);
  }

  public static java.awt.Color getContrastPaletteColorAWT(java.awt.Color color,
      SimpleColorPalette palette) {
    return FxColorUtil.fxColorToAWT(
        getContrastPaletteColor(FxColorUtil.awtColorToFX(color), palette));
  }

  public static java.awt.Color getContrastPaletteColorAWT(Color color, SimpleColorPalette palette) {
    return FxColorUtil.fxColorToAWT(getContrastPaletteColor(color, palette));
  }

  /**
   * @return The "red mean" color difference.
   */
  public static double getColorDifference(Color clr1, Color clr2) {

    final double rmean = 255 * 0.5 * (clr1.getRed() + clr2.getRed());
    final double rterm = (2 + rmean / 256) * Math.pow(255 * (clr1.getRed() - clr2.getRed()), 2);
    final double gterm = 4 * Math.pow(255 * (clr1.getGreen() - clr2.getGreen()), 2);
    final double bterm =
        (2 + (255 - rmean) / 256) * +Math.pow(+255 * (clr1.getBlue() - clr2.getBlue()), 2);

    final double sqrt = Math.sqrt(rterm + gterm + bterm);
    logger.finest(() -> "Color difference between %s and %s is %.3f".formatted(clr1, clr2, sqrt));
    return sqrt;
  }

  public static boolean isLight(Color clr) {
    return getColorDifference(clr, Color.WHITE) < 250;
  }

  public static boolean isDark(Color clr) {
    return getColorDifference(clr, Color.BLACK) < 250;
  }
}
