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

package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author https://github.com/SteffenHeu
 */
public class IonMobilogramTimeSeriesFactory {

  private IonMobilogramTimeSeriesFactory() {

  }

  /**
   * Stores a list of mobilograms. A summed intensity of each mobilogram is automatically calculated
   * and represents this series when plotted as a 2D intensity-vs time chart (accessed via {@link
   * SimpleIonMobilogramTimeSeries#getMZ(int)} and {@link SimpleIonMobilogramTimeSeries#getIntensity(int)}).
   * The mz representing a mobilogram is calculated by a weighted average based on the mzs in eah
   * mobility scan.
   *
   * @param storage     May be null if values shall be stored in ram.
   * @param mobilograms
   * @see IonMobilogramTimeSeries#copyAndReplace(MemoryMapStorage, double[], double[])
   */
  public static IonMobilogramTimeSeries of(@Nullable MemoryMapStorage storage,
      @Nonnull final List<IonMobilitySeries> mobilograms,
      @Nonnull final BinningMobilogramDataAccess mobilogramBinning) {

    double[][] summedAndWeighted = sumIntensitiesWeightMzs(mobilograms);

    mobilogramBinning.setMobilogram(mobilograms);
    final SummedIntensityMobilitySeries summedMobilogram = mobilogramBinning
        .toSummedMobilogram(storage);

    return of(storage, summedAndWeighted[0], summedAndWeighted[1], mobilograms, summedMobilogram);
  }

  public static IonMobilogramTimeSeries of(@Nullable MemoryMapStorage storage,
      @Nonnull final double[] rtMzs,
      @Nonnull final double[] rtIntensities,
      @Nonnull final List<IonMobilitySeries> mobilograms,
      @Nonnull final BinningMobilogramDataAccess mobilogramBinning) {

    mobilogramBinning.setMobilogram(mobilograms);

    return of(storage, rtMzs, rtIntensities, mobilograms,
        mobilogramBinning.toSummedMobilogram(storage));
  }

  public static IonMobilogramTimeSeries of(@Nullable MemoryMapStorage storage,
      @Nonnull final double[] rtMzs,
      @Nonnull final double[] rtIntensities,
      @Nonnull final List<IonMobilitySeries> mobilograms,
      @Nonnull final double[] mobilogramMobilities,
      @Nonnull final double[] mobilogramIntensities) {

    return of(storage, rtMzs, rtIntensities, mobilograms,
        new SummedIntensityMobilitySeries(storage, mobilogramMobilities, mobilogramIntensities));
  }

  public static IonMobilogramTimeSeries of(@Nullable MemoryMapStorage storage,
      @Nonnull final double[] rtMzs,
      @Nonnull final double[] rtIntensities,
      @Nonnull final List<IonMobilitySeries> mobilograms,
      @Nonnull final SummedIntensityMobilitySeries summedMobilogram) {

    final List<Frame> frames = new ArrayList<>(mobilograms.size());
    for (IonMobilitySeries ims : mobilograms) {
      final Frame frame = ims.getSpectra().get(0).getFrame();
      frames.add(frame);
    }

    return of(storage, rtMzs, rtIntensities, mobilograms, frames, summedMobilogram);
  }

  public static IonMobilogramTimeSeries of(@Nullable MemoryMapStorage storage,
      @Nonnull final double[] rtMzs,
      @Nonnull final double[] rtIntensities,
      @Nonnull final List<IonMobilitySeries> mobilograms,
      @Nonnull final List<Frame> frames,
      @Nonnull final SummedIntensityMobilitySeries summedMobilogram) {

    return new SimpleIonMobilogramTimeSeries(storage, rtMzs, rtIntensities, mobilograms, frames,
        summedMobilogram);
  }

  private static double[][] sumIntensitiesWeightMzs(List<IonMobilitySeries> mobilograms) {
    double[] summedIntensities = new double[mobilograms.size()];
    double[] weightedMzs = new double[mobilograms.size()];

    final CenterFunction cf = new CenterFunction(CenterMeasure.AVG, Weighting.LINEAR);

    final int maxNumDetected = mobilograms.stream()
        .mapToInt(IonMobilitySeries::getNumberOfValues).max().getAsInt();

    double[] tmpIntensities = new double[maxNumDetected];
    double[] tmpMzs = new double[maxNumDetected];

    for (int i = 0; i < mobilograms.size(); i++) {
      final IonMobilitySeries ims = mobilograms.get(i);
      final int numValues = ims.getNumberOfValues();
      ims.getIntensityValues(tmpIntensities);
      for (int j = 0; j < numValues; j++) {
        summedIntensities[i] += tmpIntensities[j];
      }
    }

    for (int i = 0; i < mobilograms.size(); i++) {
      final IonMobilitySeries ims = mobilograms.get(i);
      final int numValues = ims.getNumberOfValues();
      Arrays.fill(tmpMzs, 0, numValues - 1, 0d);
      Arrays.fill(tmpIntensities, 0, numValues - 1, 0d);
      ims.getIntensityValues(tmpIntensities);
      ims.getMzValues(tmpMzs);
      double weightedMz = 0;
      weightedMz = cf.calcCenter(tmpMzs, tmpIntensities);

//      for (int j = 0; j < numValues; j++) {
//        weightedMz += tmpMzs[j] * (tmpIntensities[j] / summedIntensities[i]);
//      }

      // due to added zeros, the summed intensity might have been 0 -> NaN
      if (Double.compare(weightedMz, Double.NaN) == 0) {
        weightedMz = 0d;
      }
      weightedMzs[i] = weightedMz;
    }
    return new double[][]{weightedMzs, summedIntensities};
  }
}
