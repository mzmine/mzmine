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

package io.github.mzmine.util.color;

import io.github.mzmine.util.javafx.FxColorUtil;
import javafx.scene.paint.Color;

public class ColorUtils {

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
    return Math.abs(color1.getRed() - color2.getRed()) + Math
        .abs(color1.getGreen() - color2.getGreen()) + Math.abs(color1.getBlue() - color2.getBlue());
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

  public static java.awt.Color getContrastPaletteColorAWT(java.awt.Color color, SimpleColorPalette palette) {
    return FxColorUtil
        .fxColorToAWT(getContrastPaletteColor(FxColorUtil.awtColorToFX(color), palette));
  }

  public static java.awt.Color getContrastPaletteColorAWT(Color color, SimpleColorPalette palette) {
    return FxColorUtil
        .fxColorToAWT(getContrastPaletteColor(color, palette));
  }

}
