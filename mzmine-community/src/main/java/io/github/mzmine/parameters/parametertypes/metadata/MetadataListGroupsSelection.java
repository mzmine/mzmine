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
import io.github.mzmine.util.StringUtils;
import java.util.Collection;
import java.util.LinkedHashSet;
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
public final class MetadataListGroupsSelection implements MetadataGroupsSelection {

  public static MetadataListGroupsSelection NONE = new MetadataListGroupsSelection("", Set.of());

  private final @NotNull String columnName;
  /**
   * As entered by the user, only trimmed - this is what is shown and saved.
   */
  private final @NotNull Set<@NotNull String> groups;
  /**
   * Lower cased copy of {@link #groups} used for matching, see {@link #matchesValue(Object)}.
   */
  private final @NotNull Set<@NotNull String> normalizedGroups;


  public MetadataListGroupsSelection(@NotNull String columnName,
      @NotNull Collection<String> groups) {
    this.columnName = columnName.trim();
    this.groups = groups.stream().map(String::trim).filter(StringUtils::hasValue)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    this.normalizedGroups = this.groups.stream().map(StringUtils::normalizeStripLowerCase)
        .collect(Collectors.toSet());
  }

  /**
   * @return Checks if the current metadata table contains the specified column and the specified
   * values. Case insensitive, see {@link #matchesValue(Object)}.
   */
  @Override
  public boolean isValid() {
    if (columnName.isBlank() || groups.isEmpty()) {
      return false;
    }

    final Map<RawDataFile, Object> columnValues = getColumnData();
    if (columnValues == null) {
      return false;
    }

    // usually few values in groups so ok to stream for each value
    return columnValues.values().stream().anyMatch(this::matchesValue);
  }

  /**
   * Matches ignoring case and surrounding whitespace, so that a metadata column holding {@code QC}
   * and a selection of {@code qc} refer to the same group.
   */
  @Override
  public boolean matchesValue(@Nullable Object value) {
    return value != null && normalizedGroups.contains(StringUtils.normalizeStripLowerCase(value));
  }

  @Override
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
