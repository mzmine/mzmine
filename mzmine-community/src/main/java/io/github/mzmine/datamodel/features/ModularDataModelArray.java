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
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModularDataModelArray implements ModularDataModel/*, Map<DataType, Object>*/ {

  protected final ModularDataModelSchema schema;
  private Object[] data;

  public ModularDataModelArray(final ModularDataModelSchema schema) {
    this.schema = schema;
    data = schema.allocateNew();
  }

  @Override
  public boolean isEmpty() {
    return schema.getReadOnlyTypes().isEmpty();
  }

  @Override
  public <T> boolean set(DataType<T> type, T value) {
    if (type instanceof MissingValueType) {
      throw new UnsupportedOperationException(
          STR."Type \{type.getClass()} is not meant to be added to a feature.");
    }

    if (value == null && !schema.containsDataType(type)) {
      // nothing to do if we dont have the entry and dont want to add some else than null
      return false;
    }

    final int index = schema.getIndex(type, true);
    final Object old = data[index];
    data[index] = value;

    if (!Objects.equals(old, value)) {
      List<DataTypeValueChangeListener<?>> listeners = getValueChangeListeners().get(type);
      if (!Objects.equals(old, value)) {
        if (listeners != null) {
          for (DataTypeValueChangeListener listener : listeners) {
            listener.valueChanged(this, type, old, value);
          }
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public <T> @Nullable T get(DataType<T> key) {
//    schema.lockRead() do we need to lock? don't think so
    final int index = schema.getIndex(key, false);
    if (index != -1) {
      return (T) data[index];
    }
    return null;
  }

  @Override
  public <T> @Nullable T getOrDefault(DataType<T> type, @Nullable T defaultValue) {
    final T value = get(type);
    return value == null ? defaultValue : value;
  }

  @Override
  public <T> @NotNull T getNonNullElse(DataType<T> type, @NotNull T defaultValue) {
    return Objects.requireNonNullElse(get(type), defaultValue);
  }

  @Override
  public <T> void remove(DataType<T> type) {
    if (!schema.containsDataType(type)) {
      return;
    }

    set(type, null);
  }

  @Override
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getValueChangeListeners() {
    return schema.getValueChangeListeners();
  }

  @Override
  public Stream<Entry<DataType, Object>> stream() {
    final Set<DataType> types = Set.copyOf(schema.getReadOnlyTypes().keySet());
    return types.stream().map(type -> new SimpleEntry<>(type, get(type)));
  }

  @Override
  public Set<DataType> getTypes() {
    return schema.getReadOnlyTypes().keySet();
  }

  public void putAll(@NotNull Map<? extends DataType, ?> m) {

    final List<? extends Map.Entry<? extends DataType, ?>> nonNullValueEntries = m.entrySet()
        .stream().filter(e -> e.getValue() != null).toList();

    schema.addDataTypes(
        nonNullValueEntries.stream().map(Map.Entry::getKey).toArray(DataType[]::new));
    nonNullValueEntries.forEach(e -> set(e.getKey(), e.getValue()));
  }

  public void clear() {
    for (int i = 0; i < data.length; i++) {
      data[i] = null;
    }
  }

  public @NotNull Collection<Object> values() {
    return Arrays.stream(data).filter(Objects::nonNull).toList();
  }

  /**
   * @param newSize the new size of the data array
   * @return currently always true
   */
  boolean ensureCapacity(int newSize) {
    if (data.length < newSize) {
      data = Arrays.copyOf(data, newSize);
    } else if (data.length > newSize) {
      throw new IllegalStateException(
          "Data size was greater than new size %d : %d".formatted(data.length, newSize));
    }
    return true;
  }
}






