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

package io.github.mzmine.datamodel.features.columnar_data.columns.mmap;

import io.github.mzmine.datamodel.features.types.abstr.EnumDataType;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Any enum value memory mapped
 *
 * @param <T>
 */
public class EnumTypeMemorySegmentColumn<T extends Enum<T> & UniqueIdSupplier> extends AbstractMemorySegmentColumn<T> {

  @NotNull
  private final Class<T> clazz;

  /**
   * @param enumType any enum data type
   */
  public EnumTypeMemorySegmentColumn(@NotNull final MemoryMapStorage storage, int initialCapacity,
      @NotNull EnumDataType<T> enumType) {
    this(storage, initialCapacity, enumType.getValueClass());
  }

  /**
   * @param clazz the value class for example of {@link EnumDataType}
   */
  public EnumTypeMemorySegmentColumn(@NotNull final MemoryMapStorage storage, int initialCapacity,
      @NotNull Class<T> clazz) {
    super(storage, initialCapacity);
    this.clazz = clazz;
  }

  public T[] values() {
    return clazz.getEnumConstants();
  }

  @Override
  protected @NotNull ValueLayout getValueLayout() {
    return ValueLayout.JAVA_INT;
  }

  @Override
  public @Nullable T get(final int index) {
    int value = data.getAtIndex(ValueLayout.JAVA_INT, index);
    if (value < 0) {
      return null;
    }
    return values()[value];
  }

  @Override
  public void set(final @NotNull MemorySegment data, final int index, final T value) {
    data.setAtIndex(ValueLayout.JAVA_INT, index, value == null ? -1 : value.ordinal());
  }

}
