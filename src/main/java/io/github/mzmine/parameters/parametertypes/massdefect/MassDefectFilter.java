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

package io.github.mzmine.parameters.parametertypes.massdefect;

import io.github.mzmine.main.MZmineCore;

public class MassDefectFilter {

  public static final MassDefectFilter ALL = new MassDefectFilter(0, 1) {
    @Override
    public boolean contains(double mz) {
      return true;
    }
  };

  private final double lower;
  private final double upper;

  public MassDefectFilter(double lower, double upper) {

    if (lower < 0 || lower > 1) {
      throw new IllegalArgumentException("Lower mass defect range must be between 0 and 1.");
    }
    if (upper < 0 || upper > 1) {
      throw new IllegalArgumentException("Upper mass defect range must be between 0 and 1.");
    }

    this.lower = lower;
    this.upper = upper;
  }

  public boolean contains(final double mz) {
    final double massDefect = mz - Math.floor(mz);
    if (lower > upper) { // 0.90 - 0.15
      final boolean massDefectOk =
          (massDefect >= lower && massDefect <= 1.0d) || (massDefect >= 0.0d
              && massDefect <= upper);
      if(!massDefectOk) {
        return false;
      }
    } else { // 0.4 - 0.8
      final boolean massDefectOk = lower <= massDefect && massDefect <= upper;
      if(!massDefectOk) {
        return false;
      }
    }
    return true;
  }

  public double getLowerEndpoint() {
    return lower;
  }

  public double getUpperEndpoint() {
    return upper;
  }

  @Override
  public String toString() {
    var format = MZmineCore.getConfiguration().getGuiFormats();
    return format.mz(lower) + ".." + format.mz(upper);
  }
}
