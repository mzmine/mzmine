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
import io.github.mzmine.datamodel.data.types.DataType;
import javafx.collections.ObservableMap;

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

  default <T> void set(DataType<T> type, T value) {
    if (!getTypes().containsKey(type.getClass()))
      throw new TypeColumnUndefinedException(this, type.getClass());

    DataType realType = getTypes().get(type.getClass());
    if (type.checkValidValue(value)) {
      getMap().put(realType, value);
    }
    // wrong data type. Check code that supplied this data
    else
      throw new WrongTypeException(type.getClass(), value);
  }

  default <T> void set(Class<? extends DataType<T>> tclass, T value) {
    if (!getTypes().containsKey(tclass))
      throw new TypeColumnUndefinedException(this, tclass);

    DataType type = getTypeColumn(tclass);
    if (type.checkValidValue(value)) {
      getMap().put(type, value);
    }
    // wrong data type. Check code that supplied this data
    else
      throw new WrongTypeException(type.getClass(), value);
  }


  /**
   * Only sets the value if column is defined in FeatureList. The {@link #set(DataType, Object)}
   * method is preferred as it will throw an Exception on setting an undefined DataType.
   * 
   * @param <T>
   * @param type
   * @param value
   */
  default <T> void setIfAvailable(DataType<T> type, T value) {
    if (!getTypes().containsKey(type.getClass()))
      return;

    DataType realType = getTypes().get(type.getClass());
    if (type.checkValidValue(value)) {
      getMap().put(realType, value);
    }
    // wrong data type. Check code that supplied this data
    else
      throw new WrongTypeException(type.getClass(), value);
  }

  /**
   * Only sets the value if column is defined in FeatureList. The {@link #set(Class, Object)} method
   * is preferred as it will throw an Exception on setting an undefined DataType.
   * 
   * @param <T>
   * @param type
   * @param value
   */
  default <T> void setIfAvailable(Class<? extends DataType<T>> tclass, T value) {
    if (!getTypes().containsKey(tclass))
      return;

    DataType type = getTypeColumn(tclass);
    if (type.checkValidValue(value)) {
      getMap().put(type, value);
    }
    // wrong data type. Check code that supplied this data
    else
      throw new WrongTypeException(type.getClass(), value);
  }

  default void remove(Class<? extends DataType<?>> tclass) {
    DataType type = getTypeColumn(tclass);
    if (type != null)
      getMap().remove(type);
  }

  default Stream<Entry<DataType, Object>> stream() {
    return getMap().entrySet().stream();
  }
}
