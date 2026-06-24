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

import java.util.concurrent.locks.StampedLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This column synchronizes the resizing of the backing data structures and the write operations
 * optimistically.
 *
 * @param <T> data type of values
 */
public final class OptimisticallySynchronizedDataColumn<T> extends AbstractDataColumn<T> {

  /**
   * actually use read lock to set values write lock is only used when the backing data array is
   * changed optimistic read will fail here only if backing data array was changed for a new one
   */
  private final StampedLock resizeLock = new StampedLock();
  @NotNull
  private final AbstractDataColumn<T> delegate;

  public OptimisticallySynchronizedDataColumn(@NotNull final AbstractDataColumn<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public @Nullable T get(final int index) {
    return delegate.get(index);
  }

  @Override
  public @Nullable T set(final int index, final @Nullable T value) {
    // actually use read lock to set values
    // write lock is only used when the backing data array is changed
    // optimistic read will fail here only if backing data array was changed for a new one
    long stamp = resizeLock.tryOptimisticRead();
    T old = delegate.set(index, value);
    if (!resizeLock.validate(stamp)) {
      // was currently resized - so wait for finish and then write the value to the new data structure
      stamp = resizeLock.readLock();
      try {
        // do not overwrite old because the old value should already be saved there
        // the new backing data structure may be updated during the first call
        delegate.set(index, value);
      } finally {
        resizeLock.unlockRead(stamp);
      }
    }
    return old;
  }

  @Override
  public boolean ensureCapacity(final int requiredCapacity) {
    if (requiredCapacity > capacity()) {
      return resizeTo(requiredCapacity);
    }
    return false;
  }

  /**
   * Resize with write lock
   */
  protected boolean resizeTo(final int finalSize) {
    long stamp = resizeLock.writeLock();
    try {
      // check size again - might have been changed already
      if (delegate.capacity() < finalSize) {
        return delegate.resizeTo(finalSize);
      } else {
        return false;
      }
    } finally {
      resizeLock.unlockWrite(stamp);
    }
  }

  @Override
  public int capacity() {
    long stamp = resizeLock.tryOptimisticRead();
    int capacity = delegate.capacity();
    if (!resizeLock.validate(stamp)) {
      stamp = resizeLock.readLock();
      try {
        capacity = delegate.capacity();
      } finally {
        resizeLock.unlockRead(stamp);
      }
    }
    return capacity;
  }
}
