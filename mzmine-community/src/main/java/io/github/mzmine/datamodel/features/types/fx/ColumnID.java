/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.fx;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class to keep track of columns in a
 * {@link io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX}. Is set in
 * the {@link io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX}
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
  private final String uniqueIdStr;

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
    uniqueIdStr = buildUniqueIdString(type, dt, subcolumnIndex);
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

  public String getUniqueIdString() {
    return uniqueIdStr;
  }

  public int getSubColIndex() {
    return subcolumnIndex;
  }

  public static String buildUniqueIdString(ColumnType type, DataType<?> dt, int subcolumnIndex) {
    if (subcolumnIndex >= 0 && dt instanceof SubColumnsFactory sub) {
      return buildUniqueIdString(type, dt.getUniqueID(), sub.getUniqueID(subcolumnIndex));
    } else {
      return buildUniqueIdString(type, dt.getUniqueID(), null);
    }
  }

  public static String buildUniqueIdString(ColumnType type, Class<? extends DataType<?>> parent,
      Class<? extends DataType<?>> child) {
    final DataType<?> p = DataTypes.get(parent);
    if (child != null && p instanceof SubColumnsFactory scf) {
      // scf does not have a list of all the sub-columns. we would have to iterate over all sub
      // columns to find the index, so its better to simply trust the child type actually exists.
      final DataType<?> c = DataTypes.get(child);
      return buildUniqueIdString(type, p.getUniqueID(), c.getUniqueID());
    }
    return buildUniqueIdString(type, p.getUniqueID(), null);
  }

  public static String buildUniqueIdString(ColumnType type, @NotNull String parentUniqueId,
      @Nullable String childUniqueId) {
    String featPre = type == ColumnType.FEATURE_TYPE ? "Feature:" : "";
    if (childUniqueId != null && !childUniqueId.isBlank()) {
      return "%s%s:%s".formatted(featPre, parentUniqueId, childUniqueId);
    }
    return featPre + parentUniqueId;
  }
}
