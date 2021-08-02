/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
