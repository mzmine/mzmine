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

public abstract class TriangularMatrix {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private int dimension;

  public abstract double set(int row, int column, double value);

  public abstract double get(int row, int column);

  // public abstract int getSize();
  public int getDimension() {
    return this.dimension;
  }

  protected void setDimension(int dimension) {
    this.dimension = dimension;
  }

  public void validateArguments(int row, int column) {
    if (row > column) {
      throw new IllegalArgumentException(
          "Row (" + row + " given) has to be smaller or equal than column (" + column + " given)!");
    }
  }

  public long getListIndex(int row, int column) { // Symmetrical

    if (row > column)
      return sumFormula(row) + (long) column;
    else
      return sumFormula(column) + (long) row;
  }

  public long sumFormula(long i) {
    return (i * i + i) / 2;
  }

  public void print() {

    for (int i = 0; i < getDimension(); i++) {

      logger.info("\n");

      for (int j = 0; j < getDimension(); j++) {

        logger.info(" " + this.get(i, j));
      }

    }
  }

  public abstract void printVector();

  public double[][] toTwoDimArray() {

    double[][] arr = new double[this.dimension][this.dimension];

    for (int i = 0; i < getDimension(); i++) {

      for (int j = 0; j < getDimension(); j++) {

        arr[i][j] = this.get(i, j);
      }
    }

    return arr;
  }

}
