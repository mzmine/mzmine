/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util;

import java.awt.Dimension;
import org.jfree.chart.ui.Size2D;
import com.itextpdf.text.Utilities;

public class DimensionUnitUtil {
  // dimensions
  public static enum DimUnit {
    CM, MM, PT, INCH, PX;
  }

  /**
   * Converts a pixel size to the given unit
   * 
   * @param val
   * @param unit
   * @return Float.NaN if the no conversion was defined for this unit
   */
  public static float getSizeInUnit(float value, DimUnit unit) {
    switch (unit) {
      case CM:
        return Utilities.pointsToMillimeters(value / 10.f);
      case MM:
        return Utilities.pointsToMillimeters(value);
      case INCH:
        return Utilities.pointsToInches(value);
      case PX:
      case PT:
        return value;
    }
    return Float.NaN;
  }

  /**
   * Converts pixel size to the given unit
   * 
   * @return Float.NaN if the no conversion was defined for this unit
   */
  public static float[] getSizeInUnit(float[] value, DimUnit unit) {
    float[] result = new float[value.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = getSizeInUnit(value[i], unit);
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
    switch (unit) {
      case CM:
        return Utilities.millimetersToPoints(value * 10.f);
      case MM:
        return Utilities.millimetersToPoints(value);
      case INCH:
        return Utilities.inchesToPoints(value);
      case PX:
      case PT:
        return value;
    }
    return Float.NaN;
  }

  /**
   * Converts any pixel value to a specified unit (same as getSizeInUnit(val, unit))
   * 
   * @param val
   * @param unit
   * @return Float.NaN if the no conversion was defined for this unit
   */
  public static float pixelToUnit(float value, DimUnit unit) {
    return getSizeInUnit(value, unit);
  }

  /**
   * Converts any pixel value to a specified unit (same as getSizeInUnit(val, unit))
   * 
   * @param val
   * @param unit
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
    if (Float.isNaN(p))
      return p;
    else
      return pixelToUnit(p, resultUnit);
  }
}
