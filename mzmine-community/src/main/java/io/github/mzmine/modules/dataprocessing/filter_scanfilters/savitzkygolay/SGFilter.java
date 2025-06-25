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

package io.github.mzmine.modules.dataprocessing.filter_scanfilters.savitzkygolay;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Hashtable;
import org.jetbrains.annotations.NotNull;

public class SGFilter implements ScanFilter {

  private static final Hashtable<Integer, Integer> Hvalues = new Hashtable<Integer, Integer>();
  private static final Hashtable<Integer, int[]> Avalues = new Hashtable<Integer, int[]>();

  static {
    int[] a5Ints = {17, 12, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    Avalues.put(5, a5Ints);
    int[] a7Ints = {7, 6, 3, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    Avalues.put(7, a7Ints);
    int[] a9Ints = {59, 54, 39, 14, -21, 0, 0, 0, 0, 0, 0, 0, 0};
    Avalues.put(9, a9Ints);
    int[] a11Ints = {89, 84, 69, 44, 9, -36, 0, 0, 0, 0, 0, 0, 0};
    Avalues.put(11, a11Ints);
    int[] a13Ints = {25, 24, 21, 16, 9, 0, -11, 0, 0, 0, 0, 0, 0};
    Avalues.put(13, a13Ints);
    int[] a15Ints = {167, 162, 147, 122, 87, 42, -13, -78, 0, 0, 0, 0, 0};
    Avalues.put(15, a15Ints);
    int[] a17Ints = {43, 42, 39, 34, 27, 18, 7, -6, -21, 0, 0, 0, 0};
    Avalues.put(17, a17Ints);
    int[] a19Ints = {269, 264, 249, 224, 189, 144, 89, 24, -51, -136, 0, 0, 0};
    Avalues.put(19, a19Ints);
    int[] a21Ints = {329, 324, 309, 284, 249, 204, 149, 84, 9, -76, -171, 0, 0};
    Avalues.put(21, a21Ints);
    int[] a23Ints = {79, 78, 75, 70, 63, 54, 43, 30, 15, -2, -21, -42, 0};
    Avalues.put(23, a23Ints);
    int[] a25Ints = {467, 462, 447, 422, 387, 343, 287, 222, 147, 62, -33, -138, -253};
    Avalues.put(25, a25Ints);

    Hvalues.put(5, 35);
    Hvalues.put(7, 21);
    Hvalues.put(9, 231);
    Hvalues.put(11, 429);
    Hvalues.put(13, 143);
    Hvalues.put(15, 1105);
    Hvalues.put(17, 323);
    Hvalues.put(19, 2261);
    Hvalues.put(21, 3059);
    Hvalues.put(23, 805);
    Hvalues.put(25, 5175);

  }

  private final int numOfDataPoints;

  // requires default constructor for config
  public SGFilter() {
    numOfDataPoints = 5;
  }

  public SGFilter(final ParameterSet parameters) {
    numOfDataPoints = parameters.getValue(SGFilterParameters.datapoints);
  }

  @Override
  public Scan filterScan(RawDataFile newFile, Scan scan) {

    assert Avalues.containsKey(numOfDataPoints);
    assert Hvalues.containsKey(numOfDataPoints);

    int[] aVals = Avalues.get(numOfDataPoints);
    int h = Hvalues.get(numOfDataPoints).intValue();

    // changed to also allow MS2 if selected in ScanSelection

    int marginSize = (numOfDataPoints + 1) / 2 - 1;
    double sumOfInts;

    DataPoint oldDataPoints[] = ScanUtils.extractDataPoints(scan);
    int newDataPointsLength = oldDataPoints.length - (marginSize * 2);

    // only process scans with datapoints
    if (newDataPointsLength < 1) {
      return scan;
    }

    DataPoint newDataPoints[] = new DataPoint[newDataPointsLength];

    for (int spectrumInd = marginSize; spectrumInd < (oldDataPoints.length - marginSize);
        spectrumInd++) {

      // zero intensity data points must be left unchanged
      if (oldDataPoints[spectrumInd].getIntensity() == 0) {
        newDataPoints[spectrumInd - marginSize] = oldDataPoints[spectrumInd];
        continue;
      }

      sumOfInts = aVals[0] * oldDataPoints[spectrumInd].getIntensity();

      for (int windowInd = 1; windowInd <= marginSize; windowInd++) {
        sumOfInts += aVals[windowInd] * (oldDataPoints[spectrumInd + windowInd].getIntensity()
                                         + oldDataPoints[spectrumInd - windowInd].getIntensity());
      }

      sumOfInts = sumOfInts / h;

      if (sumOfInts < 0) {
        sumOfInts = 0;
      }
      newDataPoints[spectrumInd - marginSize] = new SimpleDataPoint(
          oldDataPoints[spectrumInd].getMZ(), sumOfInts);

    }

    double[][] dp = DataPointUtils.getDataPointsAsDoubleArray(newDataPoints);
    SimpleScan newScan = new SimpleScan(newFile, scan, dp[0], dp[1]);
    return newScan;

  }

  @Override
  public @NotNull String getName() {
    return "Savitzky-Golay filter";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return SGFilterParameters.class;
  }

}
