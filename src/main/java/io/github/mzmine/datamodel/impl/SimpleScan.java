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
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * Simple implementation of the Scan interface.
 */
public class SimpleScan extends AbstractStorableSpectrum implements Scan {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final RawDataFile dataFile;
  private int scanNumber;
  private int msLevel;

  private double precursorMZ;
  private int precursorCharge;
  private float retentionTime;
  private PolarityType polarity;
  private String scanDefinition;
  private Range<Double> scanMZRange;
  private MassList massList = null;

  /**
   * Clone constructor
   */

  public SimpleScan(@Nonnull RawDataFile dataFile, Scan sc, double[] newMzValues,
      double[] newIntensityValues) {

    this(dataFile, sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(), sc.getPrecursorMZ(),
        sc.getPrecursorCharge(), newMzValues, newIntensityValues, sc.getSpectrumType(), sc.getPolarity(),
        sc.getScanDefinition(), sc.getScanningMZRange());
  }


  /**
   * Constructor for creating scan with given data
   */
  public SimpleScan(@Nonnull RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
      double precursorMZ, int precursorCharge, double mzValues[], double intensityValues[],
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange) {

    super(dataFile.getMemoryMapStorage(), mzValues, intensityValues);

    this.dataFile = dataFile;
    this.scanNumber = scanNumber;
    this.msLevel = msLevel;
    this.retentionTime = retentionTime;
    this.precursorMZ = precursorMZ;
    this.precursorCharge = precursorCharge;
    this.polarity = polarity;
    this.scanDefinition = scanDefinition;
    this.scanMZRange = scanMZRange;
    setSpectrumType(spectrumType);
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

  /**
   * @see io.github.mzmine.datamodel.Scan#getPrecursorMZ()
   */
  @Override
  public double getPrecursorMZ() {
    return precursorMZ;
  }

  /**
   * @param precursorMZ The precursorMZ to set.
   */
  public void setPrecursorMZ(double precursorMZ) {
    this.precursorMZ = precursorMZ;
  }

  /**
   * @return Returns the precursorCharge.
   */
  @Override
  public int getPrecursorCharge() {
    return precursorCharge;
  }

  /**
   * @param precursorCharge The precursorCharge to set.
   */
  public void setPrecursorCharge(int precursorCharge) {
    this.precursorCharge = precursorCharge;
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#
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
  public synchronized void addMassList(final @Nonnull MassList massList) {
    this.massList = massList;
  }

  @Override
  public MassList getMassList() {
    return massList;
  }

  @Override
  @Nonnull
  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  @Nonnull
  public PolarityType getPolarity() {
    if (polarity == null) {
      polarity = PolarityType.UNKNOWN;
    }
    return polarity;
  }

  @Override
  public String getScanDefinition() {
    if (scanDefinition == null) {
      scanDefinition = "";
    }
    return scanDefinition;
  }

  @Override
  @Nonnull
  public Range<Double> getScanningMZRange() {
    if (scanMZRange == null) {
      scanMZRange = getDataPointMZRange();
    }
    return scanMZRange;
  }

}

