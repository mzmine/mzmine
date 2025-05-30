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

import io.github.mzmine.datamodel.features.columnar_data.ColumnarModularDataModelRow;
import io.github.mzmine.datamodel.features.columnar_data.ColumnarModularDataModelSchema;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple implementation of the {@link ModularDataModel} interface. Based on a map internally, works
 * independently of q {@link ColumnarModularDataModelSchema}.
 */
public abstract class ModularDataModelMap implements ModularDataModel {

  public static final Logger logger = Logger.getLogger(ModularDataModelMap.class.getName());

  /**
   * A read only view of all types (columns) of this DataModel.
   *
   * @return types that are covered by the model
   */
  @Override
  public Set<DataType> getTypes() {
    return getMap().keySet();
  }

  @Override
  public boolean isEmpty() {
    return getMap().isEmpty();
  }

  /**
   * The map containing all mappings to the types defined in getReadOnlyTypes
   */
  public abstract Map<DataType, Object> getMap();


  /**
   * Value for this datatype
   *
   * @param <T>
   * @param type
   * @return
   */
  @Override
  public @Nullable <T extends Object> T get(DataType<T> type) {
    return (T) getMap().get(type);
  }

  /**
   * Value for this datatype or default value if no value was mapped. So only returns default if
   * there was no mapping
   *
   * @return
   */
  @Override
  public @Nullable <T> T getOrDefault(DataType<T> type, T defaultValue) {
    return (T) getMap().getOrDefault(type, defaultValue);
  }

  /**
   * Value for this datatype or default value if no value was mapped or the mapped value was null
   *
   * @return
   */
  @Override
  public @NotNull <T> T getNonNullElse(DataType<T> type, @NotNull T defaultValue) {
    return (T) requireNonNullElse(getMap().getOrDefault(type, null), defaultValue);
  }

  /**
   * Set the value
   *
   * @param <T>
   * @param type
   * @param value
   * @return true if the new value is different than the old
   */
  @Override
  public <T> boolean set(DataType<T> type, T value) {
    if (type instanceof MissingValueType) {
      throw new UnsupportedOperationException(
          "Type %s is not meant to be added to a feature.".formatted(type.getClass()));
    }

    Object old = getMap().put(type, value);
    // send changes to all listeners for this data type
    List<DataTypeValueChangeListener<?>> listeners = getValueChangeListeners().get(type);
    if (!Objects.equals(old, value)) {
      if (listeners != null) {
        for (DataTypeValueChangeListener listener : listeners) {
          listener.valueChanged(this, type, old, value);
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Should only be called whenever a DataType column is removed from this model.
   *
   * @param type the data type to be removed
   */
  @Override
  public <T> void remove(DataType<T> type) {
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
  @Override
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getValueChangeListeners() {
    return Map.of();
  }

  /**
   * Stream all map.entries
   */
  @Override
  public Stream<Entry<DataType, Object>> stream() {
    return getMap().entrySet().stream();
  }

}
