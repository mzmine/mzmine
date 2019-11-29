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

package io.github.mzmine.datamodel.data.types.fx;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.data.types.modifiers.SubColumnsFactory;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;

/**
 * Default cell factory for a DataType
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 * @param <T>
 */
public class DataTypeCellFactory<T extends DataType> implements
    Callback<TreeTableColumn<ModularFeatureListRow, T>, TreeTableCell<ModularFeatureListRow, T>> {

  private RawDataFile raw;
  private Class<? extends DataType> dataTypeClass;
  private int subcolumn = -1;


  public DataTypeCellFactory(RawDataFile raw, Class<? extends DataType> dataTypeClass) {
    this(raw, dataTypeClass, -1);
  }

  public DataTypeCellFactory(RawDataFile raw, Class<? extends DataType> dataTypeClass,
      int subcolumn) {
    this.dataTypeClass = dataTypeClass;
    this.raw = raw;
    this.subcolumn = subcolumn;
  }

  @Override
  public TreeTableCell<ModularFeatureListRow, T> call(
      TreeTableColumn<ModularFeatureListRow, T> param) {
    return new TreeTableCell<>() {
      @Override
      protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setGraphic(null);
          setText(null);
        } else {
          // sub columns provide values
          if (item instanceof SubColumnsFactory) {
            // get sub column value
            SubColumnsFactory sub = (SubColumnsFactory) item;
            Node n = sub.getSubColNode(subcolumn, this, param, item, raw);
            setGraphic(n);
            setText(n != null ? null : sub.getFormattedSubColValue(subcolumn));
            setTooltip(new Tooltip(sub.getFormattedSubColValue(subcolumn)));
          } else {
            if (item instanceof GraphicalColumType) {
              Node node = ((GraphicalColumType) item).getCellNode(this, param, item, raw);
              setGraphic(node);
              setText(null);
              setTooltip(new Tooltip(item.getFormattedString()));
            } else {
              setTooltip(new Tooltip(item.getFormattedString()));
              setText(item.getFormattedString());
              setGraphic(null);
            }
          }
        }
        setAlignment(Pos.CENTER);
      }
    };
  }


}
