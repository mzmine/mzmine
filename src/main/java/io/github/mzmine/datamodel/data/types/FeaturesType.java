/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data.types;

import java.util.Map;
import java.util.Map.Entry;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.DataTypeMap;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.modifiers.SubColumnsFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.control.TreeTableColumn;

/**
 * This FeaturesType contains features for each RawDataFile. Sub columns for samples and charts are
 * created.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class FeaturesType extends DataType<ObservableMap<RawDataFile, ModularFeature>>
    implements SubColumnsFactory {

  public FeaturesType(Map<RawDataFile, ModularFeature> map) {
    super(FXCollections.unmodifiableObservableMap(FXCollections.observableMap(map)));
  }

  @Override
  public String getHeaderString() {
    return "Features";
  }

  @Override
  public void addSubColumns(TreeTableColumn parent) {
    TreeTableColumn[] cols = new TreeTableColumn[value.size()];
    int i = 0;
    for (Entry<RawDataFile, ModularFeature> entry : value.entrySet()) {
      RawDataFile raw = entry.getKey();
      // create column per name
      TreeTableColumn<ModularFeatureListRow, String> sampleCol =
          new TreeTableColumn<>(raw.getName());
      // add sub columns of feature
      DataTypeMap map = entry.getValue().getMap();
      map.stream().forEach(dataType -> {
        sampleCol.getColumns().add(dataType.createColumn(raw));
      });

      // add all
      cols[i] = sampleCol;
      i++;
    }
    parent.getColumns().addAll(cols);
  }

  @Override
  public TreeTableColumn<ModularFeatureListRow, ? extends DataType> createColumn(RawDataFile raw) {
    TreeTableColumn<ModularFeatureListRow, ? extends DataType> col =
        new TreeTableColumn<>(getHeaderString());
    addSubColumns(col);
    return col;
  }

}
