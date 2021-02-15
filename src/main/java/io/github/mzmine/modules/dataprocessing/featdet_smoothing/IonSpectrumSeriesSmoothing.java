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
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingParameters.MobilitySmoothingType;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("unchecked")
public class IonSpectrumSeriesSmoothing<T extends IonSpectrumSeries> {

  private static Logger logger = Logger.getLogger(IonSpectrumSeriesSmoothing.class.getName());

  private final T origSeries;
  private final List<? extends MassSpectrum> allSpectra;
  private final MemoryMapStorage newStorage;
  private final MobilitySmoothingType mobilitySmoothingType;

  /**
   * @param origSeries
   * @param newStorage
   * @param allSpectra List of all spectra/scans/frames used to build the feature. A list of these
   *                   can usually be obtained from the {@link io.github.mzmine.datamodel.features.ModularFeatureList}
   *                   via {@link io.github.mzmine.datamodel.features.ModularFeatureList#getSeletedScans(RawDataFile)}.
   *                   For ion mobility,
   */
  IonSpectrumSeriesSmoothing(T origSeries, MemoryMapStorage newStorage,
      List<? extends MassSpectrum> allSpectra, MobilitySmoothingType mobilitySmoothingType) {
    this.origSeries = origSeries;
    this.newStorage = newStorage;
    this.allSpectra = allSpectra;
    this.mobilitySmoothingType = mobilitySmoothingType;
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
    List<IonMobilitySeries> smoothedMobilograms = null;
    if (origSeries instanceof SimpleIonMobilogramTimeSeries) {
      smoothedMobilograms = new ArrayList<>();
      for (IonMobilitySeries mobilogram : ((SimpleIonMobilogramTimeSeries) origSeries)
          .getMobilograms()) {
        List<? extends MassSpectrum> mobilityScans = mobilogram.getSpectrum(0).getFrame()
            .getMobilityScans();
        IonSpectrumSeriesSmoothing<IonMobilitySeries> smoothing = new IonSpectrumSeriesSmoothing<>(
            mobilogram, newStorage, (List<MassSpectrum>) mobilityScans, mobilitySmoothingType);
        smoothedMobilograms.add(smoothing.smooth(rtWeights, mobilityWeights));
      }
    }

    // this is the case if 0 was selected.
    /*if (rtWeights.length == 1 && origSeries instanceof TimeSeries) {
      return (T) origSeries.copy(newStorage);
    } else
    if (mobilityWeights.length == 1 && origSeries instanceof MobilitySeries) {
      return (T) origSeries.copy(newStorage);
    }*/

    // use mobilityWeights for SimpleIonMobilitySeries
    final double[] weights =
        (origSeries instanceof SimpleIonMobilitySeries) ? mobilityWeights : rtWeights;

    // if mobility shouldn't be smoothed, just make a copy
    /*if (weights.length == 1 && origSeries instanceof MobilitySeries
        && mobilitySmoothingType != MobilitySmoothingType.INDIVIDUAL) {
      return (T) origSeries.copy(newStorage);
    } else {
      // todo check if this is correct - if no rt smoothing is wanted, mobility shall be smoothed anyway.

    }*/

    double[] newIntensities = smoothIntensities(origSeries, weights);
    double[] origMz = DataPointUtils.getDoubleBufferAsArray(origSeries.getMZValues());
    if (origSeries instanceof SimpleIonMobilitySeries) {
      return (T) new SimpleIonMobilitySeries(newStorage, origMz, newIntensities,
          origSeries.getSpectra());
    } else if (origSeries instanceof SimpleIonTimeSeries) {
      return (T) ((SimpleIonTimeSeries) origSeries)
          .copyAndReplace(newStorage, origMz, newIntensities);
    } else if (origSeries instanceof SimpleIonMobilogramTimeSeries) {
      // todo recalc the summed series, if individual mobilograms were smoothed
      return (T) ((SimpleIonMobilogramTimeSeries) origSeries)
          .copyAndReplace(newStorage, origMz, newIntensities, smoothedMobilograms,
              smoothSummedMobilogram(mobilityWeights));
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

  private List<? extends MassSpectrum> findEligibleSpectra(List<? extends MassSpectrum> allSpectra,
      List<? extends MassSpectrum> featureSpectra, int filtersize) {
    int firstSpectrumIndex = allSpectra.indexOf(featureSpectra.get(0));
    int lastSpectrumIndex = allSpectra.indexOf(featureSpectra.get(featureSpectra.size() - 1));

    if (firstSpectrumIndex - filtersize > 0) {
      firstSpectrumIndex -= filtersize;
    } else {
      firstSpectrumIndex = 0;
    }

    if (lastSpectrumIndex + filtersize < allSpectra.size()) {
      lastSpectrumIndex += filtersize;
    } else {
      lastSpectrumIndex = allSpectra.size() - 1;
    }

    return allSpectra.subList(firstSpectrumIndex, lastSpectrumIndex);
  }

  /**
   * This method handles the case that mobility smoothing was either off, in which case the original
   * intensities are used, or that mobility smoothing was set to smooth the summed series, in which
   * case the smoothed intensities are calculated here.
   *
   * @param mobilityWeights
   * @return
   */
  @Nullable
  private double[] smoothSummedMobilogram(double[] mobilityWeights) {
    if (mobilitySmoothingType == MobilitySmoothingType.INDIVIDUAL && mobilityWeights.length != 1) {
      // individual series were smoothed, we need to recalculate.
      return null;
    }
    double[] intensities = DataPointUtils.getDoubleBufferAsArray(
        ((SimpleIonMobilogramTimeSeries) origSeries).getSummedMobilogram().getIntensityValues());
    if (mobilityWeights.length == 1) {
      // rt was smoothed, nothing to do here, return the original intensities.
      return intensities;
    }
    double[] smoothedSummedMobilogram = SavitzkyGolayFilter.convolve(intensities, mobilityWeights);
    if (mobilitySmoothingType == MobilitySmoothingType.SUMMED && mobilityWeights.length > 1) {
      for (int i = 0; i < smoothedSummedMobilogram.length; i++) {
        if (intensities[i] == 0 || smoothedSummedMobilogram[i] < 0) {
          smoothedSummedMobilogram[i] = 0d;
        }
      }
    } else {
      logger.warning("Unexpected behaviour during smoothing, please contact the developers.");
    }

    return smoothedSummedMobilogram;
  }


  private double[] smoothIntensities(T origSeries, double[] weights) {

    if (weights.length == 1) {
      return DataPointUtils.getDoubleBufferAsArray(origSeries.getIntensityValues());
    }

    if (mobilitySmoothingType == MobilitySmoothingType.SUMMED
        && origSeries instanceof SimpleIonMobilitySeries) {
      return DataPointUtils.getDoubleBufferAsArray(origSeries.getIntensityValues());
    }

    final double[] newIntensities = new double[origSeries.getNumberOfValues()];
    List<MassSpectrum> eligibleSpectra = findEligibleSpectra(allSpectra,
        origSeries.getSpectra(), weights.length);

    int numScans = eligibleSpectra.size();

    // add no new data points for scans, just use smoothed ones where we had dps in the first place
    final double[] origIntensities = new double[numScans];
    for (int i = 0; i < numScans; i++) {
      origIntensities[i] = origSeries.getIntensityForSpectrum(eligibleSpectra.get(i));
    }

    final double[] smoothed = SavitzkyGolayFilter.convolve(origIntensities, weights);

    int newIntensityIndex = 0;
    List<MassSpectrum> origSpectra = origSeries.getSpectra();
    for (int i = 0; i < numScans; i++) {
      // check if we originally had a data point for that spectrum, otherwise continue.
      int index = origSpectra.indexOf(eligibleSpectra.get(i));
      if (index == -1) {
        continue;
      }
      // smoothing might produce negative intensities
      smoothed[i] = (smoothed[i] > 0) ? smoothed[i] : 0d;
      // keep zeros we put on flanking data points during chromatogram building
      newIntensities[newIntensityIndex] =
          (Double.compare(origSeries.getIntensity(index), 0d) != 0) ? smoothed[i] : 0d;
      newIntensityIndex++;
      // once we found all values, we can stop
      if (newIntensityIndex >= origSpectra.size()) {
        break;
      }
    }

    return newIntensities;
  }
}
