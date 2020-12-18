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
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityMassSpectrum;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StorableMobilityMassSpectrum implements MobilityMassSpectrum {

  private final Frame frame;
  private final DataPoint highestDataPoint;
  private final double totalIonCount;
  private final int spectrumNumber;
  private Range<Double> dataPointsMzRange;
  private final int storageId;

  public StorableMobilityMassSpectrum(MobilityMassSpectrum originalSpectrum,
      final int storageId) {
    this.frame = originalSpectrum.getFrame();
    this.totalIonCount = originalSpectrum.getTIC();
    this.highestDataPoint = originalSpectrum.getHighestDataPoint();
    this.spectrumNumber = originalSpectrum.getSpectrumNumber();
    this.storageId = storageId;
  }

  @Nonnull
  @Override
  public Range<Double> getDataPointMZRange() {
    if (dataPointsMzRange == null) {
      dataPointsMzRange = ScanUtils.findMzRange(getDataPoints());
    }
    return dataPointsMzRange;
  }

  @Nullable
  @Override
  public DataPoint getHighestDataPoint() {
    return highestDataPoint;
  }

  @Override
  public double getTIC() {
    return totalIonCount;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return frame.getSpectrumType();
  }

  @Override
  public int getNumberOfDataPoints() {
    return ((IMSRawDataFileImpl)getFrame().getDataFile()).getDataPointsLengths().get(storageId);
  }

  @Nonnull
  @Override
  public DataPoint[] getDataPoints() {
    try {
      return ((IMSRawDataFileImpl)frame.getDataFile()).readDataPoints(this.storageId);
    } catch (IOException e) {
      e.printStackTrace();
      return new DataPoint[0];
    }
  }

  @Nonnull
  @Override
  public DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange) {
    return ScanUtils.getDataPointsByMass(getDataPoints(), mzRange);
  }

  @Nonnull
  @Override
  public DataPoint[] getDataPointsOverIntensity(double intensity) {
    return ScanUtils.getFiltered(getDataPoints(), intensity);
  }

  @Override
  public double getMobility() {
    return frame.getMobilityForSubSpectrum(spectrumNumber);
  }

  @Override
  public MobilityType getMobilityType() {
    return frame.getMobilityType();
  }

  @Override
  public Frame getFrame() {
    return frame;
  }

  @Override
  public float getRetentionTime() {
    return frame.getRetentionTime();
  }

  @Override
  public int getSpectrumNumber() {
    return spectrumNumber;
  }
}
