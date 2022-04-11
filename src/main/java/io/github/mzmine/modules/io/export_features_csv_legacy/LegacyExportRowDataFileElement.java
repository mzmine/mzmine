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

package io.github.mzmine.modules.io.export_features_csv_legacy;

public enum LegacyExportRowDataFileElement {

  FEATURE_STATUS("Feature status"),//
  FEATURE_NAME("Feature name"), //
  FEATURE_MZ("Feature m/z"), //
  FEATURE_RT("Feature RT"), //
  FEATURE_RT_START("Feature RT start"), //
  FEATURE_RT_END("Feature RT end"), //
  FEATURE_DURATION("Feature duration time"), //
  // needs to be named Peak for GNPS (for now)
  FEATURE_HEIGHT("Peak height"), //
  FEATURE_AREA("Peak area"), //
  FEATURE_ION_MOBILITY("Ion mobility"), //
  FEATURE_ION_MOBILITY_UNIT("Ion mobility unit"), //
  FEATURE_CCS("CCS"), //
  FEATURE_CHARGE("Feature charge"), //
  FEATURE_DATAPOINTS("Feature # data points"), //
  FEATURE_FWHM("Feature FWHM"), //
  FEATURE_TAILINGFACTOR("Feature tailing factor"), //
  FEATURE_ASYMMETRYFACTOR("Feature asymmetry factor"), //
  FEATURE_MZMIN("Feature m/z min"), //
  FEATURE_MZMAX("Feature m/z max");

  private final String name;

  LegacyExportRowDataFileElement(String name) {
    this.name = name;
  }

  public String toString() {
    return this.name;
  }

}
