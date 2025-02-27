package io.github.mzmine.datamodel.features.columnar_data.columns;

import java.util.concurrent.locks.StampedLock;

/**
 * This column synchronizes the resizing of the backing data structures and the write operations
 * optimistically.
 *
 * @param <T> data type of values
 */
public final class OptimisticallySynchronizedDataColumn<T> extends AbstractDataColumn<T> {

  private final StampedLock resizeLock = new StampedLock();
  private final AbstractDataColumn<T> delegate;

  public OptimisticallySynchronizedDataColumn(final AbstractDataColumn<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public T get(final int index) {
    return delegate.get(index);
  }

  @Override
  public T set(final int index, final T value) {
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
