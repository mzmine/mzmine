/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;


/**
 * A bridge between byte {@linkplain ByteBuffer buffers} and {@linkplain InputStream input streams}.
 *
 * <p>
 * Java's {@linkplain FileChannel#map(MapMode, long, long) memory-mapping facilities} have the
 * severe limitation of mapping at most {@link Integer#MAX_VALUE} bytes, as they expose the content
 * of a file using a {@link MappedByteBuffer}. This class can
 * {@linkplain #map(FileChannel, MapMode) expose a file of arbitrary length} as a
 * {@linkplain RepositionableStream repositionable} {@link MeasurableInputStream} that is actually
 * based on an array of {@link MappedByteBuffer}s, each mapping a <em>chunk</em> of
 * {@link #CHUNK_SIZE} bytes.
 *
 * @author Sebastiano Vigna
 * @since 1.2
 */
public class ByteBufferInputStream extends InputStream {
  private static int CHUNK_SHIFT = 30;

  /** The size of a chunk created by {@link #map(FileChannel, MapMode)}. */
  public static final long CHUNK_SIZE = 1L << CHUNK_SHIFT;

  /** The underlying byte buffers. */
  private final ByteBuffer[] byteBuffer;

  /**
   * An array parallel to {@link #byteBuffer} specifying which buffers do not need to be
   * {@linkplain ByteBuffer#duplicate() duplicated} before being used.
   */
  private final boolean[] readyToUse;

  /** The number of byte buffers. */
  private final int n;

  /** The current buffer. */
  private int curr;

  /** The current mark as a position, or -1 if there is no mark. */
  private long mark;

  /** The overall size of this input stream. */
  private final long size;

  /** The capacity of the last buffer. */
  private final int lastBufferCapacity;

  /** Number of bytes to be read before forcefully returning -1 */
  private long remainingBytes;

  /**
   * Creates a new byte-buffer input stream from a single {@link ByteBuffer}.
   *
   * @param byteBuffer the underlying byte buffer.
   */
  public ByteBufferInputStream(final ByteBuffer byteBuffer) {
    this(new ByteBuffer[] {byteBuffer}, byteBuffer.capacity(), 0, new boolean[1]);
  }

  /**
   * Creates a new byte-buffer input stream.
   *
   * @param byteBuffer the underlying byte buffers.
   * @param size the sum of the {@linkplain ByteBuffer#capacity() capacities} of the byte buffers.
   * @param curr the current buffer (reading will start at this buffer from its current position).
   * @param readyToUse an array parallel to <code>byteBuffer</code> specifying which buffers do not
   *        need to be {@linkplain ByteBuffer#duplicate() duplicated} before being used (the process
   *        will happen lazily); the array will be used internally by the newly created byte-buffer
   *        input stream.
   */
  protected ByteBufferInputStream(final ByteBuffer[] byteBuffer, final long size, final int curr,
      final boolean[] readyToUse) {
    this.byteBuffer = byteBuffer;
    this.n = byteBuffer.length;
    this.curr = curr;
    this.size = size;
    this.readyToUse = readyToUse;

    mark = -1;

    for (int i = 0; i < n; i++)
      if (i < n - 1 && byteBuffer[i].capacity() != CHUNK_SIZE)
        throw new IllegalArgumentException();
    lastBufferCapacity = byteBuffer[n - 1].capacity();
  }

  /**
   * Creates a new byte-buffer input stream by mapping a given file channel.
   *
   * @param fileChannel the file channel that will be mapped.
   * @param mapMode this must be {@link MapMode#READ_ONLY}.
   * @return a new byte-buffer input stream over the contents of <code>fileChannel</code>.
   * @throws IOException if any.
   */
  public static ByteBufferInputStream map(final FileChannel fileChannel, final MapMode mapMode)
      throws IOException {
    final long size = fileChannel.size();
    final int chunks = (int) ((size + (CHUNK_SIZE - 1)) / CHUNK_SIZE);
    final ByteBuffer[] byteBuffer = new ByteBuffer[chunks];
    for (int i = 0; i < chunks; i++)
      byteBuffer[i] =
          fileChannel.map(mapMode, i * CHUNK_SIZE, Math.min(CHUNK_SIZE, size - i * CHUNK_SIZE));
    byteBuffer[0].position(0);
    final boolean[] readyToUse = new boolean[chunks];
    Arrays.fill(readyToUse, true);
    return new ByteBufferInputStream(byteBuffer, size, 0, readyToUse);
  }

  private ByteBuffer byteBuffer(final int n) {
    if (readyToUse[n])
      return byteBuffer[n];
    readyToUse[n] = true;
    return byteBuffer[n] = byteBuffer[n].duplicate();
  }

  private long remaining() {
    return curr == n - 1 ? byteBuffer(curr).remaining()
        : byteBuffer(curr).remaining() + ((long) (n - 2 - curr) << CHUNK_SHIFT)
            + lastBufferCapacity;
  }

  /**
   * <p>available.</p>
   *
   * @return a int.
   */
  public int available() {
    final long available = remaining();
    return available <= Integer.MAX_VALUE ? (int) available : Integer.MAX_VALUE;
  }

  /** {@inheritDoc} */
  @Override
  public boolean markSupported() {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void mark(final int unused) {
    mark = position();
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void reset() throws IOException {
    if (mark == -1)
      throw new IOException();
    position(mark);
  }

  /** {@inheritDoc} */
  @Override
  public long skip(final long n) throws IOException {
    final long toSkip = Math.min(remaining(), n);
    position(position() + toSkip);
    return toSkip;
  }

  private int readBuffer() {
    if (!byteBuffer(curr).hasRemaining()) {
      if (curr < n - 1)
        byteBuffer(++curr).position(0);
      else
        return -1;
    }

    return byteBuffer[curr].get() & 0xFF;
  }

  /** {@inheritDoc} */
  @Override
  public int read() {
    return (remainingBytes-- <= 0 ? -1 : readBuffer());
  }

  /** {@inheritDoc} */
  public int read(final byte[] b, final int offset, final int length) {
    if (length == 0)
      return 0;
    final long remaining = remaining();
    if (remaining == 0)
      return -1;
    final int realLength = (int) Math.min(remaining, length);
    int read = 0;
    while (read < realLength) {
      int rem = byteBuffer(curr).remaining();
      if (rem == 0)
        byteBuffer(++curr).position(0);
      byteBuffer[curr].get(b, offset + read, Math.min(realLength - read, rem));
      read += Math.min(realLength, rem);
    }
    return realLength;
  }

  /**
   * <p>length.</p>
   *
   * @return a long.
   */
  public long length() {
    return size;
  }

  /**
   * <p>position.</p>
   *
   * @return a long.
   */
  public long position() {
    return ((long) curr << CHUNK_SHIFT) + byteBuffer(curr).position();
  }

  /**
   * <p>position.</p>
   *
   * @param newPosition a long.
   */
  public void position(long newPosition) {
    newPosition = Math.min(newPosition, length());
    if (newPosition == length()) {
      final ByteBuffer buffer = byteBuffer(curr = n - 1);
      buffer.position(buffer.capacity());
      return;
    }

    curr = (int) (newPosition >>> CHUNK_SHIFT);
    byteBuffer(curr).position((int) (newPosition - ((long) curr << CHUNK_SHIFT)));
  }

  /**
   * <p>copy.</p>
   *
   * @return a {@link ByteBufferInputStream} object.
   */
  public ByteBufferInputStream copy() {
    return new ByteBufferInputStream(byteBuffer.clone(), size, curr, new boolean[n]);
  }

  /**
   * <p>constrain.</p>
   *
   * @param position a long.
   * @param remainingBytes a long.
   */
  public void constrain(long position, long remainingBytes) {
    this.position(position);
    this.remainingBytes = remainingBytes;
  }

}
