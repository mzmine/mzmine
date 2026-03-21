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
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteAndRelativeInt;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The actual filter with prepared groups of files. Created by
 * {@link MinimumSamplesFilterConfig#createFilter(List)}
 */
public final class MinimumSamplesFilter {

  private final @NotNull AbsoluteAndRelativeInt minSamples;
  private final @Nullable MetadataColumn<?> column;
  private final @Nullable String group;
  private final @NotNull List<RawDataFile> files;
  private final @Nullable List<List<RawDataFile>> groupedFiles;

  /**
   * @param minSamples   min samples
   * @param column       the column or null
   * @param group        if present then only filter for this group - this means that the grouped
   *                     files will only contain the raw data files from this group. This is done
   *                     before creation in {@link MinimumSamplesFilterConfig#createFilter(List)}
   * @param files        all data files
   * @param groupedFiles grouped by column or null if there is no column. If a group is set then
   *                     only provide the grouped files for this single group.
   */
  MinimumSamplesFilter(@NotNull AbsoluteAndRelativeInt minSamples,
      @Nullable MetadataColumn<?> column, @Nullable String group, @NotNull List<RawDataFile> files,
      @Nullable List<List<RawDataFile>> groupedFiles) {
    this.minSamples = minSamples;
    this.column = column;
    this.group = group;
    this.files = files;
    this.groupedFiles = groupedFiles;
  }

  public boolean matches(FeatureListRow row) {
    if (groupedFiles == null) {
      // just match against all files
      int detections = 0;
      for (RawDataFile raw : files) {
        if (row.getFeature(raw) != null) {
          detections++;
        }
      }
      return minSamples.checkGreaterEqualMax(files.size(), detections);
    }

    // check groups in a column
    for (List<RawDataFile> raws : groupedFiles) {
      if (raws.isEmpty()) {
        continue;
      }
      int detections = 0;
      for (RawDataFile raw : raws) {
        if (row.getFeature(raw) != null) {
          detections++;
        }
      }
      if (minSamples.checkGreaterEqualMax(raws.size(), detections)) {
        return true;
      }
    }
    // no match
    return false;
  }

  /**
   * Tests for mismatch between parameters
   *
   * @return null if all correct or a message if invalid config
   */
  public @Nullable String getInvalidConfigMessage(String filterName, FeatureList featureList) {
    if (groupedFiles == null) {
      // check all files
      final int totalSamples = files.size();
      final int numMinSamples = minSamples().getMaximumValue(totalSamples);

      if (numMinSamples > totalSamples) {
        var errorMessage = """
            The "%s" parameter in the feature list rows filter step requires %d samples, but \
            the processed feature list %s only contains %d samples. Check the feature list rows \
            filter and adjust the minimum number of samples. Relative percentages help to scale this parameter automatically from small to large datasets.
            The current processing step and all following will be cancelled.""".formatted(
            filterName, numMinSamples, featureList, totalSamples);

        return errorMessage;
      }
    } else {
      // any group needs to match the requirements then the filter is OK
      for (List<RawDataFile> files : groupedFiles) {
        final int totalSamples = files.size();
        final int numMinSamples = minSamples().getMaximumValue(totalSamples);

        if (numMinSamples <= totalSamples) {
          return null;
        }
      }
      // if no group had enough samples through error
      var errorMessage = """
          The "%s" parameter in the feature list rows filter step requires %d samples in any group, but \
          the processed feature list %s had NO group that satisfies this filter in column %s (%d groups). Check the feature list rows \
          filter and adjust the minimum number of samples. Relative percentages help to scale this parameter automatically from small to large datasets.
          The current processing step and all following will be cancelled.""".formatted(filterName,
          minSamples.abs(), featureList, column.getTitle(), groupedFiles.size());

      return errorMessage;
    }
    return null;
  }

  public @NotNull AbsoluteAndRelativeInt minSamples() {
    return minSamples;
  }

  public @Nullable MetadataColumn<?> column() {
    return column;
  }

  public @Nullable String group() {
    return group;
  }

  public @NotNull List<RawDataFile> files() {
    return files;
  }

  public @Nullable List<List<RawDataFile>> groupedFiles() {
    return groupedFiles;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (MinimumSamplesFilter) obj;
    return Objects.equals(this.minSamples, that.minSamples) && Objects.equals(this.column,
        that.column) && Objects.equals(this.group, that.group) && Objects.equals(this.files,
        that.files) && Objects.equals(this.groupedFiles, that.groupedFiles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(minSamples, column, group, files, groupedFiles);
  }

  @Override
  public String toString() {
    return "MinimumSamplesFilter[" + "minSamples=" + minSamples + ", " + "column=" + column + ", "
        + "group=" + group + ", " + "files=" + files + ", " + "groupedFiles=" + groupedFiles + ']';
  }

}
