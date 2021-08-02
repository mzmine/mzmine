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

package io.github.mzmine.modules.visualization.rawdataoverview;

import io.github.mzmine.datamodel.Scan;

/*
 * Raw data overview raw data table model class class
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class ScanDescription {

  private Scan scan;
  private String scanNumber;
  private String retentionTime;
  private String msLevel;
  private String precursorMz;
  private String mzRange;
  private String scanType;
  private String polarity;
  private String definition;
  private String basePeak;
  private String basePeakIntensity;


  public ScanDescription(Scan scan, String scanNumber,
      String retentionTime, String msLevel, String precursorMz,
      String mzRange, String scanType, String polarity, String definition, String basePeak,
      String basePeakIntensity) {
    this.scan = scan;
    this.scanNumber = scanNumber;
    this.retentionTime = retentionTime;
    this.msLevel = msLevel;
    this.precursorMz = precursorMz;
    this.mzRange = mzRange;
    this.scanType = scanType;
    this.polarity = polarity;
    this.definition = definition;
    this.basePeak = basePeak;
    this.basePeakIntensity = basePeakIntensity;
  }


  public Scan getScan() {
    return scan;
  }

  public String getScanNumber() {
    return scanNumber;
  }


  public void setScanNumber(String scanNumber) {
    this.scanNumber = scanNumber;
  }


  public String getRetentionTime() {
    return retentionTime;
  }


  public void setRetentionTime(String retentionTime) {
    this.retentionTime = retentionTime;
  }


  public String getMsLevel() {
    return msLevel;
  }


  public void setMsLevel(String msLevel) {
    this.msLevel = msLevel;
  }

  public String getMzRange() {
    return mzRange;
  }


  public void setMzRange(String mzRange) {
    this.mzRange = mzRange;
  }

  public String getScanType() {
    return scanType;
  }


  public void setScanType(String scanType) {
    this.scanType = scanType;
  }


  public String getPolarity() {
    return polarity;
  }


  public void setPolarity(String polarity) {
    this.polarity = polarity;
  }


  public String getPrecursorMz() {
    return precursorMz;
  }


  public void setPrecursorMz(String precursorMz) {
    this.precursorMz = precursorMz;
  }


  public String getDefinition() {
    return definition;
  }


  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public String getBasePeak() {
    return basePeak;
  }

  public void setBasePeak(String basePeak) {
    this.basePeak = basePeak;
  }

  public String getBasePeakIntensity() {
    return basePeakIntensity;
  }

  public void setBasePeakIntensity(String basePeakIntensity) {
    this.basePeakIntensity = basePeakIntensity;
  }
}
