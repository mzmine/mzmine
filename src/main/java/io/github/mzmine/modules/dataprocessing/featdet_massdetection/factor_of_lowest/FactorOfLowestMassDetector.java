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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest;

import gnu.trove.list.array.TDoubleArrayList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

/**
 * Remove peaks below the minimum intensity x a user defined factor
 */
public class FactorOfLowestMassDetector implements MassDetector {

  @Override
  public double[][] getMassValues(MassSpectrum spectrum, ParameterSet parameters) {
    // most likely working on {@link ScanDataAccess}

    final double noiseFactor = parameters.getValue(
        FactorOfLowestMassDetectorParameters.noiseFactor);

    // get the minimum intensity and base noise on this
    double noiseLevel = minIntensity(spectrum) * noiseFactor;

    // use number of centroid signals as base array list capacity
    final int points = spectrum.getNumberOfDataPoints();

    // lists of primitive doubles
    TDoubleArrayList mzs = new TDoubleArrayList(points);
    TDoubleArrayList intensities = new TDoubleArrayList(points);
    // Find possible mzPeaks
    for (int i = 0; i < points; i++) {
      // Is intensity above the noise level?
      double intensity = spectrum.getIntensityValue(i);
      if (intensity >= noiseLevel) {
        // add data point
        mzs.add(spectrum.getMzValue(i));
        intensities.add(intensity);
      }
    }
    return new double[][]{mzs.toArray(), intensities.toArray()};
  }

  @Override
  public double[][] getMassValues(double[] mzs, double[] intensities, ParameterSet parameters) {
    assert mzs.length == intensities.length;

    final double noiseFactor = parameters.getValue(
        FactorOfLowestMassDetectorParameters.noiseFactor);
    return getMassValues(mzs, intensities, noiseFactor);
  }

  public double[][] getMassValues(double[] mzs, double[] intensities, double noiseFactor) {
    assert mzs.length == intensities.length;

    // get the minimum intensity and base noise on this
    double noiseLevel = minIntensity(intensities) * noiseFactor;

    // use number of centroid signals as base array list capacity
    final int points = mzs.length;
    // lists of primitive doubles
    TDoubleArrayList pickedMZs = new TDoubleArrayList(points);
    TDoubleArrayList pickedIntensities = new TDoubleArrayList(points);

    // Find possible mzPeaks
    for (int i = 0; i < points; i++) {
      // Is intensity above the noise level?
      if (intensities[i] >= noiseLevel) {
        // Yes, then mark this index as mzPeak
        pickedMZs.add(mzs[i]);
        pickedIntensities.add(intensities[i]);
      }
    }
    return new double[][]{pickedMZs.toArray(), pickedIntensities.toArray()};
  }

  private double minIntensity(double[] rawIntensities) {
    double minIntensity = Double.MAX_VALUE;
    for (double v : rawIntensities) {
      if (v < minIntensity) {
        minIntensity = v;
      }
    }
    if (Double.compare(minIntensity, Double.MAX_VALUE) == 0) {
      minIntensity = 0;
    }
    return minIntensity;
  }

  private double minIntensity(MassSpectrum spec) {
    double minIntensity = Double.MAX_VALUE;
    for (int i = 0; i < spec.getNumberOfDataPoints(); i++) {
      final double value = spec.getIntensityValue(i);
      if (value < minIntensity && value > 0) {
        minIntensity = value;
      }
    }
    if (Double.compare(minIntensity, Double.MAX_VALUE) == 0) {
      minIntensity = 0;
    }
    return minIntensity;
  }

  @Override
  public @NotNull String getName() {
    return "Factor of lowest signal";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return FactorOfLowestMassDetectorParameters.class;
  }

}
