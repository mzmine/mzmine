/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.javafx.groupabletreeview;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.Comparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Groups feature lists by a metadata column of their raw data file. Only applies to feature lists
 * with a single raw data file. Aligned feature lists (multiple raw data files) are grouped under
 * "Aligned feature lists".
 */
public final class FeatureListByMetadataStrategy implements GroupingStrategy<FeatureList> {

  private final MetadataColumn<?> column;

  public FeatureListByMetadataStrategy(@NotNull final MetadataColumn<?> column) {
    this.column = column;
  }

  @Override
  public @NotNull String displayName() {
    return column.getTitle() + " (Metadata)";
  }

  @Override
  public @Nullable String getGroupName(@NotNull final FeatureList item) {
    if (item.isAligned()) {
      return "Aligned feature lists";
    }
    final RawDataFile rawDataFile = item.getRawDataFile(0);
    final Object value = ProjectService.getMetadata().getValue(column, rawDataFile);
    if (value == null) {
      return null;
    }
    final String str = value.toString();
    return str.isEmpty() ? null : str;
  }

  @Override
  public boolean isCustom() {
    return false;
  }

  @Override
  public @NotNull Comparator<FeatureList> itemComparator() {
    // more applied methods first
    return Comparator.<FeatureList>comparingInt(fl -> fl.getAppliedMethods().size()).reversed();
  }

  public @NotNull MetadataColumn<?> getColumn() {
    return column;
  }
}
