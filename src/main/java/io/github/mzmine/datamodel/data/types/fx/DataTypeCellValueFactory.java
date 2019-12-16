/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data.types.fx;

import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularDataModel;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.DataType;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.util.Callback;

/**
 * Default data cell type factory
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 * @param <T>
 */
public class DataTypeCellValueFactory implements
    Callback<TreeTableColumn.CellDataFeatures<ModularFeatureListRow, Object>, ObservableValue<Object>>,
    Function<CellDataFeatures<ModularFeatureListRow, Object>, ModularDataModel> {
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private RawDataFile raw;
  private DataType<?> type;
  private final @Nonnull Function<CellDataFeatures<ModularFeatureListRow, Object>, ModularDataModel> dataMapSupplier;

  public DataTypeCellValueFactory(RawDataFile raw, DataType<?> type) {
    this(raw, type, null);
  }

  public DataTypeCellValueFactory(RawDataFile raw, DataType<?> type,
      Function<CellDataFeatures<ModularFeatureListRow, Object>, ModularDataModel> dataMapSupplier) {
    this.type = type;
    this.raw = raw;
    this.dataMapSupplier = dataMapSupplier == null ? this : dataMapSupplier;
  }

  @Override
  public ObservableValue<Object> call(CellDataFeatures<ModularFeatureListRow, Object> param) {
    final ModularDataModel map = dataMapSupplier.apply(param);
    if (map == null) {
      logger.log(Level.WARNING,
          "There was no DataTypeMap for the column of DataType "
              + type.getClass().descriptorString() + " and raw file "
              + (raw == null ? "NONE" : raw.getName()));
      return null;
    }

    return (ObservableValue<Object>) map.get(type);
  }


  /**
   * The default way to get the DataMap. FeatureListRow (for raw==null), Feature for raw!=null.
   */
  @Override
  public ModularDataModel apply(CellDataFeatures<ModularFeatureListRow, Object> param) {
    if (raw != null) {
      // find data type map for feature for this raw file
      Map<RawDataFile, ModularFeature> features = param.getValue().getValue().getFeatures();
      // no features
      if (features.get(raw) == null)
        return null;
      return features.get(raw);
    } else {
      // use feature list row DataTypeMap
      return param.getValue().getValue();
    }
  }
}
