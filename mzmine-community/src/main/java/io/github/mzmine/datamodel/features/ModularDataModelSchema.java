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

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.github.mzmine.util.concurrent.CloseableResourceLock;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class ModularDataModelSchema {

  private static final Logger logger = Logger.getLogger(ModularDataModelSchema.class.getName());

  private final Object2IntMap<DataType> indexMap = new Object2IntOpenHashMap<>();
  private final Object2IntMap<DataType> readOnlyIndexMap = Object2IntMaps.unmodifiable(indexMap);

  private final AtomicInteger nextIndex = new AtomicInteger(0);
  /**
   * The current size of all arrays controlled by this datamodel.
   */
  private final AtomicInteger arrayInitialisationSize;
  private final int sizeIncrement = 5;

  /**
   * A lock that controls the access and specifically the resizing of all
   * {@link ModularDataModelArray}s controlled by this schema.
   */
  private final CloseableReentrantReadWriteLock resizeLock = new CloseableReentrantReadWriteLock();

  private final Supplier<@NotNull Stream<? extends ModularDataModelArray>> modelSupplier;
  private final String modelName;

  private final Map<DataType<?>, List<DataTypeValueChangeListener<?>>> dataTypeValueChangedListeners = new HashMap<>();
  private final List<DataTypesChangedListener> dataTypesChangeListeners = new ArrayList<>();

  public ModularDataModelSchema(
      final @NotNull Supplier<Stream<? extends ModularDataModelArray>> modelSupplier,
      String modelName, int initialSize) {
    this.modelSupplier = modelSupplier;
    this.modelName = modelName;
    indexMap.defaultReturnValue(-1);
    arrayInitialisationSize = new AtomicInteger(initialSize);
  }

  /**
   * Gets the index of the type. if the type does not have an index yet,
   *
   * @param type        the data type
   * @param addIfAbsent if the type shall be added automatically if it is not contained yet.
   * @return the index of the data type or -1 if the type is not in this schema yet.
   */
  public int getIndex(final DataType type, final boolean addIfAbsent) {
//    logger.finest(modelName + " Trying to lock for read");
//    try (var _ = resizeLock.lockRead()) {
//      logger.finest(modelName + " Read lock acquired");
    final int index = indexMap.getInt(type);
    if (index == -1 && addIfAbsent) {
      return addDataType(type);
    } else {
      return index;
    }
//    }
  }

  public boolean containsDataType(final DataType type) {
//    logger.finest(modelName + " Trying to lock for read");
//    try (var _ = resizeLock.lockRead()) {
//      logger.finest(modelName + " Read lock acquired");
    return indexMap.getInt(type) != -1;
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
      int currentFreeCap = getCurrentFreeCapacity();
      if (toAdd.size() > currentFreeCap) {
        // increase size to add all new types
        resizeDataModels(Math.max(sizeIncrement, toAdd.size() - currentFreeCap));
      }
      for (DataType dataType : toAdd) {
        indexMap.put(dataType, nextIndex.getAndIncrement());
//        logger.finest("%s: adding data type %s at %d".formatted(modelName, dataType.getUniqueID(),
//            indexMap.getInt(dataType)));
      }
    }

    final List<DataType> addedCopy = List.copyOf(toAdd);
    for (var listener : dataTypesChangeListeners) {
      listener.onChange(addedCopy, List.of());
    }
  }

  private int getCurrentFreeCapacity() {
//    logger.finest(modelName + " Trying to lock for read");
//    try (var _ = resizeLock.lockRead()) {
//      logger.finest(modelName + " Read lock acquired");
    return arrayInitialisationSize.get() - nextIndex.get();
//    }
  }

  public int addDataType(final DataType type) {
    int currentIndex = getIndex(type, false);
    if (currentIndex != -1) {
      return currentIndex;
    }

//    logger.finest(modelName + ": index -1 for data type " + type.getUniqueID());

    try (var _ = resizeLock.lockWrite()) {
      // double-checked lock
      currentIndex = getIndex(type, false);
      if (currentIndex != -1) {
        return currentIndex;
      }

      final int next = nextIndex.get();
      if (next >= arrayInitialisationSize.get()) {
        resizeDataModels(sizeIncrement);
      }
      indexMap.put(type, nextIndex.getAndIncrement());
//      logger.finest("%s: adding data type %s at %d".formatted(modelName, type.getUniqueID(),
//          indexMap.getInt(type)));
      return next;
    }
  }

  /**
   * @return read lock
   */
  CloseableResourceLock lockRead() {
    return resizeLock.lockRead();
  }

  /**
   * Resize all array to the new schema
   *
   * @param resizeBy number of columns to add
   */
  private void resizeDataModels(final int resizeBy) {
    final Instant start = Instant.now();
    try (var _ = resizeLock.lockWrite()) {
      final int newSize = arrayInitialisationSize.addAndGet(resizeBy);
      // use map in parallel stream as forEach
      long updated = modelSupplier.get().parallel().filter(model -> model.ensureCapacity(newSize))
          .count();
      logger.fine("Updated %d data models to a new size of %d types.".formatted(updated, newSize));
    }
    final Instant end = Instant.now();
    logger.finest(
        "Resizing %s data models from %d to %d took %s ns.".formatted(modelSupplier.get().count(),
            arrayInitialisationSize.get() - resizeBy, arrayInitialisationSize.get(),
            Duration.between(start, end).toNanos()));
  }

  public Object2IntMap<DataType> getReadOnlyTypes() {
//    logger.finest(modelName + " Trying to lock for read");
//    try (var _ = resizeLock.lockRead()) {
//      logger.finest(modelName + " Read lock acquired");
    return readOnlyIndexMap;
//    }
  }

  public Object[] allocateNew() {
//    logger.finest(modelName + " Trying to lock for read");
//    try (var _ = resizeLock.lockRead()) {
//      logger.finest(modelName + " Read lock acquired");
    return new Object[arrayInitialisationSize.get()];
//    }
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
