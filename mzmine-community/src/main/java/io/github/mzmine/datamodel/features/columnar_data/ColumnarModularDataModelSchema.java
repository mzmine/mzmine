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
import io.github.mzmine.datamodel.features.ModularDataModelArray;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.github.mzmine.util.concurrent.CloseableResourceLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class ColumnarModularDataModelSchema {

  private static final Logger logger = Logger.getLogger(
      ColumnarModularDataModelSchema.class.getName());

  private final MemoryMapStorage storage;
  private final HashMap<DataType, DataColumn> columns = new HashMap<>();

  /**
   * The current length of the columns
   */
  private int columnLength;
  private final AtomicInteger nextRow = new AtomicInteger(0);
  private final int sizeIncrement = 5000;

  /**
   * A lock that controls the access and specifically the resizing of all
   * {@link ModularDataModelArray}s controlled by this schema.
   */
  private final CloseableReentrantReadWriteLock resizeLock = new CloseableReentrantReadWriteLock();

  private final String modelName;

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

  public void addDataType(final DataType type) {
    if (containsDataType(type)) {
      return;
    }

//    logger.finest(modelName + ": index -1 for data type " + type.getUniqueID());

    try (var _ = resizeLock.lockWrite()) {
      // double-checked lock
      if (containsDataType(type)) {
        return;
      }

      columns.put(type, type.createDataColumn(storage, columnLength));
      logger.finest("%s: adding data type %s".formatted(modelName, type.getUniqueID()));
    }
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
          .filter(column -> column.resizeTo(finalSize)).count();

      logger.info("""
          Resized %d of %d columns in model %s""".formatted(success, columns.size(), modelName));

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
}
