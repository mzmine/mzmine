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

package io.github.mzmine.datamodel.data_access;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.RangeUtils;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class SummedMobilogramDataAccess implements IntensitySeries, MobilitySeries {

  private final RangeMap<Double, Double> mobilityToIntensity = TreeRangeMap.create();
  private final Map<Range<Double>, Double> mapOfRanges;
  private final IMSRawDataFile dataFile;

  private final double[] intensities;
  private final double[] mobilities;
  private double mz;

  public SummedMobilogramDataAccess(@Nonnull final IMSRawDataFile rawDataFile) {
    this.dataFile = rawDataFile;
    final Double maxTic = rawDataFile.getDataMaxTotalIonCurrent(1);
    assert maxTic != null && !maxTic.isNaN();

    final List<Frame> frames = rawDataFile.getFrames(1).stream()
        .filter(frame -> frame.getTIC() >= maxTic * 0.8).limit(10).collect(
            Collectors.toList());
    final double smallestDelta = frames.stream()
        .mapToDouble(IonMobilityUtils::getSmallestMobilityDelta).min().orElse(-1);

    assert Double.compare(-1, smallestDelta) != 0;

    // make a range map that contains all mobility values we could find
    for (final Frame frame : frames) {
      double[] mobilities = DataPointUtils.getDoubleBufferAsArray(frame.getMobilities());
      for (int i = 0; i < mobilities.length; i++) {
        Entry<Range<Double>, Double> entry = mobilityToIntensity.getEntry(mobilities[i]);
        if (entry == null) {
          mobilityToIntensity
              .put(Range
                      .closed(mobilities[i] - smallestDelta / 2, mobilities[i] + smallestDelta / 2),
                  0d);
        }
      }
    }

    mapOfRanges = mobilityToIntensity.asMapOfRanges();
    final int numEntries = mapOfRanges.size();
    intensities = new double[numEntries];
    mobilities = new double[numEntries];

    int i = 0;
    for (Range<Double> doubleRange : mapOfRanges.keySet()) {
      mobilities[i] = RangeUtils.rangeCenter(doubleRange);
      i++;
    }

  }

  private void clearIntensities() {
//    mapOfRanges.entrySet().forEach(entry -> entry.setValue(0d));
    for (int i = 0; i < mobilities.length; i++) {
      var entry = mobilityToIntensity.getEntry(mobilities[i]);
      mobilityToIntensity.put(entry.getKey(), 0d);
    }
  }

  public void setSummedMobilogram(@Nonnull final SummedIntensityMobilitySeries summedMobilogram) {
    clearIntensities();
    for (int i = 0; i < summedMobilogram.getNumberOfValues(); i++) {
      final Entry<Range<Double>, Double> entry = mobilityToIntensity
          .getEntry(summedMobilogram.getMobility(i));
      if (entry == null) {
        throw new RuntimeException(
            "Summed mobilogram contains a mobility value not being contained in the raw data file.");
      }
      double intensity = summedMobilogram.getIntensity(i);
      mobilityToIntensity.put(entry.getKey(), intensity);
    }

    int i = 0;
    for (Double value : mapOfRanges.values()) {
      intensities[i] = value;
      i++;
    }
    mz = summedMobilogram.getMZ();
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    throw new UnsupportedOperationException(
        "This data access is designed to loop over intensities/mobilities.");
  }

  @Override
  public double[] getIntensityValues(double[] dst) {
    throw new UnsupportedOperationException(
        "This data access is designed to loop over intensities/mobilities.");
  }

  @Override
  public double getIntensity(int index) {
    return intensities[index];
  }

  @Override
  public int getNumberOfValues() {
    return intensities.length;
  }

  @Override
  public double getMobility(int index) {
    return mobilities[index];
  }

  public double getMz() {
    return mz;
  }

  public IMSRawDataFile getDataFile() {
    return dataFile;
  }
}
