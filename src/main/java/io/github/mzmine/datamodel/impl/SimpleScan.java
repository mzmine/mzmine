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

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * Simple implementation of the Scan interface.
 */
public class SimpleScan implements Scan {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final RawDataFile dataFile;
  private int scanNumber;
  private int msLevel;

  private double precursorMZ;
  private int precursorCharge;
  private float retentionTime;
  private Range<Double> mzRange;
  private DoubleBuffer mzValues, intensityValues;
  private int basePeak = -1;
  private double totalIonCurrent;
  private MassSpectrumType spectrumType;
  private PolarityType polarity;
  private String scanDefinition;
  private Range<Double> scanMZRange;

  /**
   * Clone constructor
   */

  public SimpleScan(@Nonnull RawDataFile dataFile, Scan sc) {
    this(dataFile, sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(), sc.getPrecursorMZ(),
        sc.getPrecursorCharge(), null, null, sc.getSpectrumType(), sc.getPolarity(),
        sc.getScanDefinition(), sc.getScanningMZRange());
  }


  /**
   * Constructor for creating scan with given data
   */
  public SimpleScan(@Nonnull RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
      double precursorMZ, int precursorCharge, double mzValues[], double intensityValues[],
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange) {

    Preconditions.checkNotNull(dataFile);

    // save scan data
    this.dataFile = dataFile;
    this.scanNumber = scanNumber;
    this.msLevel = msLevel;
    this.retentionTime = retentionTime;
    this.precursorMZ = precursorMZ;
    this.spectrumType = spectrumType;
    this.precursorCharge = precursorCharge;
    this.polarity = polarity;
    this.scanDefinition = scanDefinition;
    this.scanMZRange = scanMZRange;
    if (mzValues != null && intensityValues != null)
      setDataPoints(mzValues, intensityValues);

  }


  public void setDataPoints(DataPoint dataPoints[]) {
    double mzValues[] = new double[dataPoints.length];
    double intensityValues[] = new double[dataPoints.length];
    for (int i = 0; i < dataPoints.length; i++) {
      mzValues[i] = dataPoints[i].getMZ();
      intensityValues[i] = dataPoints[i].getIntensity();
    }
    setDataPoints(mzValues, intensityValues);
  }

  /**
   * @param dataPoints
   */
  public synchronized void setDataPoints(double mzValues[], double intensityValues[]) {

    assert mzValues.length == intensityValues.length;

    for (int i = 0; i < mzValues.length - 1; i++) {
      if (mzValues[i] > mzValues[i + 1]) {
        throw new IllegalArgumentException("The m/z values must be sorted in ascending order");
      }
    }

    MemoryMapStorage storage = dataFile.getMemoryMapStorage();
    try {
      this.mzValues = storage.storeData(mzValues);
      this.intensityValues = storage.storeData(intensityValues);
    } catch (IOException e) {
      e.printStackTrace();
      logger.log(Level.SEVERE,
          "Error while storing data points on disk, keeping them in memory instead", e);
      this.mzValues = DoubleBuffer.wrap(mzValues);
      this.intensityValues = DoubleBuffer.wrap(intensityValues);
    }


    totalIonCurrent = 0;

    // find m/z range and base peak
    if (intensityValues.length > 0) {

      basePeak = 0;
      mzRange = Range.closed(mzValues[0], mzValues[mzValues.length - 1]);

      for (int i = 0; i < intensityValues.length; i++) {

        if (intensityValues[i] > intensityValues[basePeak]) {
          basePeak = i;
        }

        totalIonCurrent += intensityValues[i];

      }

    } else {
      mzRange = Range.singleton(0.0);
      basePeak = -1;
    }

  }

  /**
   * @see io.github.mzmine.datamodel.Scan#getNumberOfDataPoints()
   */
  @Override
  public int getNumberOfDataPoints() {
    return mzValues.capacity();
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

  /**
   * @see io.github.mzmine.datamodel.Scan#
   */
  @Override
  @Nonnull
  public Range<Double> getDataPointMZRange() {
    return mzRange;
  }

  /**
   * @see Scan#getHighestDataPoint()
   */
  @Override
  public int getBasePeak() {
    return basePeak;
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#getSpectrumType()
   */
  @Override
  public MassSpectrumType getSpectrumType() {
    return spectrumType;
  }

  public void setSpectrumType(MassSpectrumType spectrumType) {
    this.spectrumType = spectrumType;
  }

  @Override
  public double getTIC() {
    return totalIonCurrent;
  }

  @Override
  public String toString() {
    return ScanUtils.scanToString(this, false);
  }

  @Override
  @Nonnull
  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public synchronized void addMassList(@Nonnull MassList massList) {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized void removeMassList(@Nonnull MassList massList) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nonnull
  public MassList[] getMassLists() {
    throw new UnsupportedOperationException();
  }

  @Override
  public MassList getMassList(@Nonnull String name) {
    throw new UnsupportedOperationException();
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
    if (scanMZRange == null)
      scanMZRange = getDataPointMZRange();
    return scanMZRange;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + msLevel;
    result = prime * result + ((mzRange == null) ? 0 : mzRange.hashCode());
    result = prime * result + ((polarity == null) ? 0 : polarity.hashCode());
    result = prime * result + precursorCharge;
    long temp;
    temp = Double.doubleToLongBits(precursorMZ);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + Float.floatToIntBits(retentionTime);
    result = prime * result + ((scanDefinition == null) ? 0 : scanDefinition.hashCode());
    result = prime * result + ((scanMZRange == null) ? 0 : scanMZRange.hashCode());
    result = prime * result + scanNumber;
    result = prime * result + ((spectrumType == null) ? 0 : spectrumType.hashCode());
    temp = Double.doubleToLongBits(totalIonCurrent);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SimpleScan other = (SimpleScan) obj;
    if (msLevel != other.msLevel)
      return false;
    if (mzRange == null) {
      if (other.mzRange != null)
        return false;
    } else if (!mzRange.equals(other.mzRange))
      return false;
    if (polarity != other.polarity)
      return false;
    if (precursorCharge != other.precursorCharge)
      return false;
    if (Double.doubleToLongBits(precursorMZ) != Double.doubleToLongBits(other.precursorMZ))
      return false;
    if (Float.floatToIntBits(retentionTime) != Float.floatToIntBits(other.retentionTime))
      return false;
    if (scanDefinition == null) {
      if (other.scanDefinition != null)
        return false;
    } else if (!scanDefinition.equals(other.scanDefinition))
      return false;
    if (scanMZRange == null) {
      if (other.scanMZRange != null)
        return false;
    } else if (!scanMZRange.equals(other.scanMZRange))
      return false;
    if (scanNumber != other.scanNumber)
      return false;
    if (spectrumType != other.spectrumType)
      return false;
    if (Double.doubleToLongBits(totalIonCurrent) != Double.doubleToLongBits(other.totalIonCurrent))
      return false;
    return true;
  }

  @Override
  public DoubleBuffer getMzValues() {
    return mzValues;
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return intensityValues;
  }

}

