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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.localmaxima;

import gnu.trove.list.array.TDoubleArrayList;
import org.jetbrains.annotations.NotNull;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;

/**
 * This class detects all local maxima in a given scan.
 */
public class LocalMaxMassDetector implements MassDetector {

  @Override
  public double[][] getMassValues(MassSpectrum scan, ParameterSet parameters) {

    double noiseLevel =
        parameters.getParameter(LocalMaxMassDetectorParameters.noiseLevel).getValue();

    // lists of primitive doubles
    TDoubleArrayList mzs = new TDoubleArrayList(100);
    TDoubleArrayList intensities = new TDoubleArrayList(100);

    // All data points of current m/z peak

    // Top data point of current m/z peak
    int currentMzPeakTop = 0;

    // True if we haven't reached the current local maximum yet
    boolean ascending = true;

    // Iterate through all data points
    for (int i = 0; i < scan.getNumberOfDataPoints() - 1; i++) {
      double intensity = scan.getIntensityValue(i);
      double nextIntensity = scan.getIntensityValue(i+1);

      boolean nextIsBigger =  nextIntensity > intensity;
      boolean nextIsZero = Double.compare(nextIntensity, 0d) == 0;
      boolean currentIsZero = Double.compare(intensity, 0d) == 0;

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
          mzs.add(scan.getMzValue(currentMzPeakTop));
          intensities.add(scan.getIntensityValue(currentMzPeakTop));
        }

        // Reset and start with new peak
        ascending = true;
      }
    }
    // Return an array of detected MzPeaks sorted by MZ
    return new double[][]{mzs.toArray(), intensities.toArray()};
  }

  @Override
  public @NotNull String getName() {
    return "Local maxima";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return LocalMaxMassDetectorParameters.class;
  }

}
