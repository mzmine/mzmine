/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_msn_tree;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class SimpleFullChromatogram {

  private final double[] mzs;
  private final double[] intensities;
  private final IntensityMode mode;
  private final int[] dataPoints;

  public SimpleFullChromatogram(int n, IntensityMode mode) {
    mzs = new double[n];
    intensities = new double[n];
    dataPoints = new int[n];
    this.mode = mode;
  }

  public double[] getIntensities() {
    return intensities;
  }

  public double[] getMzs() {
    return mzs;
  }

  public int[] getDataPoints() {
    return dataPoints;
  }

  public IntensityMode getMode() {
    return mode;
  }

  /**
   * @param scans the original full list of scans that was used to create this chromatogram
   * @return an ion time series with only data points > 0
   */
  public IonTimeSeries<? extends Scan> toIonTimeSeries(@Nullable MemoryMapStorage storage,
      final List<Scan> scans) {
    DoubleArrayList fmzs = new DoubleArrayList();
    DoubleArrayList fintensities = new DoubleArrayList();
    List<Scan> fscans = new ArrayList<>();
    for (int i = 0; i < scans.size(); i++) {
      if (intensities[i] > 0) {
        fmzs.add(mzs[i]);
        fintensities.add(intensities[i]);
        fscans.add(scans.get(i));
      }
    }

    return new SimpleIonTimeSeries(storage, fmzs.toDoubleArray(), fintensities.toDoubleArray(),
        fscans);
  }

  /**
   * @param scans the original full list of scans that was used to create this chromatogram
   * @return an ion time series with only data points > 0
   */
  public IonTimeSeries<? extends Scan> toFullIonTimeSeries(@Nullable MemoryMapStorage storage,
      final List<Scan> scans) {
    return new SimpleIonTimeSeries(storage, mzs, intensities, scans);
  }

  public boolean addValue(int index, double mz, double intensity) {
    return switch (mode) {
      case HIGHEST -> {
        if (intensity > intensities[index]) {
          intensities[index] = intensity;
          mzs[index] = mz;
          yield true;
        } else {
          yield false;
        }
      }
      case SUM -> {
        int n = dataPoints[index];
        intensities[index] += intensity;
        mzs[index] = (mzs[index] * n + mz) / (n + 1);
        dataPoints[index] += 1;
        yield true;
      }
      case MEAN -> {
        int n = dataPoints[index];
        intensities[index] = (intensities[index] * n + intensity) / (n + 1);
        mzs[index] = (mzs[index] * n + mz) / (n + 1);
        dataPoints[index] += 1;
        yield true;
      }
    };
  }

  public enum IntensityMode {
    HIGHEST, SUM, MEAN
  }

}
