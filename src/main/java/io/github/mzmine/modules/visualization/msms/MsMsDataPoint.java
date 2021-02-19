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

package io.github.mzmine.modules.visualization.msms;

import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;

class MsMsDataPoint {

  private static final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private static final NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  private static final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

  private static final int defaultPrecursorCharge = 1;
  private double productMz;
  private double precursorMz;
  private int precursorCharge;
  private double retentionTime;
  private double neutralLoss;
  private double precursorMass;
  private double productIntensity;
  private double precursorIntensity;

  private int scanNumber;
  private boolean isHighlighted = false;

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
    } else if (precursorCharge == 0 && precursorMass < productMz) {
      precursorMass *= defaultPrecursorCharge;
    }

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

  public void setHighlighted(boolean isHighlighted) {
    this.isHighlighted = isHighlighted;
  }

  public boolean isHighlighted() {
    return isHighlighted;
  }

  @Override
  public String toString() {
    return "Scan number: " + scanNumber + '\n'
        + "Product m/z: " + mzFormat.format(productMz) + '\n'
        + "Retention time: " + rtFormat.format(retentionTime) + '\n'
        + "Precursor m/z: " + mzFormat.format(precursorMz) + '\n'
        + "Neutral loss: " + mzFormat.format(neutralLoss) + '\n'
        + "Precursor charge: " + precursorCharge + '\n'
        + "Product intensity: " + intensityFormat.format(productIntensity) + '\n'
        + "Precursor intensity: " + intensityFormat.format(precursorIntensity);
  }

}
