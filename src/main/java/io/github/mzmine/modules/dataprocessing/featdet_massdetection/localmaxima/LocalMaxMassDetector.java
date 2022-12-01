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
