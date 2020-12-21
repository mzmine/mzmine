/*
 *
 *  * Copyright 2006-2020 The MZmine Development Team
 *  *
 *  * This file is part of MZmine.
 *  *
 *  * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  * General Public License as published by the Free Software Foundation; either version 2 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  * Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  * USA
 *
 *
 */

package io.github.mzmine.project.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.MobilityDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import java.awt.Color;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class StorableMobilogram implements Mobilogram {

  private static NumberFormat mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
  private static NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final IMSRawDataFileImpl rawDataFile;
  private final MobilityType mt;
  private final double mobility;
  private final double mz;
  private final double maximumIntensity;
  private final Range<Double> mobilityRange;
  private final Range<Double> mzRange;
  private final int storageID;
  private final int highestDataPointScanNum;

  public StorableMobilogram(Mobilogram originalMobilogram, @Nonnull IMSRawDataFileImpl rawDatafile,
      int storageID) {
    this.mt = originalMobilogram.getMobilityType();
    this.rawDataFile = rawDatafile;
    this.mz = originalMobilogram.getMZ();
    this.maximumIntensity = originalMobilogram.getMaximumIntensity();
    this.mobility = originalMobilogram.getMobility();
    this.highestDataPointScanNum = originalMobilogram.getHighestDataPoint().getScanNum();
    this.storageID = storageID;
    this.mobilityRange = originalMobilogram.getMobilityRange();
    this.mzRange = originalMobilogram.getMZRange();
  }

  /**
   * @return the median mz
   */
  @Override
  public double getMZ() {
    return mz;
  }

  /**
   * @return the median mobility
   */
  @Override
  public double getMobility() {
    return mobility;
  }

  @Override
  public double getMaximumIntensity() {
    return maximumIntensity;
  }

  @Override
  public Range<Double> getMZRange() {
    return mzRange;
  }

  @Override
  public Range<Double> getMobilityRange() {
    return mobilityRange;
  }

  @Nonnull
  @Override
  public List<MobilityDataPoint> getDataPoints() {
    try {
      return rawDataFile.loadDatapointsForMobilogram(this.storageID);
    } catch (IOException e) {
      e.printStackTrace();
      return Collections.emptyList();
    }
  }

  @Nonnull
  @Override
  public MobilityDataPoint getHighestDataPoint() {
    return getDataPoints().stream().filter(dp -> dp.getScanNum() == highestDataPointScanNum)
        .findFirst().get();
  }

  @Nonnull
  @Override
  public List<Integer> getMobilityScanNumbers() {
    return getDataPoints().stream().mapToInt(MobilityDataPoint::getScanNum).boxed()
        .collect(Collectors.toList());
  }

  @Override
  public MobilityType getMobilityType() {
    return mt;
  }

  @Override
  public String representativeString() {
    return mzFormat.format(mzRange.lowerEndpoint()) + " - " + mzFormat
        .format(mzRange.upperEndpoint())
        + " @" + mobilityFormat.format(getMobility()) + " " + getMobilityType().getUnit() + " ("
        + getDataPoints().size() + ")";
  }

  @Override
  @Nonnull
  public IMSRawDataFile getRawDataFile() {
    return rawDataFile;
  }

  public int getStorageID() {
    return storageID;
  }
}