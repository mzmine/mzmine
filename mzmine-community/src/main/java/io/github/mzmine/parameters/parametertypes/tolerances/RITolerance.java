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
import io.github.mzmine.util.RIColumn;
import io.github.mzmine.util.RIRecord;

/**
 * RITolerance allows specifying retention index tolerance for comparing two compounds It is an
 * absolute unitless tolerance based on a specific RIColumn, which specifies the stationary phase
 * within the column
 */
public class RITolerance {

  private final float tolerance;
  private final RIColumn column;

  public RITolerance(final float rtTolerance, RIColumn type) {
    this.tolerance = rtTolerance;
    this.column = type;
  }

  public float getTolerance() {
    return tolerance;
  }

  public RIColumn getColumn() {
    return column;
  }

  public Range<Float> getToleranceRange(final Float riValue) {
    // riValue may not exist depending on alkane scales
    //   Also, averaged RI is zero when riValues do not exist
    return riValue != null && riValue != 0 ? Range.closed(riValue - tolerance, riValue + tolerance)
        : Range.all();
  }

  public boolean checkWithinTolerance(Float ri, RIRecord libRI) {
    return libRI == null || getToleranceRange(libRI.getRI(column)).contains(ri);
  }

  public boolean checkWithinTolerance(final float ri1, final float ri2) {
    return getToleranceRange(ri1).contains(ri2);
  }

  public RIColumn getRIType() {
    return column;
  }

  @Override
  public String toString() {
    return tolerance + ", " + column.toString();
  }

}