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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.FeatureFullDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.ModifiableSpectra;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SmoothingAlgorithm extends MZmineModule {

  /**
   * @param series The series.
   * @return The smoothed intensity values.
   */
  @Nullable
  public <T extends Scan> double[] smoothRt(@NotNull final IonTimeSeries<T> series);

  /**
   * @param mobilogram The mobilogram.
   * @return The smoothed intensity values.
   */
  @Nullable
  public <T extends IntensitySeries & MobilitySeries> double[] smoothMobility(
      @NotNull final T mobilogram);

  /**
   * Creates a new smoothed series for the given feature. The intensities are smoothed with the
   * settings of this {@link SmoothingAlgorithm}. Smooths intensity in rt and mobility dimension.
   *
   * @return The smoothed series.
   */
  public default IonTimeSeries<? extends Scan> smoothFeature(
      @Nullable final MemoryMapStorage storage, @NotNull final IonTimeSeries<?> dataAccess,
      @NotNull final Feature feature, ZeroHandlingType zht) {

    double[] smoothedIntensities = this.smoothRt(dataAccess);

    final IonTimeSeries<? extends Scan> originalSeries = feature.getFeatureData();
    final double[] originalIntensities = new double[originalSeries.getNumberOfValues()];
    final double[] newIntensities;

    originalSeries.getIntensityValues(originalIntensities);

    if (smoothedIntensities == null) {
      // rt should not be smoothed, so just copy the old values.
      newIntensities = originalIntensities;
    } else {
      newIntensities = new double[originalSeries.getNumberOfValues()];
      int newIntensitiesIndex = 0;
      for (int i = 0; i < dataAccess.getNumberOfValues() && newIntensitiesIndex < originalSeries
          .getNumberOfValues(); i++) {
        // check if we originally did have an intensity at the current index. I know that the data
        // access contains more zeros and the zeros of different indices will be matched, but the
        // newIntensitiesIndex will "catch" up, once real intensities are reached.
        if (Double.compare(dataAccess.getIntensity(i), originalIntensities[newIntensitiesIndex])
            == 0) {
          newIntensities[newIntensitiesIndex] = smoothedIntensities[i];
          newIntensitiesIndex++;
        }
        if (newIntensitiesIndex == originalIntensities.length - 1) {
          break;
        }
      }
    }

    double[] originalMzs = new double[originalSeries.getNumberOfValues()];
    originalSeries.getMzValues(originalMzs);
    if (originalSeries instanceof IonMobilogramTimeSeries imts) {
      final SummedIntensityMobilitySeries smoothedMobilogram = smoothSummedMobilogram(storage,
          imts.getSummedMobilogram());
      return new SimpleIonMobilogramTimeSeries(storage, originalMzs, newIntensities,
          ((SimpleIonMobilogramTimeSeries) originalSeries).getMobilogramsModifiable(),
          ((ModifiableSpectra) originalSeries).getSpectraModifiable(), smoothedMobilogram);
    }

    return (IonTimeSeries<? extends Scan>) originalSeries
        .copyAndReplace(storage, originalMzs, newIntensities);
  }

  /**
   * @param storage        The storage.
   * @param originalSeries The series to smooth.
   * @return The smoothed series or the original one, if no smoothing shall be applied.
   */
  public default <T extends IntensitySeries & MobilitySeries> T smoothSummedMobilogram(
      @Nullable MemoryMapStorage storage, @NotNull final T originalSeries) {
    var smoothed = smoothMobility(originalSeries);

    if (smoothed != null) {
      double[] mobilities = IonMobilityUtils.extractMobilities(originalSeries);
      if (originalSeries instanceof SummedIntensityMobilitySeries summed) {
        return (T) new SummedIntensityMobilitySeries(storage, mobilities, smoothed);
      } else {
        throw new IllegalArgumentException(
            "Mobility series type " + originalSeries.getClass().getSimpleName()
                + " has not been implemented for smoothing yet.");
      }
    }

    return originalSeries;
  }

  private double[] getOriginalIntensities(IntensitySeries series) {
    if (series instanceof FeatureFullDataAccess access) {
      return access.getIntensityValues();
    } else {
      double[] originalIntensities = new double[series.getNumberOfValues()];
      return series.getIntensityValues(originalIntensities);
    }
  }
}
