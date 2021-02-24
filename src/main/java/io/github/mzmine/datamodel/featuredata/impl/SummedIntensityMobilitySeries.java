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

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;

/**
 * Stores a summed mobilogram based on the intesities of the frame-specific mobilograms in the
 * constructor.
 *
 * @author https://github.com/SteffenHeu
 */
public class SummedIntensityMobilitySeries implements IntensitySeries, MobilitySeries {

  final double mz;
  final DoubleBuffer intensityValues;
  final DoubleBuffer mobilityValues;

  /**
   * Creates a summed intensity and mobility series
   *
   * @param storage     May be null, if values shall be stored in ram.
   * @param mobilograms
   * @param mz
   */
  public SummedIntensityMobilitySeries(@Nullable MemoryMapStorage storage,
      List<IonMobilitySeries> mobilograms,
      double mz) {

    this.mz = mz;
    Frame exampleFrame = mobilograms.get(0).getSpectra().get(0).getFrame();
    double smallestDelta = IonMobilityUtils
        .getSmallestMobilityDelta(exampleFrame);

    // we want to preserve the order of mobilities as it is ordered in the Frame.
    boolean ascendingMobility = exampleFrame.getMobilityType() != MobilityType.TIMS;
    RangeMap<Double, Double> mobilityIntensityValues = TreeRangeMap.create();

    for (int i = 0; i < mobilograms.size(); i++) {
      IonMobilitySeries mobilogram = mobilograms.get(i);
      for (int j = 0; j < mobilogram.getNumberOfValues(); j++) {
        double intensity = mobilogram.getIntensity(j);
        double mobility = mobilogram.getMobility(j);
        Entry<Range<Double>, Double> entry = mobilityIntensityValues.getEntry(mobility);
        if (entry != null) {
          mobilityIntensityValues.put(entry.getKey(), entry.getValue() + intensity);
        } else {
          mobilityIntensityValues
              .put(Range.open(mobility - smallestDelta / 2, mobility + smallestDelta / 2),
                  intensity);
        }
      }
    }

    // we want to preserve the order of mobilities as it is ordered in the Frame.
    Map<Range<Double>, Double> mapOfRanges =
        ascendingMobility ? mobilityIntensityValues.asMapOfRanges()
            : mobilityIntensityValues.asDescendingMapOfRanges();

    double[] mobilities = mapOfRanges.keySet().stream()
        .mapToDouble(key -> (key.upperEndpoint() + key.lowerEndpoint()) / 2).toArray();
    double[] intensities = mapOfRanges.values().stream().mapToDouble(Double::doubleValue).toArray();

    DoubleBuffer tempMobility;
    DoubleBuffer tempIntensities;
    if (storage != null) {
      try {
        tempMobility = storage.storeData(mobilities);
        tempIntensities = storage.storeData(intensities);
      } catch (IOException e) {
        tempMobility = DoubleBuffer.wrap(mobilities);
        tempIntensities = DoubleBuffer.wrap(intensities);
        e.printStackTrace();
      }
    } else {
      tempMobility = DoubleBuffer.wrap(mobilities);
      tempIntensities = DoubleBuffer.wrap(intensities);
    }
    mobilityValues = tempMobility;
    intensityValues = tempIntensities;
  }

  /**
   * Package private use intended. Can be created by a {@link SimpleIonMobilogramTimeSeries} after
   * smoothing.
   *
   * @param storage
   * @param mobilities
   * @param intensities
   * @param mz
   */
  SummedIntensityMobilitySeries(MemoryMapStorage storage, double[] mobilities,
      double[] intensities, double mz) {
    this.mz = mz;
    DoubleBuffer tempMobility;
    DoubleBuffer tempIntensities;
    if (storage != null) {
      try {
        tempMobility = storage.storeData(mobilities);
        tempIntensities = storage.storeData(intensities);
      } catch (IOException e) {
        tempMobility = DoubleBuffer.wrap(mobilities);
        tempIntensities = DoubleBuffer.wrap(intensities);
        e.printStackTrace();
      }
    } else {
      tempMobility = DoubleBuffer.wrap(mobilities);
      tempIntensities = DoubleBuffer.wrap(intensities);
    }
    this.mobilityValues = tempMobility;
    this.intensityValues = tempIntensities;
  }

  public int getNumberOfDataPoints() {
    return getMobilityValues().capacity();
  }

  public double getIntensity(int index) {
    return getIntensityValues().get(index);
  }

  /**
   * Note: Since this is a summed mobilogram, the data points were summed at a given mobility, not
   * necessarily at the same mobility scan number. Therefore, a list of scans is not provided.
   *
   * @param index
   * @return
   */
  public double getMobility(int index) {
    return getMobilityValues().get(index);
  }

  public DoubleBuffer getIntensityValues() {
    return intensityValues;
  }

  public DoubleBuffer getMobilityValues() {
    return mobilityValues;
  }

  @Override
  public double[] getIntensityValues(double[] dst) {
    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    intensityValues.get(0, dst);
    return dst;
  }

  public double getMZ() {
    return mz;
  }

}
