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
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class ColumnarModularFeatureListRowsSchema extends ColumnarModularDataModelSchema {

  private static final Logger logger = Logger.getLogger(
      ColumnarModularFeatureListRowsSchema.class.getName());

  private final Map<RawDataFile, ModularFeature[]> features;

  public ColumnarModularFeatureListRowsSchema(final MemoryMapStorage storage,
      final String modelName, final int initialSize, final @NotNull List<RawDataFile> dataFiles) {
    super(storage, modelName, initialSize);

    features = HashMap.newHashMap(dataFiles.size());
    dataFiles.forEach(raw -> features.put(raw, new ModularFeature[initialSize]));
  }

  @Override
  public void resizeColumnsTo(final int finalSize) {
    try (var _ = resizeLock.lockWrite()) {
      if (columnLength >= finalSize) {
        return;
      }
      // resize
      long success = columns.values().stream().parallel()
          .filter(column -> column.ensureCapacity(finalSize)).count();

//      logger.info("""
//          Resized %d of %d columns in model %s to %d rows""".formatted(success, columns.size(),
//          modelName, finalSize));

      columnLength = finalSize;

      // resize features arrays
      for (final Entry<RawDataFile, ModularFeature[]> entry : features.entrySet()) {
        final ModularFeature[] copyColumn = Arrays.copyOf(entry.getValue(), finalSize);
        entry.setValue(copyColumn);
      }
    }
    super.resizeColumnsTo(finalSize);
  }

  /**
   * @param rowIndex the row index
   * @param raw      feature for this raw file
   * @param feature  the feature to set
   * @return the old feature or null if there was no previous mapping
   */
  public ModularFeature setFeature(final int rowIndex, final RawDataFile raw,
      final ModularFeature feature) {
    final ModularFeature[] featuresCol = features.get(raw);
    if (featuresCol == null) {
      return null; // previous behaviour was to return null
    }
    var old = featuresCol[rowIndex];
    featuresCol[rowIndex] = feature;
    return old;
  }

  public ModularFeature getFeature(final int rowIndex, final RawDataFile raw) {
    final ModularFeature[] featuresCol = features.get(raw);
    if (featuresCol == null) {
      return null; // previous behaviour was to return null
    }
    return featuresCol[rowIndex];
  }

  /**
   * @param rowIndex row index
   * @return non-null stream of features of one row
   */
  public Stream<ModularFeature> streamFeatures(final int rowIndex) {
    return features.values().stream().map(column -> column[rowIndex]).filter(Objects::nonNull);
  }
}
