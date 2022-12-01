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

package io.github.mzmine.modules.tools.msmsspectramerge;

import io.github.mzmine.datamodel.DataPoint;

public interface IntensityMergeMode {

  static IntensityMergeMode[] values() {
    return new IntensityMergeMode[] {SUM, MAXIMUM, AVERAGE};
  }

  public double merge(DataPoint[] sources);

  public static final IntensityMergeMode MAXIMUM = new IntensityMergeMode() {
    @Override
    public double merge(DataPoint[] sources) {
      double max = 0d;
      for (DataPoint p : sources)
        max = Math.max(p.getIntensity(), max);
      return max;
    }

    @Override
    public String toString() {
      return "maximum intensity";
    }
  };
  public static final IntensityMergeMode SUM = new IntensityMergeMode() {
    @Override
    public double merge(DataPoint[] sources) {
      double sum = 0d;
      for (DataPoint p : sources)
        sum += p.getIntensity();
      return sum;
    }

    @Override
    public String toString() {
      return "sum intensities";
    }
  };
  public static final IntensityMergeMode AVERAGE = new IntensityMergeMode() {
    @Override
    public double merge(DataPoint[] sources) {
      double avg = 0d;
      for (DataPoint p : sources)
        avg += p.getIntensity();
      return avg / sources.length;
    }

    @Override
    public String toString() {
      return "mean intensity";
    }
  };

}
