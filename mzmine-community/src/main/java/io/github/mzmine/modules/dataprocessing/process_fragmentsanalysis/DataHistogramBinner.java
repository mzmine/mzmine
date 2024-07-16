/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.process_fragmentsanalysis;

/**
 * Bins data into an existing array
 */
public class DataHistogramBinner {

  private final int[] data;
  private final double binWidth;
  private final double minValue;
  private final double maxValueExclusive;
  private long numberOfDataPoints = 0;

  public DataHistogramBinner(final int numberOfBins, final double minValue,
      final double maxValueExclusive) {
    this((maxValueExclusive - minValue) / numberOfBins, minValue, maxValueExclusive);
  }

  public DataHistogramBinner(final double binWidth, final double minValue,
      final double maxValueExclusive) {
    double width = maxValueExclusive - minValue;
    this.data = new int[(int) Math.ceil(width / binWidth)];
    this.binWidth = binWidth;
    this.minValue = minValue;
    this.maxValueExclusive = maxValueExclusive;
  }

  public boolean addValue(final double value) {
    if (value < minValue || value >= maxValueExclusive) {
      return false;
    }

    int bin = (int) Math.floor((value - minValue) / binWidth);
    data[bin]++;
    numberOfDataPoints++;
    return true;
  }

  /**
   * @return -1 for out of binning data range, otherwise the frequency of this value
   */
  public int getBinFrequency(final double value) {
    if (value < minValue || value >= maxValueExclusive) {
      return -1;
    }

    int bin = (int) Math.floor((value - minValue) / binWidth);
    return data[bin];
  }

  public int getNumBins() {
    return data.length;
  }

  public long getNumberOfDataPoints() {
    return numberOfDataPoints;
  }

  public int[] getHistogram() {
    return data;
  }

  public double getMinValue() {
    return minValue;
  }

  public double getMaxValueExclusive() {
    return maxValueExclusive;
  }

  public double getBinWidth() {
    return binWidth;
  }
}
