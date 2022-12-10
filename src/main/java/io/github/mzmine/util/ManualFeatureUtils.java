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
