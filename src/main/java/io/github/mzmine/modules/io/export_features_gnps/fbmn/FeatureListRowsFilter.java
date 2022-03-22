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

package io.github.mzmine.modules.io.export_features_gnps.fbmn;

import io.github.mzmine.datamodel.features.FeatureListRow;

/**
 * Define which rows to export
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public enum FeatureListRowsFilter {
  ALL, ONLY_WITH_MS2, MS2_OR_ION_IDENTITY, MS2_AND_ION_IDENTITY;

  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }

  /**
   * Filter a row
   *
   * @return true if row conforms to the filter
   */
  public boolean accept(FeatureListRow row) {
    return switch (this) {
      case ALL -> true;
      case ONLY_WITH_MS2 -> row.hasMs2Fragmentation();
      case MS2_OR_ION_IDENTITY -> row.hasMs2Fragmentation() || row.hasIonIdentity();
      case MS2_AND_ION_IDENTITY -> row.hasMs2Fragmentation() && row.hasIonIdentity();
    };
  }

  /**
   * @return all rows accepted by this filter require MS2
   */
  public boolean requiresMS2() {
    return switch (this) {
      case ALL, MS2_OR_ION_IDENTITY -> false;
      case ONLY_WITH_MS2, MS2_AND_ION_IDENTITY -> true;
    };
  }
}
