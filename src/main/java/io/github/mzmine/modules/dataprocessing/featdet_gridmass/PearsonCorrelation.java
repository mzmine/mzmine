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

package io.github.mzmine.modules.dataprocessing.featdet_gridmass;

class PearsonCorrelation {

  private int count; // Number of numbers that have been entered.
  private double sumX = 0;
  private double sumY = 0;
  private double sumXX = 0;
  private double sumYY = 0;
  private double sumXY = 0;

  void enter(double x, double y) {
    // Add the number to the dataset.
    count++;
    sumX += x;
    sumY += y;
    sumXX += x * x;
    sumYY += y * y;
    sumXY += x * y;
  }

  int getCount() {
    // Return number of items that have been entered.
    return count;
  }

  double meanX() {
    return sumX / count;
  }

  double meanY() {
    return sumY / count;
  }

  double stdevX() {
    if (count < 2)
      return meanX();
    return Math.sqrt((sumXX - sumX * sumX / count) / (count - 1));
  }

  double stdevY() {
    if (count < 2)
      return meanY();
    return Math.sqrt((sumYY - sumY * sumY / count) / (count - 1));
  }

  double correlation() {
    double numerator = count * sumXY - sumX * sumY;
    int n = (count > 50 ? count - 1 : count);
    double denominator = Math.sqrt(n * sumXX - sumX * sumX) * Math.sqrt(n * sumYY - sumY * sumY);
    double c = (count < 3 ? 0 : numerator / denominator);
    return c;
  }
}
