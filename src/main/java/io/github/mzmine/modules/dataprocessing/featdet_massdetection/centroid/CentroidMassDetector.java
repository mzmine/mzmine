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
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;

/**
 * Remove peaks below the given noise level.
 */
public class CentroidMassDetector implements MassDetector {

  @Override
  public double[][] getMassValues(MassSpectrum spectrum, ParameterSet parameters) {

    double noiseLevel =
        parameters.getParameter(CentroidMassDetectorParameters.noiseLevel).getValue();

    // use number of centroid signals as base array list capacity
    int initialSize = spectrum.getNumberOfDataPoints();
    // lists of primitive doubles
    TDoubleArrayList mzs = new TDoubleArrayList(initialSize);
    TDoubleArrayList intensities = new TDoubleArrayList(initialSize);

    // Find possible mzPeaks
    int points = spectrum.getNumberOfDataPoints();
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
  public @Nonnull String getName() {
    return "Centroid";
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return CentroidMassDetectorParameters.class;
  }

}
