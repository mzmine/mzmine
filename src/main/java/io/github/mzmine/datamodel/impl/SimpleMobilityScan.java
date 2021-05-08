/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.masslist.MobilityScanMassList;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author https://github.com/SteffenHeu
 * @see io.github.mzmine.datamodel.MobilityScan
 */
public class SimpleMobilityScan implements MobilityScan {

  private static final Logger logger = Logger.getLogger(SimpleMobilityScan.class.getName());

  private final SimpleFrame frame;
  private MassList massList = null;
  private final int storageOffset;
  private final int numDataPoints;
  private final int mobilityScanNumber;
  private final int basePeakIndex;

  public SimpleMobilityScan(int mobilityScanNumber, SimpleFrame frame, int storageOffset,
      int numDataPoints, @Nullable Integer basePeakIndex) {
    this.frame = frame;
    this.mobilityScanNumber = mobilityScanNumber;
    this.storageOffset = storageOffset;
    this.numDataPoints = numDataPoints;
    this.basePeakIndex = (basePeakIndex != null) ? basePeakIndex : -1;
  }

  @Override
  public int getNumberOfDataPoints() {
    return numDataPoints;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return frame.getSpectrumType();
  }

  @Override
  public double[] getMzValues(@Nonnull double[] dst) {
    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    frame.getMobilityScanMzValues(this, dst);
    return dst;
  }

  @Override
  public double[] getIntensityValues(@Nonnull double[] dst) {
    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    frame.getMobilityScanIntensityValues(this, dst);
    return dst;
  }

  @Override
  public double getMzValue(int index) {
    return frame.getMobilityScanMzValue(this, index);
  }

  @Override
  public double getIntensityValue(int index) {
    return frame.getMobilityScanIntensityValue(this, index);
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    if (basePeakIndex == -1) {
      return null;
    }
    return getMzValue(basePeakIndex);
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    if (basePeakIndex == -1) {
      return null;
    }
    return getIntensityValue(basePeakIndex);
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    return basePeakIndex == -1 ? null : basePeakIndex;
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    return Range.closed(getMzValue(0), getMzValue(getNumberOfDataPoints() - 1));
  }

  @Nullable
  @Override
  public Double getTIC() {
    throw new UnsupportedOperationException("Intentionally unimplemented to safe RAM.");
  }

  @Nonnull
  @Override
  public RawDataFile getDataFile() {
    return getFrame().getDataFile();
  }

  @Override
  public double getMobility() {
    return frame.getMobilityForMobilityScanNumber(mobilityScanNumber);
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
  public int getMobilityScanNumber() {
    return mobilityScanNumber;
  }

  @Nullable
  @Override
  public ImsMsMsInfo getMsMsInfo() {
    return frame.getImsMsMsInfoForMobilityScan(mobilityScanNumber);
  }

  /**
   * @return Used to retrieve this scans storage offset when reading mz/intensity values. Not
   *         intended for public usage, therefore not declared in {@link MobilityScan}.
   */
  int getStorageOffset() {
    return storageOffset;
  }

  @Override
  public void addMassList(@Nonnull MassList massList) {
    if (!(massList instanceof MobilityScanMassList) && !(massList instanceof ScanPointerMassList)) {
      throw new IllegalArgumentException(
          "Cannot mass lists of type " + massList.getClass().getName() + " to MobilityScan");
    }
    this.massList = massList;
  }

  @Override
  public MassList getMassList() {
    return massList;
  }

  @Override
  public Iterator<DataPoint> iterator() {
    return new DataPointIterator(this);
  }

  @Override
  public Stream<DataPoint> stream() {
    return Streams.stream(this);
  }

  private class DataPointIterator implements Iterator<DataPoint>, DataPoint {

    private final MassSpectrum spectrum;
    // We start at -1 so the first call to next() moves us to index 0
    private int cursor = -1;

    DataPointIterator(MassSpectrum spectrum) {
      this.spectrum = spectrum;
    }

    @Override
    public boolean hasNext() {
      return (cursor + 1) < spectrum.getNumberOfDataPoints();
    }

    @Override
    public DataPoint next() {
      cursor++;
      return this;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public double getMZ() {
      return spectrum.getMzValue(cursor);
    }

    @Override
    public double getIntensity() {
      return spectrum.getIntensityValue(cursor);
    }

  }
}
