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

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_manual.ManualFeature;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * @author SteffenHeu (steffen.heuckeroth@gmx.de / steffen.heuckeroth@uni-muenster.de)
 */
public class ManualFeatureUtils {

  /**
   * @param dataFile The raw data file
   * @param rtRange  The rt range of the feature
   * @param mzRange  The mz range of the feature
   * @return The manual feature or null of no features were found
   */
  public static ManualFeature pickFeatureManually(RawDataFile dataFile, Range<Float> rtRange,
      Range<Double> mzRange) {
    ManualFeature newFeature = new ManualFeature(dataFile);
    boolean dataPointFound = false;

    Scan[] scanNumbers = dataFile.getScanNumbers(1, rtRange);

    for (Scan scan : scanNumbers) {
      // Find most intense m/z feature
      DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);

      if (basePeak != null) {
        if (basePeak.getIntensity() > 0) {
          dataPointFound = true;
        }
        newFeature.addDatapoint(scan, basePeak);
      } else {
        final double mzCenter = (mzRange.lowerEndpoint() + mzRange.upperEndpoint()) / 2.0;
        DataPoint fakeDataPoint = new SimpleDataPoint(mzCenter, 0);
        newFeature.addDatapoint(scan, fakeDataPoint);
      }
    }

    if (dataPointFound) {
      newFeature.finalizeFeature();
      return newFeature;
    }
    return null;
  }
}
