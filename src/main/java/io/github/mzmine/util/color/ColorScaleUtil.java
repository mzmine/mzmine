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

import java.awt.Color;

public class ColorScaleUtil {

  /**
   * Get color of gradient between min and max color
   * 
   * @param min
   * @param max
   * @param minValue
   * @param maxValue
   * @param value
   * @return
   */
  public static Color getColor(Color min, Color max, double minValue, double maxValue,
      double value) {
    // hue saturation brightness
    float[] minHSB = Color.RGBtoHSB(min.getRed(), min.getGreen(), min.getBlue(), null);
    float[] maxHSB = Color.RGBtoHSB(max.getRed(), max.getGreen(), max.getBlue(), null);

    double diff = maxValue - minValue;
    double p = (Math.max(value, minValue) - minValue) / diff;
    if (p > 1)
      p = 1;

    // gradient
    float h = (float) ((maxHSB[0] - minHSB[0]) * p + minHSB[0]);
    float s = (float) ((maxHSB[1] - minHSB[1]) * p + minHSB[1]);
    float b = (float) ((maxHSB[2] - minHSB[2]) * p + minHSB[2]);
    return Color.getHSBColor(h, s, b);
  }
}
