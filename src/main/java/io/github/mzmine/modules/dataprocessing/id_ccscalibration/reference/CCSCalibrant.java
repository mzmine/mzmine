/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class CCSCalibrant {

  public static final double N2_MASS = 28.006148008;
  private Double foundMz;
  private Float foundMobility;
  private final double libraryMass;
  private final float libraryMobility;
  private final float libraryCCS;
  private final int libraryCharge;

  public CCSCalibrant(Double foundMz, Float foundMobility, double libraryMass,
      float libraryMobility, float libraryCCS, int libraryCharge) {
    this.foundMz = foundMz;
    this.foundMobility = foundMobility;
    this.libraryMass = libraryMass;
    this.libraryMobility = libraryMobility;
    this.libraryCCS = libraryCCS;
    this.libraryCharge = libraryCharge;
  }

  public CCSCalibrant(@NotNull FeatureListRow row, double libraryMass, float libraryMobility,
      float libraryCCS, int libraryCharge) {
    this(row.getAverageMZ(), row.getAverageMobility(), libraryMass, libraryMobility, libraryCCS,
        libraryCharge);
  }

  @Override
  public String toString() {
    return String.format("Found: m/z %.4f mobility %.4f - Expected: mz %4f mobility %.4f ccs %.2f",
        foundMz(), foundMobility(), libraryMz(), libraryMobility(), libraryCCS());
  }

  public double getN2Gamma() {
    return 1 / (double) libraryCharge() * Math.sqrt(
        libraryMass * Math.abs(libraryCharge) / (libraryMass * libraryCharge + N2_MASS));
  }

  public Double foundMz() {
    return foundMz;
  }

  public Float foundMobility() {
    return foundMobility;
  }

  public double libraryMz() {
    return libraryMass;
  }

  public float libraryMobility() {
    return libraryMobility;
  }

  public float libraryCCS() {
    return libraryCCS;
  }

  public int libraryCharge() {
    return libraryCharge;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (CCSCalibrant) obj;
    return Double.doubleToLongBits(this.foundMz) == Double.doubleToLongBits(that.foundMz)
        && Double.doubleToLongBits(this.foundMobility) == Double.doubleToLongBits(
        that.foundMobility) && Double.doubleToLongBits(this.libraryMass) == Double.doubleToLongBits(
        that.libraryMass) && Float.floatToIntBits(this.libraryMobility) == Float.floatToIntBits(
        that.libraryMobility) && Float.floatToIntBits(this.libraryCCS) == Float.floatToIntBits(
        that.libraryCCS) && this.libraryCharge == that.libraryCharge;
  }

  @Override
  public int hashCode() {
    return Objects.hash(foundMz, foundMobility, libraryMass, libraryMobility, libraryCCS,
        libraryCharge);
  }

  public void setFoundMz(Double foundMz) {
    this.foundMz = foundMz;
  }

  public void setFoundMobility(Float foundMobility) {
    this.foundMobility = foundMobility;
  }
}
