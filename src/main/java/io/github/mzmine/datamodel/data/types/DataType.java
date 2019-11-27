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

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.DataTypeMap;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.modifiers.GraphicalCellData;
import io.github.mzmine.datamodel.data.types.modifiers.SubColumnsFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;

/**
 * Class of data types: Provides formatters
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 * @param <T>
 */
public abstract class DataType<T> {

  protected final T value;

  public DataType(T value) {
    this.value = value;
  }

  public T getValue() {
    return value;
  }

  /**
   * A formatted string representation of the value
   * 
   * @return
   */
  public String getFormattedString() {
    if (value != null)
      return value.toString();
    else
      return "";
  }

  /**
   * The header string (name) of this data type
   * 
   * @return
   */
  public abstract String getHeaderString();

  /**
   * Create a TreeTableColumn
   * 
   * @param raw null if this is a FeatureListRow column. For Feature columns: the raw data file
   *        specifies the feature.
   * 
   * @return
   */
  public TreeTableColumn<ModularFeatureListRow, ? extends DataType> createColumn(
      final @Nullable RawDataFile raw) {
    // create column
    TreeTableColumn<ModularFeatureListRow, ? extends DataType> col =
        new TreeTableColumn<>(getHeaderString());

    if (this instanceof SubColumnsFactory) {
      col.setSortable(false);
      // add sub columns (no value factory needed for parent column)
      List<TreeTableColumn<ModularFeatureListRow, ?>> children =
          ((SubColumnsFactory) this).createSubColumns();
      col.getColumns().addAll(children);
      return col;
    } else {
      col.setSortable(true);
      // define observable
      col.setCellValueFactory(r -> {
        final DataTypeMap map;
        if (raw != null) {
          // find data type map for feature for this raw file
          ObservableMap<RawDataFile, ModularFeature> features =
              r.getValue().getValue().getFeatures();
          // no features
          // TODO somehow: the HashMap fails to retrieve values for RawDataFiles
          // TODO look at equals and hashCode() method!
          if (features.get(raw) == null)
            return null;
          map = features.get(raw).getMap();
        } else {
          // use feature list row DataTypeMap
          map = r.getValue().getValue().getMap();
        }

        Optional<? extends DataType> o = map.get(this.getClass());
        final SimpleObjectProperty<DataType<?>> property =
            new SimpleObjectProperty<>(o.orElse(null));
        // listen for changes in this rows DataTypeMap
        map.getObservableMap().addListener((
            MapChangeListener.Change<? extends Class<? extends DataType>, ? extends DataType> change) -> {
          if (this.getClass().equals(change.getKey())) {
            property.set(map.get(this.getClass()).orElse(null));
          }
        });
        return property;
      });

      // value representation
      col.setCellFactory(param -> new TreeTableCell<ModularFeatureListRow, DataType<?>>() {
        @Override
        protected void updateItem(DataType<?> item, boolean empty) {
          super.updateItem(item, empty);
          if (item == null || empty) {
            setGraphic(null);
            setText(null);
          } else {
            if (item instanceof GraphicalCellData) {
              Node node = ((GraphicalCellData) item).getCellNode(this, param);
              setGraphic(node);
              setText(null);
            } else {
              setText(item.getFormattedString());
              setGraphic(null);
            }
          }
          setAlignment(Pos.CENTER);
        }
      });
    }
    return col;
  }
}
