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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;
import javafx.beans.property.Property;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
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
   * @param type
   * @return true if value is not null
   */
  @Nullable
  default <T extends Object> boolean hasValueFor(DataType<T> type) {
    return getMap().get(type) != null;
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
   * @return true if the new value is different than the old
   */
  default <T> boolean set(DataType<T> type, T value) {
    return set((Class) type.getClass(), value);
  }

  /**
   * Set the value
   *
   * @param <T>
   * @param tclass
   * @param value
   * @return true if the new value is different than the old
   */
  default <T> boolean set(Class<? extends DataType<T>> tclass, T value) {
    // type in defined columns?
    if (!getTypes().containsKey(tclass)) {
      throw new TypeColumnUndefinedException(this, tclass);
    }

    DataType<T> realType = getTypeColumn(tclass);
    Object old = getMap().put(realType, value);
    // send changes to all listeners for this data type
    List<DataTypeValueChangeListener<?>> listeners = getValueChangeListeners().get(realType);
    if (!Objects.equals(old, value)) {
      if (listeners != null) {
        for (DataTypeValueChangeListener listener : listeners) {
          listener.valueChanged(this, realType, old, value);
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Should only be called whenever a DataType column is removed from this model.
   *
   * @param tclass the data type class to be removed
   */
  default <T> void remove(Class<? extends DataType<T>> tclass) {
    DataType type = getTypeColumn(tclass);
    if (type != null) {
      Object old = getMap().remove(type);
      if (old != null) {
        List<DataTypeValueChangeListener<?>> listeners = getValueChangeListeners().get(type);
        if (listeners != null) {
          for (DataTypeValueChangeListener listener : listeners) {
            listener.valueChanged(this, type, old, null);
          }
        }
      }
    }
  }

  /**
   * Maps listeners to their {@link DataType}s. Default returns an empty list.
   */
  default @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getValueChangeListeners() {
    return Map.of();
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
