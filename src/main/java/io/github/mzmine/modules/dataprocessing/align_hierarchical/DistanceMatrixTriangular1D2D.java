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

package io.github.mzmine.modules.dataprocessing.align_hierarchical;

import java.util.logging.Logger;

import org.gnf.clustering.DistanceMatrix;

public class DistanceMatrixTriangular1D2D implements DistanceMatrix {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private int dimension;

  private final LargeArrayFloat list;

  public DistanceMatrixTriangular1D2D(int nRowCount) {

    list = new LargeArrayFloat(sumFormula(nRowCount));
    dimension = nRowCount;
  }

  public DistanceMatrixTriangular1D2D(DistanceMatrix distanceMatrix2) {

    this.dimension = distanceMatrix2.getRowCount();
    this.list = new LargeArrayFloat(sumFormula(this.dimension));

    for (int i = 0; i < this.dimension; ++i) {
      for (int j = i; j < this.dimension; ++j) {
        this.setValue(i, j, distanceMatrix2.getValue(i, j));
      }
    }
  }

  static public long getListIndex(int row, int column) { // Symmetrical

    if (row > column)
      return sumFormula(row) + (long) column;
    else
      return sumFormula(column) + (long) row;
  }

  static public long sumFormula(long i) {
    return (i * i + i) / 2;
  }

  @Override
  public int getRowCount() {
    return dimension;
  }

  @Override
  public int getColCount() {
    return dimension;
  }

  @Override
  public float getValue(int nRow, int nCol) {

    return list.get(getListIndex(nRow, nCol));
  }

  @Override
  public void setValue(int nRow, int nCol, float fVal) {

    list.set(getListIndex(nRow, nCol), fVal);
  }

  // ---------------------------------------

  public void printVector() {
    // System.out.println(Arrays.toString(this.getVector()));
    logger.info(list.toString());
  }

  // -
  public void print() {

    for (int i = 0; i < this.dimension; i++) {

      System.out.println("\n");

      for (int j = 0; j < this.dimension; j++) {

        System.out.println(" " + this.getValue(i, j));
      }

    }
  }

  // -
  public double[][] toTwoDimArray() {

    double[][] arr = new double[this.dimension][this.dimension];

    for (int i = 0; i < this.dimension; i++) {

      for (int j = 0; j < this.dimension; j++) {

        arr[i][j] = this.getValue(i, j);
      }
    }

    return arr;
  }

}
