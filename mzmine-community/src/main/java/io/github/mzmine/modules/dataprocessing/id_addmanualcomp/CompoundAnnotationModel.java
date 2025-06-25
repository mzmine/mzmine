/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_addmanualcomp;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javax.validation.constraints.NotNull;

public class CompoundAnnotationModel {

  private final MapProperty<DataType, Object> dataModel = new SimpleMapProperty<>(
      FXCollections.observableHashMap());

  private final ObjectProperty<@NotNull FeatureListRow> row = new SimpleObjectProperty<>();

  public CompoundAnnotationModel() {
  }

  public ObservableMap<DataType, Object> getDataModel() {
    return dataModel.get();
  }

  public MapProperty<DataType, Object> dataModelProperty() {
    return dataModel;
  }

  public <T> void setType(DataType<T> type, T value) {
    dataModel.put(type, value);
  }

  public <T> ObjectBinding<T> valueAt(DataType<T> type) {
    final ObjectBinding<Object> objectObjectBinding = dataModel.valueAt(type);
    return (ObjectBinding<T>) objectObjectBinding;
  }


  public @NotNull FeatureListRow getRow() {
    return row.get();
  }

  public void setRow(@NotNull FeatureListRow row) {
    this.row.set(row);
  }

  public ObjectProperty<@NotNull FeatureListRow> rowProperty() {
    return row;
  }
}
