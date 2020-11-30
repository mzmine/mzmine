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
import com.google.common.primitives.Ints;

import io.github.mzmine.datamodel.*;
import io.github.mzmine.util.scans.ScanUtils;

import javax.annotation.Nonnull;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Simple implementation of the Scan interface.
 */
public class SimpleScan implements Scan {

  private RawDataFile dataFile;
  private int scanNumber;
  private int msLevel;
  private int fragmentScans[];
  private DataPoint dataPoints[];
  private double precursorMZ;
  private int precursorCharge;
  private float retentionTime;
  private Range<Double> mzRange;
  private DataPoint basePeak;
  private double totalIonCurrent;
  private MassSpectrumType spectrumType;
  private PolarityType polarity;
  private String scanDefinition;
  private Range<Double> scanMZRange;

  private double mobility;
  private MobilityType mobilityType;

  /**
   * Clone constructor
   */
  public SimpleScan(Scan sc) {
    this(sc.getDataFile(), sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(),
        sc.getPrecursorMZ(), sc.getPrecursorCharge(), sc.getFragmentScanNumbers(),
        sc.getDataPoints(), sc.getSpectrumType(), sc.getPolarity(), sc.getScanDefinition(),
        sc.getScanningMZRange(), sc.getMobility(), sc.getMobilityType());
  }

  /**
   * Constructor for creating scan with given data
   */
  public SimpleScan(RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
      double precursorMZ, int precursorCharge, int fragmentScans[], DataPoint[] dataPoints,
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange) {

    this(dataFile, scanNumber, msLevel, retentionTime, precursorMZ, precursorCharge, fragmentScans,
        dataPoints, spectrumType, polarity, scanDefinition, scanMZRange, -1.d, MobilityType.NONE);
  }

  /**
   * Constructor for creating scan with given data
   */
  public SimpleScan(RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
      double precursorMZ, int precursorCharge, int fragmentScans[], DataPoint[] dataPoints,
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, double mobility, MobilityType mobilityType) {

    // save scan data
    this.dataFile = dataFile;
    this.scanNumber = scanNumber;
    this.msLevel = msLevel;
    this.retentionTime = retentionTime;
    this.precursorMZ = precursorMZ;
    this.fragmentScans = fragmentScans;
    this.spectrumType = spectrumType;
    this.precursorCharge = precursorCharge;
    this.polarity = polarity;
    this.scanDefinition = scanDefinition;
    this.scanMZRange = scanMZRange;
    this.mobility = mobility;
    this.mobilityType = mobilityType;

    if (dataPoints != null) {
      setDataPoints(dataPoints);
    }
  }

  /**
   * @return Returns scan datapoints
   */
  @Nonnull
  public DataPoint[] getDataPoints() {
    return dataPoints;
  }

  /**
   * @return Returns scan datapoints within a given range
   */
  @Nonnull
  public DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange) {

    int startIndex, endIndex;
    for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
      if (dataPoints[startIndex].getMZ() >= mzRange.lowerEndpoint()) {
        break;
      }
    }

    for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
      if (dataPoints[endIndex].getMZ() > mzRange.upperEndpoint()) {
        break;
      }
    }

    DataPoint pointsWithinRange[] = new DataPoint[endIndex - startIndex];

    // Copy the relevant points
    System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0, endIndex - startIndex);

    return pointsWithinRange;
  }

  /**
   * @return Returns scan datapoints over certain intensity
   */
  @Nonnull
  public DataPoint[] getDataPointsOverIntensity(double intensity) {
    int index;
    Vector<DataPoint> points = new Vector<DataPoint>();

    for (index = 0; index < dataPoints.length; index++) {
      if (dataPoints[index].getIntensity() >= intensity) {
        points.add(dataPoints[index]);
      }
    }

    DataPoint pointsOverIntensity[] = points.toArray(new DataPoint[0]);

    return pointsOverIntensity;
  }

  /**
   * @param dataPoints
   */
  public void setDataPoints(DataPoint[] dataPoints) {

    this.dataPoints = dataPoints;
    mzRange = Range.singleton(0.0);
    basePeak = null;
    totalIonCurrent = 0;

    // find m/z range and base peak
    if (dataPoints.length > 0) {

      basePeak = dataPoints[0];
      mzRange = Range.singleton(dataPoints[0].getMZ());

      for (DataPoint dp : dataPoints) {

        if (dp.getIntensity() > basePeak.getIntensity()) {
          basePeak = dp;
        }

        mzRange = mzRange.span(Range.singleton(dp.getMZ()));
        totalIonCurrent += dp.getIntensity();

      }

    }

  }

  /**
   * @see io.github.mzmine.datamodel.Scan#getNumberOfDataPoints()
   */
  public int getNumberOfDataPoints() {
    return dataPoints.length;
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#getScanNumber()
   */
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
   * @return Mobility if measured, -1 otherwise
   */
  public double getMobility() {
    return mobility;
  }

  @Override
  public MobilityType getMobilityType() {
    return mobilityType;
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#
   */
  @Nonnull
  public Range<Double> getDataPointMZRange() {
    return mzRange;
  }

  /**
   * @see Scan#getHighestDataPoint()
   */
  public DataPoint getHighestDataPoint() {
    return basePeak;
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#getFragmentScanNumbers()
   */
  public int[] getFragmentScanNumbers() {
    return fragmentScans;
  }

  /**
   * @param fragmentScans The fragmentScans to set.
   */
  public void setFragmentScanNumbers(int[] fragmentScans) {
    this.fragmentScans = fragmentScans;
  }

  public void addFragmentScan(int fragmentScan) {
    TreeSet<Integer> fragmentsSet = new TreeSet<Integer>();
    if (fragmentScans != null) {
      for (int frag : fragmentScans) {
        fragmentsSet.add(frag);
      }
    }
    fragmentsSet.add(fragmentScan);
    fragmentScans = Ints.toArray(fragmentsSet);
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#getSpectrumType()
   */
  public MassSpectrumType getSpectrumType() {
    return spectrumType;
  }

  public void setSpectrumType(MassSpectrumType spectrumType) {
    this.spectrumType = spectrumType;
  }

  public double getTIC() {
    return totalIonCurrent;
  }

  public String toString() {
    return ScanUtils.scanToString(this, false);
  }

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
}

