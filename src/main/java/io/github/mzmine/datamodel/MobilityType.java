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

package io.github.mzmine.datamodel;

import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;

/**
 * Stores information on mobility type, axis labels and units.
 * <p>
 * For an overview of these ion mobility types see https://doi.org/10.1002/jssc.201700919
 *
 * @author https://github.com/SteffenHeu
 */
public enum MobilityType {

  /**
   * Undefined
   */
  NONE("none", "none", "none"), //
  /**
   * Mixed types in row bindings - aligned samples from different instruments
   */
  MIXED("none", "none", "mixed"), //
  TIMS("1/k0", "Vs/cm^2", "TIMS"), // trapped ion mobility spectrometry
  DRIFT_TUBE("Drift time", "ms", "DTIMS"), // drift tube
  TRAVELING_WAVE("Drift time", "ms", "TWIMS"), // traveling wave ion mobility spectrometry
  FAIMS("TODO", "TODO", "FAIMS"); // field asymmetric waveform ion mobility spectrometry

  private final String axisLabel;
  private final String unit;
  private final String name;

  MobilityType(String axisLabel, String unit, String name) {
    this.axisLabel = axisLabel;
    this.unit = unit;
    this.name = name;
  }

  public String getAxisLabel() {
    UnitFormat uf = MZmineCore.getConfiguration().getUnitFormat();
    StringBuilder sb = new StringBuilder("Mobility (");
    sb.append(axisLabel);
    sb.append(")");
    return uf.format(sb.toString(), getUnit());
  }

  public String getUnit() {
    return unit;
  }


  @Override
  public String toString() {
    return name;
  }
}
