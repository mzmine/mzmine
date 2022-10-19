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

/**
 * Used to build chromatograms with a predefined number of scans that span the whole range. Can be
 * converted to an {@link IonTimeSeries}
 */
public class SimpleFullChromatogram {

  private final double[] mzs;
  private final double[] intensities;
  private final IntensityMode mode;
  private final int[] dataPoints;

  /**
   * @param numberOfScans all scans that this chromatogram spans (e.g., all MS1 positive mode)
   * @param mode          defines how to merge intensity values
   */
  public SimpleFullChromatogram(int numberOfScans, IntensityMode mode) {
    mzs = new double[numberOfScans];
    intensities = new double[numberOfScans];
    dataPoints = new int[numberOfScans];
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
   * @return an ion time series with only data points > 0 with 1 leading and trailing zero around
   * detected data points if in scan range
   */
  public IonTimeSeries<? extends Scan> toIonTimeSeries(@Nullable MemoryMapStorage storage,
      final List<Scan> scans) {
    if (mzs.length == 0) {
      return IonTimeSeries.EMPTY;
    }

    DoubleArrayList fmzs = new DoubleArrayList();
    DoubleArrayList fintensities = new DoubleArrayList();
    List<Scan> fscans = new ArrayList<>();

    int wasZero = 0;
    for (int i = 0; i < scans.size(); i++) {
      if (intensities[i] == 0) {
        wasZero++;
        // add first zero after data
        if (wasZero == 1) {
          addDP(scans, fmzs, fintensities, fscans, i);
        }
      } else {
        // add one leading 0 if available
        if (wasZero > 0) {
          addDP(scans, fmzs, fintensities, fscans, i - 1);
        }

        addDP(scans, fmzs, fintensities, fscans, i);
        wasZero = 0;
      }
    }

    return new SimpleIonTimeSeries(storage, fmzs.toDoubleArray(), fintensities.toDoubleArray(),
        fscans);
  }

  private void addDP(final List<Scan> scans, final DoubleArrayList fmzs,
      final DoubleArrayList fintensities, final List<Scan> fscans, final int i) {
    fmzs.add(mzs[i]);
    fintensities.add(intensities[i]);
    fscans.add(scans.get(i));
  }

  /**
   * @param scans the original full list of scans that was used to create this chromatogram
   * @return an ion time series that spans all scans
   */
  public IonTimeSeries<? extends Scan> toFullIonTimeSeries(@Nullable MemoryMapStorage storage,
      final List<Scan> scans) {
    return new SimpleIonTimeSeries(storage, mzs, intensities, scans);
  }

  /**
   * Add value and directly compute the new value
   *
   * @param scanIndex the index of the current scan in all selected scans
   * @param mz        the mz to add
   * @param intensity the intensity to add (based on the defined IntensityMode)
   * @return false if mode was {@link IntensityMode#HIGHEST} and the new signal was lower than the
   * already existing value
   */
  public boolean addValue(int scanIndex, double mz, double intensity) {
    return switch (mode) {
      case HIGHEST -> {
        if (intensity > intensities[scanIndex]) {
          intensities[scanIndex] = intensity;
          mzs[scanIndex] = mz;
          yield true;
        } else {
          yield false;
        }
      }
      case SUM -> {
        int n = dataPoints[scanIndex];
        intensities[scanIndex] += intensity;
        mzs[scanIndex] = (mzs[scanIndex] * n + mz) / (n + 1);
        dataPoints[scanIndex] += 1;
        yield true;
      }
      case MEAN -> {
        int n = dataPoints[scanIndex];
        intensities[scanIndex] = (intensities[scanIndex] * n + intensity) / (n + 1);
        mzs[scanIndex] = (mzs[scanIndex] * n + mz) / (n + 1);
        dataPoints[scanIndex] += 1;
        yield true;
      }
    };
  }

  public enum IntensityMode {
    HIGHEST, SUM, MEAN
  }

}
