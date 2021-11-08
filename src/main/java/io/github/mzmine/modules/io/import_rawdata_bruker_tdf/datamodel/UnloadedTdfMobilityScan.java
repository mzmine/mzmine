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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFUtils;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UnloadedTdfMobilityScan implements MobilityScan, MassList {

  protected final Frame frame;
  protected final int mobilityScanIndex;
  protected final TDFUtils utils;
  protected double[] mzs;
  protected double[] intensities;
  protected int numberOfDataPoints = -1;

  public UnloadedTdfMobilityScan(Frame frame, int mobilityScanIndex, @NotNull final TDFUtils utils) {
    this.utils = utils;
    this.frame = frame;
    this.mobilityScanIndex = mobilityScanIndex;
  }

  public UnloadedTdfMobilityScan(Frame frame, int mobilityScanIndex, @NotNull double[] mzs,
      @NotNull double[] intensities) {
    assert mzs != null && intensities != null;
    assert mzs.length == intensities.length;
    this.frame = frame;
    this.mobilityScanIndex = mobilityScanIndex;
    this.mzs = mzs;
    this.intensities = intensities;
    this.numberOfDataPoints = intensities.length;
    utils = null;
  }

  @Override
  public int getNumberOfDataPoints() {
    if (numberOfDataPoints == -1) {
      loadRawData();
    }
    return numberOfDataPoints;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return frame.getSpectrumType();
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    if (mzs == null) {
      loadRawData();
    }
    assert dst.length >= mzs.length;
    System.arraycopy(mzs, 0, dst, 0, mzs.length);
    return dst;
  }

  @Override
  public void getMzValues(double[] dst, int offset) {
    if(mzs == null) {
      loadRawData();
    }
    assert offset + getNumberOfDataPoints() <= dst.length;
    System.arraycopy(mzs, 0, dst, offset, mzs.length);
  }

  @Override
  public void getIntensityValues(double[] dst, int offset) {
    if(intensities == null) {
      loadRawData();
    }
    assert offset + getNumberOfDataPoints() <= dst.length;
    System.arraycopy(intensities, 0, dst, offset, intensities.length);
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    if (intensities == null) {
      loadRawData();
    }
    assert dst.length >= intensities.length;
    System.arraycopy(intensities, 0, dst, 0, intensities.length);
    return dst;
  }

  @Override
  public double getMzValue(int index) {
    if (mzs == null) {
      loadRawData();
    }
    return mzs[index];
  }

  @Override
  public double getIntensityValue(int index) {
    if (intensities == null) {
      loadRawData();
    }
    return intensities[index];
  }

  @Override
  public @Nullable Double getBasePeakMz() {
    return null;
  }

  @Override
  public @Nullable Double getBasePeakIntensity() {
    return null;
  }

  @Override
  public @Nullable Integer getBasePeakIndex() {
    return null;
  }

  @Override
  public @Nullable Range<Double> getDataPointMZRange() {
    return Range.closed(getMzValue(0), getMzValue(getNumberOfDataPoints() - 1));
  }

  @Override
  public @Nullable Double getTIC() {
    return null;
  }

  @Override
  public Stream<DataPoint> stream() {
    return null;
  }

  @Override
  public @NotNull RawDataFile getDataFile() {
    return getFrame().getDataFile();
  }

  @Override
  public double getMobility() {
    return ((TdfImsRawDataFileImpl) frame.getDataFile()).getMobilitiesForFrame(
        frame.getFrameId())[mobilityScanIndex];
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

  @Nullable
  @Override
  public MsMsInfo getMsMsInfo() {
    return frame.getImsMsMsInfoForMobilityScan(mobilityScanIndex);
  }

  @Override
  public int getMobilityScanNumber() {
    return mobilityScanIndex;
  }

  @Override
  public @Nullable MassList getMassList() {
    return new UnloadedTdfMobilityScanMassList(getFrame(), getMobilityScanNumber(),
        ((UndloadedTDFFrame) getFrame()).getMobilityScanNoiseLevel(), utils);
  }

  @Override
  public void addMassList(@NotNull MassList massList) {
    // do nothing
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    return null;
  }

  protected void loadRawData() {
    final TDFUtils tdfUtils =
        utils != null ? utils : ((TdfImsRawDataFileImpl) frame.getDataFile()).getTdfUtils();

    final List<double[][]> doubles = tdfUtils.loadDataPointsForFrame(frame.getFrameId(),
        mobilityScanIndex, (long) mobilityScanIndex + 1);
    mzs = doubles.get(0)[0];
    intensities = doubles.get(0)[1];
    numberOfDataPoints = mzs.length;
  }
}
