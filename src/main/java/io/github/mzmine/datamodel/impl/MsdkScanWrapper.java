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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.msdk.IsolationInfo;
import io.github.mzmine.datamodel.msdk.MsScan;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.io.import_rawdata_mzml.ConversionUtils;
import java.util.Iterator;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MsdkScanWrapper implements Scan {

  // wrap this scan
  private final MsScan scan;

  public MsdkScanWrapper(MsScan scan) {
    this.scan = scan;
  }

  @Override
  public int getNumberOfDataPoints() {
    return scan.getNumberOfDataPoints();
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return ConversionUtils.msdkToMZmineSpectrumType(scan.getSpectrumType());
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    return scan.getMzValues(dst);
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException(
        "Unsupported operation. MSDK scan uses float array and the conversion in this method is not efficient.");
  }

  @Override
  public double getMzValue(int index) {
    return scan.getMzValues()[index];
  }

  @Override
  public double getIntensityValue(int index) {
    return scan.getIntensityValues()[index];
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    throw new UnsupportedOperationException(
        "Unsupported operation. MSDK scan is not supposed to be used here.");
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    throw new UnsupportedOperationException(
        "Unsupported operation. MSDK scan is not supposed to be used here.");
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    throw new UnsupportedOperationException(
        "Unsupported operation. MSDK scan is not supposed to be used here.");
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    return scan.getMzRange();
  }

  @Nullable
  @Override
  public Double getTIC() {
    return Double.valueOf(scan.getTIC());
  }

  @Override
  public Stream<DataPoint> stream() {
    throw new UnsupportedOperationException(
        "Unsupported operation. MSDK scan is not supposed to be used here.");
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException(
        "Unsupported operation. MSDK scan is not supposed to be used here.");
  }

  @NotNull
  @Override
  public RawDataFile getDataFile() {
    throw new UnsupportedOperationException(
        "Unsupported operation. MSDK scan is not supposed to be used here.");
  }

  @Override
  public int getScanNumber() {
    return scan.getScanNumber();
  }

  @NotNull
  @Override
  public String getScanDefinition() {
    return scan.getScanDefinition();
  }

  @Override
  public int getMSLevel() {
    return scan.getMsLevel();
  }

  @Override
  public float getRetentionTime() {
    return scan.getRetentionTime();
  }

  @NotNull
  @Override
  public Range<Double> getScanningMZRange() {
    return scan.getScanningRange();
  }

  @Override
  public double getPrecursorMZ() {
    return scan.getIsolations().stream().findFirst().map(IsolationInfo::getPrecursorMz).orElse(-1d);
  }

  @Override
  public int getPrecursorCharge() {
    return scan.getIsolations().stream().findFirst().map(IsolationInfo::getPrecursorCharge)
        .orElse(-1);
  }

  @NotNull
  @Override
  public PolarityType getPolarity() {
    return ConversionUtils.msdkToMZminePolarityType(scan.getPolarity());
  }


  @Nullable
  @Override
  public MassList getMassList() {
    return null;
  }

  @Override
  public void addMassList(@NotNull MassList massList) {

  }
}
