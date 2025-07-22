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
import io.github.mzmine.parameters.parametertypes.rangeortolerancetable.Tolerance;
import org.jetbrains.annotations.NotNull;

public class NanometerTolerance implements Tolerance<Double> {

  final double tolerance;

  public NanometerTolerance(double tolerance) {
    this.tolerance = tolerance;
  }

  @Override
  public Range<Double> getToleranceRange(@NotNull final Double value) {
    return getToleranceRange(value.doubleValue());
  }

  public Range<Double> getToleranceRange(double value) {
    return Range.closed(value-tolerance, tolerance + value);
  }

  @Override
  public Double getTolerance() {
    return tolerance;
  }
}
