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

package io.github.mzmine.datamodel.features.columnar_data.columns;

import io.github.mzmine.datamodel.features.columnar_data.columns.arrays.NullableDoubleArrayColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.arrays.NullableFloatArrayColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.arrays.NullableIntArrayColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.arrays.ObjectArrayColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.mmap.AlignmentScoreMemorySegmentColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.mmap.DoubleRangeMemorySegmentColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.mmap.EnumTypeMemorySegmentColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.mmap.FloatRangeMemorySegmentColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.mmap.NullableDoubleMemorySegmentColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.mmap.NullableFloatMemorySegmentColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.mmap.NullableIntMemorySegmentColumn;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.abstr.EnumDataType;
import io.github.mzmine.datamodel.features.types.alignment.AlignmentMainType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.DoubleRangeType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.DoubleType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatRangeType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import io.github.mzmine.util.MemoryMapStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class to create {@link DataColumn}s for a
 * {@link io.github.mzmine.datamodel.features.columnar_data.ColumnarModularDataModelSchema}. It is
 * strongly recommended to use synchronized columns if the data model is written
 * into/modified/resized after its creation.
 */
public class DataColumns {

  public static @NotNull NullableDoubleDataColumn ofDouble(@Nullable MemoryMapStorage storage,
      int size) {
    return storage == null ? new NullableDoubleArrayColumn(size)
        : new NullableDoubleMemorySegmentColumn(storage, size);
  }

  public static @NotNull NullableFloatDataColumn ofFloat(@Nullable MemoryMapStorage storage,
      int size) {
    return storage == null ? new NullableFloatArrayColumn(size)
        : new NullableFloatMemorySegmentColumn(storage, size);
  }

  public static @NotNull NullableIntDataColumn ofInt(@Nullable MemoryMapStorage storage, int size) {
    return storage == null ? new NullableIntArrayColumn(size)
        : new NullableIntMemorySegmentColumn(storage, size);
  }

  @SuppressWarnings("unchecked")
  public static @NotNull <T> DataColumn<T> ofTypeSynchronized(DataType<T> type,
      @Nullable MemoryMapStorage storage, int size) {
    return new OptimisticallySynchronizedDataColumn<>(ofType(type, storage, size));
  }

  public static @NotNull <T> DataColumn<T> ofSynchronized(AbstractDataColumn<T> column) {
    return new OptimisticallySynchronizedDataColumn<>(column);
  }

  @SuppressWarnings("unchecked")
  public static @NotNull <T> AbstractDataColumn<T> ofType(DataType<T> type,
      @Nullable MemoryMapStorage storage, int size) {
    if (storage == null) {
      return inMemory(type, size);
    }
    return (AbstractDataColumn<T>) switch (type) {
      case EnumDataType<?> et -> new EnumTypeMemorySegmentColumn<>(storage, size, et);
      case IntegerType _ -> ofInt(storage, size);
      case DoubleType _ -> ofDouble(storage, size);
      case FloatType _ -> ofFloat(storage, size);
      case FloatRangeType _ -> new FloatRangeMemorySegmentColumn(storage, size);
      case DoubleRangeType _ -> new DoubleRangeMemorySegmentColumn(storage, size);
      case AlignmentMainType _ -> new AlignmentScoreMemorySegmentColumn(storage, size);
      default -> new ObjectArrayColumn<>(size);
    };
  }

  /**
   * Create column always in memory
   *
   * @param type type of column
   * @param size number of elements
   */
  @SuppressWarnings("unchecked")
  public static @NotNull <T> AbstractDataColumn<T> inMemory(final DataType<T> type,
      final int size) {
    return (AbstractDataColumn<T>) switch (type) {
      case IntegerType _ -> ofInt(null, size);
      case DoubleType _ -> ofDouble(null, size);
      case FloatType _ -> ofFloat(null, size);
      default -> new ObjectArrayColumn<>(size);
    };
  }

}
