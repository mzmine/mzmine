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

import java.util.ArrayList;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;

/**
 * Remove peaks below the given noise level.
 */
public class CentroidMassDetector implements MassDetector {

  @Override
  public DataPoint[] getMassValues(MassSpectrum spectrum, ParameterSet parameters) {

    double noiseLevel =
        parameters.getParameter(CentroidMassDetectorParameters.noiseLevel).getValue();

    ArrayList<DataPoint> mzPeaks = new ArrayList<DataPoint>();

    // Find possible mzPeaks
    for (int i = 0; i < spectrum.getNumberOfDataPoints(); i++) {

      // Is intensity above the noise level?
      if (spectrum.getIntensityValue(i) >= noiseLevel) {
        // Yes, then mark this index as mzPeak
        DataPoint newDP =
            new SimpleDataPoint(spectrum.getMzValue(i), spectrum.getIntensityValue(i));
        mzPeaks.add(newDP);
      }
    }
    return mzPeaks.toArray(new DataPoint[0]);
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
