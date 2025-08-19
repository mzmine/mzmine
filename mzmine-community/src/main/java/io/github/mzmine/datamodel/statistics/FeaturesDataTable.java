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
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FeaturesDataTable extends AbstractRowArrayDataTable {

  public static final FeaturesDataTable EMPTY = new FeaturesDataTable(List.of(),
      new FeatureListRowAbundances[0]);

  private final @NotNull List<RawDataFile> dataFiles;
  private final @NotNull FeatureListRowAbundances[] dataRows;
  // thought about lazy initialization but then the map would be volatile with many reads that may slow down
  // initialization of map should be fast - memory overhead should be fine
  // dont expose the exact class outside as this might change in the future. public uses Map<T, Integer>
  private final Object2IntMap<FeatureListRow> featureRowIndexMap;
  private final Object2IntMap<RawDataFile> dataFileIndexMap;

  // properties that may be set:
  private @NotNull DataTableProcessingHistory processingHistory;

  public FeaturesDataTable(@NotNull List<RawDataFile> dataFiles,
      @NotNull FeatureListRowAbundances[] dataRows) {
    this(dataFiles, dataRows, new DataTableProcessingHistory());
  }

  public FeaturesDataTable(@NotNull List<RawDataFile> dataFiles,
      @NotNull FeatureListRowAbundances[] dataRows, @NotNull DataTableProcessingHistory history) {
    this(dataFiles, dataRows, null, null, history);
  }

  /**
   * Local constructor to
   *
   * @param featureRowIndexMap null to recreate the index
   * @param dataFileIndexMap   null to recreate the index
   */
  FeaturesDataTable(@NotNull List<RawDataFile> dataFiles,
      @NotNull FeatureListRowAbundances[] dataRows,
      @Nullable Object2IntMap<FeatureListRow> featureRowIndexMap,
      @Nullable Object2IntMap<RawDataFile> dataFileIndexMap,
      @NotNull DataTableProcessingHistory history) {
    this.dataFiles = dataFiles;
    this.dataRows = dataRows;
    this.processingHistory = history;

    // simple check if data is correct
    if (dataFileIndexMap != null) {
      if (dataFileIndexMap.size() != dataFiles.size()) {
        throw new IllegalArgumentException(
            "Number of raw data files (%d) does not match its own index (size=%d).".formatted(
                dataFiles.size(), dataFileIndexMap.size()));
      }

      for (int i = 0; i < dataFiles.size(); i++) {
        if (dataFileIndexMap.getInt(dataFiles.get(i)) != i) {
          throw new IllegalArgumentException(
              "Data file index map does not match the position of samples in the list.");
        }
      }
    }
    if (featureRowIndexMap != null) {
      if (featureRowIndexMap.size() != dataRows.length) {
        throw new IllegalArgumentException(
            "Number of feature rows (%d) does not match its own index (size=%d).".formatted(
                dataRows.length, featureRowIndexMap.size()));
      }
      for (int i = 0; i < dataRows.length; i++) {
        if (featureRowIndexMap.getInt(dataRows[i].row()) != i) {
          throw new IllegalArgumentException(
              "Feature row index map does not match the position of rows in the array.");
        }
      }
    }

    // index data if needed
    this.featureRowIndexMap = featureRowIndexMap != null ? featureRowIndexMap
        : CollectionUtils.indexMapUnordered(
            Arrays.stream(dataRows).map(FeatureListRowAbundances::row).toList());
    this.dataFileIndexMap =
        dataFileIndexMap != null ? dataFileIndexMap : CollectionUtils.indexMapUnordered(dataFiles);
  }

  /**
   * Creates a copy with new rows, e.g., after sorting. Reuses data files and index
   */
  public FeaturesDataTable copyWithNewRows(FeatureListRowAbundances[] newRows) {
    // reuse data file map
    return new FeaturesDataTable(dataFiles, newRows, null, dataFileIndexMap, processingHistory);
  }

  public int getFeatureIndex(FeatureListRow row) {
    return featureRowIndexMap.getInt(row);
  }

  public int getSampleIndex(RawDataFile dataFile) {
    return dataFileIndexMap.getInt(dataFile);
  }

  public void setValue(FeatureListRow row, RawDataFile dataFile, double value) {
    setValue(getFeatureIndex(row), getSampleIndex(dataFile), value);
  }

  public double getValue(FeatureListRow row, RawDataFile dataFile) {
    return getValue(getFeatureIndex(row), getSampleIndex(dataFile));
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

  public @NotNull Stream<FeatureListRowAbundances> streamDataRows() {
    return Arrays.stream(dataRows);
  }

  public @NotNull List<FeatureListRow> getFeatureListRows() {
    return streamDataRows().map(FeatureListRowAbundances::row).toList();
  }

  public double[] getFeatureData(FeatureListRow row, boolean copy) {
    return getFeatureData(getFeatureIndex(row), copy);
  }

  @Override
  public double[] getFeatureData(int rowIndex, boolean copy) {
    final double[] abundances = dataRows[rowIndex].abundances();
    if (copy) {
      return Arrays.copyOf(abundances, abundances.length);
    }
    return abundances;
  }

  public double[] getSampleData(RawDataFile raw) {
    return getSampleData(getSampleIndex(raw));
  }

  @Override
  public FeaturesDataTable copy() {
    final var dataClone = Arrays.stream(dataRows).map(FeatureListRowAbundances::copy)
        .toArray(FeatureListRowAbundances[]::new);
    return new FeaturesDataTable(dataFiles, dataClone, featureRowIndexMap, dataFileIndexMap,
        processingHistory);
  }

  /**
   * @param group selected raw data files
   * @return a subset of all rows but only selected raw data files
   */
  public FeaturesDataTable subsetBySamples(List<RawDataFile> group) {
    // find index of raw data files
    final Map<RawDataFile, Integer> fileIndexMap = CollectionUtils.indexMapOrdered(dataFiles);

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

    // only reuse the rows as they are the same - samples indexes are recomputed
    return new FeaturesDataTable(group, subData, featureRowIndexMap, null, processingHistory);
  }

  /**
   * Subset by {@link FeatureListRow} list
   *
   * @param subsetRows only keep those rows
   * @return a new data table with the same samples but subset of rows
   */
  public FeaturesDataTable subsetByFeatures(List<FeatureListRow> subsetRows) {
    final FeatureListRowAbundances[] filtered = subsetRows.stream().map(this::getFeatureRow)
        .toArray(FeatureListRowAbundances[]::new);

    // recompute row index map
    return new FeaturesDataTable(dataFiles, filtered, null, dataFileIndexMap, processingHistory);
  }

  public @NotNull FeatureListRowAbundances getFeatureRow(FeatureListRow row) {
    return getFeatureRow(getFeatureIndex(row));
  }

  public @NotNull FeatureListRowAbundances getFeatureRow(int rowIndex) {
    return dataRows[rowIndex];
  }

  public void setProcessingHistory(@NotNull DataTableProcessingHistory history) {
    this.processingHistory = history;
  }

  public @NotNull DataTableProcessingHistory getProcessingHistory() {
    return processingHistory;
  }

}
