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

package io.github.mzmine.modules.dataprocessing.align_hierarchical;

import java.util.logging.Logger;

public class TriangularMatrixDouble extends TriangularMatrix {

  private static final Logger logger = Logger.getLogger(TriangularMatrixDouble.class.getName());

  private final LargeArrayDouble list;

  public TriangularMatrixDouble(int dimension) {

    list = new LargeArrayDouble(sumFormula(dimension));
    this.setDimension(dimension);
  }

  @Override
  public double set(int row, int column, double value) {

    long listIndex = getListIndex(row, column);
    double oldValue = list.get(listIndex);
    list.set(listIndex, value);

    return oldValue;
  }

  @Override
  public double get(int row, int column) {

    return list.get(getListIndex(row, column));
  }

  @Override
  public void printVector() {
    logger.info(list.toString());
  }

}
