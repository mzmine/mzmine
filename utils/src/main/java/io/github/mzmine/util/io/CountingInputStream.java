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

package io.github.mzmine.util.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;

/**
 * Counts the bytes read from the wrapped stream, so a task can report progress against the file
 * size without counting lines or entries up front.
 * <p>
 * The jdk has no counting stream, guava and commons-io both have one but guava is a dependency we
 * want to be able to drop and commons-io deprecated theirs in 2.18. This is the whole feature.
 * <p>
 * The count is safe to read from another thread than the one reading the stream, which parsers that
 * hand their lines to a parallel stream rely on.
 */
public class CountingInputStream extends FilterInputStream {

  private final AtomicLong count = new AtomicLong(0L);
  private long markedCount = 0L;

  public CountingInputStream(@NotNull final InputStream in) {
    super(in);
  }

  /**
   * @return the number of bytes read from the wrapped stream so far
   */
  public long getCount() {
    return count.get();
  }

  @Override
  public int read() throws IOException {
    final int read = in.read();
    if (read != -1) {
      count.incrementAndGet();
    }
    return read;
  }

  @Override
  public int read(final byte @NotNull [] b, final int off, final int len) throws IOException {
    final int read = in.read(b, off, len);
    if (read > 0) {
      count.addAndGet(read);
    }
    return read;
  }

  @Override
  public long skip(final long n) throws IOException {
    final long skipped = in.skip(n);
    if (skipped > 0) {
      count.addAndGet(skipped);
    }
    return skipped;
  }

  @Override
  public synchronized void mark(final int readlimit) {
    in.mark(readlimit);
    markedCount = count.get();
  }

  @Override
  public synchronized void reset() throws IOException {
    in.reset();
    count.set(markedCount);
  }
}
