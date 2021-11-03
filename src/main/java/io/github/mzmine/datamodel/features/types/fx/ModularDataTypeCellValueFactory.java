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
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ModularType;
import io.github.mzmine.datamodel.features.types.ModularTypeMap;
import java.text.MessageFormat;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;

/**
 * Cell type factory for ModularType
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ModularDataTypeCellValueFactory implements
    Callback<CellDataFeatures<ModularFeatureListRow, Object>, ObservableValue<Object>>,
    Function<ModularFeatureListRow, ModularDataModel> {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final RawDataFile raw;
  private final ModularType modularParentType;
  private final DataType<?> subType;

  private final @NotNull Function<ModularFeatureListRow, ModularDataModel> dataMapSupplier;

  public ModularDataTypeCellValueFactory(RawDataFile raw, ModularType modularParentType,
      DataType<?> subType) {
    this(raw, modularParentType, subType, null);
  }

  public ModularDataTypeCellValueFactory(RawDataFile raw, ModularType modularParentType,
      DataType<?> subType,
      Function<ModularFeatureListRow, ModularDataModel> dataMapSupplier) {
    this.modularParentType = modularParentType;
    this.subType = subType;
    this.raw = raw;
    this.dataMapSupplier = dataMapSupplier == null ? this : dataMapSupplier;
  }

  @Override
  public ObservableValue<Object> call(CellDataFeatures<ModularFeatureListRow, Object> param) {
    final ModularDataModel model = dataMapSupplier.apply(param.getValue().getValue());
    if (model == null) {
      logger.log(Level.WARNING, "There was no DataTypeMap for the column of DataType "
                                + modularParentType.getClass().toString() + "and sub type "
                                + subType.getClass().toString() + " and raw file " + (raw == null
          ? "NONE" : raw.getName()));
      return null;
    }
    try {
      ModularTypeMap parentMap = model.get(modularParentType);
      if (parentMap == null) {
        return null;
      }
      Object value = parentMap.get(subType);
      if (value == null) {
        return null;
      }
      // todo try read only wrapper
      return new ReadOnlyObjectWrapper<>(value);
//
//      Property property = subType.createProperty();
//      property.setValue(value);
//      return (ObservableValue<Object>) property;
    } catch (Exception ex) {
      logger.log(Level.WARNING, MessageFormat
          .format("Cannot get sub type {0} of ModularType {1}", subType.getClass().toString(),
              model.getClass().toString()), ex);
//      throw ex;
    }
    return null;
  }


  /**
   * The default way to get the DataMap. FeatureListRow (for raw==null), Feature for raw!=null.
   */
  @Override
  public ModularDataModel apply(ModularFeatureListRow row) {
    if (raw != null) {
      // find data type map for feature for this raw file
      Map<RawDataFile, ModularFeature> features = row.getFilesFeatures();
      // no features
      if (features.get(raw) == null) {
        return null;
      }
      return features.get(raw);
    } else {
      // use feature list row DataTypeMap
      return row;
    }
  }
}
