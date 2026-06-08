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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.MetadataColumnDoesNotExistException;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt.Mode;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.StringUtils;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The config for a min samples filter
 *
 * @param minSamples min samples
 * @param columnName metadata column can be null to apply to all samples. The column name that will
 *                   be converted to an actual column.
 * @param group      if not null then filter for only this group
 */
public record MinimumSamplesFilterConfig(@NotNull AbsoluteAndRelativeInt minSamples,
                                         @Nullable String columnName, @Nullable String group) {

  public MinimumSamplesFilterConfig(@NotNull AbsoluteAndRelativeInt minSamples) {
    this(minSamples, null);
  }

  public MinimumSamplesFilterConfig(@NotNull AbsoluteAndRelativeInt minSamples,
      @Nullable String columnName) {
    this(minSamples, columnName, null);
  }

  public static final MinimumSamplesFilterConfig DEFAULT = new MinimumSamplesFilterConfig(
      new AbsoluteAndRelativeInt(1, 0f, Mode.ROUND_DOWN), null);

  /**
   * @return a filter that can be applied to filter rows
   * @throws MetadataColumnDoesNotExistException if column is missing
   */
  public @NotNull MinimumSamplesFilter createFilter(List<RawDataFile> files)
      throws MetadataColumnDoesNotExistException {
    return createFilter(files, ProjectService.getMetadata());
  }

  /**
   * @return a filter that can be applied to filter rows
   * @throws MetadataColumnDoesNotExistException if column is missing
   */
  public @NotNull MinimumSamplesFilter createFilter(List<RawDataFile> files,
      @NotNull MetadataTable metadata) throws MetadataColumnDoesNotExistException {
    if (StringUtils.hasValue(columnName)) {
      // group by column
      final MetadataColumn<?> column = metadata.getColumnByName(columnName);
      if (column == null) {
        throw new MetadataColumnDoesNotExistException(columnName);
      }
      final Map<?, List<RawDataFile>> groups = metadata.groupFilesByColumn(files, column);

      // only use one group or all groups depending on group field
      final List<List<RawDataFile>> groupedFiles;
      if (group != null) {
        final List<RawDataFile> g = groups.get(group);
        if (g == null) {
          throw new IllegalArgumentException(
              "Group " + group + " does not exist in metadata in column " + columnName);
        }
        groupedFiles = List.of(g);
      } else {
        groupedFiles = List.copyOf(groups.values());
      }

      return new MinimumSamplesFilter(minSamples, column, group, files, groupedFiles);
    }

    // all samples no grouping
    return new MinimumSamplesFilter(minSamples, null, null, files, null);
  }

}
