/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.features.types.DataType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ImportType {

  private BooleanProperty selected;
  private StringProperty csvColumnName;
  private ObjectProperty<DataType<?>> dataType;

  private int columnIndex = -1;

  public ImportType(Boolean selected, String csvColumnName, DataType<?> dataType) {
    this.selected = new SimpleBooleanProperty(selected);
    this.csvColumnName = new SimpleStringProperty(csvColumnName);
    this.dataType = new SimpleObjectProperty<>(dataType);
  }

  public boolean isSelected() {
    return selected.get();
  }

  public void setSelected(boolean selected) {
    this.selected.set(selected);
  }

  public BooleanProperty selectedProperty() {
    return selected;
  }

  public String getCsvColumnName() {
    return csvColumnName.get();
  }

  public void setCsvColumnName(String csvColumnName) {
    this.csvColumnName.set(csvColumnName);
  }

  public StringProperty csvColumnName() {
    return csvColumnName;
  }

  public DataType<?> getDataType() {
    return dataType.get();
  }

  public void setDataType(DataType<?> dataType) {
    this.dataType.set(dataType);
  }

  public ObjectProperty<DataType<?>> dataTypeProperty() {
    return dataType;
  }

  /**
   * @return The column index if specified. This value is not set in the gui and has to be
   * determined from the file.
   */
  public int getColumnIndex() {
    return columnIndex;
  }

  /**
   * @param columnIndex The column index. This value is not set in the gui and has to be determined
   *                    from the file.
   */
  public void setColumnIndex(int columnIndex) {
    this.columnIndex = columnIndex;
  }

}
