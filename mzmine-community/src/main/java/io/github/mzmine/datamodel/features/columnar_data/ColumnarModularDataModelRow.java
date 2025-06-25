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

package io.github.mzmine.datamodel.features.columnar_data;

import io.github.mzmine.datamodel.features.DataTypeValueChangeListener;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A row in a {@link ColumnarModularDataModelSchema} where the index was obtained from
 * {@link ColumnarModularDataModelSchema#addRowGetIndex()}.
 */
public class ColumnarModularDataModelRow implements ModularDataModel {

  @NotNull
  protected final ColumnarModularDataModelSchema schema;
  protected final int modelRowIndex;

  public ColumnarModularDataModelRow(@NotNull final ColumnarModularDataModelSchema schema) {
    this.schema = schema;
    modelRowIndex = schema.addRowGetIndex();
  }

  @Override
  public boolean isEmpty() {
    return schema.isEmpty();
  }

  @Override
  public <T> boolean set(DataType<T> type, T value) {
    return schema.set(this, modelRowIndex, type, value);
  }

  @Override
  public <T> @Nullable T get(DataType<T> key) {
    return schema.get(modelRowIndex, key);
  }

  @Override
  public <T> @Nullable T getOrDefault(DataType<T> type, @Nullable T defaultValue) {
    return schema.getOrDefault(modelRowIndex, type, defaultValue);
  }

  @Override
  public <T> @NotNull T getNonNullElse(DataType<T> type, @NotNull T defaultValue) {
    return schema.getNonNullElse(modelRowIndex, type, defaultValue);
  }

  @Override
  public <T> void remove(@NotNull DataType<T> type) {
    // cannot completely remove the type from the schema - this should be called directly on the schema
    // so on a row we can only set null
    set(type, null);
  }

  @Override
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getValueChangeListeners() {
    return schema.getValueChangeListeners();
  }

  @Override
  public Stream<Entry<DataType, Object>> stream() {
    return schema.streamValues(modelRowIndex);
  }

  @Override
  public Set<DataType> getTypes() {
    return schema.getTypes();
  }

  public void putAll(@NotNull Map<? extends DataType, ?> m) {
    // in case multiple data types are new
    schema.addDataTypes(m.keySet().toArray(DataType[]::new));
    m.forEach((type, value) -> schema.set(this, modelRowIndex, type, value));
  }

}






