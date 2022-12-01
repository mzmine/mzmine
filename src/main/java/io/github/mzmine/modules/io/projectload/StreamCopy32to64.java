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

package io.github.mzmine.modules.io.projectload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import io.github.mzmine.util.StreamCopy;

public class StreamCopy32to64 extends StreamCopy {

  /**
   * Copy the data from inputStream to outputStream while converting float values to doubles. Used
   * to open old MZmine projects that saved the data points using floats.
   *
   * @param input InputStream
   * @param output OutputStream
   */
  @Override
  public void copy(InputStream input, OutputStream output, long totalLength) throws IOException {

    this.totalLength = totalLength;

    ReadableByteChannel in = Channels.newChannel(input);
    WritableByteChannel out = Channels.newChannel(output);

    // Allocate buffers
    ByteBuffer floatByteBuffer = ByteBuffer.allocate(Float.BYTES * 2000000);
    ByteBuffer doubleByteBuffer = ByteBuffer.allocate(Double.BYTES * 2000000);

    int len = 0;

    while ((len = in.read(floatByteBuffer)) != -1) {

      if (canceled)
        return;

      // Convert the float buffer to the double buffer
      floatByteBuffer.flip();

      while (floatByteBuffer.hasRemaining()) {
        float f = floatByteBuffer.getFloat();
        double d = f;
        doubleByteBuffer.putDouble(d);
      }

      // Write the output buffer
      doubleByteBuffer.flip();
      out.write(doubleByteBuffer);

      // Clear both buffers
      floatByteBuffer.clear();
      doubleByteBuffer.clear();

      copiedLength += len;
    }

    finished = true;


  }

}
