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

package io.github.mzmine.datamodel.data;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.data.types.DataType;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

public interface ModularDataModel {

  /**
   * All types (columns) of this DataModel
   * 
   * @return
   */
  public ObservableList<DataType> getTypes();

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
    return getTypes().stream().filter(t -> t.getClass().equals(tclass)).findFirst().get();
  }

  /**
   * Adds the type column to the getTypes list. This needs to be done for each new value
   * 
   * @param types
   */
  default void addTypeColumn(DataType<?>... types) {
    for (DataType<?> t : types) {
      if (!getTypes().contains(t))
        getTypes().add(t);
    }
  }

  /**
   * Optional.ofNullable(value)
   * 
   * @param <T>
   * @param type
   * @return
   */
  default <T> Entry<DataType<T>, Optional<T>> getEntry(DataType<T> type) {
    return new SimpleEntry<>(type, get(type));
  }

  /**
   * Optional.ofNullable(value)
   * 
   * @param <T>
   * @param tclass
   * @return
   */
  default <T> Entry<DataType<T>, Optional<T>> getEntry(Class<? extends DataType<T>> tclass) {
    DataType<T> type = getTypeColumn(tclass);
    return getEntry(type);
  }

  /**
   * Optional.ofNullable(value)
   * 
   * @param <T>
   * @param type
   * @return
   */
  default <T> Optional<T> get(DataType<T> type) {
    return Optional.ofNullable((T) getMap().get(type));
  }

  /**
   * Optional.ofNullable(value)
   * 
   * @param <T>
   * @param tclass
   * @return
   */
  default <T> Optional<T> get(Class<? extends DataType<T>> tclass) {
    DataType<T> type = getTypeColumn(tclass);
    return get(type);
  }

  /**
   * Optional.ofNullable(type.getFormattedString(value))
   * 
   * @param <T>
   * @param type
   * @return
   */
  default <T> Optional<String> getFormattedString(DataType<T> type) {
    return get(type).map(v -> type.getFormattedString(v));
  }

  /**
   * Optional.ofNullable(type.getFormattedString(value))
   * 
   * @param <T>
   * @param tclass
   * @return
   */
  default <T> Optional<String> getFormattedString(Class<? extends DataType<T>> tclass) {
    DataType<T> type = getTypeColumn(tclass);
    return getFormattedString(type);
  }


  //
  default void set(@Nonnull DataType type, Object value) {
    if (type.checkValidValue(value))
      getMap().put(type, value);
    // wrong data type. Check code that supplied this data
    else
      throw new WrongTypeException(type.getClass(), value);
  }

  default <T> void set(Class<? extends DataType<T>> tclass, Object value) {
    DataType type = getTypeColumn(tclass);
    set(type, value);
  }

  default void remove(Class<? extends DataType<?>> key) {
    getMap().remove(key);
  }

  default Stream<Entry<DataType, Object>> stream() {
    return getMap().entrySet().stream();
  }
}
