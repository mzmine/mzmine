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

package io.github.mzmine.util;

import java.awt.Dimension;
import org.jfree.chart.ui.Size2D;

public class DimensionUnitUtil {

  public static final float MM_PER_PIXEL = 0.352778f;
  public static final float PIXEL_PER_INCH = 72f;

  /**
   * Converts a pixel size to the given unit
   *
   * @param unit
   * @return Float.NaN if the no conversion was defined for this unit
   */
  public static float getSizeInUnit(float pixel, DimUnit unit) {
    return switch (unit) { // values derived from unitconverters.net
      case CM -> getSizeInUnit(pixel, DimUnit.MM) / 10;
      case MM -> pixel * MM_PER_PIXEL;
      case INCH -> pixel / PIXEL_PER_INCH;
      case PX, PT -> pixel;
    };
  }

  /**
   * Converts pixel size to the given unit
   *
   * @return Float.NaN if the no conversion was defined for this unit
   */
  public static float[] getSizeInUnit(float[] pixel, DimUnit unit) {
    float[] result = new float[pixel.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = getSizeInUnit(pixel[i], unit);
    }
    return result;
  }

  /**
   * Converts a pixel size to the given unit
   *
   * @return Float.NaN if the no conversion was defined for this unit
   */
  public static Size2D getSizeInUnit(Size2D size, DimUnit unit) {
    return new Size2D(getSizeInUnit((float) size.getWidth(), unit),
        getSizeInUnit((float) size.getWidth(), unit));
  }

  /**
   * Converts a pixel size to the given unit
   *
   * @return Float.NaN if the no conversion was defined for this unit
   */
  public static Size2D getSizeInUnit(Dimension size, DimUnit unit) {
    return new Size2D(getSizeInUnit((float) size.getWidth(), unit),
        getSizeInUnit((float) size.getWidth(), unit));
  }

  /**
   * Converts any value+unit to pixel
   *
   * @param value
   * @param unit
   * @return Float.NaN if the no conversion was defined for this unit
   */
  public static float toPixel(float value, DimUnit unit) {
    // convert to pt
    return switch (unit) { // values derived from unitconverters.net
      case CM -> toPixel(value * 10, DimUnit.MM);
      case MM -> value / MM_PER_PIXEL;
      case INCH -> value * PIXEL_PER_INCH;
      case PX, PT -> value;
    };
  }

  /**
   * Converts any pixel value to a specified unit (same as getSizeInUnit(val, unit))
   *
   * @return Float.NaN if the no conversion was defined for this unit
   */
  public static float pixelToUnit(float value, DimUnit unit) {
    return getSizeInUnit(value, unit);
  }

  /**
   * Converts any pixel value to a specified unit (same as getSizeInUnit(val, unit))
   *
   * @return Float.NaN if the no conversion was defined for this unit
   */
  public static double pixelToUnit(double value, DimUnit unit) {
    return getSizeInUnit((float) value, unit);
  }

  /**
   * Converts a value from one to another unit
   *
   * @param val
   * @param startUnit
   * @param resultUnit
   * @return Float.NaN if the no conversion was defined for this unit
   */
  public static float changeUnit(float val, DimUnit startUnit, DimUnit resultUnit) {
    float p = toPixel(val, startUnit);
    if (Float.isNaN(p)) {
      return p;
    } else {
      return pixelToUnit(p, resultUnit);
    }
  }

  // dimensions
  public static enum DimUnit {
    CM, MM, PT, INCH, PX;
  }


}
