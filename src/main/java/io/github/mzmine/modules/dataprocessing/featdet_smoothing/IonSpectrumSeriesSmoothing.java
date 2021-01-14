/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;

public class IonSpectrumSeriesSmoothing<T extends IonSpectrumSeries> {

  private final T origSeries;
  private final List<MassSpectrum> allSpectra;
  private final MemoryMapStorage newStorage;

  IonSpectrumSeriesSmoothing(T origSeries, MemoryMapStorage newStorage,
      List<MassSpectrum> allSpectra) {
    if (origSeries instanceof SimpleIonMobilogramTimeSeries) {
      throw new IllegalArgumentException(
          "Direct smoothing of " + SimpleIonMobilogramTimeSeries.class.getName()
              + " is not supported, since averaged intensities and mobilities are calculated based "
              + "on the individual " + SimpleIonMobilitySeries.class.getName()
              + ". Smooth those instead.");
    }
    this.origSeries = origSeries;
    this.newStorage = newStorage;
    this.allSpectra = allSpectra;
  }

  public T smooth(double[] weights) {
    int numScans = allSpectra.size();

    final double[] origIntensities = new double[numScans];
    for (int i = 0; i < numScans; i++) {
      origIntensities[i] = origSeries.getIntensityForSpectrum(allSpectra.get(i));
    }

    final double[] smoothed = SavitzkyGolayFilter.convolve(origIntensities, weights);

    // add no new data points for scans, just use smoothed ones where we had dps in the first place
    final double[] newIntensities = new double[origSeries.getNumberOfValues()];
    int newIntensityIndex = 0;
    List<MassSpectrum> origSpectra = origSeries.getSpectra();
    for (int i = 0; i < numScans; i++) {
      // check if we originally had a data point for that spectrum, otherwise continue.
      int index = origSpectra.indexOf(allSpectra.get(i));
      if (index == -1) {
        continue;
      }
      // keep zeros we put on flanking data points during chromatogram building
      newIntensities[newIntensityIndex] =
          (Double.compare(origSeries.getIntensity(index), 0d) != 0) ? smoothed[i] : 0d;
      newIntensityIndex++;
    }

    double[] origMz = DataPointUtils.getDoubleBufferAsArray(origSeries.getMZValues());
    if (origSeries instanceof SimpleIonMobilitySeries) {
      return (T) new SimpleIonMobilitySeries(newStorage, origMz, newIntensities,
          origSeries.getSpectra());
    } else if (origSeries instanceof SimpleIonTimeSeries) {
      return (T) new SimpleIonTimeSeries(newStorage, origMz, newIntensities,
          origSeries.getSpectra());
    } else {
      throw new IllegalArgumentException(
          "Smoothing of " + origSeries.getClass().getName() + " is not yet supported.");
    }
  }
}
