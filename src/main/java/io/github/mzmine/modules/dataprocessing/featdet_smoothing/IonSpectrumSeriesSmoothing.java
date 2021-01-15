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
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.datamodel.featuredata.TimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

public class IonSpectrumSeriesSmoothing<T extends IonSpectrumSeries> {

  private final T origSeries;
  private final List<? extends MassSpectrum> allSpectra;
  private final MemoryMapStorage newStorage;

  /**
   * @param origSeries
   * @param newStorage
   * @param allSpectra List of all spectra/scans/frames used to build the feature. A list of these
   *                   can usually be obtained from the {@link io.github.mzmine.datamodel.features.ModularFeatureList}
   *                   via {@link io.github.mzmine.datamodel.features.ModularFeatureList#getSeletedScans(RawDataFile)}.
   *                   For ion mobility,
   */
  IonSpectrumSeriesSmoothing(T origSeries, MemoryMapStorage newStorage,
      List<? extends MassSpectrum> allSpectra) {
    this.origSeries = origSeries;
    this.newStorage = newStorage;
    this.allSpectra = allSpectra;
  }

  /**
   * Smooths the given {@link IonSpectrumSeries} with the specified weights. If no mobility
   * dimension was included in the series, the value of mobilityWeights will have no effect.
   *
   * @param rtWeights       weights for rt-intensity smoothing
   * @param mobilityWeights weights for mobility-intensity smoothing
   * @return the smoothed series.
   */
  public T smooth(@Nonnull double[] rtWeights, @Nonnull double[] mobilityWeights) {
    // smooth mobilograms in case there are any, use the mobilityWeights there.
    List<SimpleIonMobilitySeries> smoothedMobilograms = null;
    if (origSeries instanceof SimpleIonMobilogramTimeSeries) {
      smoothedMobilograms = new ArrayList<>();
      for (SimpleIonMobilitySeries mobilogram : ((SimpleIonMobilogramTimeSeries) origSeries)
          .getMobilograms()) {
        List<? extends MassSpectrum> mobilityScans = mobilogram.getSpectrum(0).getFrame()
            .getMobilityScans();
        IonSpectrumSeriesSmoothing<SimpleIonMobilitySeries> smoothing = new IonSpectrumSeriesSmoothing<>(
            mobilogram, newStorage, (List<MassSpectrum>) mobilityScans);
        smoothedMobilograms.add(smoothing.smooth(rtWeights, mobilityWeights));
      }
    }

    // this is the case if 0 was selected.
    if (rtWeights.length == 1 && origSeries instanceof TimeSeries) {
      return (T) origSeries.copy(newStorage);
    } else if (mobilityWeights.length == 1 && origSeries instanceof MobilitySeries) {
      return (T) origSeries.copy(newStorage);
    }

    int numScans = allSpectra.size();

    final double[] origIntensities = new double[numScans];
    for (int i = 0; i < numScans; i++) {
      origIntensities[i] = origSeries.getIntensityForSpectrum(allSpectra.get(i));
    }

    // use mobilityWeights for SimpleIonMobilitySeries
    final double[] weights =
        (origSeries instanceof SimpleIonMobilitySeries) ? mobilityWeights : rtWeights;
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
      return (T) ((SimpleIonTimeSeries) origSeries)
          .copyAndReplace(newStorage, origMz, newIntensities);
    } else if (origSeries instanceof SimpleIonMobilogramTimeSeries) {
      return (T) ((SimpleIonMobilogramTimeSeries) origSeries)
          .copyAndReplace(newStorage, origMz, newIntensities, smoothedMobilograms);
    } else {
      throw new IllegalArgumentException(
          "Smoothing of " + origSeries.getClass().getName() + " is not yet supported.");
    }
  }

  /**
   * Smooths the given {@link IonSpectrumSeries} with the specified weights.
   *
   * @param weights Weights in rt dimension. If the given series also posess
   * @return
   */
  public T smooth(@Nonnull double[] weights) {
    return smooth(weights, weights);
  }
}
