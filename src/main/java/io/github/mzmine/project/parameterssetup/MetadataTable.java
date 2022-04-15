/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.project.parameterssetup;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.project.parameterssetup.columns.MetadataColumn;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Holds the metadata of a project and represents it as a table (parameters are columns).
 */
public class MetadataTable {

  private final Map<MetadataColumn<?>, Map<RawDataFile, Object>> data;

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
    data.putIfAbsent(column, new HashMap<>());
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
  public <T> void setValue(MetadataColumn<T> column, RawDataFile file, T value) {
    if (!data.containsKey(column)) {
      addColumn(column);
    }

    data.get(column).put(file, value);
  }
}
