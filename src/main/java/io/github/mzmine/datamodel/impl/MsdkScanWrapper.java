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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.msdk.datamodel.ActivationInfo;
import io.github.msdk.datamodel.IsolationInfo;
import io.github.msdk.datamodel.MsScan;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_mzml.ConversionUtils;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLCV;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLMsScan;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MsdkScanWrapper implements Scan {

  // wrap this scan
  private final MsScan scan;
  private final MsMsInfo msMsInfo;
  private final double[] mzs;
  private final float[] intensities;

  public MsdkScanWrapper(MsScan scan) {
    this.scan = scan;

    // preload as getMzValue(i) is inefficient in MSDK scans
    mzs = scan.getMzValues();
    intensities = scan.getIntensityValues();

    scan.getIsolations();
    if (!scan.getIsolations().isEmpty()) {
      IsolationInfo isolationInfo = scan.getIsolations().get(0);
      ActivationInfo activationInfo = isolationInfo.getActivationInfo();
      Float energy = activationInfo != null && activationInfo.getActivationEnergy() != null
          ? activationInfo.getActivationEnergy().floatValue() : null;
      ActivationMethod activationMethod = activationInfo != null ? ActivationMethod.valueOf(
          activationInfo.getActivationType().name()) : null;

      msMsInfo = new DDAMsMsInfoImpl(isolationInfo.getPrecursorMz(),
          isolationInfo.getPrecursorCharge(), energy, this, null, scan.getMsLevel(),
          activationMethod, null);
    } else {
      msMsInfo = null;
    }
  }

  @Override
  public int getNumberOfDataPoints() {
    return mzs.length;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return ConversionUtils.msdkToMZmineSpectrumType(scan.getSpectrumType());
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException(
        "Unsupported operation. MSDK scan uses float array and the conversion in this method is not efficient.");
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException(
        "Unsupported operation. MSDK scan uses float array and the conversion in this method is not efficient.");
  }

  @Override
  public double getMzValue(int index) {
    return mzs[index];
  }

  @Override
  public double getIntensityValue(int index) {
    return intensities[index];
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
  public @Nullable MsMsInfo getMsMsInfo() {
    return msMsInfo;
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

  @Override
  public @Nullable Float getInjectionTime() {
    try {
      return ((MzMLMsScan) scan).getScanList().getScans().get(0).getCVParamsList().stream()
          .filter(p -> MzMLCV.cvIonInjectTime.equals(p.getAccession()))
          .map(p -> p.getValue().map(Float::parseFloat)).map(Optional::get).findFirst()
          .orElse(null);
    } catch (Exception ex) {
      // float parsing error
      return null;
    }
  }
}
