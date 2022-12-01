/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
