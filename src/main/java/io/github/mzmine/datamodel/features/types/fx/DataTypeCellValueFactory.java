/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
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
