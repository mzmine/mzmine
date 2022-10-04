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


package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.main.MZmineCore;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;


/**
 * Chromatogram.
 */
public class ADAPChromatogram {

  // Data points of the chromatogram (map of scan number -> m/z feature)
  // private Hashtable<Integer, DataPoint> dataPointsMap;
  private final TreeMap<Scan, DataPoint> dataPointsMap = new TreeMap<>();
  public int tmp_see_same_scan_count = 0;
  // Chromatogram m/z weighted
  private double mz;
  private double mzSum = 0;
  private int mzN = 0;

  /**
   * Initializes this Chromatogram
   */
  public ADAPChromatogram() {
  }

  public Collection<DataPoint> getDataPoints() {
    return dataPointsMap.values();
  }


  /**
   * Check for a minimum number of continuous scans
   *
   * @param allScans        all scans used to build chromatograms
   * @param intensityThresh minimum intensity to consider data point connected
   * @param minimumScanSpan minimum number of connected dp
   * @return true if a minimum number of scans are connected (without holes)
   */
  public boolean matchesMinContinuousDataPoints(Scan[] allScans, double intensityThresh,
      int minimumScanSpan, double minHeight) {
    int connectedScans = 0;
    double maxCurrentHeight = 0d;
    for (Scan scan : allScans) {
      final DataPoint dataPoint = getDataPoint(scan);
      if (dataPoint != null && dataPoint.getIntensity() >= intensityThresh) {
        connectedScans++;
        // track height of current segment
        if (maxCurrentHeight < dataPoint.getIntensity()) {
          maxCurrentHeight = dataPoint.getIntensity();
        }
        // check conditions
        if (connectedScans >= minimumScanSpan && maxCurrentHeight >= minHeight) {
          return true;
        }
      } else {
        connectedScans = 0;
      }
    }
    return false;
  }

  /**
   * This method adds a MzFeature to this Chromatogram. All values of this Chromatogram (rt, m/z,
   * intensity and ranges) are updated on request
   */
  public void addMzFeature(Scan scanNumber, DataPoint mzValue) {
    // If we already have a mz value for the scan number then we need to add the intensities
    // together before putting it into the dataPointsMap, otherwise the chromatogram is only
    // representing the intensities of the last added point for that scan.
    //
    // For now just don't add the point if we have it already. The highest point will be the
    // first one added
    if (dataPointsMap.containsKey(scanNumber)) {
      tmp_see_same_scan_count += 1;
      return;
    }
    if (mzValue == null) {
      return;
    }

    dataPointsMap.put(scanNumber, mzValue);
    mzSum += mzValue.getMZ();
    mzN++;
    mz = mzSum / mzN;
  }

  public DataPoint getDataPoint(Scan scanNumber) {
    return dataPointsMap.get(scanNumber);
  }

  /**
   * This method returns m/z value of the chromatogram
   */
  private double getMZ() {
    return mz;
  }

  /**
   * This method returns a string with the basic information that defines this feature
   *
   * @return String information
   */
  @Override
  public String toString() {
    return "Chromatogram " + MZmineCore.getConfiguration().getMZFormat().format(mz) + " m/z";
  }

  public @NotNull Collection<Scan> getScanNumbers() {
    return dataPointsMap.keySet();
  }

  /**
   * Adds zeros on the side of each consecutive number of scans.
   *
   * @param minGap The minimum number of missing scans to be found, to fill up with zeros.
   * @param zeros  The number of zeros to add. zeros <= minGap
   */
  public void addNZeros(Scan[] allChromatogramScans, int minGap, int zeros) {
    assert minGap >= zeros;
    // add the same data point multiple times
    final SimpleDataPoint zeroDataPoint = new SimpleDataPoint(getMZ(), 0d);

    int allScansIndex; // contains the index of the next dp after a gap
    Map<Scan, DataPoint> dataPointsToAdd = new HashMap<>();

    Scan[] detectedScans = dataPointsMap.keySet().toArray(Scan[]::new);

    // loop through all scans
    int nextDetectedScanInAllIndex = -1;
    int nextDetectedScanIndex = 0;
    int currentGap = 0;
    int added;
    for (allScansIndex = 0; allScansIndex < allChromatogramScans.length; allScansIndex++) {
      added = 0;
      // was a DP detected in this scan?
      if (allChromatogramScans[allScansIndex] == detectedScans[nextDetectedScanIndex]) {
        if (currentGap >= minGap) {
          // add leading zeros before allScansIndex
          for (int i = 1; i <= zeros && i <= currentGap && (allScansIndex - i) >= 0; i++) {
            // add zero data points
            dataPointsToAdd.put(allChromatogramScans[allScansIndex - i], zeroDataPoint);
            added++;
          }
          currentGap -= added;
          // add trailing zeros after last detected
          if (currentGap > 0 && nextDetectedScanInAllIndex >= 0) {
            for (int i = 1; i <= zeros && i <= currentGap
                            && (nextDetectedScanInAllIndex + i) < allChromatogramScans.length;
                i++) {
              // add zero data points after
              dataPointsToAdd.put(allChromatogramScans[nextDetectedScanInAllIndex + i],
                  zeroDataPoint);
            }
          }
        }
        currentGap = 0;
        nextDetectedScanIndex++;
        nextDetectedScanInAllIndex = allScansIndex;

        // no more detected scans
        if (nextDetectedScanIndex == detectedScans.length) {
          // add trailing zeros after last detected
          for (int i = 1;
              i <= zeros && (nextDetectedScanInAllIndex + i) < allChromatogramScans.length; i++) {
            // add zero data points after
            dataPointsToAdd.put(allChromatogramScans[nextDetectedScanInAllIndex + i],
                zeroDataPoint);
          }
          break;
        }
      } else {
        currentGap++;
      }

      // last datapoint
      if (allScansIndex == allChromatogramScans.length - 1) {
        if (currentGap >= minGap) {
          // add trailing zeros after last detected
          if (currentGap > 0 && nextDetectedScanInAllIndex >= 0) {
            for (int i = 1; i <= zeros && i <= currentGap
                            && (nextDetectedScanInAllIndex + i) < allChromatogramScans.length;
                i++) {
              // add zero data points after
              dataPointsToAdd.put(allChromatogramScans[nextDetectedScanInAllIndex + i],
                  zeroDataPoint);
            }
          }
        }
      }
    }
    dataPointsMap.putAll(dataPointsToAdd);
  }

}
