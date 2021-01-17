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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.localmaxima;

import java.util.ArrayList;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;

/**
 * This class detects all local maxima in a given scan.
 */
public class LocalMaxMassDetector implements MassDetector {

  @Override
  public DataPoint[] getMassValues(MassSpectrum scan, ParameterSet parameters) {

    double noiseLevel =
        parameters.getParameter(LocalMaxMassDetectorParameters.noiseLevel).getValue();

    // List of found mz peaks
    ArrayList<DataPoint> mzPeaks = new ArrayList<DataPoint>();

    // All data points of current m/z peak

    // Top data point of current m/z peak
    int currentMzPeakTop = 0;

    // True if we haven't reached the current local maximum yet
    boolean ascending = true;

    // Iterate through all data points
    for (int i = 0; i < scan.getNumberOfDataPoints() - 1; i++) {

      boolean nextIsBigger = scan.getIntensityValue(i + 1) > scan.getIntensityValue(i);
      boolean nextIsZero = scan.getIntensityValue(i + 1) == 0;
      boolean currentIsZero = scan.getIntensityValue(i) == 0;

      // Ignore zero intensity regions
      if (currentIsZero)
        continue;

      // Check for local maximum
      if (ascending && (!nextIsBigger)) {
        currentMzPeakTop = i;
        ascending = false;
        continue;
      }

      // Check for the end of the peak
      if ((!ascending) && (nextIsBigger || nextIsZero)) {

        // Add the m/z peak if it is above the noise level
        if (scan.getIntensityValue(currentMzPeakTop) > noiseLevel) {
          mzPeaks.add(new SimpleDataPoint(scan.getMzValue(currentMzPeakTop),
              scan.getIntensityValue(currentMzPeakTop)));
        }

        // Reset and start with new peak
        ascending = true;

      }

    }
    return mzPeaks.toArray(new DataPoint[0]);
  }

  @Override
  public @Nonnull String getName() {
    return "Local maxima";
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return LocalMaxMassDetectorParameters.class;
  }

}
