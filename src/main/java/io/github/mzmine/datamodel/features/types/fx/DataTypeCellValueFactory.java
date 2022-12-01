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

package io.github.mzmine.datamodel.features.types.fx;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.util.Callback;

/**
 * Default data cell type factory
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class DataTypeCellValueFactory implements
    Callback<TreeTableColumn.CellDataFeatures<ModularFeatureListRow, Object>, ObservableValue<Object>> {

  private final RawDataFile raw;
  private final DataType type;
  private final SubColumnsFactory parentType;
  private final int subColIndex;

  public DataTypeCellValueFactory(RawDataFile raw, DataType<?> type, SubColumnsFactory parentType,
      int subColIndex) {
    this.raw = raw;
    this.type = type;
    this.parentType = parentType;
    this.subColIndex = subColIndex;
  }

  @Override
  public ObservableValue<Object> call(CellDataFeatures<ModularFeatureListRow, Object> param) {
    ModularFeatureListRow row = param.getValue().getValue();
    // feature or row type?
    final ModularDataModel model = getModel(row);
    if (model == null) {
      //logger.log(Level.WARNING, "There was no DataTypeMap for the column of DataType "
      //    + type.getClass().toString() + " and raw file " + (raw == null ? "NONE" : raw.getName()));
      return null;
    }

    if (parentType != null && parentType instanceof DataType parent) {
      Object value = model.get(parent);
      Object subColValue = parentType.getSubColValue(subColIndex, value);
      return subColValue == null ? null : new SimpleObjectProperty<>(subColValue);
    } else {
      Object value = model.get(type);
      return value == null ? null : new SimpleObjectProperty<>(value);
    }
  }

  /**
   * The default way to get the DataMap. FeatureListRow (for raw==null), Feature for raw!=null.
   */
  public ModularDataModel getModel(ModularFeatureListRow row) {
    return raw == null ? row : row.getFeature(raw);
  }
}
