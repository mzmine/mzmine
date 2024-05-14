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

package io.github.mzmine.modules.visualization.msms;

import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;

class MsMsDataPoint {

  private static final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private static final NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  private static final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

  //private static final int defaultPrecursorCharge = 1;
  private double productMz;
  private double precursorMz;
  private int precursorCharge;
  private double retentionTime;
  private double neutralLoss;
  private double precursorMass;
  private double productIntensity;
  private double precursorIntensity;

  private int scanNumber;
  /**
   * 0 - point is not highlighted, 1, 2, 3 - different colors
   */
  private int highlightType = 0;

  public MsMsDataPoint(int scanNumber, double productMz, double precursorMz, int precursorCharge,
      double retentionTime, double productIntensity, double precursorIntensity) {
    this.scanNumber = scanNumber;
    this.productMz = productMz;
    this.precursorMz = precursorMz;
    this.precursorCharge = precursorCharge;
    this.retentionTime = retentionTime;
    this.productIntensity = productIntensity;
    this.precursorIntensity = precursorIntensity;

    precursorMass = precursorMz;
    if (precursorCharge > 0) {
      precursorMass *= precursorCharge;
    }/* else if (precursorCharge == 0 && precursorMass < productMz) {
      precursorMass *= defaultPrecursorCharge;
    }*/

    neutralLoss = precursorMass - productMz;
  }

  public int getPrecursorCharge() {
    return precursorCharge;
  }

  public double getPrecursorMz() {
    return precursorMz;
  }

  public double getPrecursorMass() {
    return precursorMass;
  }

  public double getRetentionTime() {
    return retentionTime;
  }

  public double getProductMZ() {
    return productMz;
  }

  public double getNeutralLoss() {
    return neutralLoss;
  }

  public double getProductIntensity() {
    return productIntensity;
  }

  public double getPrecursorIntensity() {
    return precursorIntensity;
  }

  public int getScanNumber() {
    return scanNumber;
  }

  public void setHighlighted(int highlightType) {
    this.highlightType = highlightType;
  }

  public int getHighlighted() {
    return highlightType;
  }

  @Override
  public String toString() {
    return "Scan number: " + scanNumber
        + "\nRetention time: " + rtFormat.format(retentionTime)
        + "\nProduct m/z: " + mzFormat.format(productMz)
        + "\nPrecursor m/z: " + mzFormat.format(precursorMz)
        + "\nNeutral loss: " + mzFormat.format(neutralLoss)
        + "\nPrecursor charge: " + (precursorCharge < 0 ? "Unknown" : precursorCharge)
        + "\nProduct intensity: " + intensityFormat.format(productIntensity)
        + "\nPrecursor intensity: " + intensityFormat.format(precursorIntensity);
  }

}
