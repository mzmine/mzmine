/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
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
  ObservableMap<Class<? extends DataType>, DataType> getTypes();

  /**
   * The map containing all mappings to the types defined in getTypes
   *
   * @param
   * @return
   */
  ObservableMap<DataType, Object> getMap();

  default boolean isEmpty() {
    return getMap().isEmpty();
  }
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
   * Value for this datatype or default value if no value was mapped. So only returns default if
   * there was no mapping
   *
   * @return
   */
  @Nullable
  default <T> T getOrDefault(Class<? extends DataType<T>> tclass, T defaultValue) {
    DataType<T> type = getTypeColumn(tclass);
    return getOrDefault(type, defaultValue);
  }

  /**
   * Value for this datatype or default value if no value was mapped. So only returns default if
   * there was no mapping
   *
   * @return
   */
  @Nullable
  default <T> T getOrDefault(DataType<T> type, T defaultValue) {
    return (T) getMap().getOrDefault(type, defaultValue);
  }

  /**
   * Value for this datatype or default value if no value was mapped or the mapped value was null
   *
   * @return
   */
  @NotNull
  default <T> T getNonNullElse(Class<? extends DataType<T>> tclass, @NotNull T defaultValue) {
    DataType<T> type = getTypeColumn(tclass);
    return getNonNullElse(type, defaultValue);
  }

  /**
   * Value for this datatype or default value if no value was mapped or the mapped value was null
   *
   * @return
   */
  @NotNull
  default <T> T getNonNullElse(DataType<T> type, @NotNull T defaultValue) {
    return (T) requireNonNullElse(getMap().getOrDefault(type, null), defaultValue);
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
   * @param typeClass the type class
   * @return true if value is not null
   */
  @Nullable
  default <T> boolean hasValueFor(Class<? extends DataType<T>> typeClass) {
    return get(typeClass) != null;
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
    // automatically add columns if new
    getTypes().computeIfAbsent(tclass, key -> DataTypes.get(tclass));

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
