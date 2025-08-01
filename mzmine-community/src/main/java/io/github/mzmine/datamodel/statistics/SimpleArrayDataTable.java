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

package io.github.mzmine.datamodel.statistics;

/**
 * Just an array data table
 */
public class SimpleArrayDataTable extends AbstractRowArrayDataTable {

  /**
   * data array as [rows=features][columns=samples]
   */
  protected final double[][] data;

  /**
   * @param data data array as [rows][columns]
   */
  public SimpleArrayDataTable(double[][] data) {
    this.data = data;
  }


  @Override
  public int getNumberOfSamples() {
    return getNumberOfFeatures() == 0 ? 0 : data[0].length;
  }

  @Override
  public int getNumberOfFeatures() {
    return data.length;
  }

  @Override
  public double[] getFeatureData(int index, boolean copy) {
    return copy ? data[index].clone() : data[index];
  }

  @Override
  public SimpleArrayDataTable copy() {
    final double[][] copyData = new double[data.length][];
    for (int i = 0; i < data.length; i++) {
      copyData[i] = data[i].clone();
    }
    return new SimpleArrayDataTable(copyData);
  }
}
