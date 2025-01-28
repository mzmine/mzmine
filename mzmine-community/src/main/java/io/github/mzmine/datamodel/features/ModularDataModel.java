/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ModularDataModel {

  Set<DataType> getTypes();

  boolean isEmpty();

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
    DataType<T> type = DataTypes.get(tclass);
    return getEntry(type);
  }

  /**
   * Value for this datatype
   *
   * @param tclass
   * @return
   */
  default <T> T get(Class<? extends DataType<T>> tclass) {
    DataType<T> type = DataTypes.get(tclass);
    return get(type);
  }

  @Nullable <T extends Object> T get(DataType<T> type);

  /**
   * Value for this datatype or default value if no value was mapped. So only returns default if
   * there was no mapping
   *
   * @return
   */
  @Nullable
  default <T> T getOrDefault(Class<? extends DataType<T>> tclass, T defaultValue) {
    DataType<T> type = DataTypes.get(tclass);
    return getOrDefault(type, defaultValue);
  }

  @Nullable <T> T getOrDefault(DataType<T> type, @Nullable T defaultValue);

  /**
   * Value for this datatype or default value if no value was mapped or the mapped value was null
   *
   * @return
   */
  @NotNull
  default <T> T getNonNullElse(Class<? extends DataType<T>> tclass, @NotNull T defaultValue) {
    DataType<T> type = DataTypes.get(tclass);
    return getNonNullElse(type, defaultValue);
  }

  @NotNull <T> T getNonNullElse(DataType<T> type, @NotNull T defaultValue);

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
    DataType<T> type = DataTypes.get(tclass);
    return getFormattedString(type);
  }

  /**
   * must trigger all {@link ModularDataModel#getValueChangeListeners()}
   */
  <T> boolean set(DataType<T> type, T value);

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
    DataType<T> type = DataTypes.get(tclass);
    return set(type, value);
  }

  /**
   * Should only be called whenever a DataType column is removed from this model.
   *
   * @param tclass the data type class to be removed
   */
  default <T> void remove(Class<? extends DataType<T>> tclass) {
    DataType type = DataTypes.get(tclass);
    remove(type);
  }

  <T> void remove(DataType<T> type);

  @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getValueChangeListeners();

  Stream<Entry<DataType, Object>> stream();

  default void forEach(BiConsumer<DataType, Object> action) {
    stream().forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
  }
}
