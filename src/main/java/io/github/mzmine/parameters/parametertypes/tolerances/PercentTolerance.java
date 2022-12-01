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

package io.github.mzmine.parameters.parametertypes.tolerances;

import com.google.common.collect.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class to determine if two values lie within a specified tolerance region.
 *
 * @author https://github.com/steffenheu
 */
public class PercentTolerance {

  private final double tolerance;

  /**
   * @param percentTolerance The tolerance in percent. 0.1 = 10%, 1.5 = 150 %
   */
  public PercentTolerance(final double percentTolerance) {
    tolerance = percentTolerance;
  }

  /**
   * @return The error in per cent. 10 % = 0.1
   */
  public static double getPercentError(int base, int value) {
    return Math.abs(value - base) / (double) base;
  }

  /**
   * @return The error in per cent. 10 % = 0.1
   */
  public static double getPercentError(long base, long value) {
    return Math.abs(value - base) / (double) base;
  }

  /**
   * @return The error in per cent. 10 % = 0.1
   */
  public static float getPercentError(float base, float value) {
    return Math.abs(value - base) / base;
  }

  /**
   * @return The error in per cent. 10 % = 0.1
   */
  public static double getPercentError(double base, double value) {
    return Math.abs(value - base) / base;
  }

  /**
   * @return The error in per cent. 10 % = 0.1
   */
  public static Float getPercentError(@Nullable Float base, @Nullable Float value) {
    if (base == null || value == null) {
      return null;
    }
    return Math.abs(value - base) / base;
  }

  /**
   * @return The error in per cent. 10 % = 0.1
   */
  public static Double getPercentError(@Nullable Double base, @Nullable Double value) {
    if (base == null || value == null) {
      return null;
    }
    return Math.abs(value - base) / base;
  }

  /**
   * @return The error in per cent. 10 % = 0.1
   */
  public static Double getPercentError(@Nullable Integer base, @Nullable Integer value) {
    if (base == null || value == null) {
      return null;
    }
    return Math.abs(value - base) / (double) base;
  }

  public boolean matches(int base, int value) {
    return Double.compare(getPercentError(base, value), tolerance) <= 0;
  }

  public boolean matches(long base, long value) {
    return Double.compare(getPercentError(base, value), tolerance) <= 0;
  }

  public boolean matches(float base, float value) {
    return Double.compare(getPercentError(base, value), tolerance) <= 0;
  }

  public boolean matches(double base, double value) {
    return Double.compare(getPercentError(base, value), tolerance) <= 0;
  }

  /**
   * @param base  The base value of this range ("what it is supposed to be")
   * @param value The value to be checked.
   * @return true or false. false if any of the input type is null
   */
  public <T extends Number, V extends Number> boolean matches(@Nullable T base, @Nullable V value) {
    if (base == null || value == null) {
      return false;
    }
    return Double.compare(getPercentError(base.doubleValue(), value.doubleValue()), tolerance) <= 0;
  }

  public <T extends Number> Range<Double> getToleranceRange(@NotNull T value) {
    final double tol = value.doubleValue() * tolerance;
    return Range.closed(value.doubleValue() - tol, value.doubleValue() + tol);
  }

  public double getTolerance() {
    return tolerance;
  }

}
