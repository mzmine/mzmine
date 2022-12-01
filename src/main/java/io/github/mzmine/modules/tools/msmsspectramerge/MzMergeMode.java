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
import io.github.mzmine.util.scans.ScanUtils;

public interface MzMergeMode {

  public static MzMergeMode[] values() {
    return new MzMergeMode[] {MOST_INTENSE, WEIGHTED_AVERAGE_CUTOFF_OUTLIERS, WEIGHTED_AVERAGE};
  }

  public double merge(DataPoint[] sources);

  public static MzMergeMode WEIGHTED_AVERAGE = new MzMergeMode() {
    @Override
    public double merge(DataPoint[] sources) {
      double mz = 0d, intens = 0d;
      for (DataPoint d : sources) {
        mz += d.getMZ() * d.getIntensity();
        intens += d.getIntensity();
      }
      return mz / intens;
    }

    @Override
    public String toString() {
      return "weighted average";
    }
  };

  public static MzMergeMode MOST_INTENSE = new MzMergeMode() {
    @Override
    public double merge(DataPoint[] sources) {
      double mz = Double.NEGATIVE_INFINITY, intens = Double.NEGATIVE_INFINITY;
      for (DataPoint d : sources) {
        if (d.getIntensity() > intens) {
          mz = d.getMZ();
          intens = d.getIntensity();
        }
      }
      return mz;
    }

    @Override
    public String toString() {
      return "most intense";
    }
  };

  public static MzMergeMode WEIGHTED_AVERAGE_CUTOFF_OUTLIERS = new MzMergeMode() {
    @Override
    public double merge(DataPoint[] sources) {
      if (sources.length >= 4) {
        sources = sources.clone();
        ScanUtils.sortDataPointsByMz(sources);
        int i = (int) (sources.length * 0.25);
        double mz = 0d, intens = 0d;
        for (int k = i; k < sources.length - i; ++k) {
          mz += sources[k].getMZ() * sources[k].getIntensity();
          intens += sources[k].getIntensity();
        }
        return mz / intens;
      } else
        return WEIGHTED_AVERAGE.merge(sources);
    }

    @Override
    public String toString() {
      return "weighted average (remove outliers)";
    }
  };

}
