/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class CCSCalibrant {

  public static final double N2_MASS = 28.006148008;
  private final double libraryMz;
  private final float libraryMobility;
  private final float libraryCCS;
  private final int libraryCharge;
  private Double foundMz;
  private Float foundMobility;

  public CCSCalibrant(Double foundMz, Float foundMobility, double libraryMz,
      float libraryMobility, float libraryCCS, int libraryCharge) {
    this.foundMz = foundMz;
    this.foundMobility = foundMobility;
    this.libraryMz = libraryMz;
    this.libraryMobility = libraryMobility;
    this.libraryCCS = libraryCCS;
    this.libraryCharge = libraryCharge;
  }

  public CCSCalibrant(@NotNull FeatureListRow row, double libraryMz, float libraryMobility,
      float libraryCCS, int libraryCharge) {
    this(row.getAverageMZ(), row.getAverageMobility(), libraryMz, libraryMobility, libraryCCS,
        libraryCharge);
  }

  @Override
  public String toString() {
    return String.format("Found: m/z %.4f mobility %.4f - Expected: mz %4f mobility %.4f ccs %.2f",
        foundMz(), foundMobility(), libraryMz(), libraryMobility(), libraryCCS());
  }

  /**
   * @return modified reduced mass related coefficient for the calibrant in the drift gas N2
   */
  public double getN2Gamma() {
    final double libraryMass = libraryMz * Math.abs(libraryCharge);
    final double v =
        (1 / (double) Math.abs(libraryCharge())) * Math.sqrt(libraryMass / (libraryMass + N2_MASS));
    return v;
  }

  public Double foundMz() {
    return foundMz;
  }

  public Float foundMobility() {
    return foundMobility;
  }

  public double libraryMz() {
    return libraryMz;
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
        that.foundMobility) && Double.doubleToLongBits(this.libraryMz) == Double.doubleToLongBits(
        that.libraryMz) && Float.floatToIntBits(this.libraryMobility) == Float.floatToIntBits(
        that.libraryMobility) && Float.floatToIntBits(this.libraryCCS) == Float.floatToIntBits(
        that.libraryCCS) && this.libraryCharge == that.libraryCharge;
  }

  @Override
  public int hashCode() {
    return Objects.hash(foundMz, foundMobility, libraryMz, libraryMobility, libraryCCS,
        libraryCharge);
  }

  public void setFoundMz(Double foundMz) {
    this.foundMz = foundMz;
  }

  public void setFoundMobility(Float foundMobility) {
    this.foundMobility = foundMobility;
  }
}
