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
import javax.annotation.Nonnull;

public class MeanFilter implements ScanFilter {

  @Override
  public Scan filterScan(RawDataFile newFile, Scan sc, ParameterSet parameters) {

    double windowLength =
        parameters.getParameter(MeanFilterParameters.oneSidedWindowLength).getValue();

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
    SimpleScan newScan =
        new SimpleScan(newFile, sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(),
            sc.getPrecursorMZ(), sc.getPrecursorCharge(), dp[0], dp[1], MassSpectrumType.CENTROIDED,
            sc.getPolarity(), sc.getScanDefinition(), sc.getScanningMZRange());
    return newScan;

  }

  @Override
  public @Nonnull String getName() {
    return "Mean filter";
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return MeanFilterParameters.class;
  }
}
