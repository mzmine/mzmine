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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.exceptions.TypeColumnUndefinedException;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
  public ObservableMap<DataType, Property<?>> getMap();

  /**
   * Get DataType column of this DataModel
   * 
   * @param <T>
   * @param tclass
   * @return
   */
  default <T extends Property<?>> DataType<T> getTypeColumn(Class<? extends DataType<T>> tclass) {
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
  default <T extends Property<?>> boolean hasTypeColumn(Class<? extends DataType<T>> tclass) {
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
  default <T extends Property<?>> Entry<DataType<T>, T> getEntry(DataType<T> type) {
    return new SimpleEntry<>(type, get(type));
  }

  /**
   * Optional.ofNullable(value)
   * 
   * @param <T>
   * @param tclass
   * @return
   */
  default <T extends Property<?>> Entry<DataType<T>, T> getEntry(
      Class<? extends DataType<T>> tclass) {
    DataType<T> type = getTypeColumn(tclass);
    return getEntry(type);
  }

  /**
   * Value for this datatype
   * 
   * @param <T>
   * @param type
   * @return
   */
  default Object getValue(DataType type) {
    return getMap().get(type).getValue();
  }

  /**
   * Value for this datatype
   * 
   * @param <T>
   * @param tclass
   * @return
   */
  default Object getValue(Class tclass) {
    DataType type = getTypeColumn(tclass);
    return get(type).getValue();
  }

  /**
   * Property for this datatype
   * 
   * @param <T>
   * @param type
   * @return
   */
  default <T extends Property<?>> T get(DataType<T> type) {
    return (T) getMap().get(type);
  }

  /**
   * Property for this datatype
   * 
   * @param <T>
   * @param tclass
   * @return
   */
  default <T extends Property<?>> T get(Class<? extends DataType<T>> tclass) {
    DataType<T> type = getTypeColumn(tclass);
    return get(type);
  }

  /**
   * type.getFormattedString(value)
   * 
   * @param <T>
   * @param type
   * @return
   */
  default <T extends Property<?>> String getFormattedString(DataType<T> type) {
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
   * setProperty should only be called after adding a new DataType column (e.g., by the
   * FeatureList). To set the value of wrapping Property<?> call
   * {@link ModularDataModel#set(Class, Object)}
   * 
   * @param <T>
   * @param type
   * @param value
   */
  default void setProperty(DataType<?> type, Property<?> value) {
    // type in defined columns?
    if (!getTypes().containsKey(type.getClass()))
      throw new TypeColumnUndefinedException(this, type.getClass());

    DataType realType = getTypes().get(type.getClass());
    // only set datatype -> property value once
    if (getMap().get(realType) == null)
      getMap().put(realType, value);
  }

  /**
   * Set the value that is wrapped inside a property
   * 
   * @param <T>
   * @param type
   * @param value
   */
  default <T extends Property<?>> void set(DataType<T> type, Object value) {
    set((Class) type.getClass(), value);
  }

  /**
   * Set the value that is wrapped inside a property
   * 
   * @param <T>
   * @param tclass
   * @param value
   */
  default <T extends Property<?>> void set(Class<? extends DataType<T>> tclass, Object value) {
    // type in defined columns?
    if (!getTypes().containsKey(tclass))
      throw new TypeColumnUndefinedException(this, tclass);

    DataType realType = getTypeColumn(tclass);
    Property property = get(realType);
    // lists need to be ObservableList
    if (value instanceof List && !(value instanceof ObservableList))
      property.setValue(FXCollections.observableList((List) value));
    else if (value instanceof Map && !(value instanceof ObservableMap))
      property.setValue(FXCollections.observableMap((Map) value));
    else
      property.setValue(value);
  }

  /**
   * Should only be called whenever a DataType column is removed from this model. To remove the
   * value of the underlying Property<?> call {@link ModularDataModel#set(DataType, Object)}
   * 
   * @param <T>
   * @param tclass
   */
  default <T extends Property<?>> void removeProperty(Class<? extends DataType<T>> tclass) {
    DataType type = getTypeColumn(tclass);
    if (type != null)
      getMap().remove(type);
  }

  /**
   * Stream all map.entries
   * 
   * @return
   */
  default Stream<Entry<DataType, Property<?>>> stream() {
    return getMap().entrySet().stream();
  }
}
