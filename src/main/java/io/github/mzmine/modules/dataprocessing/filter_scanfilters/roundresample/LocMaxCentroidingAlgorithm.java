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

package io.github.mzmine.modules.dataprocessing.filter_scanfilters.roundresample;

import java.util.ArrayList;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;

/*
 * Adapted from MSDK: https://github.com/msdk/msdk/blob/master/msdk-rawdata/
 * msdk-rawdata-centroiding/src/main/java/io/github/
 * msdk/rawdata/centroiding/LocalMaximaCentroidingAlgorithm.java
 */
public class LocMaxCentroidingAlgorithm {

  public static DataPoint[] centroidScan(DataPoint dataPoints[]) {

    double mzBuffer[] = new double[dataPoints.length];
    double intensityBuffer[] = new double[dataPoints.length];

    // Load data points
    for (int i = 0; i < dataPoints.length; ++i) {
      mzBuffer[i] = dataPoints[i].getMZ();
      intensityBuffer[i] = dataPoints[i].getIntensity();
    }

    final int numOfDataPoints = dataPoints.length;
    int newNumOfDataPoints = 0;

    // If there are no data points, just return the scan
    ArrayList<DataPoint> newDataPoints = new ArrayList<DataPoint>();
    if (numOfDataPoints == 0) {
      for (int i = 0; i < numOfDataPoints; ++i) {
        newDataPoints.add(new SimpleDataPoint(mzBuffer[i], intensityBuffer[i]));
      }
      return newDataPoints.toArray(new SimpleDataPoint[newDataPoints.size()]);
    }

    int localMaximumIndex = 0;
    int rangeBeginning = 0, rangeEnd;
    boolean ascending = true;

    // Iterate through all data points
    for (int i = 0; i < numOfDataPoints - 1; i++) {

      final boolean nextIsBigger = intensityBuffer[i + 1] > intensityBuffer[i];
      final boolean nextIsZero = intensityBuffer[i + 1] == 0f;
      final boolean currentIsZero = intensityBuffer[i] == 0f;

      // Ignore zero intensity regions
      if (currentIsZero) {
        continue;
      }

      // Add current (non-zero) data point to the current m/z peak
      rangeEnd = i;

      // Check for local maximum
      if (ascending && (!nextIsBigger)) {
        localMaximumIndex = i;
        ascending = false;
        continue;
      }

      // Check for the end of the peak
      if ((!ascending) && (nextIsBigger || nextIsZero)) {

        final int numOfPeakDataPoints = rangeEnd - rangeBeginning;

        // Add the m/z peak if it has at least 4 data points
        if (numOfPeakDataPoints >= 4) {

          // Add the new data point
          mzBuffer[newNumOfDataPoints] = mzBuffer[localMaximumIndex];
          intensityBuffer[newNumOfDataPoints] = intensityBuffer[localMaximumIndex];
          newNumOfDataPoints++;

        }

        // Reset and start with new peak
        ascending = true;
        rangeBeginning = i;
      }

    }

    // Store the new data points
    for (int i = 0; i < newNumOfDataPoints; ++i) {
      newDataPoints.add(new SimpleDataPoint(mzBuffer[i], intensityBuffer[i]));
    }

    return newDataPoints.toArray(new DataPoint[newDataPoints.size()]);

  }

}
