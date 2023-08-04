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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.features.types.DataType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ImportType {

  private final BooleanProperty selected;
  private final StringProperty csvColumnName;
  private final ObjectProperty<DataType<?>> dataType;

  private int columnIndex = -1;

  public ImportType(Boolean selected, String csvColumnName, DataType<?> dataType) {
    this.selected = new SimpleBooleanProperty(selected);
    this.csvColumnName = new SimpleStringProperty(csvColumnName);
    this.dataType = new SimpleObjectProperty<>(dataType);
  }

  @Override
  public String toString() {
    return "ImportType{" + selected.get() + ", in csv=" + csvColumnName.get() + ", type="
        + dataType.get() + ", index=" + columnIndex + '}';
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
