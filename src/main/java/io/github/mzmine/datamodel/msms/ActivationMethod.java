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

package io.github.mzmine.datamodel.msms;

public enum ActivationMethod {

  CID("CID", "collision induced dissociation", "eV"), //
  HCD("HCD", "higher-energy C-trap dissociation", "a.u."), //
  ECD("ECD", "electron capture dissociation", ""), //
  ETD("ETD", "electron transfer dissociation", ""),
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
}
