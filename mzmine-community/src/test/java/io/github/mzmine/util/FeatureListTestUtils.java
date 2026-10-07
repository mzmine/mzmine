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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared test utilities for creating synthetic feature lists with N raw data files and N rows.
 */
public final class FeatureListTestUtils {

  private FeatureListTestUtils() {
  }

  /**
   * Creates {@code count} raw data files named {@code prefix_1, prefix_2, …}, each with an
   * acquisition timestamp of {@code startTime + i * interval}.
   */
  public static @NotNull List<RawDataFileImpl> createRawFiles(int count, @NotNull String prefix,
      @NotNull LocalDateTime startTime, @NotNull Duration interval) {
    final List<RawDataFileImpl> files = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      final String name = prefix + "_" + (i + 1);
      final RawDataFileImpl file = new RawDataFileImpl(name, null, null);
      file.setStartTimeStamp(startTime.plus(interval.multipliedBy(i)));
      files.add(file);
    }
    return files;
  }

  /**
   * Creates a {@link ModularFeatureList} containing the given files and {@code numRows} rows, where
   * every feature for every file has the same height = area = {@code abundance}, mz = 100.0 and rt
   * = 5.0. Values can also be changed after creating the featurelist.
   */
  public static @NotNull ModularFeatureList createFeatureList(@NotNull String name,
      @NotNull List<? extends RawDataFile> files, int numRows, float abundance) {
    final ModularFeatureList featureList = new ModularFeatureList(name, null,
        files.toArray(new RawDataFile[0]));
    final List<Float> abundances = new ArrayList<>(files.size());
    for (int i = 0; i < files.size(); i++) {
      abundances.add(abundance);
    }
    for (int row = 1; row <= numRows; row++) {
      addRow(featureList, row, files, abundances);
    }
    return featureList;
  }

  /**
   * Adds a row to {@code featureList} where each {@code files[i]} gets a feature with height = area
   * = {@code abundances.get(i)}, mz = 100.0 and rt = 5.0.
   */
  public static @NotNull ModularFeatureListRow addRow(@NotNull ModularFeatureList featureList,
      int rowId, @NotNull List<? extends RawDataFile> files, @NotNull List<@Nullable Float> abundances) {
    return addRow(featureList, rowId, files, abundances, 100.0, 5.0f);
  }

  /**
   * Adds a row to {@code featureList} where each {@code files[i]} gets a feature with height = area
   * = {@code abundances.get(i)}, the given {@code mz} and {@code rt}.
   *
   * <p>The short-form overload delegates here with mz = 100.0, rt = 5.0.
   */
  public static @NotNull ModularFeatureListRow addRow(@NotNull ModularFeatureList featureList,
      int rowId, @NotNull List<? extends RawDataFile> files, @NotNull List<@Nullable Float> abundances,
      double mz, float rt) {
    if (files.size() != abundances.size()) {
      throw new IllegalArgumentException(
          "files and abundances must have the same size: " + files.size() + " vs "
              + abundances.size());
    }
    final ModularFeatureListRow row = new ModularFeatureListRow(featureList, rowId);
    for (int i = 0; i < files.size(); i++) {
      final Float abundance = abundances.get(i);
      if (abundance != null) {
        final RawDataFile file = files.get(i);
        final ModularFeature feature = new ModularFeature(featureList, file,
            FeatureStatus.DETECTED);
        feature.setHeight(abundance);
        feature.setArea(abundance);
        feature.setMZ(mz);
        feature.setRT(rt);
        row.addFeature(file, feature, false);
      }
    }
    row.applyRowBindings();
    featureList.addRow(row);
    return row;
  }
}
