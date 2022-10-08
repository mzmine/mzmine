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

package io.github.mzmine.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class StreamCopy {

  protected long copiedLength, totalLength;
  protected boolean canceled = false, finished = false;

  /**
   * Copy the data from inputStream to outputStream using nio channels
   *
   * @param input InputStream
   * @param output OutputStream
   */
  public void copy(InputStream input, OutputStream output) throws IOException {
    this.copy(input, output, 0);
  }

  /**
   * Copy the data from inputStream to outputStream using nio channels
   *
   * @param input InputStream
   * @param output OutputStream
   */
  public void copy(InputStream input, OutputStream output, long totalLength) throws IOException {

    this.totalLength = totalLength;

    ReadableByteChannel in = Channels.newChannel(input);
    WritableByteChannel out = Channels.newChannel(output);

    // Allocate 1MB buffer
    ByteBuffer bbuffer = ByteBuffer.allocate(1 << 20);

    int len = 0;

    while ((len = in.read(bbuffer)) != -1) {

      if (canceled)
        return;

      bbuffer.flip();
      out.write(bbuffer);
      bbuffer.clear();
      copiedLength += len;
    }

    finished = true;

  }

  /**
   *
   * @return the progress of the "copy()" function copying the data from one stream to another
   */
  public double getProgress() {
    if (finished)
      return 1.0;
    if (totalLength == 0)
      return 0;
    if (copiedLength >= totalLength)
      return 1.0;
    return copiedLength / (double)totalLength;
  }

  /**
   * Cancel the copying
   */
  public void cancel() {
    canceled = true;
  }

  /**
   * Checks if copying is finished
   */
  public boolean finished() {
    return finished;
  }
}
