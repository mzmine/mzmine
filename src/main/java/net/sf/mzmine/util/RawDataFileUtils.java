/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;

import com.google.common.collect.Range;

import io.github.msdk.util.RawDataFileUtil;

/**
 * Raw data file related utilities
 */
public class RawDataFileUtils {

  public static @Nonnull Range<Double> findTotalRTRange(RawDataFile dataFiles[], int msLevel) {
    Range<Double> rtRange = null;
    for (RawDataFile file : dataFiles) {
      Range<Double> dfRange = file.getDataRTRange(msLevel);
      if (dfRange == null)
        continue;
      if (rtRange == null)
        rtRange = dfRange;
      else
        rtRange = rtRange.span(dfRange);
    }
    if (rtRange == null)
      rtRange = Range.singleton(0.0);
    return rtRange;
  }

  public static @Nonnull Range<Double> findTotalMZRange(RawDataFile dataFiles[], int msLevel) {
    Range<Double> mzRange = null;
    for (RawDataFile file : dataFiles) {
      Range<Double> dfRange = file.getDataMZRange(msLevel);
      if (dfRange == null)
        continue;
      if (mzRange == null)
        mzRange = dfRange;
      else
        mzRange = mzRange.span(dfRange);
    }
    if (mzRange == null)
      mzRange = Range.singleton(0.0);
    return mzRange;
  }

  /**
   * Returns true if the given data file has mass lists for all MS1 scans
   * 
   */
  public static boolean hasMassLists(RawDataFile dataFile) {
    for (int scanNum : dataFile.getScanNumbers(1)) {
      Scan scan = dataFile.getScan(scanNum);
      if (scan.getMassLists().length == 0)
        return false;
    }
    return true;
  }

  public static int getClosestScanNumber(RawDataFile dataFile, double rt) {

    int scanNums[] = dataFile.getScanNumbers();
    if (scanNums.length == 0)
      return -1;
    int best = 0;
    double bestRt = dataFile.getScan(scanNums[0]).getRetentionTime();

    for (int i = 1; i < scanNums.length; i++) {
      double thisRt = dataFile.getScan(scanNums[i]).getRetentionTime();
      if (Math.abs(bestRt - rt) > Math.abs(thisRt - rt)) {
        best = i;
        bestRt = thisRt;
      }
    }
    return scanNums[best];
  }
}


