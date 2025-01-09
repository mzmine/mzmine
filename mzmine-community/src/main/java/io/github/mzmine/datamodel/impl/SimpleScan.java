/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
import java.lang.foreign.MemorySegment;
import java.nio.DoubleBuffer;
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
   * clone scan with new data
   */
  public SimpleScan(@NotNull RawDataFile dataFile, Scan sc, double[] newMzValues,
      double[] newIntensityValues) {

    this(dataFile, sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(), sc.getMsMsInfo(),
        newMzValues, newIntensityValues, sc.getSpectrumType(), sc.getPolarity(),
        sc.getScanDefinition(), sc.getScanningMZRange(), sc.getInjectionTime());
  }

  /**
   * clone scan with new data
   */
  public SimpleScan(@NotNull RawDataFile dataFile, Scan sc, MemorySegment newMzValues,
      MemorySegment newIntensityValues) {

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

  public SimpleScan(@NotNull RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
      @Nullable MsMsInfo msMsInfo, MemorySegment mzValues, MemorySegment intensityValues,
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, @Nullable Float injectionTime) {

    super(mzValues, intensityValues);

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

  @NotNull
  @Override
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
  @Nullable
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

