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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.jetbrains.annotations.NotNull;

/**
 * Remove peaks below the given noise level.
 */
public class CentroidMassDetector implements MassDetector {

  private final double noiseLevel;

  /**
   * required to create a default instance via reflection
   */
  public CentroidMassDetector() {
    this(0);
  }

  public CentroidMassDetector(final double noiseLevel) {
    this.noiseLevel = noiseLevel;
  }

  public static double[][] getMassValues(double[] mzs, double[] intensities, double noiseLevel) {
    assert mzs.length == intensities.length;

    // use number of centroid signals as base array list capacity
    final int points = mzs.length;
    // lists of primitive doubles
    DoubleArrayList pickedMZs = new DoubleArrayList(points);
    DoubleArrayList pickedIntensities = new DoubleArrayList(points);

    // Find possible mzPeaks
    for (int i = 0; i < points; i++) {
      // Is intensity above the noise level?
      if (intensities[i] >= noiseLevel) {
        // Yes, then mark this index as mzPeak
        pickedMZs.add(mzs[i]);
        pickedIntensities.add(intensities[i]);
      }
    }
    return new double[][]{pickedMZs.toDoubleArray(), pickedIntensities.toDoubleArray()};
  }

  @Override
  public boolean filtersActive() {
    return noiseLevel > 0;
  }

  @Override
  public MassDetector create(ParameterSet parameters) {
    var noiseLevel = parameters.getValue(CentroidMassDetectorParameters.noiseLevel);
    return new CentroidMassDetector(noiseLevel);
  }

  @Override
  public double[][] getMassValues(double[] mzs, double[] intensities,
      final @NotNull MassSpectrumType type) {
    assert mzs.length == intensities.length;
    return getMassValues(mzs, intensities, noiseLevel);
  }

  @Override
  public double[][] getMassValues(MassSpectrum spectrum) {

    // use number of centroid signals as base array list capacity
    final int points = spectrum.getNumberOfDataPoints();
    // lists of primitive doubles
    DoubleArrayList mzs = new DoubleArrayList(points);
    DoubleArrayList intensities = new DoubleArrayList(points);

    // Find possible mzPeaks
    for (int i = 0; i < points; i++) {
      // Is intensity above the noise level or m/z value corresponds to isotope mass?
      double intensity = spectrum.getIntensityValue(i);
      double mz = spectrum.getMzValue(i);
      if (intensity >= noiseLevel) {
        // Yes, then mark this index as mzPeak
        mzs.add(mz);
        intensities.add(intensity);
      }
    }
    return new double[][]{mzs.toDoubleArray(), intensities.toDoubleArray()};
  }

  @Override
  public @NotNull String getName() {
    return "Centroid";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return CentroidMassDetectorParameters.class;
  }

}
