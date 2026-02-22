/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

import java.util.LinkedList;
import org.apache.commons.math3.stat.regression.SimpleRegression;

public class SlopeEdgeDetector implements EdgeDetector {

  private final int windowSize;
  private final LinkedList<Data> history = new LinkedList<>();

  public SlopeEdgeDetector(int windowSize) {
    this.windowSize = windowSize;
    if(windowSize < 2) {
      throw new RuntimeException("Window size must be at least 2");
    }
  }

  private final SimpleRegression regression = new SimpleRegression();

  @Override
  public int detectLeftMinimum(double[] y, int startIndex) {
    history.clear();
    regression.clear();
    final double normFactor = 1; //y[startIndex];

    if (startIndex - windowSize < 0) {
      return 0;
    }

    // fill initial points
    for (int i = startIndex; i > startIndex - windowSize; i--) {
      final Data data = new Data(i, y[i] / normFactor);
      regression.addData(data.x(), data.y());
      history.add(data);
    }

    if (regression.getSlope() <= 0) { // slope == 0 also counts as break condition
      return startIndex - windowSize;
    }

    int absMinIndex = startIndex;

    for (int i = startIndex - windowSize; i > 0; i--) {
      final Data data = new Data(i, y[i] / normFactor);
      regression.addData(data.x(), data.y());
      history.add(data);

      if (y[i] < y[absMinIndex]) {
        absMinIndex = i;
      }

      Data old = history.poll();
      regression.removeData(old.x(), old.y());
      if (regression.getSlope() <= 0) { // slope == 0 also counts as break condition
        return i + windowSize / 2;
//        return absMinIndex;
      }
    }
    return absMinIndex;
  }

  @Override
  public int detectRightMinimum(double[] y, int startIndex) {
    history.clear();
    regression.clear();
    final double normFactor = 1; //y[startIndex];

//    if (startIndex - windowSize < 0) {
//      return 0;
//    }

    // fill initial points
    for (int i = startIndex; i < startIndex + windowSize && i < y.length; i++) {
      final Data data = new Data(i, y[i] / normFactor);
      regression.addData(data.x(), data.y());
      history.add(data);
    }

    if (regression.getSlope() > 0) {
      while (regression.getN() > 2) {
        Data last = history.pollLast();
        regression.removeData(last.x, last.y);
      }
      return Math.toIntExact(startIndex + regression.getN());
    }

    int absMinIndex = startIndex;

    for (int i = startIndex + windowSize; i < y.length; i++) {
      final Data data = new Data(i, y[i] / normFactor);
      regression.addData(data.x(), data.y());
      history.add(data);

      if (y[i] < y[absMinIndex]) {
        absMinIndex = i;
      }

      Data old = history.poll();
      regression.removeData(old.x(), old.y());
      if (regression.getSlope() > 0) {
        return i - windowSize / 2;
//        return absMinIndex;
      }
    }
    return absMinIndex;
  }

  record Data(int x, double y) {

  }
}
