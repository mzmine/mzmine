/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
