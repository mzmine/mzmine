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

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellFactory;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellValueFactory;
import io.github.mzmine.datamodel.features.types.fx.EditComboCellFactory;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public abstract class ListWithSubType<T> extends ListDataType<T> implements
    SubColumnsFactory<List<T>> {

  private static final Logger logger = Logger.getLogger(ListWithSubType.class.getName());

  /**
   * The unmodifiable list of sub data types. Order reflects the initial order of columns.
   *
   * @return
   */
  @NotNull
  public abstract List<DataType> getSubDataTypes();

  @Override
  @NotNull
  public List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable RawDataFile raw) {
    final ListWithSubType thisType = this;
    // add column for each sub data type
    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();

    List<DataType> subTypes = getSubDataTypes();
    // create column per name
    for (int index = 0; index < getNumberOfSubColumns(); index++) {
      DataType type = subTypes.get(index);
      TreeTableColumn<ModularFeatureListRow, Object> col = new TreeTableColumn<>(
          type.getHeaderString());
      DataTypeCellValueFactory cellValueFactory = new DataTypeCellValueFactory(raw, this);
      col.setCellValueFactory(cellValueFactory);
      if (type instanceof EditableColumnType) {
        col.setCellFactory(new EditComboCellFactory(raw, this, index));
      } else {
        col.setCellFactory(new DataTypeCellFactory(raw, this, index));
      }
      // add column
      cols.add(col);
    }

    return cols;
  }

  /**
   * Sub DataType is the sub column at index (see {@link #getSubDataTypes()#})
   *
   * @param index
   * @return
   */
  public DataType getSubTypeAt(int index) {
    return index >= 0 && index < getSubDataTypes().size() ? getSubDataTypes().get(index) : null;
  }


  @NotNull
  @Override
  public int getNumberOfSubColumns() {
    return getSubDataTypes().size();
  }

  @Nullable
  @Override
  public String getHeader(int subcolumn) {
    List<DataType> list = getSubDataTypes();
    if (subcolumn >= 0 && subcolumn < list.size()) {
      return list.get(subcolumn).getHeaderString();
    } else {
      throw new IndexOutOfBoundsException(
          "Sub column index " + subcolumn + " is out of range " + list.size());
    }
  }

  @Override
  @Nullable
  public String getFormattedSubColValue(int subcolumn,
      TreeTableCell<ModularFeatureListRow, Object> cell,
      TreeTableColumn<ModularFeatureListRow, Object> coll, Object value, RawDataFile raw) {
    DataType sub = getSubTypeAt(subcolumn);
    if (sub == null) {
      return "";
    }
    Object realVal = value == null ? sub.getDefaultValue() : value;
    if (realVal == null) {
      return "";
    }

    Object subvalue = null;
    try {
      List<T> list = ((List<T>) realVal);
      subvalue = list.isEmpty() ? null : getSubColValue(sub, list);
      return sub.getFormattedString(subvalue);
    } catch (Exception ex) {
      logger.log(Level.WARNING, String.format(
          "Error while formatting sub column value in type %s. Sub type %s cannot format value of %s",
          this.getClass(), sub.getClass(), sub, (subvalue == null ? "" : subvalue.getClass())), ex);
      return "";
    }
  }

  /**
   * The sub column value for a specific subType in a value.
   *
   * @param subType the sub type
   * @param value   the value (often the first in the list of values)
   * @return the sub column value or null if value==null or if sub column empty.
   */
  protected abstract <K> @Nullable K getSubColValue(@NotNull DataType<K> subType,
      @Nullable List<T> value);

  @Nullable
  @Override
  public Node getSubColNode(int subcolumn, TreeTableCell<ModularFeatureListRow, Object> cell,
      TreeTableColumn<ModularFeatureListRow, Object> coll, Object cellData, RawDataFile raw) {
    DataType sub = getSubTypeAt(subcolumn);
    if (sub instanceof LinkedGraphicalType lgType) {
      return lgType.getCellNode(cell, coll, null, raw);
    } else if (cellData == null || sub == null || !(sub instanceof GraphicalColumType gcType)) {
      return null;
    } else {
      return gcType.getCellNode(cell, coll, cellData, raw);
    }
  }

}
