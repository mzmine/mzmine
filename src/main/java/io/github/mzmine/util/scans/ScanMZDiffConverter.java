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

package io.github.mzmine.util.scans;


import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanMZDiffConverter {


  /**
   * Takes all data points of a scan or masslist and converts them to a mz differences array. The
   * resulting data points contain a deltaMZ and the number n of occurance in the scan. (e.g. [18,
   * 3] would correspond to three losses of H2O)
   * 
   * @param scan
   * @param maxDiff absolute max difference of neutral loss mass (in Da)
   * @return
   */
  public static DataPoint[] getAllMZDiff(DataPoint[] scan, MZTolerance maxDiff, double minHeight,
                                         int maxSignals) {
    if (maxSignals <= 0 || scan.length <= maxSignals)
      return getAllMZDiff(scan, maxDiff, minHeight);
    else {
      DataPoint[] max = ScanUtils.getMostAbundantSignals(scan, maxSignals);
      return getAllMZDiff(max, maxDiff, minHeight);
    }
  }

  /**
   * Takes all data points of a scan or masslist and converts them to a mz differences array. The
   * resulting data points contain a deltaMZ and the number n of occurance in the scan. (e.g. [18,
   * 3] would correspond to three losses of H2O)
   * 
   * @param scan
   * @param maxDiff
   * @param maxDiff absolute max difference of neutral loss mass (in Da)
   * @return
   */
  public static DataPoint[] getAllMZDiff(DataPoint[] scan, MZTolerance maxDiff, double minHeight) {
    Map<Double, Integer> diffMap = new HashMap<>();

    for (int i = 0; i < scan.length - 1; i++) {
      if (scan[i].getIntensity() >= minHeight) {
        for (int j = i + 1; j < scan.length; j++) {
          if (scan[j].getIntensity() >= minHeight) {
            double delta = Math.abs(scan[i].getMZ() - scan[j].getMZ());
            // find existing
            Map.Entry<Double, Integer> diff = findMatch(diffMap, delta, maxDiff);
            if (diff != null) {
              diffMap.remove(diff);
              // add new average
              Integer count = diff.getValue();
              Double averageMZDiff = (diff.getKey() * count + delta) / (Double) (count+1d);
              diffMap.put(averageMZDiff,count+1);
            }
            else {
              diffMap.put(delta, 1);
            }
          }
        }
      }
    }
    return diffMap.entrySet().stream().map(entry -> new SimpleDataPoint(entry.getKey(), entry.getValue())).toArray(DataPoint[]::new);
  }

  /**
   * Is already in list?
   * 
   * @return
   */
  public static Map.Entry<Double, Integer> findMatch(Map<Double, Integer> map, double delta,
                                                     MZTolerance maxDiff) {
    return map.entrySet().stream().filter(entry -> maxDiff.checkWithinTolerance(entry.getKey(), delta)).findFirst().orElse(null);
  }

  /**
   * 
   * @param diffAligned list of aligned data points in scans [a, b]
   * @param indexA
   * @param indexB
   */
  public static int getOverlapOfAlignedDiff(List<DataPoint[]> diffAligned, int indexA, int indexB) {
    int overlap = 0;
    for (DataPoint[] dps : diffAligned) {
      DataPoint a = dps[indexA];
      DataPoint b = dps[indexB];
      if (a != null && b != null) {
        overlap += Math.min(a.getIntensity(), b.getIntensity());
      }
    }
    return overlap;
  }

}
