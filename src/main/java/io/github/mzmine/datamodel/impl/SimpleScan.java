/*
 *  Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.util.scans.ScanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple implementation of the Scan interface.
 */
public class SimpleScan extends AbstractStorableSpectrum implements Scan {

  public static final String XML_SCAN_TYPE = "simplescan";
  protected final Float injectionTime;
  @NotNull
  private final RawDataFile dataFile;
  private int scanNumber;
  private int msLevel;
  private float retentionTime;
  private PolarityType polarity;
  private String scanDefinition;
  private Range<Double> scanMZRange;
  private MassList massList = null;
  private MsMsInfo msMsInfo;

  /**
   * Clone constructor
   */
  public SimpleScan(@NotNull RawDataFile dataFile, Scan sc, double[] newMzValues,
      double[] newIntensityValues) {

    this(dataFile, sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(), sc.getMsMsInfo(),
        newMzValues, newIntensityValues, sc.getSpectrumType(), sc.getPolarity(),
        sc.getScanDefinition(), sc.getScanningMZRange(), sc.getInjectionTime());
  }


  /**
   * Constructor for creating scan with given data
   */
  public SimpleScan(@NotNull RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
      @Nullable MsMsInfo msMsInfo, double[] mzValues, double[] intensityValues,
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange) {

    this(dataFile, scanNumber, msLevel, retentionTime, msMsInfo, mzValues, intensityValues,
        spectrumType, polarity, scanDefinition, scanMZRange, null);
  }

  public SimpleScan(@NotNull RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
      @Nullable MsMsInfo msMsInfo, double[] mzValues, double[] intensityValues,
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, @Nullable Float injectionTime) {

    super(dataFile.getMemoryMapStorage(), mzValues, intensityValues);

    this.dataFile = dataFile;
    this.scanNumber = scanNumber;
    this.msLevel = msLevel;
    this.retentionTime = retentionTime;
    this.polarity = polarity;
    this.scanDefinition = scanDefinition;
    this.scanMZRange = scanMZRange;
    setSpectrumType(spectrumType);
    setMsMsInfo(msMsInfo);
    this.injectionTime = injectionTime;
  }


  /**
   * @see io.github.mzmine.datamodel.Scan#getScanNumber()
   */
  @Override
  public int getScanNumber() {
    return scanNumber;
  }

  /**
   * @param scanNumber The scanNumber to set.
   */
  public void setScanNumber(int scanNumber) {
    this.scanNumber = scanNumber;
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#getMSLevel()
   */
  @Override
  public int getMSLevel() {
    return msLevel;
  }

  /**
   * @param msLevel The msLevel to set.
   */
  public void setMSLevel(int msLevel) {
    this.msLevel = msLevel;
  }

  @Override
  public @Nullable MsMsInfo getMsMsInfo() {
    return msMsInfo;
  }

  public void setMsMsInfo(@Nullable MsMsInfo info) {
    msMsInfo = info; // in case its null
    if (info != null) {
      msMsInfo = info.createCopy();
      msMsInfo.setMsMsScan(this);
    }
  }

  /**
   *
   */
  @Override
  public float getRetentionTime() {
    return retentionTime;
  }

  /**
   * @param retentionTime The retentionTime to set.
   */
  public void setRetentionTime(float retentionTime) {
    this.retentionTime = retentionTime;
  }


  @Override
  public String toString() {
    return ScanUtils.scanToString(this, false);
  }

  @Override
  public synchronized void addMassList(final @NotNull MassList massList) {
    // we are not going into any details if this.massList equals massList
    // do not call listeners if the same object is passed multiple times
    if (this.massList == massList) {
      return;
    }
    MassList old = this.massList;
    this.massList = massList;

    if (dataFile != null) {
      dataFile.applyMassListChanged(this, old, massList);
    }
  }

  @Override
  public MassList getMassList() {
    return massList;
  }

  @Override
  @NotNull
  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  @NotNull
  public PolarityType getPolarity() {
    if (polarity == null) {
      polarity = PolarityType.UNKNOWN;
    }
    return polarity;
  }

  @NotNull
  @Override
  public String getScanDefinition() {
    if (scanDefinition == null) {
      scanDefinition = "";
    }
    return scanDefinition;
  }

  @Override
  @NotNull
  public Range<Double> getScanningMZRange() {
    if (scanMZRange == null) {
      scanMZRange = getDataPointMZRange();
    }
    return scanMZRange;
  }

  @Override
  public @Nullable Float getInjectionTime() {
    return injectionTime;
  }
}

