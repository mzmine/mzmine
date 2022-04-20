/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public abstract class ListWithSubsType<T> extends ListDataType<T> implements
    SubColumnsFactory, EditableColumnType {

  private static final Logger logger = Logger.getLogger(ListWithSubsType.class.getName());

  protected static <T> SimpleEntry<Class<? extends DataType>, Function<T, Object>> createEntry(
      Class<? extends DataType> clazz, Function<T, Object> function) {
    return new SimpleEntry<>(clazz, function);
  }

  /**
   * The unmodifiable list of sub data types. Order reflects the initial order of columns.
   *
   * @return list of sub data types
   */
  @NotNull
  public abstract List<DataType> getSubDataTypes();

  @Override
  @NotNull
  public List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType) {
    // add column for each sub data type
    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();

    List<DataType> subTypes = getSubDataTypes();
    // create column per name
    for (int index = 0; index < getNumberOfSubColumns(); index++) {
      DataType type = subTypes.get(index);
      if (this.getClass().isInstance(type)) {
        // create a special column for this type that actually represents the list of data
        cols.add(DataType.createStandardColumn(type, raw, this, index));
      } else {
        // create all other columns
        var col = type.createColumn(raw, this, index);
        // override type in CellValueFactory with this parent type
        cols.add(col);
      }
    }

    return cols;
  }

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
  public String getUniqueID(int subcolumn) {
    // do not change unique ID
    List<DataType> list = getSubDataTypes();
    if (subcolumn >= 0 && subcolumn < list.size()) {
      return list.get(subcolumn).getUniqueID();
    } else {
      throw new IndexOutOfBoundsException(
          "Sub column index " + subcolumn + " is out of range " + list.size());
    }
  }

  @Override
  public @NotNull DataType<?> getType(int index) {
    if (index < 0 || index >= getSubDataTypes().size()) {
      throw new IndexOutOfBoundsException(
          String.format("Sub column index %d is out of bounds %d", index,
              getSubDataTypes().size()));
    }
    return getSubDataTypes().get(index);
  }

  @Override
  @Nullable
  public String getFormattedSubColValue(int subcolumn, Object value) {
    DataType sub = getType(subcolumn);
    if (sub == null) {
      return "";
    }
    if (value == null) {
      return sub.getFormattedString(sub.getDefaultValue());
    }

    Object subvalue = null;
    try {
      List<T> list = ((List<T>) value);
      subvalue = list.isEmpty() ? sub.getDefaultValue() : getSubColValue(sub, list);
      return sub.getFormattedString(subvalue);
    } catch (Exception ex) {
      logger.log(Level.WARNING, String.format(
          "Error while formatting sub column value in type %s. Sub type %s cannot format value of %s",
          this.getClass().getName(), sub.getClass().getName(),
          (subvalue == null ? "null" : subvalue.getClass())), ex);
      return "";
    }
  }

  @Override
  public @Nullable Object getSubColValue(int subcolumn, Object value) {
    DataType sub = getType(subcolumn);
    if (sub == null) {
      return null;
    }
    if (value == null) {
      return sub.getDefaultValue();
    }

    Object subvalue = null;
    try {
      List<T> list = ((List<T>) value);
      return list.isEmpty() ? sub.getDefaultValue() : getSubColValue(sub, list);
    } catch (Exception ex) {
      logger.log(Level.WARNING, String.format(
          "Error while getting sub column value in type %s. Sub type %s cannot get value of %s",
          this.getClass().getName(), sub.getClass().getName(),
          (subvalue == null ? "" : subvalue.getClass())), ex);
      return "";
    }
  }

  /**
   * The sub column value for a specific subType in a value.
   *
   * @param subType the sub type
   * @param list    the list
   * @return the sub column value or null if value==null or if sub column empty.
   */
  protected <K> @Nullable K getSubColValue(@NotNull DataType<K> subType,
      @Nullable List<T> list) {
    if (list == null || list.isEmpty()) {
      return subType.getDefaultValue();
    } else {
      if (this.getClass().isInstance(subType)) {
        // all ions
        return (K) list;
      } else {
        // get value for first ion
        return (K) getMapper().get(subType.getClass()).apply(list.get(0));
      }
    }
  }

  /**
   * Mapper from first list element to sub column value
   *
   * @return
   */
  protected abstract Map<Class<? extends DataType>, Function<T, Object>> getMapper();

}
