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
import io.github.mzmine.datamodel.features.columnar_data.columns.DataColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.DataColumns;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.github.mzmine.util.concurrent.CloseableResourceLock;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The data schema for a columnar data model. Columns will define how to store, read and write data.
 * Columns are already either of the correct length or larger to accommodate new row additions. If
 * another row is added, columns size is ensured and potentially resized.
 * <p>
 * Use {@link #addRowGetIndex()} as a non-blocking way to add a new row to this data model. Resizing
 * is done automatically with optimistic {@link StampedLock}.
 */
public class ColumnarModularDataModelSchema {

  private static final Logger logger = Logger.getLogger(
      ColumnarModularDataModelSchema.class.getName());

  /**
   * Only present if memory mapping active
   */
  @Nullable
  protected final MemoryMapStorage storage;

  /**
   * Each data type has its own DataColumn usually created in the factory {@link DataColumns}.
   */
  protected final Map<DataType, DataColumn> columns = new ConcurrentHashMap<>(20);
  /**
   * A lock that controls specifically the resizing of all {@link DataColumn}s controlled by this
   * schema. If more rows are added than the size allows, a write lock will block the creation of
   * new columns until resizing is finished.
   */
  protected final CloseableReentrantReadWriteLock resizeLock = new CloseableReentrantReadWriteLock();
  protected final String modelName;
  private final Map<DataType, DataColumn> readOnlyColumns = Collections.unmodifiableMap(columns);
  private final AtomicInteger nextRow = new AtomicInteger(0);
  private final @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> dataTypeValueChangedListeners = new ConcurrentHashMap<>();
  private final @NotNull List<DataTypesChangedListener> dataTypesChangeListeners = new CopyOnWriteArrayList<>();
  /**
   * The current length of the columns. This value should only change withing a
   * resizeLock.writeLock
   */
  protected volatile int columnLength;

  public ColumnarModularDataModelSchema(final @Nullable MemoryMapStorage storage, String modelName,
      int initialSize) {
    this.storage = storage;
    this.modelName = modelName;
    columnLength = initialSize;
  }

  public boolean containsDataType(@NotNull final DataType type) {
    return columns.containsKey(type);
  }

  public void addDataTypes(final DataType... types) {
    List<DataType> toAdd = new ArrayList<>(types.length);
    for (DataType type : types) {
      if (!containsDataType(type)) {
        toAdd.add(type);
      }
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
        // for now use synchronized DataColumns
        columns.put(dataType, DataColumns.ofTypeSynchronized(dataType, storage, columnLength));
//        logger.finest("%s: adding data type %s at %d".formatted(modelName, dataType.getUniqueID(),
//            indexMap.getInt(dataType)));
      }
    }

    final List<DataType> addedCopy = List.copyOf(toAdd);
    for (var listener : dataTypesChangeListeners) {
      listener.onChange(addedCopy, List.of());
    }
  }

  protected <T> DataColumn<T> getColumn(@NotNull final DataType<T> type) {
    return columns.get(type);
  }

  /**
   * @return read lock
   */
  CloseableResourceLock lockRead() {
    return resizeLock.lockRead();
  }

  /**
   * Fast method to obtain the new index. Only blocks during resizing of columns.
   *
   * @return the next row index
   */
  public int addRowGetIndex() {
    final int index = nextRow.getAndIncrement();
    if (index < 0) {
      // overflow detected
      // trap forever in negative
      nextRow.set(Integer.MIN_VALUE);
      throw new IndexOutOfBoundsException(
          "Index out of bounds. Data model has reached its maximum number of rows. This may point to too much noise being detected. Revise parameters.");
    }

    final int currentColumnLength = columnLength;
    if (index >= currentColumnLength) {
      // double size of columns
      // apply some minimum and maximum resize

      // avoid int overflow
      final int newSize = MathUtils.capMaxInt(
          (long) currentColumnLength + MathUtils.withinBounds((int) (currentColumnLength * 1d), 10,
              250000));
      resizeColumnsTo(newSize);
    }
    return index;
  }

  void resizeColumnsTo(final int finalSize) {
    try (var _ = resizeLock.lockWrite()) {
      if (columnLength >= finalSize) {
        return;
      }
      // resize
      // uses count instead of forEach as forEach in parallel might not block the carrier thread
      long success = columns.values().stream().parallel()
          .filter(column -> column.ensureCapacity(finalSize)).count();

      logger.finest("""
          Resized %d of %d columns in model %s to %d rows""".formatted(success, columns.size(),
          modelName, finalSize));

      columnLength = finalSize;
    }
  }

  /**
   * @return A map of listeners for values of specific data types.
   */
  public @NotNull Map<DataType<?>, List<DataTypeValueChangeListener<?>>> getValueChangeListeners() {
    return dataTypeValueChangedListeners;
  }

  public void addDataTypeValueChangeListener(@NotNull DataType type,
      @NotNull final DataTypeValueChangeListener<?> listener) {
    dataTypeValueChangedListeners.compute(type, (key, list) -> {
      if (list == null) {
        list = new ArrayList<>();
      }
      list.add(listener);
      return list;
    });
  }

  public void removeDataTypeValueChangeListener(@NotNull DataType type, @Nullable final DataTypeValueChangeListener<?> listener) {
    dataTypeValueChangedListeners.compute(type, (key, list) -> {
      if (list == null || list.isEmpty()) {
        return null;
      }
      list.remove(listener);
      return list.isEmpty() ? null : list;
    });
  }

  /**
   * @param listener The listener to add. null will be ignored. Will react to added or removed data
   *                 types, not edited values.
   */
  public void addDataTypesChangeListener(@Nullable DataTypesChangedListener listener) {
    if (listener == null) {
      return;
    }
    dataTypesChangeListeners.add(listener);
  }

  /**
   * @return All current data types listeners.
   */
  public @NotNull List<DataTypesChangedListener> getDataTypesChangeListeners() {
    return dataTypesChangeListeners;
  }

  /**
   * @param listener The listener to remove. Null will be ignored.
   */
  public void removeDataTypesChangeListener(@Nullable DataTypesChangedListener listener) {
    if (listener == null) {
      return;
    }
    dataTypesChangeListeners.remove(listener);
  }

  public boolean isEmpty() {
    return columns.isEmpty();
  }

  public int getNumberOfTypes() {
    return columns.size();
  }

  /**
   * Setting the value of a model in this schema.
   *
   * @param model    the model of this row - used in listeners
   * @param rowIndex the rowIndex of model
   * @param type     the type for the column
   * @param value    the value to set
   * @param <T>      value type
   * @return true if the value was updated, false otherwise.
   */
  public <T> boolean set(final ModularDataModel model, final int rowIndex, DataType<T> type,
      T value) {
    if (type instanceof MissingValueType) {
      throw new UnsupportedOperationException(
          "Type %s is not meant to be added to a feature.".formatted(type.getClass()));
    }

    DataColumn column = columns.get(type);
    if (column == null) {
      if (value == null) {
        // nothing to do if we dont have the entry and dont want to add some else than null
        return false;
      }
      // add column
      addDataTypes(type);
      column = getColumn(type);
    }

    /*
     * using a lock inside each column because they might be resized at different points in time,
     * because memory mapped columns usually make greater resizing increments
     * as it is more expensive compared to in memory array columns.
     * Also they are resized on different treads
     */
    final Object old = column.get(rowIndex);
    column.set(rowIndex, value);

    if (!Objects.equals(old, value)) {
      List<DataTypeValueChangeListener<?>> listeners = getValueChangeListeners().get(type);
      if (listeners != null) {
        for (DataTypeValueChangeListener listener : listeners) {
          listener.valueChanged(model, type, old, value);
        }
      }
      return true;
    }
    return false;
  }

  /**
   * This method is lock-free. The reason is that when adding rows to this model triggers a resize
   * to the backing data model in DataColumn, either the new or old backing array/MemorySegment
   * contains the same values.
   *
   * @return value of row with key
   */
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

  /**
   * Remove the column
   */
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

  /**
   * @return a stream of all values even those that are null
   */
  public Stream<Entry<DataType, Object>> streamValues(int rowIndex) {
    return streamColumns().map(col -> new SimpleEntry<>(col, get(rowIndex, col)));
  }


}
