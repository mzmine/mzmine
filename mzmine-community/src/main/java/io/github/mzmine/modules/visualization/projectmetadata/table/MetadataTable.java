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

package io.github.mzmine.modules.visualization.projectmetadata.table;

import static io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn.DATE_HEADER;
import static io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn.FILENAME_HEADER;
import static io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn.SAMPLE_TYPE_HEADER;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.preferences.FeatureListPreferences;
import io.github.mzmine.modules.visualization.projectmetadata.MetadataColumnDoesNotExistException;
import io.github.mzmine.modules.visualization.projectmetadata.MetadataValueDoesNotExistException;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.StringUtils;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the metadata of a project and represents it as a table (parameters are columns).
 * <p>
 * State is only ever changed through the mutating methods of this class so that every change is
 * reflected in {@link #getVersion()}. Accessors therefore hand out unmodifiable views - never the
 * backing collections. Read the table through {@link #getColumns()} and
 * {@link #getColumnData(MetadataColumn)}, and use {@link #batchUpdate(Runnable)} to group several
 * changes into a single version increment.
 */
public class MetadataTable {

  private static final Logger logger = Logger.getLogger(MetadataTable.class.getName());

  /**
   * The backing data. Only mutated by methods of this class, never handed out directly.
   */
  private final Map<MetadataColumn<?>, Map<RawDataFile, Object>> data = new HashMap<>();
  /**
   * Live but unmodifiable view of the column keys.
   */
  private final Set<MetadataColumn<?>> columnsView = Collections.unmodifiableSet(data.keySet());
  /**
   * Counts all mutations of this table so that consumers can cache values derived from the metadata
   * and invalidate them on change, see {@link FeatureListPreferences}.
   */
  private final AtomicLong version = new AtomicLong(0);
  // enable auto detection is on in project metadata but off during import
  private final boolean enableAutoDetection;
  /**
   * Nesting level of {@link #batchUpdate(Runnable)}. While greater than zero, mutations only flag
   * {@link #batchModified} instead of incrementing the {@link #version}.
   */
  private int batchDepth = 0;
  private boolean batchModified = false;

  public MetadataTable() {
    this(true);
  }

  public MetadataTable(boolean enableAutoDetection) {
    this(enableAutoDetection, Map.of());
  }

  public MetadataTable(@NotNull Map<MetadataColumn<?>, ? extends Map<RawDataFile, ?>> data) {
    this(true, data);
  }

  public MetadataTable(boolean enableAutoDetection,
      @NotNull Map<MetadataColumn<?>, ? extends Map<RawDataFile, ?>> data) {
    this.enableAutoDetection = enableAutoDetection;
    // defensive copy
    data.forEach((column, values) -> getModifiableColumnData(column).putAll(values));
  }


  /**
   * Modification counter that is incremented on every change of columns or values. Allows consumers
   * to cache derived values and invalidate them once the metadata changes.
   *
   * @return the current version as modification counter
   */
  public long getVersion() {
    return version.get();
  }

  /**
   * Groups several mutations into a single {@link #getVersion()} increment. Nested calls are
   * supported, only the outermost one increments. The version is also incremented when the updates
   * throw, because the table may already have been changed partially.
   *
   * @param updates mutations applied through the methods of this table
   */
  public void batchUpdate(@NotNull Runnable updates) {
    batchDepth++;
    try {
      updates.run();
    } finally {
      batchDepth--;
      if (batchDepth == 0 && batchModified) {
        batchModified = false;
        version.incrementAndGet();
      }
    }
  }

  /**
   * Marks the table as changed. Increments the version unless a {@link #batchUpdate(Runnable)} is
   * active - then the increment is deferred to the end of the batch.
   */
  private void modified() {
    if (batchDepth > 0) {
      batchModified = true;
      return;
    }
    version.incrementAndGet();
  }

  /**
   * The mutable value map of a column, created if the column is new. Never handed out, callers
   * outside this class use {@link #getColumnData(MetadataColumn)}. Does not increment the version -
   * callers must call {@link #modified()}.
   */
  private @NotNull Map<RawDataFile, Object> getModifiableColumnData(
      @NotNull MetadataColumn<?> column) {
    return data.computeIfAbsent(column, _ -> new ConcurrentHashMap<>());
  }

  /**
   * Clear the metadata table up.
   */
  public void clearData() {
    data.clear();
    modified();
  }

  /**
   * Replaces all columns and values of this table. Counts as a single change.
   *
   * @param newData columns mapped to their file to value maps, copied into this table
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void setData(@NotNull Map<MetadataColumn<?>, ? extends Map<RawDataFile, ?>> newData) {
    batchUpdate(() -> {
      clearData();
      newData.forEach((column, values) -> setValues((MetadataColumn) column, values));
    });
  }

  /**
   * Add new parameter column to the metadata table.
   *
   * @param column new parameter column
   */
  public void addColumn(@NotNull MetadataColumn<?> column) {
    getModifiableColumnData(column);
    modified();
  }

  /**
   * Remove parameter column from the metadata table.
   *
   * @param column parameter column
   */
  public void removeColumn(@NotNull MetadataColumn<?> column) {
    data.remove(column);
    modified();
  }

  /**
   * Remove parameter column from the metadata table.
   *
   * @param name column name
   */
  public void removeColumn(@NotNull String name) {
    boolean any = data.keySet().removeIf(key -> key.getTitle().equals(name));
    if (any) {
      modified();
    }
  }

  /**
   * Removes multiple columns. Counts as a single change.
   *
   * @param columns columns to remove, columns that are not in this table are ignored
   */
  public void removeColumns(@NotNull Collection<? extends MetadataColumn<?>> columns) {
    data.keySet().removeAll(columns);
    modified();
  }

  /**
   * Add file to the table and try to set the date column
   *
   * @param newFile file to be added
   */
  public void addFile(@NotNull RawDataFile newFile) {
    if (!enableAutoDetection) {
      return;
    }
    // try to set a value of a start time stamp parameter for a sample
    try {
      batchUpdate(() -> {
        // is usually saved as ZonedDateTime with 2022-06-01T18:36:09Z where the Z stands for UTC
        MetadataColumn dateCol = getColumnByName(DATE_HEADER);
        if (dateCol == null) {
          dateCol = new DateMetadataColumn(DATE_HEADER, "Run start time stamp of the sample");
        }
        setValue(dateCol, newFile, newFile.getStartTimeStamp());

        assignSampleType(newFile);
      });
    } catch (Exception ignored) {
      logger.warning("Cannot set date " + newFile.getStartTimeStamp());
    }
  }

  /**
   * Writes the default sample type guessed from the file name. The user may overwrite it with any
   * group name afterwards - from then on the column value is authoritative, see
   * {@link SampleTypeFilter}.
   */
  private void assignSampleType(@NotNull RawDataFile newFile) {
    final MetadataColumn<String> sampleTypeColumn = getSampleTypeColumn();
    setValue(sampleTypeColumn, newFile, SampleType.ofFile(newFile).toString());
  }

  /**
   * The sample type column. Creates the column and fills it with the auto detected sample types if
   * it does not exist yet.
   */
  @SuppressWarnings("unchecked")
  public @NotNull MetadataColumn<String> getSampleTypeColumn() {
    final MetadataColumn<?> col = getColumnByName(SAMPLE_TYPE_HEADER);
    if (col != null) {
      return (MetadataColumn<String>) col;
    }

    final StringMetadataColumn sampleType = new StringMetadataColumn(SAMPLE_TYPE_HEADER,
        "The type of the sample");
    batchUpdate(() -> {
      addColumn(sampleType);
      // column was just created, add default sample types.
      // collect the files first because assigning the values changes the data that is read here
      final Map<RawDataFile, String> defaults = getRawDataFilesUnsorted().stream()
          .collect(Collectors.toMap(Function.identity(), raw -> SampleType.ofFile(raw).toString()));
      setValues(sampleType, defaults);
    });
    return sampleType;
  }

  /**
   * Maybe push into MetadataTable?
   */
  public @Nullable DateMetadataColumn getRunDateColumn() {
    return (DateMetadataColumn) getColumnByName(DATE_HEADER);
  }

  /**
   * Remove the all parameters values for a passed file.
   *
   * @param file file for which parameters values should be deleted.
   */
  public void removeFile(@NotNull RawDataFile file) {
    // iterate through the all parameters and try to delete the parameters
    // values mapped to the passed file
    for (var values : data.values()) {
      values.remove(file);
    }
    modified();
  }

  /**
   * Removes all values of multiple files. Counts as a single change.
   *
   * @param files files for which parameter values should be deleted
   */
  public void removeFiles(@NotNull Collection<RawDataFile> files) {
    for (var values : data.values()) {
      values.keySet().removeAll(files);
    }
    modified();
  }

  /**
   * Is the specified metadata column obtained in the metadata table?
   *
   * @param column project parameter column
   * @return true if it's contained, false otherwise
   */
  public boolean hasColumn(@Nullable MetadataColumn<?> column) {
    return data.containsKey(column);
  }

  /**
   * Return parameters columns of the metadata table. Add or remove columns through
   * {@link #addColumn(MetadataColumn)} and {@link #removeColumn(MetadataColumn)}.
   *
   * @return unmodifiable view of the parameters columns
   */
  public @NotNull Set<MetadataColumn<?>> getColumns() {
    return columnsView;
  }

  /**
   * Return parameter column with the corresponding parameter name.
   *
   * @param name name of the parameter
   * @return parameterColumn or null in case if the parameter with the passed name isn't obtained in
   * the metadata table
   */
  public @Nullable MetadataColumn<?> getColumnByName(@Nullable String name) {
    // filename column is not added because this would duplicate the RawDataFile.name method
    if (FILENAME_HEADER.equals(name)) {
      return createDataFileColumn();
    }
    //
    for (MetadataColumn<?> column : getColumns()) {
      if (column.getTitle().equals(name)) {
        return column;
      }
    }

    return null;
  }

  /**
   * Return parameter value of the corresponding RawData file.
   *
   * @param column      project parameter column
   * @param rawDataFile RawData file
   * @param <T>         type of the project parameter
   * @return parameter value
   */
  @SuppressWarnings("unchecked")
  public <T> @Nullable T getValue(@Nullable MetadataColumn<T> column,
      @Nullable RawDataFile rawDataFile) {
    var row = data.get(column);
    if (row != null) {
      return (T) row.get(rawDataFile);
    }

    return null;
  }

  /**
   * Try to set particular value of the parameter of the RawData file. The parameter column will be
   * added in case if it wasn't previously obtained in the table.
   *
   * @param column project parameter column
   * @param file   RawData file
   * @param value  value to be set
   * @param <T>    type of the parameter
   */
  public <T> void setValue(@NotNull MetadataColumn<T> column, @NotNull RawDataFile file,
      @Nullable T value) {
    final Map<RawDataFile, Object> values = getModifiableColumnData(column);

    // this check is necessary because a ConcurrentMap can not contain null values
    if (value == null) {
      values.remove(file);
    } else {
      values.put(file, value);
    }
    // increment after the change so that caches never store a value with the new version
    modified();
  }

  /**
   * Sets multiple values of one column, the column is created if it does not exist yet. Counts as a
   * single change.
   *
   * @param column project parameter column
   * @param values file to value mapping, a null value removes the entry of that file
   * @param <T>    type of the parameter
   */
  public <T> void setValues(@NotNull MetadataColumn<T> column,
      @NotNull Map<RawDataFile, ? extends T> values) {
    final Map<RawDataFile, Object> columnData = getModifiableColumnData(column);

    values.forEach((file, value) -> {
      // this check is necessary because a ConcurrentMap can not contain null values
      if (value == null) {
        columnData.remove(file);
      } else {
        columnData.put(file, value);
      }
    });
    modified();
  }

  /**
   * Sets the values of one file in multiple columns, missing columns are created. Counts as a
   * single change.
   *
   * @param file   RawData file
   * @param values column to value mapping, a null value removes the entry of that column
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void setValues(@NotNull RawDataFile file, @NotNull Map<MetadataColumn<?>, ?> values) {
    batchUpdate(
        () -> values.forEach((column, value) -> setValue((MetadataColumn) column, file, value)));
  }

  /**
   * Column titles
   *
   * @return array of column titles
   */
  public String[] getColumnTitles() {
    return getColumns().stream().map(MetadataColumn::getTitle).toArray(String[]::new);
  }

  /**
   * Groups the files by the value in the metadata column.
   *
   * @param <T>
   * @return If the column is null, an empty map is returned. If the column is not in the table, an
   * error is thrown.
   * @throws MetadataColumnDoesNotExistException If the column does not exist. Does not throw if the
   *                                             column is null.
   */
  @NotNull
  public <T> Map<T, List<RawDataFile>> groupFilesByColumn(@Nullable MetadataColumn<T> column)
      throws MetadataColumnDoesNotExistException {
    if (column == null) {
      return Map.of();
    }
    final Map<RawDataFile, Object> fileValueMap = data.get(column);
    if (fileValueMap == null) {
      throw new MetadataColumnDoesNotExistException(column.getTitle());
    }
    return groupFilesByColumn(fileValueMap.keySet(), column);
  }

  /**
   * Groups the files by the value in the metadata column.
   *
   * @param <T>
   * @return If the column is null, an empty map is returned. If the column is not in the table, an
   * error is thrown.
   * @throws MetadataColumnDoesNotExistException If the column does not exist. Does not throw if the
   *                                             column is null.
   */
  @NotNull
  public <T> Map<T, List<RawDataFile>> groupFilesByColumn(@NotNull Collection<RawDataFile> raws,
      @Nullable MetadataColumn<T> column) throws MetadataColumnDoesNotExistException {
    if (column == null) {
      return Map.of();
    }
    final Map<RawDataFile, Object> fileValueMap = data.get(column);
    if (fileValueMap == null) {
      throw new MetadataColumnDoesNotExistException(column.getTitle());
    }
    return raws.stream().filter(raw -> fileValueMap.get(raw) != null)
        .collect(Collectors.groupingBy(raw -> (T) fileValueMap.get(raw)));
  }

  /**
   * Groups the files by the value in the metadata column. If raw data files have no mapping (null)
   * they will be put into a group at first index. Sorted by doubleValue ascending.
   *
   * @param <T>
   * @return If the column is null, a list with the input files is returned. If the column is not in
   * the table, an error is thrown. If values are null - the first entry will be a list for all null
   * value entries. Each list has at least 1 element.
   * @throws MetadataColumnDoesNotExistException If the column does not exist. Does not throw if the
   *                                             column is null.
   */
  @NotNull
  public <T> List<SamplesGroupedBy<T>> groupFilesByColumnIncludeNull(
      @Nullable MetadataColumn<T> column) throws MetadataColumnDoesNotExistException {
    final List<RawDataFile> files = ProjectService.getProject().getCurrentRawDataFiles();
    return groupFilesByColumnIncludeNull(files, column);
  }

  /**
   * Groups the files by the value in the metadata column. If raw data files have no mapping (null)
   * they will be put into a group at first index. Sorted by doubleValue ascending.
   *
   * @param <T>
   * @return If the column is null, a list with the input files is returned. If the column is not in
   * the table, an error is thrown. If values are null - the first entry will be a list for all null
   * value entries. Each list has at least 1 element.
   * @throws MetadataColumnDoesNotExistException If the column does not exist. Does not throw if the
   *                                             column is null.
   */
  @NotNull
  public <T> List<SamplesGroupedBy<T>> groupFilesByColumnIncludeNull(
      @NotNull Collection<RawDataFile> raws, @Nullable MetadataColumn<T> column)
      throws MetadataColumnDoesNotExistException {
    if (column == null) {
      return List.of(new SamplesGroupedBy<>(null, List.copyOf(raws), 0));
    }
    final Map<RawDataFile, Object> fileValueMap = data.get(column);
    if (fileValueMap == null) {
      throw new MetadataColumnDoesNotExistException(column.getTitle());
    }

    final Map<Object, @NotNull List<RawDataFile>> nonNullMap = new HashMap<>();
    List<RawDataFile> nullsList = new ArrayList<>();
    for (RawDataFile raw : raws) {
      final Object value = fileValueMap.get(raw);
      if (value == null || StringUtils.isBlank(value.toString())) {
        nullsList.add(raw);
      } else {
        // add to specific list
        final List<RawDataFile> list = nonNullMap.computeIfAbsent(value, _ -> new ArrayList<>());
        list.add(raw);
      }
    }
    boolean hasNulls = !nullsList.isEmpty();
    final List<SamplesGroupedBy<T>> groups = new ArrayList<>(
        nonNullMap.size() + (hasNulls ? 1 : 0));

    int counter = hasNulls ? 1 : 0;

    // in case we have string values - sort by toString as we will then just number each group.
    // numbers and dates will be sorted after the convertToDouble conversion
    final List<Entry<Object, @NotNull List<RawDataFile>>> entries = nonNullMap.entrySet().stream()
        .sorted(Comparator.comparing(e -> e.getKey().toString())).toList();

    for (Entry<Object, @NotNull List<RawDataFile>> e : entries) {
      final List<RawDataFile> rawFiles = e.getValue();
      final Object value = e.getKey();
      final double doubleValue = convertValueToDouble(value, counter);
      groups.add(new SamplesGroupedBy<>((T) value, rawFiles, doubleValue));
      counter++;
    }
    // requires sorting by value which is always comparable in table
    groups.sort(Comparator.comparing(SamplesGroupedBy::doubleValue));

    if (hasNulls) {
      // add null always first if present
      final DoubleSummaryStatistics summary = groups.stream()
          .mapToDouble(SamplesGroupedBy::doubleValue).summaryStatistics();
      double nullValue = summary.getCount() == 0 ? 0
          // min - step
          : summary.getMin() - (summary.getMax() - summary.getMin()) / summary.getCount();
      groups.addFirst(new SamplesGroupedBy<>(null, nullsList, nullValue));
    }

    return groups;
  }

  /**
   * Date or number value is converted to double and other objects will use default
   *
   * @return Double.NaN for null values, doubles for dates and number, and string default for not
   * empty strings (blank strings are handled like null as NaN)
   */
  public static double convertValueToDouble(@Nullable Object value, double stringDefaultValue) {
    return switch (value) {
      case null -> Double.NaN;
      case Number n -> n.doubleValue();
      case LocalDateTime date -> date.toEpochSecond(ZoneOffset.UTC);
      case String s -> s.isBlank() ? Double.NaN : stringDefaultValue;
      default -> stringDefaultValue;
    };
  }


  /**
   * Matches against all raw data files currently in the project, whereas
   * {@link #getMatchingFiles(Collection, MetadataColumn, Object)} is limited to a given subset of
   * files.
   *
   * @param column The column
   * @param value  The column value to match to.
   * @return A list of files associated to the column value or null, if the column value does not
   * exist.
   */
  public <T> List<RawDataFile> getMatchingProjectFiles(@NotNull MetadataColumn<T> column,
      @NotNull T value)
      throws MetadataColumnDoesNotExistException, MetadataValueDoesNotExistException {
    final List<RawDataFile> allFiles = ProjectService.getProject().getCurrentRawDataFiles();
    return getMatchingFiles(allFiles, column, value);
  }

  /**
   * @param raws   The list of files to search in
   * @param column The column
   * @param value  The column value to match to.
   * @return A list of files associated to the column value or null, if the column value does not
   * exist.
   */
  public <T> List<RawDataFile> getMatchingFiles(@NotNull Collection<RawDataFile> raws,
      @NotNull MetadataColumn<T> column, @NotNull T value)
      throws MetadataColumnDoesNotExistException, MetadataValueDoesNotExistException {
    final Map<T, List<RawDataFile>> valueFilesMap = groupFilesByColumn(raws, column);
    final List<RawDataFile> files = valueFilesMap.get(value);
    if (files == null) {
      throw new MetadataValueDoesNotExistException(column, value.toString());
    }
    // must be the files of the raws subset, not of the whole project
    return files;
  }

  /**
   * @param column The column
   * @return A list of files associated to the column value or an empty list if the column value
   * does not exist.
   */
  public <T> Map<T, List<RawDataFile>> groupFilesByColumnValues(@NotNull MetadataColumn<T> column,
      T[] columnValues)
      throws MetadataColumnDoesNotExistException, MetadataValueDoesNotExistException {
    final Map<T, List<RawDataFile>> groupedFiles = groupFilesByColumn(column);
    return Arrays.stream(columnValues).collect(Collectors.toMap(colVal -> colVal,
        colVal -> Optional.ofNullable(groupedFiles.get(colVal)).orElse(List.of())));
  }

  public <T> @NotNull List<T> getDistinctColumnValuesOrThrow(@NotNull MetadataColumn<T> column) {
    final Map<RawDataFile, Object> fileValueMap = data.get(column);
    if (fileValueMap == null) {
      throw new MetadataColumnDoesNotExistException(column.getTitle());
    }
    return fileValueMap.values().stream().distinct().map(o -> (T) o).toList();
  }

  /**
   * @return the list of distinct unique values or an empty list if column does not exist
   */
  public <T> @NotNull List<T> getDistinctColumnValues(@Nullable MetadataColumn<T> column) {
    final Map<RawDataFile, Object> fileValueMap = data.get(column);
    if (fileValueMap == null) {
      return List.of();
    }
    return fileValueMap.values().stream().distinct().map(o -> (T) o).toList();
  }

  /**
   * @param sampleType a predefined sample type to filter for
   * @return list of raw data files that match the type in the sample type column, matched ignoring
   * case and surrounding whitespace
   */
  public @NotNull List<RawDataFile> getFilesOfSampleType(final @NotNull SampleType sampleType) {
    return getFilesOfSampleType(sampleType.toString());
  }

  /**
   * @param sampleType a sample type value, either a predefined {@link SampleType} or a custom group
   *                   name the user defined in the metadata
   * @return list of raw data files that match the value in the sample type column, matched ignoring
   * case and surrounding whitespace. Empty list if nothing matches.
   */
  public @NotNull List<RawDataFile> getFilesOfSampleType(final @NotNull String sampleType) {
    final MetadataColumn<String> sampleTypeColumn = getSampleTypeColumn();
    final SampleTypeFilter filter = SampleTypeFilter.ofValues(sampleType);
    final Map<RawDataFile, Object> columnData = getColumnData(sampleTypeColumn);
    if (columnData == null) {
      return List.of();
    }
    return columnData.entrySet().stream().filter(e -> filter.matchesValue(e.getValue()))
        .map(Entry::getKey).toList();
  }

  public @NotNull StringMetadataColumn createDataFileColumn() {
    return new StringMetadataColumn(FILENAME_HEADER, "");
  }

  /**
   * In place - Merge this and newMetadata using newMetadata values if they are not null. Counts as
   * a single change.
   *
   * @param newMetadata priority over this
   * @return this metadata
   */
  @NotNull
  @SuppressWarnings({"unchecked", "rawtypes"})
  public MetadataTable merge(final @NotNull MetadataTable newMetadata) {
    batchUpdate(() -> newMetadata.data.forEach((columnInNewTable, values) -> {
      // values are never null in a table, so all values of the new table take priority.
      // columns without any value are not merged in
      if (!values.isEmpty()) {
        setValues((MetadataColumn) columnInNewTable, values);
      }
    }));
    return this;
  }

  /**
   * The values of one column as an unmodifiable view. Apply changes through
   * {@link #setValue(MetadataColumn, RawDataFile, Object)} or
   * {@link #setValues(MetadataColumn, Map)} so that {@link #getVersion()} tracks them.
   *
   * @return view of the file to value mapping or null if the column is null or not in this table
   */
  public @Nullable Map<RawDataFile, Object> getColumnData(final @Nullable MetadataColumn<?> col) {
    if (col == null) {
      return null;
    }
    // filename column is not in data so create it here
    if (FILENAME_HEADER.equals(col.getTitle())) {
      return Collections.unmodifiableMap(
          ProjectService.getProject().getCurrentRawDataFiles().stream()
              .collect(Collectors.toMap(Function.identity(), RawDataFile::getName)));
    }

    final Map<RawDataFile, Object> values = data.get(col);
    return values == null ? null : Collections.unmodifiableMap(values);
  }

  /**
   * @return unsorted list of all raw data files with values
   */
  public @NotNull List<RawDataFile> getRawDataFilesUnsorted() {
    return data.values().stream()
        .<RawDataFile>mapMulti((d, consumer) -> d.keySet().forEach(consumer)).distinct().toList();
  }
}
