/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.msms;

import io.github.msdk.datamodel.ActivationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ActivationMethod {

  CID("CID", "collision induced dissociation", "eV"), //
  HCD("HCD", "higher-energy C-trap dissociation", "a.u."), //
  ECD("ECD", "electron capture dissociation", ""), //
  ETD("ETD", "electron transfer dissociation", ""), //
  ETHCD("ETHCD", "Electron-transfer and higher-energy collision dissociation", ""), //
  EAD("EAD", "electron activated dissociation", "eV"), //
  UVPD("UVPD", "Ultraviolet photodissociation", ""), //
  UNKNOWN("N.A.", "Unknown", "");

  private final String abbreviation;
  private final String name;
  private final String unit;

  ActivationMethod(String abbreviation, String name, String unit) {
    this.abbreviation = abbreviation;
    this.name = name;
    this.unit = unit;
  }

  public String getAbbreviation() {
    return abbreviation;
  }

  public String getName() {
    return name;
  }

  public String getUnit() {
    return unit;
  }


  @Override
  public String toString() {
    return getAbbreviation();
  }

  @NotNull
  public static ActivationMethod fromActivationType(@Nullable ActivationType type) {
    return switch (type) {
      case null -> ActivationMethod.UNKNOWN;
      case CID -> CID;
      case HCD -> HCD;
      case ECD -> ECD;
      case ETD -> ETD;
      case ETHCD -> ETHCD;
      case UVPD -> UVPD;
      case UNKNOWN -> UNKNOWN;
    };
  }
}
