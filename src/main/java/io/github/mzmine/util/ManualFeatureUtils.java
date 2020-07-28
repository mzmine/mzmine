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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_manual.ManualPeak;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * @author SteffenHeu (steffen.heuckeroth@gmx.de / steffen.heuckeroth@uni-muenster.de)
 */
public class ManualFeatureUtils {

  /**
   * @param dataFile The raw data file
   * @param rtRange  The rt range of the feature
   * @param mzRange  The mz range of the feature
   * @return The manual peak or null of no peaks were found
   */
  public static ManualPeak pickFeatureManually(RawDataFile dataFile, Range<Double> rtRange,
      Range<Double> mzRange) {
    ManualPeak newPeak = new ManualPeak(dataFile);
    boolean dataPointFound = false;

    int[] scanNumbers = dataFile.getScanNumbers(1, rtRange);

    for (int scanNumber : scanNumbers) {

      // Get next scan
      Scan scan = dataFile.getScan(scanNumber);

      // Find most intense m/z peak
      DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);

      if (basePeak != null) {
        if (basePeak.getIntensity() > 0) {
          dataPointFound = true;
        }
        newPeak.addDatapoint(scan.getScanNumber(), basePeak);
      } else {
        final double mzCenter = (mzRange.lowerEndpoint() + mzRange.upperEndpoint()) / 2.0;
        DataPoint fakeDataPoint = new SimpleDataPoint(mzCenter, 0);
        newPeak.addDatapoint(scan.getScanNumber(), fakeDataPoint);
      }
    }

    if (dataPointFound) {
      newPeak.finalizePeak();
      return newPeak;
    }
    return null;
  }
}
