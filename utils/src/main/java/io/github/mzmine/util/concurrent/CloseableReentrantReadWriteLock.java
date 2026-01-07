/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.concurrent;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * {@link ReentrantReadWriteLock} is not autoclosable - therefore its wrapped with easy functions to
 * lock:
 * <p>
 * Use
 * <pre>
 * try(var autocloseable = taskLock.lockWrite())
 * </pre>
 */
public class CloseableReentrantReadWriteLock {

  private final ReentrantReadWriteLock internal = new ReentrantReadWriteLock();

  /**
   * Use
   * <pre>
   * try(var autocloseable = taskLock.lockRead())
   * </pre>
   *
   * @return an {@link AutoCloseable} once the ReadLock has been acquired
   */
  public CloseableResourceLock lockRead() {
    ReadLock readLock = internal.readLock();
    readLock.lock();
    return readLock::unlock;
  }

  /**
   * Use
   * <pre>
   * try(var autocloseable = taskLock.lockWrite())
   * </pre>
   *
   * @return an {@link AutoCloseable} once the WriteLock has been acquired.
   */
  public CloseableResourceLock lockWrite() {
    WriteLock writeLock = internal.writeLock();
    writeLock.lock();
    return writeLock::unlock;
  }

  public void withWriteLock(Runnable writeOperation) {
    try (var lock = lockWrite()) {
      writeOperation.run();
    }
  }
}