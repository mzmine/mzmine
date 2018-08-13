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

package net.sf.mzmine.modules.visualization.mzhistogram.chart;

import java.util.function.Supplier;
import org.jfree.data.Range;
import net.sf.mzmine.util.maths.DoubleArraySupplier;

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
    if (range == null)
      findRange();
    if (range == null)
      return new Range(0, 0);
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
        if (data[i] > max)
          max = data[i];
        if (data[i] < min)
          min = data[i];
      }
      setRange(new Range(min, max));
    }
  }

  public double size() {
    return data != null ? data.length : 0;
  }
}
