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

package io.github.mzmine.modules.visualization.combinedmodule;

import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;

public class CombinedModuleDataPoint {

  private double mzValue;
  private int scanNumber;
  private double precursorMZ;
  private int precursorCharge;
  private double retentionTime;
  private double neutralLoss;
  private double precursorMass;
  private String label;
  private static int defaultPrecursorCharge = 2;

  public CombinedModuleDataPoint(double mzValue, int scanNumber, double precursorMZ,
      int precursorCharge,
      double retentionTime) {
    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    this.mzValue = mzValue;
    this.scanNumber = scanNumber;
    this.precursorMZ = precursorMZ;
    this.precursorCharge = precursorCharge;
    this.retentionTime = retentionTime;

    precursorMass = precursorMZ;
    if (precursorCharge > 0) {
      precursorMass *= precursorCharge;
    }

    if ((precursorCharge == 0) && (precursorMass < mzValue)) {
      precursorMass *= defaultPrecursorCharge;
    }

    neutralLoss = mzValue; /* precursorMass - mzValue; */

    StringBuilder sb = new StringBuilder();
    sb.append("loss: ");
    sb.append(mzFormat.format(neutralLoss));
    sb.append(", m/z ");
    sb.append(mzFormat.format(mzValue));
    sb.append(", scan #").append(scanNumber).append(", RT ");
    sb.append(rtFormat.format(retentionTime));
    sb.append(", m/z ");
    sb.append(mzFormat.format(precursorMZ));
    if (precursorCharge > 0) {
      sb.append(" (charge ").append(precursorCharge).append(")");
    }
    label = sb.toString();

  }

  /**
   * @return Returns the mzValue.
   */
  double getMzValue() {
    return mzValue;
  }

  /**
   * @return Returns the precursorCharge.
   */
  int getPrecursorCharge() {
    return precursorCharge;
  }

  /**
   * @return Returns the precursorMZ.
   */
  double getPrecursorMZ() {
    return precursorMZ;
  }

  /**
   * @return Returns the precursor mass, or m/z if charge is unknown.
   */
  double getPrecursorMass() {
    return precursorMass;
  }

  /**
   * @return Returns the retentionTime.
   */
  double getRetentionTime() {
    return retentionTime;
  }

  /**
   * @return Returns the scanNumber.
   */
  int getScanNumber() {
    return scanNumber;
  }

  String getName() {
    return label;
  }

  double getNeutralLoss() {
    return neutralLoss;
  }
}
