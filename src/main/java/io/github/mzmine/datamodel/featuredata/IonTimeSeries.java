/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.featuredata;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to store a consecutive number of data points (mz and intensity values).
 *
 * @param <T>
 * @author https://github.com/SteffenHeu
 */
public interface IonTimeSeries<T extends Scan> extends IonSpectrumSeries<T>, TimeSeries {

  /**
   * @param scan
   * @return The intensity value for the given scan or 0 if the no intensity was measured at that
   * scan.
   */
  @Override
  default double getIntensityForSpectrum(Scan scan) {
    int index = getSpectra().indexOf(scan);
    if (index != -1) {
      return getIntensity(index);
    }
    return 0;
  }

  /**
   * @param scan
   * @return The mz for the given scan or 0 if no intensity was measured at that scan.
   */
  @Override
  default double getMzForSpectrum(Scan scan) {
    int index = getSpectra().indexOf(scan);
    if (index != -1) {
      return getMZ(index);
    }
    return 0;
  }

  @Override
  IonTimeSeries<T> subSeries(@Nullable MemoryMapStorage storage, @NotNull List<T> subset);

  @Override
  IonSpectrumSeries<T> copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull double[] newMzValues, @NotNull double[] newIntensityValues);

  /**
   * Remaps the values of the given series onto another set of scans to gain access to all RT
   * values. This should only be used for preview purposes, since buffers cannot be reused. If a set
   * of features is processed, a {@link io.github.mzmine.datamodel.data_access.FeatureDataAccess}
   * should be used.
   *
   * @param series   The series.
   * @param newScans The scans. Have to contain all scans in the series.
   * @return A {@link SimpleIonTimeSeries} with the new rt values.
   * @throws IllegalStateException If newScans did not contain all scans in the series.
   */
  static IonTimeSeries<Scan> remapRtAxis(@NotNull final IonTimeSeries<? extends Scan> series,
      @NotNull final List<? extends Scan> newScans) {
    assert series.getNumberOfValues() <= newScans.size();

    int seriesIndex = 0;
    final double[] newIntensities = new double[newScans.size()];
    final double avgMz = MathUtils
        .calcAvg(DataPointUtils.getDoubleBufferAsArray(series.getMZValues()));
    final double[] newMzs = new double[newScans.size()];
    Arrays.fill(newMzs, avgMz);

    for (int i = 0; i < newScans.size() && seriesIndex < series.getNumberOfValues(); i++) {
      if (series.getSpectrum(seriesIndex).equals(newScans.get(i))) {
        newIntensities[i] = series.getIntensity(seriesIndex);
        newMzs[i] = series.getIntensity(seriesIndex);
        seriesIndex++;
      } else {
        newIntensities[i] = 0d;
      }
    }
    if (seriesIndex < series.getNumberOfValues()) {
      throw new IllegalStateException(
          "Incomplete rt remap. newScans did not contain all scans in the series.");
    }

    return new SimpleIonTimeSeries(null, newMzs, newIntensities, (List<Scan>) newScans);
  }
}
