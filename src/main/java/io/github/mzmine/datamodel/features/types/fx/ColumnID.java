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

package io.github.mzmine.datamodel.features.types.fx;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class to keep track of columns in a {@link io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX}.
 * Is set in the {@link io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX}
 * because the data type itself does not know, if it creates a row or feature column.
 * <p>
 * For usage example see {@link FeatureTableFX#applyColumnVisibility}.
 */
public class ColumnID {

  @NotNull
  private final ColumnType type;

  @NotNull
  private final DataType dt;

  @Nullable
  private final RawDataFile raw;
  private final int subcolumnIndex;
  /**
   * This is the raw data independent header: can be used for maps
   */
  @NotNull
  private final String combinedHeader;

  /**
   * @param dt   The {@link DataType} this column represents
   * @param type The {@link ColumnType} to determine if this is a feature or row type column.
   * @param raw  The {@link RawDataFile} this column belongs to or null if not a features type
   *             column.
   */
  public ColumnID(@NotNull DataType dt, @NotNull ColumnType type, @Nullable RawDataFile raw,
      int subcolumnIndex) {

    this.subcolumnIndex = subcolumnIndex;
    this.dt = dt;
    this.type = type;
    this.raw = raw;
    String featPre = type == ColumnType.FEATURE_TYPE ? "Feature:" : "";
    if (subcolumnIndex >= 0 && dt instanceof SubColumnsFactory sub) {
      this.combinedHeader = featPre + dt.getHeaderString() + ":" + sub.getHeader(subcolumnIndex);
    } else {
      this.combinedHeader = featPre + dt.getHeaderString();
    }
  }

  /**
   * Checks if all column Disregards the raw data file. Used to check if a column is visible for
   * example
   *
   * @param other
   * @return true if equal (without raw)
   */
  public boolean typesMatch(ColumnID other) {
    return type == other.type && dt.equals(other.dt) && subcolumnIndex == other.subcolumnIndex;
  }

  /**
   * Match if combined headers equal
   *
   * @return true if combined headers equal
   */
  public boolean typesMatch(String combinedHeader) {
    return this.combinedHeader.equals(combinedHeader);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ColumnID columnID = (ColumnID) o;
    return type == columnID.type && dt.equals(columnID.dt) && Objects.equals(raw, columnID.raw)
           && subcolumnIndex == columnID.subcolumnIndex;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, dt, raw, subcolumnIndex);
  }

  @NotNull
  public ColumnType getType() {
    return type;
  }

  @NotNull
  public DataType getDataType() {
    return dt;
  }

  @Nullable
  public RawDataFile getRaw() {
    return raw;
  }

  public String getFormattedString() {
    return "DataType: " + dt.getHeaderString() + "\tType: " + type.toString() + "\tRawDataFile: "
           + raw;
  }

  @Override
  public String toString() {
    return combinedHeader;
  }

  /**
   * The combined header disregards the raw data file and can therefore be used in maps to point at
   * all feature columns of this type
   *
   * @return the combined header e.g., Height or Feature:rt range:min for a row type or feature
   * type, respectively
   */
  public String getCombinedHeaderString() {
    return combinedHeader;
  }

  public int getSubColIndex() {
    return subcolumnIndex;
  }

}
