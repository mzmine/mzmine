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

package io.github.mzmine.modules.dataprocessing.filter_scanfilters.mean;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Vector;
import org.jetbrains.annotations.NotNull;

public class MeanFilter implements ScanFilter {


  private final double windowLength;

  // requires default constructor for config
  public MeanFilter() {
    windowLength = 0;
  }

  public MeanFilter(final ParameterSet parameters) {
    windowLength = parameters.getParameter(MeanFilterParameters.oneSidedWindowLength).getValue();
  }

  @Override
  public Scan filterScan(RawDataFile newFile, Scan sc) {

    // changed to also allow MS2 if selected in ScanSelection

    Vector<Double> massWindow = new Vector<Double>();
    Vector<Double> intensityWindow = new Vector<Double>();

    double currentMass;
    double lowLimit;
    double hiLimit;
    double mzVal;

    double elSum;

    DataPoint oldDataPoints[] = ScanUtils.extractDataPoints(sc);
    DataPoint newDataPoints[] = new DataPoint[oldDataPoints.length];

    int addi = 0;
    for (int i = 0; i < oldDataPoints.length; i++) {

      currentMass = oldDataPoints[i].getMZ();
      lowLimit = currentMass - windowLength;
      hiLimit = currentMass + windowLength;

      // Remove all elements from window whose m/z value is less than the
      // low limit
      if (massWindow.size() > 0) {
        mzVal = massWindow.get(0).doubleValue();
        while ((massWindow.size() > 0) && (mzVal < lowLimit)) {
          massWindow.remove(0);
          intensityWindow.remove(0);
          if (massWindow.size() > 0) {
            mzVal = massWindow.get(0).doubleValue();
          }
        }
      }

      // Add new elements as long as their m/z values are less than the hi
      // limit
      while ((addi < oldDataPoints.length) && (oldDataPoints[addi].getMZ() <= hiLimit)) {
        massWindow.add(oldDataPoints[addi].getMZ());
        intensityWindow.add(oldDataPoints[addi].getIntensity());
        addi++;
      }

      elSum = 0;
      for (int j = 0; j < intensityWindow.size(); j++) {
        elSum += (intensityWindow.get(j)).doubleValue();
      }

      newDataPoints[i] = new SimpleDataPoint(currentMass, elSum / intensityWindow.size());

    }

    double[][] dp = DataPointUtils.getDataPointsAsDoubleArray(newDataPoints);
    // Create filtered scan
    SimpleScan newScan = new SimpleScan(newFile, sc.getScanNumber(), sc.getMSLevel(),
        sc.getRetentionTime(), sc.getMsMsInfo() != null ? sc.getMsMsInfo().createCopy() : null,
        dp[0], dp[1], MassSpectrumType.CENTROIDED, sc.getPolarity(), sc.getScanDefinition(),
        sc.getScanningMZRange());
    return newScan;

  }

  @Override
  public @NotNull String getName() {
    return "Mean filter";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return MeanFilterParameters.class;
  }
}
