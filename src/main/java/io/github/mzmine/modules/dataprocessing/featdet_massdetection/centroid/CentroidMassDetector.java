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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid;

import gnu.trove.list.array.TDoubleArrayList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import javax.annotation.Nonnull;

/**
 * Remove peaks below the given noise level.
 */
public class CentroidMassDetector implements MassDetector {

  @Override
  public double[][] getMassValues(MassSpectrum spectrum, ParameterSet parameters) {

    final double noiseLevel =
        parameters.getParameter(CentroidMassDetectorParameters.noiseLevel).getValue();

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
        // Yes, then mark this index as mzPeak
        mzs.add(spectrum.getMzValue(i));
        intensities.add(intensity);
      }
    }
    return new double[][]{mzs.toArray(), intensities.toArray()};
  }

  @Override
  public double[][] getMassValues(double[] mzs, double[] intensities, ParameterSet parameters) {
    assert mzs.length == intensities.length;

    final double noiseLevel =
        parameters.getParameter(CentroidMassDetectorParameters.noiseLevel).getValue();

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

  @Override
  public @Nonnull String getName() {
    return "Centroid";
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return CentroidMassDetectorParameters.class;
  }

}
