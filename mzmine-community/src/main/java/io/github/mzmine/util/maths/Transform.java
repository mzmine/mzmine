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

package io.github.mzmine.util.maths;

/**
 * Different math transformations
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
@FunctionalInterface
public interface Transform {

  public static final Transform LOG = Math::log;
  public static final Transform SQRT = Math::sqrt;

  public double transform(double v);

  /**
   * transforms the input data and keeps 0 values as 0 (some transforms like log are undefined at
   * 0). Another external option would be to replace the 0 values by noise or the minimum value.
   *
   * @param v to transform
   */
  default double transformKeep0(double v) {
    return Double.compare(v, 0d) == 0 ? v : transform(v);
  }

  /**
   * transforms the input data and keeps 0 values as 0 (some transforms like log are undefined at
   * 0). Another external option would be to replace the 0 values by noise or the minimum value.
   *
   * @param values to transform
   */
  default void transformKeep0(double[] values) {
    for (int i = 0; i < values.length; i++) {
      double v = values[i];
      values[i] = transformKeep0(v);
    }
  }
}



