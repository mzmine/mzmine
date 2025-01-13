/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import io.github.mzmine.modules.visualization.projectmetadata.MetadataColumnDoesNotExistException;
import io.github.mzmine.modules.visualization.projectmetadata.MetadataValueDoesNotExistException;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the metadata of a project and represents it as a table (parameters are columns).
 */
public class MetadataTable {

  private static final Logger logger = Logger.getLogger(MetadataTable.class.getName());
  private final Map<MetadataColumn<?>, Map<RawDataFile, Object>> data;
  // enable auto detection is on in project metadata but off during import
  private final boolean enableAutoDetection;

  public MetadataTable() {
    this(true);
  }

  public MetadataTable(boolean enableAutoDetection) {
    this(enableAutoDetection, new HashMap<>());
  }

  public MetadataTable(Map<MetadataColumn<?>, Map<RawDataFile, Object>> data) {
    this(true, data);
  }

  public MetadataTable(boolean enableAutoDetection,
      Map<MetadataColumn<?>, Map<RawDataFile, Object>> data) {
    this.enableAutoDetection = enableAutoDetection;
    this.data = data;
  }


  public Map<MetadataColumn<?>, Map<RawDataFile, Object>> getData() {
    return data;
  }

  /**
   * Clear the metadata table up.
   */
  public void clearData() {
    data.clear();
  }

  /**
   * Add new parameter column to the metadata table.
   *
   * @param column new parameter column
   */
  public void addColumn(MetadataColumn<?> column) {
    data.putIfAbsent(column, new ConcurrentHashMap<>());
  }

  /**
   * Remove parameter column from the metadata table.
   *
   * @param column parameter column
   */
  public void removeColumn(MetadataColumn<?> column) {
    data.remove(column);
  }

  /**
   * Remove parameter column from the metadata table.
   *
   * @param name column name
   */
  public void removeColumn(String name) {
    data.keySet().removeIf(key -> key.getTitle().equals(name));
  }

  /**
   * Add file to the table and try to set the date column
   *
   * @param newFile file to be added
   */
  public void addFile(RawDataFile newFile) {
    // try to set a value of a start time stamp parameter for a sample
    try {
      if (enableAutoDetection) {
        // is usually saved as ZonedDateTime with 2022-06-01T18:36:09Z where the Z stands for UTC
        MetadataColumn dateCol = getColumnByName(DATE_HEADER);
        if (dateCol == null) {
          dateCol = new DateMetadataColumn(DATE_HEADER, "Run start time stamp of the sample");
        }
        setValue(dateCol, newFile, newFile.getStartTimeStamp());

        assignSampleType(newFile);
      }
    } catch (Exception ignored) {
      logger.warning("Cannot set date " + newFile.getStartTimeStamp());
    }
  }

  private void assignSampleType(RawDataFile newFile) {
    final MetadataColumn<String> sampleTypeColumn = getSampleTypeColumn();
    setValue(sampleTypeColumn, newFile, SampleType.ofFile(newFile).toString());
  }

  public MetadataColumn<String> getSampleTypeColumn() {
    final MetadataColumn<?> col = getColumnByName(SAMPLE_TYPE_HEADER);
    if (col == null) {
      final StringMetadataColumn sampleType = new StringMetadataColumn(SAMPLE_TYPE_HEADER,
          "The type of the sample");
      addColumn(sampleType);
      // column was just created, add default sample types
      data.values().stream().flatMap(m -> m.keySet().stream()).distinct()
          .forEach(this::assignSampleType);
      return sampleType;
    }
    return (MetadataColumn<String>) col;
  }

  /**
   * Maybe push into MetadataTable?
   */
  public DateMetadataColumn getRunDateColumn() {
    return (DateMetadataColumn) getColumnByName(DATE_HEADER);
  }

  /**
   * Remove the all parameters values for a passed file.
   *
   * @param file file for which parameters values should be deleted.
   */
  public void removeFile(RawDataFile file) {
    // iterate through the all parameters and try to delete the parameters
    // values mapped to the passed file
    for (var param : data.keySet()) {
      data.get(param).remove(file);
    }
  }

  /**
   * Is the specified metadata column obtained in the metadata table?
   *
   * @param column project parameter column
   * @return true if it's contained, false otherwise
   */
  public boolean hasColumn(MetadataColumn<?> column) {
    return data.containsKey(column);
  }

  /**
   * Return parameters columns of the metadata table.
   *
   * @return set with the parameters columns
   */
  public Set<MetadataColumn<?>> getColumns() {
    return data.keySet();
  }

  /**
   * Return parameter column with the corresponding parameter name.
   *
   * @param name name of the parameter
   * @return parameterColumn or null in case if the parameter with the passed name isn't obtained in
   * the metadata table
   */
  public MetadataColumn<?> getColumnByName(String name) {
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
  public <T> T getValue(MetadataColumn<T> column, RawDataFile rawDataFile) {
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
  public <T> void setValue(MetadataColumn<T> column, RawDataFile file, @Nullable T value) {
    if (!data.containsKey(column)) {
      addColumn(column);
    }

    // this check is necessary because a ConcurrentMap can't contain null values
    if (value == null) {
      data.get(column).remove(file);
    } else {
      data.get(column).put(file, value);
    }
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
   * @param column The column
   * @param value  The column value to match to.
   * @return A list of files associated to the column value or null, if the column value does not
   * exist.
   */
  public <T> List<RawDataFile> getMatchingFiles(@NotNull MetadataColumn<T> column, @NotNull T value)
      throws MetadataColumnDoesNotExistException, MetadataValueDoesNotExistException {
    final Map<T, List<RawDataFile>> valueFilesMap = groupFilesByColumn(column);
    final List<RawDataFile> files = valueFilesMap.get(value);
    if (files == null) {
      throw new MetadataValueDoesNotExistException(column, value.toString());
    }
    return groupFilesByColumn(column).get(value);
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

  public <T> List<T> getDistinctColumnValues(MetadataColumn<T> column) {
    final Map<RawDataFile, Object> fileValueMap = data.get(column);
    if (fileValueMap == null) {
      throw new MetadataColumnDoesNotExistException(column.getTitle());
    }
    return fileValueMap.values().stream().distinct().map(o -> (T) o).toList();
  }

  /**
   * @param sampleType a sample type to filter for
   * @return list of raw data files that match type in type column
   */
  public List<RawDataFile> getFilesOfSampleType(final SampleType sampleType) {
    var sampleTypeColumn = getSampleTypeColumn();
    if (sampleTypeColumn == null) {
      return List.of();
    }
    return getMatchingFiles(sampleTypeColumn, sampleType.toString());
  }

  public StringMetadataColumn createDataFileColumn() {
    return new StringMetadataColumn(FILENAME_HEADER, "");
  }

  /**
   * In place - Merge this and newMetadata using newMetadata values if they are not null.
   *
   * @param newMetadata priority over this
   * @return this metadata
   */
  @NotNull
  public MetadataTable merge(final MetadataTable newMetadata) {
    newMetadata.getData().forEach((column, data) -> {
      data.forEach((raw, value) -> {
        if (value != null) {
          setValue((MetadataColumn) column, raw, value);
        }
      });
    });
    return this;
  }

  @Nullable
  public Map<RawDataFile, Object> getColumnData(final MetadataColumn<?> col) {
    if (col == null) {
      return null;
    }
    // filename column is not in data so create it here
    if (FILENAME_HEADER.equals(col.getTitle())) {
      return ProjectService.getProject().getCurrentRawDataFiles().stream()
          .collect(Collectors.toMap(Function.identity(), RawDataFile::getName));
    }

    return data.get(col);
  }
}
