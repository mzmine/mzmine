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
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Metadata1GroupSelection(@NotNull String columnName,
                                      @NotNull String groupStr) implements MetadataGroupsSelection {

  public static Metadata1GroupSelection NONE = new Metadata1GroupSelection("", "");

  public Metadata1GroupSelection(@NotNull String columnName, @NotNull String groupStr) {
    this.columnName = columnName.trim();
    this.groupStr = groupStr.trim();
  }

  /**
   * @return Checks if the current metadata table contains the specified column and the specified
   * value. Case sensitive.
   */
  @Override
  public boolean isValid() {
    if (columnName.isBlank() || groupStr.isBlank()) {
      return false;
    }

    final Map<RawDataFile, Object> columnValues = getColumnData();
    if (columnValues == null) {
      return false;
    }

    return columnValues.values().stream().anyMatch(this::matchesValue);
  }

  @Override
  public boolean matchesValue(@Nullable Object value) {
    return value != null && value.toString().equals(groupStr);
  }

}
