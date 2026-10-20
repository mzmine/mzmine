/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface MetadataGroupsSelection permits Metadata2GroupsSelection,
    Metadata1GroupSelection, MetadataListGroupsSelection {

  @NotNull String columnName();

  /**
   * @param metadata the column and values are searched in this metadata table
   * @return true if the metadata table contains the column and the selected values
   */
  boolean isValid(@NotNull MetadataTable metadata);

  boolean matchesValue(@Nullable Object value);

  /**
   * @param metadata the column is searched in this metadata table
   * @return The actual column from the metadata table or null.
   */
  default @Nullable MetadataColumn<?> getColumn(@NotNull MetadataTable metadata) {
    return metadata.getColumnByName(columnName());
  }

  /**
   * @param metadata the column data is taken from this metadata table
   * @return The actual column data from the metadata table or null.
   */
  default @Nullable Map<RawDataFile, Object> getColumnData(@NotNull MetadataTable metadata) {
    return metadata.getColumnData(getColumn(metadata));
  }

  /**
   * @param metadata the values of the files are taken from this metadata table
   * @return A list of files that match have the same value as the groups in the specified
   * {@link #columnName} in the {@link MetadataTable}. Empty list if the column does not exist or it
   * does not contain the value.
   */
  default @NotNull List<RawDataFile> getMatchingFiles(@NotNull MetadataTable metadata,
      @NotNull List<RawDataFile> dataFiles) {
    final Map<RawDataFile, Object> column = getColumnData(metadata);
    if (!isValid(metadata) || column == null) {
      return List.of();
    }

    return dataFiles.stream().filter(raw -> matchesValue(column.get(raw))).toList();
  }

  /**
   * @param metadata the values of the files are taken from this metadata table
   * @return A list of files that do NOT match the values in the groups in the specified
   * {@link #columnName} in the {@link MetadataTable}. Copy of input list if the column does not
   * exist or it does not contain the value.
   */
  default @NotNull List<RawDataFile> removeMatchingFilesCopy(@NotNull MetadataTable metadata,
      @NotNull List<RawDataFile> dataFiles) {
    final Map<RawDataFile, Object> column = getColumnData(metadata);
    if (!isValid(metadata) || column == null) {
      return List.copyOf(dataFiles);
    }

    return dataFiles.stream().filter(raw -> !matchesValue(column.get(raw))).toList();
  }

}
