/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import static io.github.mzmine.util.MathUtils.within;

import io.github.mzmine.javafx.util.FxColorUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.scene.paint.Color;

public class ColorUtils {

  // make sure brightness works for dark and light mode
  public static final double minBrightness = 0.33;
  public static final double maxBrightness = 0.75;
  public static final double minSaturation = 0.35;
  private static final Logger logger = Logger.getLogger(ColorUtils.class.getName());

  /**
   * Basic value to use as minimum. The value is arbitrary now and not tested. might change in the
   * future.
   */
  public static double MIN_REDMEAN_COLOR_DIFF = 65;

  public static final java.awt.Color TRANSPARENT_AWT = new java.awt.Color(0, 0, 0, 0);
  public static final Color TRANSPARENT_FX = new Color(0, 0, 0, 0);

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
//    logger.finest(() -> "Color difference between %s and %s is %.3f".formatted(clr1, clr2, sqrt));
    return sqrt;
  }

  public static boolean isLight(Color clr) {
    return getColorDifference(clr, Color.WHITE) < 250;
  }

  public static boolean isDark(Color clr) {
    return getColorDifference(clr, Color.BLACK) < 250;
  }

  public static double maxBrightnessWidth() {
    return maxBrightness - minBrightness;
  }

  public static double centerBrightness() {
    return (maxBrightness + minBrightness) / 2d;
  }

  /**
   * Color around the base color. Brightness and saturation are scaled
   *
   * @param bRange brightness width
   */
  public static List<Color> colorFadeLighter(final int steps, final Color base,
      final double bRange) {
    if (steps == 0) {
      return List.of();
    }
    if (steps == 1) {
      return List.of(base);
    }

    if (base.getSaturation() == 0) {
      return colorFadeGray(steps, bRange);
    }

    List<Color> resultColors = new ArrayList<>(steps);

    double h = base.getHue();

    // start saturation --> 1  - tend to higher saturations rather than lower
    double sRange = bRange * 1.5;
    double maxS = within(base.getSaturation() + 0.1 + sRange, minSaturation, 1);
    double minS = maxS - sRange;
    if (minS < minSaturation) {
      minS = minSaturation;
      maxS = within(minS + sRange, minSaturation, 1);
    }

    // start in center brightness to allow both light and dark mode
    // prefer lighter colors
    double maxB = within(base.getBrightness() + bRange * 0.6, minBrightness, maxBrightness);
    double minB = maxB - bRange;
    if (minB < minBrightness) {
      minB = minBrightness;
      maxB = within(minB + bRange, minBrightness, maxBrightness);
    }

    // option to start always in the center - but this makes colors quite dull and they only depend on hue
//    double minB = within(centerBrightness() - bRange / 2d, minBrightness, maxBrightness);
//    double maxB = within(minB + bRange, minBrightness, maxBrightness);

    // first half only increases brightness with max saturation
    double halfN = Math.max(Math.floor(steps / 2d), 1);
    double stepB = (maxB - minB) / (halfN);
    double b = minB;
    int step = 0;
    for (; step < halfN; step++) {
      resultColors.add(Color.hsb(h, maxS, b));
      b = within(b + stepB, minB, maxB);
    }

    // reduce saturation
    double stepS = (maxS - minS) / Math.max(steps - halfN - 1, 1);
    double s = maxS;
    for (; step < steps; step++) {
      resultColors.add(Color.hsb(h, s, maxB));
      s = within(s - stepS, minS, maxS); // need to modify after adding the last brightness step
    }
    return resultColors;
  }

  /**
   * Color around 50% gray +- bRange/2
   *
   * @param bRange brightness width
   */
  public static List<Color> colorFadeGray(final int steps, double bRange) {
    if (steps == 0) {
      return List.of();
    }
    if (steps == 1) {
      return List.of(Color.hsb(0, 0, centerBrightness()));
    }
    int n = Math.max(steps - 1, 1);
    bRange = Math.min(bRange, maxBrightnessWidth());
    double maxB = within(centerBrightness() + bRange / 2d, minBrightness, maxBrightness);

    List<Color> resultColors = new ArrayList<>(steps);
    for (int step = 0; step < steps; step++) {
      resultColors.add(Color.hsb(0, 0, maxB - bRange * (step / (double) n)));
    }
    return resultColors;
  }

  public static boolean isTransparent(Color clr) {
    return clr.getOpacity() < 0.4;
  }

  public static Color invert(Color clr) {
    return new Color(1 - clr.getRed(), 1 - clr.getGreen(), 1 - clr.getBlue(), clr.getOpacity());
  }

  public static java.awt.Color invert(java.awt.Color clr) {
    return new java.awt.Color(255 - clr.getRed(), 255 - clr.getGreen(), 255 - clr.getBlue(),
        clr.getAlpha());
  }
}
