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

package io.github.mzmine.parameters.parametertypes.metadata;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Metadata2GroupsSelection(@NotNull String columnName, @NotNull String groupA,
                                       @NotNull String groupB) implements MetadataGroupsSelection {

  public final static Metadata2GroupsSelection NONE = new Metadata2GroupsSelection("", "", "");

  public Metadata2GroupsSelection(@NotNull String columnName, @NotNull String groupA,
      @NotNull String groupB) {
    this.columnName = columnName.trim();
    this.groupA = groupA.trim();
    this.groupB = groupB.trim();
  }

  /**
   * @return Checks if the current metadata table contains the specified column and the specified
   * value. Case sensitive.
   */
  @Override
  public boolean isValid() {
    if (columnName.isBlank() || groupA.isBlank() || groupB.isBlank()) {
      return false;
    }

    final Map<RawDataFile, Object> columnValues = getColumnData();
    if (columnValues == null) {
      return false;
    }

    // requires both A and B to have at least one value
    boolean hasA = false;
    boolean hasB = false;
    for (Object value : columnValues.values()) {
      if (!hasA && matchesA(value)) {
        hasA = true;
      }
      if (!hasB && matchesB(value)) {
        hasB = true;
      }
      if (hasA && hasB) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean matchesValue(@Nullable Object value) {
    if (value == null) {
      return false;
    }
    final String valueStr = value.toString();
    return groupA.equals(valueStr) || groupB.equals(valueStr);
  }

  public boolean matchesA(@Nullable Object value) {
    return value != null && groupA.equals(value.toString());
  }

  public boolean matchesB(@Nullable Object value) {
    return value != null && groupB.equals(value.toString());
  }

  /**
   * @return A list of files that match the same value as {@link #groupA()} in the specified
   * {@link #columnName} in the {@link MetadataTable}. Empty list if the column does not exist or it
   * does not contain the value.
   */
  public List<RawDataFile> getMatchingFilesA(List<RawDataFile> rawFiles) {
    final Map<RawDataFile, Object> column = getColumnData();
    if (!isValid() || column == null) {
      return List.of();
    }

    return rawFiles.stream().filter(raw -> matchesA(column.get(raw))).toList();
  }

  /**
   * @return A list of files that match the same value as {@link #groupB()} in the specified
   * {@link #columnName} in the {@link MetadataTable}. Empty list if the column does not exist or it
   * does not contain the value.
   */
  public List<RawDataFile> getMatchingFilesB(List<RawDataFile> rawFiles) {
    final Map<RawDataFile, Object> column = getColumnData();
    if (!isValid() || column == null) {
      return List.of();
    }

    return rawFiles.stream().filter(raw -> matchesB(column.get(raw))).toList();
  }
}
