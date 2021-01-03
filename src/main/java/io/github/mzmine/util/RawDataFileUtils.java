/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.util;

import javafx.collections.ObservableList;
import javax.annotation.Nonnull;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;

/**
 * Raw data file related utilities
 */
public class RawDataFileUtils {

  public static @Nonnull Range<Float> findTotalRTRange(RawDataFile dataFiles[], int msLevel) {
    Range<Float> rtRange = null;
    for (RawDataFile file : dataFiles) {
      Range<Float> dfRange = file.getDataRTRange(msLevel);
      if (dfRange == null)
        continue;
      if (rtRange == null)
        rtRange = dfRange;
      else
        rtRange = rtRange.span(dfRange);
    }
    if (rtRange == null)
      rtRange = Range.singleton(0.0f);
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
    for (Scan scan : dataFile.getScanNumbers(1)) {
      if (scan.getMassLists().length == 0)
        return false;
    }
    return true;
  }

  public static Scan getClosestScanNumber(RawDataFile dataFile, double rt) {

    ObservableList<Scan> scanNums = dataFile.getScans();
    if (scanNums.size() == 0)
      return null;
    int best = 0;
    double bestRt = scanNums.get(0).getRetentionTime();

    for (int i = 1; i < scanNums.size(); i++) {
      double thisRt = scanNums.get(i).getRetentionTime();
      if (Math.abs(bestRt - rt) > Math.abs(thisRt - rt)) {
        best = i;
        bestRt = thisRt;
      }
    }
    return scanNums.get(best);
  }
}
