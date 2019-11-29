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
import io.github.mzmine.datamodel.data.types.CommentType;
import io.github.mzmine.datamodel.data.types.DataType;
import javafx.geometry.Pos;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * Default cell factory for a DataType
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 * @param <T>
 */
public class EditableDataTypeCellFactory<T extends DataType> implements
    Callback<TreeTableColumn<ModularFeatureListRow, T>, TreeTableCell<ModularFeatureListRow, T>> {

  private RawDataFile raw;
  private Class<? extends DataType> dataTypeClass;
  private int subcolumn = -1;


  public EditableDataTypeCellFactory(RawDataFile raw, Class<? extends DataType> dataTypeClass) {
    this(raw, dataTypeClass, -1);
  }

  public EditableDataTypeCellFactory(RawDataFile raw, Class<? extends DataType> dataTypeClass,
      int subcolumn) {
    this.dataTypeClass = dataTypeClass;
    this.raw = raw;
    this.subcolumn = subcolumn;
  }

  @Override
  public TreeTableCell<ModularFeatureListRow, T> call(
      TreeTableColumn<ModularFeatureListRow, T> param) {

    TextFieldTreeTableCell<ModularFeatureListRow, T> cell = new TextFieldTreeTableCell<>();
    StringConverter<T> converter = new StringConverter<T>() {
      @Override
      public String toString(T t) {
        return t == null ? "" : t.getFormattedString();
      }

      @Override
      public T fromString(String string) {
        return (T) new CommentType(string);
      }
    };
    cell.setConverter(converter);
    cell.setAlignment(Pos.CENTER);
    return cell;
  }


}
