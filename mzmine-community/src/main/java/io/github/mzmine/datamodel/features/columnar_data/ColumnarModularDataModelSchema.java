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
import io.github.mzmine.datamodel.features.DataTypesChangedListener;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularDataModelArray;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.github.mzmine.util.concurrent.CloseableResourceLock;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColumnarModularDataModelSchema {

  private static final Logger logger = Logger.getLogger(
      ColumnarModularDataModelSchema.class.getName());

  protected final MemoryMapStorage storage;
  protected final Map<DataType, DataColumn> columns = HashMap.newHashMap(20);
  private final Map<DataType, DataColumn> readOnlyColumns = Collections.unmodifiableMap(columns);

  /**
   * The current length of the columns
   */
  protected volatile int columnLength;
  private final AtomicInteger nextRow = new AtomicInteger(0);
  private final int sizeIncrement = 5000;

  /**
   * A lock that controls the access and specifically the resizing of all
   * {@link ModularDataModelArray}s controlled by this schema.
   */
  protected final CloseableReentrantReadWriteLock resizeLock = new CloseableReentrantReadWriteLock();

  protected final String modelName;

  private final Map<DataType<?>, List<DataTypeValueChangeListener<?>>> dataTypeValueChangedListeners = new HashMap<>();
  private final List<DataTypesChangedListener> dataTypesChangeListeners = new ArrayList<>();

  public ColumnarModularDataModelSchema(final MemoryMapStorage storage, String modelName,
      int initialSize) {
    this.storage = storage;
    this.modelName = modelName;
    columnLength = initialSize;
  }

  public boolean containsDataType(final DataType type) {
//    logger.finest(modelName + " Trying to lock for read");
//    try (var _ = resizeLock.lockRead()) {
//      logger.finest(modelName + " Read lock acquired");
    return columns.containsKey(type);
//    }
  }

  public void addDataTypes(final DataType... types) {
    List<DataType> toAdd = new ArrayList<>(types.length);
//    logger.finest(modelName + " Trying to lock for read");
//    try (var _ = resizeLock.lockRead()) {
//      logger.finest(modelName + " Read lock acquired");
    for (DataType type : types) {
      if (!containsDataType(type)) {
        toAdd.add(type);
      }
//      }
    }
    if (toAdd.isEmpty()) {
      return;
    }
//    logger.finest("%s: no index for %d data types %s".formatted(modelName, toAdd.size(),
//        toAdd.stream().map(DataType::getUniqueID).collect(Collectors.joining(", "))));

    try (var _ = resizeLock.lockWrite()) {
      // double-checked lock
      toAdd.removeIf(this::containsDataType);
      if (toAdd.isEmpty()) {
        return;
      }
//      logger.finest("%s: adding %d data types %s".formatted(modelName, toAdd.size(),
//          toAdd.stream().map(DataType::getUniqueID).collect(Collectors.joining(", "))));
      for (DataType dataType : toAdd) {
        columns.put(dataType, dataType.createDataColumn(storage, columnLength));
//        logger.finest("%s: adding data type %s at %d".formatted(modelName, dataType.getUniqueID(),
//            indexMap.getInt(dataType)));
      }
    }

    final List<DataType> addedCopy = List.copyOf(toAdd);
    for (var listener : dataTypesChangeListeners) {
      listener.onChange(addedCopy, List.of());
    }
  }

  protected DataColumn getColumn(final DataType type) {
    return columns.get(type);
  }

  public DataColumn addDataType(final DataType type) {
    DataColumn column = getColumn(type);
    if (column != null) {
      return column;
    }

//    logger.finest(modelName + ": index -1 for data type " + type.getUniqueID());

    try (var _ = resizeLock.lockWrite()) {
      // double-checked lock
      column = getColumn(type);
      if (column != null) {
        return column;
      }

      column = type.createDataColumn(storage, columnLength);
      columns.put(type, column);
//      logger.finest("%s: adding data type %s".formatted(modelName, type.getUniqueID()));
    }
    var addedCopy = List.of(type);
    for (var listener : dataTypesChangeListeners) {
      listener.onChange(addedCopy, List.of());
    }
    return column;
  }

  /**
   * @return read lock
   */
  CloseableResourceLock lockRead() {
    return resizeLock.lockRead();
  }

  public int addRowGetIndex() {
    final int index = nextRow.getAndIncrement();
    final int currentColumnLength = columnLength;
    if (index >= currentColumnLength) {
      resizeColumnsTo(currentColumnLength + sizeIncrement);
    }
    return index;
  }

  public void resizeColumnsTo(final int finalSize) {
    try (var _ = resizeLock.lockWrite()) {
      if (columnLength >= finalSize) {
        return;
      }
      // resize
      long success = columns.values().stream().parallel()
          .filter(column -> column.ensureCapacity(finalSize)).count();

//      logger.info("""
//          Resized %d of %d columns in model %s to %d rows""".formatted(success, columns.size(),
//          modelName, finalSize));

      columnLength = finalSize;
    }
  }

  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getValueChangeListeners() {
    return dataTypeValueChangedListeners;
  }

  public void addDataTypesChangeListener(DataTypesChangedListener listener) {
    dataTypesChangeListeners.add(listener);
  }

  public List<DataTypesChangedListener> getDataTypesChangeListeners() {
    return dataTypesChangeListeners;
  }

  public boolean isEmpty() {
    return columns.isEmpty();
  }

  public int getNumberOfTypes() {
    return columns.size();
  }

  // row operations
  public <T> boolean set(final ModularDataModel model, final int rowIndex, DataType<T> type,
      T value) {
    if (type instanceof MissingValueType) {
      throw new UnsupportedOperationException(
          STR."Type \{type.getClass()} is not meant to be added to a feature.");
    }

    DataColumn column = columns.get(type);
    if (column == null) {
      if (value == null) {
        // nothing to do if we dont have the entry and dont want to add some else than null
        return false;
      }
      // add column
      column = addDataType(type);
    }

    final Object old = column.get(rowIndex);
    column.set(rowIndex, value);

    if (!Objects.equals(old, value)) {
      List<DataTypeValueChangeListener<?>> listeners = getValueChangeListeners().get(type);
      if (!Objects.equals(old, value)) {
        if (listeners != null) {
          for (DataTypeValueChangeListener listener : listeners) {
            listener.valueChanged(model, type, old, value);
          }
        }
        return true;
      }
    }
    return false;
  }

  public <T> @Nullable T get(final int rowIndex, DataType<T> key) {
//    schema.lockRead() do we need to lock? don't think so
    var column = getColumn(key);
    if (column == null) {
      return null;
    }
    return (T) column.get(rowIndex);
  }

  public <T> @Nullable T getOrDefault(final int rowIndex, DataType<T> type,
      @Nullable T defaultValue) {
    final T value = get(rowIndex, type);
    return value == null ? defaultValue : value;
  }

  public <T> @NotNull T getNonNullElse(final int rowIndex, DataType<T> type,
      @NotNull T defaultValue) {
    return Objects.requireNonNullElse(get(rowIndex, type), defaultValue);
  }

  public <T> void remove(DataType<T> type) {
    var column = getColumn(type);
    if (column == null) {
      return;
    }

    try (var _ = resizeLock.lockWrite()) {
      columns.remove(type);
    }
  }

  public Set<DataType> getTypes() {
    return readOnlyColumns.keySet();
  }

  public Set<DataType> getTypesSnapshot() {
    return Set.copyOf(columns.keySet());
  }

  public Stream<DataType> streamColumns() {
    return getTypesSnapshot().stream();
  }

  public Stream<Entry<DataType, Object>> streamValues(int rowIndex) {
    return streamColumns().map(col -> new SimpleEntry<>(col, get(rowIndex, col)));
  }


}
