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

package io.github.mzmine.gui.preferences;

public enum UnitFormat {

  ROUND_BRACKED("Label (unit)"), SQUARE_BRACKET("Label [unit]"), DIVIDE("Label / unit");

  private final String representativeString;

  UnitFormat(String representativeString) {
    this.representativeString = representativeString;
  }

  public String format(String label, String unit) {
    switch(this) {
      case SQUARE_BRACKET:
        return label + " [" + unit + "]";
      case ROUND_BRACKED:
        return label + " (" + unit + ")";
      case DIVIDE:
        return  label + " / " + unit;
      default:
        return  label + " / " + unit;
    }
  }

  @Override
  public String toString() {
    return representativeString;
  }
}
