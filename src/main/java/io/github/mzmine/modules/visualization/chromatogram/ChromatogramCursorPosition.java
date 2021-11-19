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

package io.github.mzmine.modules.visualization.chromatogram;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;

/**
 *
 */
public class ChromatogramCursorPosition {

  private double mzValue, retentionTime, intensityValue;
  private RawDataFile dataFile;
  private Scan scan;

  /**
   * @param retentionTime
   * @param mzValue
   * @param intensityValue
   * @param dataFile
   * @param scan
   */
  public ChromatogramCursorPosition(double retentionTime, double mzValue, double intensityValue,
      RawDataFile dataFile, Scan scan) {
    this.retentionTime = retentionTime;
    this.mzValue = mzValue;
    this.intensityValue = intensityValue;
    this.dataFile = dataFile;
    this.scan = scan;
  }

  /**
   * @return Returns the intensityValue.
   */
  public double getIntensityValue() {
    return intensityValue;
  }

  /**
   * @param intensityValue The intensityValue to set.
   */
  public void setIntensityValue(double intensityValue) {
    this.intensityValue = intensityValue;
  }

  /**
   * @return Returns the mzValue.
   */
  public double getMzValue() {
    return mzValue;
  }

  /**
   * @param mzValue The mzValue to set.
   */
  public void setMzValue(double mzValue) {
    this.mzValue = mzValue;
  }

  /**
   * @return Returns the retentionTime.
   */
  public double getRetentionTime() {
    return retentionTime;
  }

  /**
   * @param retentionTime The retentionTime to set.
   */
  public void setRetentionTime(float retentionTime) {
    this.retentionTime = retentionTime;
  }

  /**
   * @return Returns the dataFile.
   */
  public RawDataFile getDataFile() {
    return dataFile;
  }

  /**
   * @param dataFile The dataFile to set.
   */
  public void setDataFile(RawDataFile dataFile) {
    this.dataFile = dataFile;
  }

  /**
   * @return Returns the scanNumber.
   */
  public Scan getScan() {
    return scan;
  }

  /**
   * @param scan The scan to set.
   */
  public void setScan(Scan scan) {
    this.scan = scan;
  }

}
