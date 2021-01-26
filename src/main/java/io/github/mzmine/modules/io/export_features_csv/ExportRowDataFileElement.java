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

package io.github.mzmine.modules.io.export_features_csv;

public enum ExportRowDataFileElement {

  FEATURE_STATUS("Feature status", false), //
  FEATURE_NAME("Feature name", false), //
  FEATURE_MZ("Feature m/z", false), //
  FEATURE_RT("Feature RT", false), //
  FEATURE_RT_START("Feature RT start", false), //
  FEATURE_RT_END("Feature RT end", false), //
  FEATURE_DURATION("Feature duration time", false), //
  FEATURE_HEIGHT("Feature height", false), //
  FEATURE_AREA("Feature area", false), //
  FEATURE_CHARGE("Feature charge", false), //
  FEATURE_DATAPOINTS("Feature # data points", false), //
  FEATURE_FWHM("Feature FWHM", false), //
  FEATURE_TAILINGFACTOR("Feature tailing factor", false), //
  FEATURE_ASYMMETRYFACTOR("Feature asymmetry factor", false), //
  FEATURE_MZMIN("Feature m/z min", false), //
  FEATURE_MZMAX("Feature m/z max", false);

  private final String name;
  private final boolean common;

  ExportRowDataFileElement(String name, boolean common) {
    this.name = name;
    this.common = common;
  }

  public boolean isCommon() {
    return this.common;
  }

  @Override
  public String toString() {
    return this.name;
  }

}
