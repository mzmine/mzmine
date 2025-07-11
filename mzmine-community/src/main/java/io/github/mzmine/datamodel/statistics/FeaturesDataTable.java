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

package io.github.mzmine.datamodel.statistics;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.collections.CollectionUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public final class FeaturesDataTable implements ModifiableDataTable {

  public static final FeaturesDataTable EMPTY = new FeaturesDataTable(List.of(),
      new FeatureListRowAbundances[0]);

  private final @NotNull List<RawDataFile> dataFiles;
  private final @NotNull FeatureListRowAbundances[] dataRows;

  public FeaturesDataTable(@NotNull List<RawDataFile> dataFiles,
      @NotNull FeatureListRowAbundances[] dataRows) {
    this.dataFiles = dataFiles;
    this.dataRows = dataRows;
  }

  public Stream<double[]> streamRows() {
    return Arrays.stream(dataRows).map(FeatureListRowAbundances::abundances);
  }

  public @NotNull List<RawDataFile> getRawDataFiles() {
    return Collections.unmodifiableList(dataFiles);
  }

  @Override
  public int getNumberOfSamples() {
    return dataFiles.size();
  }

  @Override
  public int getNumberOfFeatures() {
    return dataRows.length;
  }

  public RawDataFile getRawDataFile(int index) {
    return dataFiles.get(index);
  }


  /**
   * @return map of the data file to index
   */
  public @NotNull Map<RawDataFile, Integer> getDataFileIndexMap() {
    return CollectionUtils.indexMap(getRawDataFiles());
  }

  /**
   * @return map of the row to index
   */
  public @NotNull Map<FeatureListRow, Integer> getFeatureRowIndexMap() {
    return CollectionUtils.indexMap(getFeatureListRows());
  }

  public @NotNull Stream<FeatureListRowAbundances> streamDataRows() {
    return Arrays.stream(dataRows);
  }

  public @NotNull List<FeatureListRow> getFeatureListRows() {
    return streamDataRows().map(FeatureListRowAbundances::row).toList();
  }

  @Override
  public double[] getFeatureData(int i, boolean copy) {
    final double[] abundances = dataRows[i].abundances();
    if (copy) {
      return Arrays.copyOf(abundances, abundances.length);
    }
    return abundances;
  }

  @Override
  public double[] getSampleData(int index) {
    final double[] data = new double[dataRows.length];
    for (int i = 0; i < dataRows.length; i++) {
      data[i] = dataRows[i].abundances()[index];
    }
    return data;
  }

  @Override
  public FeaturesDataTable copy() {
    final var dataClone = Arrays.stream(dataRows).map(FeatureListRowAbundances::copy)
        .toArray(FeatureListRowAbundances[]::new);
    return new FeaturesDataTable(dataFiles, dataClone);
  }

  /**
   * @param group selected raw data files
   * @return a subset of all rows but only selected raw data files
   */
  public FeaturesDataTable subsetBySamples(List<RawDataFile> group) {
    // find index of raw data files
    final Map<RawDataFile, Integer> fileIndexMap = CollectionUtils.indexMap(dataFiles);

    final int[] groupIndexes = group.stream().mapToInt(fileIndexMap::get).toArray();
    if (groupIndexes.length != group.size()) {
      for (RawDataFile raw : group) {
        if (!fileIndexMap.containsKey(raw)) {
          throw new IllegalArgumentException(
              "Raw data file " + raw.getName() + " is not in the data table.");
        }
      }
    }

    final FeatureListRowAbundances[] subData = Arrays.stream(dataRows)
        .map(row -> row.subsetByIndexes(groupIndexes)).toArray(FeatureListRowAbundances[]::new);

    return new FeaturesDataTable(group, subData);
  }

  public @NotNull FeatureListRowAbundances getFeatureRow(int rowIndex) {
    return dataRows[rowIndex];
  }
}
