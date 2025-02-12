package io.github.mzmine.datamodel.features.columnar_data;

import java.util.concurrent.locks.StampedLock;

public final class OptimisticallySynchronizedDataColumn<T> extends AbstractDataColumn<T> {

  private final StampedLock lock = new StampedLock();
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
    long stamp = lock.tryOptimisticRead();
    T old = delegate.set(index, value);
    if (!lock.validate(stamp)) {
      stamp = lock.readLock();
      try {
        // do not overwrite old because the old value should already be saved there
        // the new backing data structure may be updated during the first call
        delegate.set(index, value);
      } finally {
        lock.unlockRead(stamp);
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
    long stamp = lock.writeLock();
    try {
      return delegate.resizeTo(finalSize);
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  @Override
  public int capacity() {
    long stamp = lock.tryOptimisticRead();
    int capacity = delegate.capacity();
    if(!lock.validate(stamp)) {
      stamp = lock.readLock();
      try {
        capacity = delegate.capacity();
      } finally {
        lock.unlockRead(stamp);
      }
    }
    return capacity;
  }
}
