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

package io.github.mzmine.datamodel.features;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.exceptions.TypeColumnUndefinedException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javafx.beans.property.Property;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.Nullable;

public interface ModularDataModel {

  /**
   * All types (columns) of this DataModel
   *
   * @return
   */
  public ObservableMap<Class<? extends DataType>, DataType> getTypes();

  /**
   * The map containing all mappings to the types defined in getTypes
   *
   * @param
   * @return
   */
  public ObservableMap<DataType, Object> getMap();

  /**
   * Get DataType column of this DataModel
   *
   * @param <T>
   * @param tclass
   * @return
   */
  default <T> DataType<T> getTypeColumn(Class<? extends DataType<T>> tclass) {
    DataType<T> type = getTypes().get(tclass);
    return type;
  }

  /**
   * has DataType column of this DataModel
   *
   * @param <T>
   * @param tclass
   * @return
   */
  default <T extends Object> boolean hasTypeColumn(Class<? extends DataType<T>> tclass) {
    DataType<T> type = getTypes().get(tclass);
    return type != null;
  }

  /**
   * Optional.ofNullable(value)
   *
   * @param <T>
   * @param type
   * @return
   */
  default <T> Entry<DataType<T>, T> getEntry(DataType<T> type) {
    return new SimpleEntry<>(type, get(type));
  }

  /**
   * Optional.ofNullable(value)
   *
   * @param <T>
   * @param tclass
   * @return
   */
  default <T> Entry<DataType<T>, T> getEntry(Class<? extends DataType<T>> tclass) {
    DataType<T> type = getTypeColumn(tclass);
    return getEntry(type);
  }


  /**
   * Value for this datatype
   *
   * @param tclass
   * @return
   */
  default <T> T get(Class<? extends DataType<T>> tclass) {
    DataType<T> type = getTypeColumn(tclass);
    return get(type);
  }


  /**
   * Value for this datatype
   *
   * @param <T>
   * @param type
   * @return
   */
  @Nullable
  default <T extends Object> T get(DataType<T> type) {
    return (T) getMap().get(type);
  }

  /**
   * type.getFormattedString(value)
   *
   * @param <T>
   * @param type
   * @return
   */
  default <T> String getFormattedString(DataType<T> type) {
    return type.getFormattedString(get(type));
  }

  /**
   * type.getFormattedString(value)
   *
   * @param <T>
   * @param tclass
   * @return
   */
  default <T extends Property<?>> String getFormattedString(Class<? extends DataType<T>> tclass) {
    DataType<T> type = getTypeColumn(tclass);
    return getFormattedString(type);
  }

  /**
   * Set the value
   *
   * @param <T>
   * @param type
   * @param value
   */
  default <T> void set(DataType<T> type, T value) {
    set((Class) type.getClass(), value);
  }

  /**
   * Set the value
   *
   * @param <T>
   * @param tclass
   * @param value
   */
  default <T> void set(Class<? extends DataType<T>> tclass, T value) {
    // type in defined columns?
    if (!getTypes().containsKey(tclass)) {
      throw new TypeColumnUndefinedException(this, tclass);
    }

    DataType<T> realType = getTypeColumn(tclass);
    getMap().put(realType, value);
  }

  /**
   * Should only be called whenever a DataType column is removed from this model. To remove the
   * value of the underlying Property<?> call {@link ModularDataModel#set(DataType, Object)}
   *
   * @param tclass
   */
  default <T> void remove(Class<? extends DataType<T>> tclass) {
    DataType type = getTypeColumn(tclass);
    if (type != null) {
      getMap().remove(type);
    }
  }

  /**
   * Stream all map.entries
   *
   * @return
   */
  default Stream<Entry<DataType, Object>> stream() {
    return getMap().entrySet().stream();
  }

}
