/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
