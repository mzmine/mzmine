/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Holds the metadata of a project and represents it as a table (parameters are columns).
 */
public class MetadataTable {

  private static final Logger logger = Logger.getLogger(MetadataTable.class.getName());
  private final Map<MetadataColumn<?>, Map<RawDataFile, Object>> data;
  // use GoF "State" pattern where the state will interpret the table format (either long or wide)
  private TableExportUtility tableExportUtility = new WideTableExportUtility(this);

  public MetadataTable() {
    this.data = new HashMap<>();
  }

  public MetadataTable(Map<MetadataColumn<?>, Map<RawDataFile, Object>> data) {
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
   * Add file to the table and try to set the date column
   *
   * @param newFile
   */
  public void addFile(RawDataFile newFile) {
    // try to set a value of a start time stamp parameter for a sample
    try {
      // is usually saved as ZonedDateTime with 2022-06-01T18:36:09Z where the Z stands for UTC
      setValue(new DateMetadataColumn("run_date", "Run start time stamp of the sample"), newFile,
          newFile.getStartTimeStamp());
    } catch (Exception ignored) {
      logger.warning("Cannot set date " + newFile.getStartTimeStamp().toString());
    }
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
   * Export the metadata depending on the table format (state).
   *
   * @param file where the metadata will be exported to
   * @return was the export successful?
   */
  public boolean exportMetadata(File file) {
    return tableExportUtility.exportTo(file);
  }

  /**
   * Import the metadata depending on the table format (state).
   *
   * @param file       from which the metadata will be exported
   * @param appendMode whether the new metadata should be appended or they should replace the old
   *                   metadata
   * @return was the import successful?
   */
  public boolean importMetadata(File file, boolean appendMode) {
    return tableExportUtility.importFrom(file, appendMode);
  }

  /**
   * Update the state of the tableExportUtility.
   *
   * @param tableExportUtility the new state which represents the new table format.
   */
  public void setTableExportUtility(TableExportUtility tableExportUtility) {
    this.tableExportUtility = tableExportUtility;
  }

  /**
   * Column titles
   *
   * @return array of column titles
   */
  public String[] getColumnTitles() {
    return getColumns().stream().map(MetadataColumn::getTitle).toArray(String[]::new);
  }

  // define the header fields names of the file with imported metadata
  private enum HeaderFields {
    NAME, DESC, TYPE, FILE, VALUE
  }
}
