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

package io.github.mzmine.modules.visualization.scan_histogram.chart;

import io.github.mzmine.util.maths.DoubleArraySupplier;
import java.util.function.Supplier;
import org.jfree.data.Range;

public class HistogramData {

  // data is not binned
  private double[] data;
  private Range range;

  public HistogramData(double[] data, Range range) {
    this.data = data;
    this.range = range;
  }

  public HistogramData(double[] data, double min, double max) {
    this.data = data;
    this.range = new Range(min, max);
  }

  public HistogramData(double[] data) {
    this.data = data;
    findRange();
  }

  public HistogramData(DoubleArraySupplier data, Supplier<Range> range) {
    this(data.get(), range.get());
    this.data = data.get();
  }

  public HistogramData(DoubleArraySupplier data) {
    this(data.get());
  }

  /**
   * Data is not binned and maybe unsorted
   *
   * @return
   */
  public double[] getData() {
    return data;
  }

  public Range getRange() {
    if (range == null) {
      findRange();
    }
    if (range == null) {
      return new Range(0, 0);
    }
    return range;
  }

  protected void setRange(Range range) {
    this.range = range;
  }

  private void findRange() {
    if (data != null && data.length > 0) {
      double min = data[0];
      double max = data[0];
      for (int i = 1; i < data.length; i++) {
        if (data[i] > max) {
          max = data[i];
        }
        if (data[i] < min) {
          min = data[i];
        }
      }
      setRange(new Range(min, max));
    }
  }

  public double size() {
    return data != null ? data.length : 0;
  }
}
