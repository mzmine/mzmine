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

package io.github.mzmine.modules.visualization.msms_new;

class MsMsDataPoint {

  private final double mzValue;
  private final double precursorMZ;
  private final int precursorCharge;
  private final double retentionTime;
  private final double neutralLoss;
  private double precursorMass;
  private final double intensity;

  private static final int defaultPrecursorCharge = 1;

  private boolean isHighlighted = false;

  public MsMsDataPoint(double mzValue, double precursorMZ, int precursorCharge, double retentionTime,
      double intensity) {

    this.mzValue = mzValue;
    this.precursorMZ = precursorMZ;
    this.precursorCharge = precursorCharge;
    this.retentionTime = retentionTime;
    this.intensity = intensity;

    precursorMass = precursorMZ;
    if (precursorCharge > 0) {
      precursorMass *= precursorCharge;
    } else if (precursorCharge == 0 && precursorMass < mzValue) {
      precursorMass *= defaultPrecursorCharge;
    }

    neutralLoss = precursorMass - mzValue;

  }

  public int getPrecursorCharge() {
    return precursorCharge;
  }

  public double getPrecursorMZ() {
    return precursorMZ;
  }

  public double getPrecursorMass() {
    return precursorMass;
  }

  public double getRetentionTime() {
    return retentionTime;
  }

  public double getProductMZ() {
    return mzValue;
  }

  public double getNeutralLoss() {
    return neutralLoss;
  }

  public double getIntensity() {
    return intensity;
  }

  public void setHighlighted(boolean isHighlighted) {
    this.isHighlighted = isHighlighted;
  }

  public boolean isHighlighted() {
    return isHighlighted;
  }

}
