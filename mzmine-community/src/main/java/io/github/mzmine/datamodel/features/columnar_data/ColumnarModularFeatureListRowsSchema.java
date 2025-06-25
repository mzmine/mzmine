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

package io.github.mzmine.datamodel.features.columnar_data;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.columnar_data.columns.DataColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.DataColumns;
import io.github.mzmine.datamodel.features.columnar_data.columns.arrays.ObjectArrayColumn;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A specific implementation of a columnar data model for {@link FeatureListRow}s. This also links
 * the rows to their features.
 */
public class ColumnarModularFeatureListRowsSchema extends ColumnarModularDataModelSchema {

  private static final Logger logger = Logger.getLogger(
      ColumnarModularFeatureListRowsSchema.class.getName());

  /**
   * Sorted by name LinkedHashMap
   */
  private final Map<RawDataFile, DataColumn<ModularFeature>> filesToFeaturesColumn;

  public ColumnarModularFeatureListRowsSchema(@Nullable final MemoryMapStorage storage,
      final String modelName, final int initialSize, @NotNull List<RawDataFile> dataFiles) {
    super(storage, modelName, initialSize);

    dataFiles = dataFiles.stream().sorted(Comparator.comparing(RawDataFile::getFileName)).toList();

    filesToFeaturesColumn = LinkedHashMap.newLinkedHashMap(dataFiles.size());
    for (final RawDataFile raw : dataFiles) {
      filesToFeaturesColumn.put(raw,
          DataColumns.ofSynchronized(new ObjectArrayColumn<>(initialSize)));
    }
  }

  @Override
  void resizeColumnsTo(final int finalSize) {
    try (var _ = resizeLock.lockWrite()) {
      if (columnLength >= finalSize) {
        return;
      }
      super.resizeColumnsTo(finalSize);

      // resize raw file columns
      long success = filesToFeaturesColumn.values().stream().parallel()
          .filter(column -> column.ensureCapacity(finalSize)).count();
      logger.finest("Resized %d feature columns".formatted(success));
      columnLength = finalSize;
    }
  }

  /**
   * @param rowIndex the row index
   * @param raw      feature for this raw file
   * @param feature  the feature to set
   * @return the old feature or null if there was no previous mapping
   */
  public ModularFeature setFeature(final int rowIndex, final RawDataFile raw,
      final ModularFeature feature) {
    final DataColumn<ModularFeature> featuresCol = filesToFeaturesColumn.get(raw);
    if (featuresCol == null) {
      // raw data files in feature list cannot be changed - so no method should attempt to
      // add a feature of a missing raw data file
      throw new IllegalStateException(
          "This feature list does not contain raw data file '" + raw.getFileName()
              + "'. This points to an issue in the implementation.");
    }
    return featuresCol.set(rowIndex, feature);
  }

  public ModularFeature getFeature(final int rowIndex, final RawDataFile raw) {
    final DataColumn<ModularFeature> featuresCol = filesToFeaturesColumn.get(raw);
    if (featuresCol == null) {
      return null; // previous behaviour was to return null
    }
    return featuresCol.get(rowIndex);
  }

  /**
   * @param rowIndex row index
   * @return non-null stream of features of one row
   */
  public Stream<ModularFeature> streamFeatures(final int rowIndex) {
    return filesToFeaturesColumn.values().stream().map(column -> column.get(rowIndex))
        .filter(Objects::nonNull);
  }

  /**
   * Remove all features for a row
   *
   * @param modelRowIndex row
   * @return true if at least one feature was removed
   */
  public boolean clearFeatures(final int modelRowIndex) {
    boolean changed = false;
    for (final DataColumn<ModularFeature> col : filesToFeaturesColumn.values()) {
      final ModularFeature old = col.set(modelRowIndex, null);
      if (!changed && old != null) {
        changed = true;
      }
    }
    return changed;
  }
}
