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
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.StringUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Select multiple groups in a column
 *
 */
public final class MetadataListGroupsSelection {

  public static MetadataListGroupsSelection NONE = new MetadataListGroupsSelection("", Set.of());

  private final @NotNull String columnName;
  private final @NotNull Set<@NotNull String> groups;


  public MetadataListGroupsSelection(@NotNull String columnName,
      @NotNull Collection<String> groups) {
    this.columnName = columnName;
    this.groups = groups.stream().map(String::trim).filter(StringUtils::hasValue)
        .collect(Collectors.toSet());
  }

  /**
   * @return Checks if the current metadata table contains the specified column and the specified
   * values. Case sensitive.
   */
  public boolean isValid() {
    if (columnName.isBlank()) {
      return false;
    }

    final MetadataTable metadata = ProjectService.getMetadata();

    final MetadataColumn<?> column = getColumn();
    if (column == null) {
      return false;
    }

    final Map<RawDataFile, Object> columnValues = metadata.getData().get(column);
    // usually few values in groups so ok to stream for each value
    return columnValues.values().stream().anyMatch(this::matchesValue);
  }

  private boolean matchesValue(Object value) {
    return value != null && groups.contains(value.toString());
  }

  /**
   * @return The actual column from the metadata table or null.
   */
  @Nullable
  public MetadataColumn<?> getColumn() {
    return ProjectService.getMetadata().getColumnByName(columnName());
  }

  /**
   * @return A list of files that match have the same value as {@link #groups()} in the specified
   * {@link #columnName} in the {@link MetadataTable}. Empty list if the column does not exist or it
   * does not contain the value.
   */
  public List<RawDataFile> getMatchingFiles() {
    return getMatchingFiles(ProjectService.getProject().getCurrentRawDataFiles());
  }

  /**
   * @return A list of files that match have the same value as {@link #groups()} in the specified
   * {@link #columnName} in the {@link MetadataTable}. Empty list if the column does not exist or it
   * does not contain the value.
   */
  public List<RawDataFile> getMatchingFiles(@NotNull List<RawDataFile> dataFiles) {
    if (!isValid()) {
      return List.of();
    }

    final Map<RawDataFile, Object> column = ProjectService.getMetadata().getData().get(getColumn());

    return dataFiles.stream().filter(raw -> matchesValue(column.get(raw))).toList();
  }

  /**
   * @return A list of files that do NOT match the values in {@link #groups()} in the specified
   * {@link #columnName} in the {@link MetadataTable}. Copy of input list if the column does not
   * exist or it does not contain the value.
   */
  public List<RawDataFile> removeMatchingFilesCopy(@NotNull List<RawDataFile> dataFiles) {
    if (!isValid()) {
      return List.copyOf(dataFiles);
    }

    final Map<RawDataFile, Object> column = ProjectService.getMetadata().getData().get(getColumn());

    return dataFiles.stream().filter(raw -> !matchesValue(column.get(raw))).toList();
  }

  public @NotNull String columnName() {
    return columnName;
  }

  public @NotNull Set<@NotNull String> groups() {
    return groups;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (MetadataListGroupsSelection) obj;
    return Objects.equals(this.columnName, that.columnName) && Objects.equals(this.groups,
        that.groups);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columnName, groups);
  }

  @Override
  public String toString() {
    return "MetadataListGroupsSelection[" + "columnName=" + columnName + ", " + "groups=" + groups
        + ']';
  }

}
