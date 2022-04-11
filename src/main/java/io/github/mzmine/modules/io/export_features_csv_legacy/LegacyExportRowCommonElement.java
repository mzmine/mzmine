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

public enum LegacyExportRowCommonElement {

  ROW_ID("Export row ID"), //
  ROW_MZ("Export row m/z"), //
  ROW_RT("Export row retention time"), //
  ROW_ION_MOBILITY("Export row ion mobility"), //
  ROW_ION_MOBILITY_UNIT("Export row ion mobility unit"), //
  ROW_CCS("Export row CCS"), //
  ROW_IDENTITY("Export row identity (main ID)"), //
  ROW_IDENTITY_ALL("Export row identity (all IDs)"), //
  ROW_IDENTITY_DETAILS("Export row identity (main ID + details)"), //
  ROW_COMMENT("Export row comment"), //
  ROW_FEATURE_NUMBER("Export row number of detected features"), //
  ROW_CORR_GROUP_ID("Export correlation group ID"), //
  ROW_MOL_NETWORK_ID("Export annotation network number"), //
  ROW_BEST_ANNOTATION("Export best ion annotation"), //
  ROW_BEST_ANNOTATION_AND_SUPPORT("Export best ion annotation (+support)"), //
  ROW_NEUTRAL_MASS("Export neutral M mass");

  private final String name;

  LegacyExportRowCommonElement(String name) {
    this.name = name;
  }

  public String toString() {
    return this.name;
  }
}
