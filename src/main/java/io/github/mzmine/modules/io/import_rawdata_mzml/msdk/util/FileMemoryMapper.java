/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 * <p>
 * Used to load a {@link File File} onto a
 * {@link io.github.msdk.io.mzml.util.ByteBufferInputStream ByteBufferInputStream}
 * </p>
 */
public abstract class FileMemoryMapper {

  /**
   * <p>
   * Used to load a {@link File File} onto a
   * {@link io.github.msdk.io.mzml.util.ByteBufferInputStream ByteBufferInputStream} *
   * </p>
   *
   * @param file the {@link File File} to be mapped
   * @return a {@link io.github.msdk.io.mzml2.util.io.ByteBufferInputStream ByteBufferInputStream}
   * @throws IOException if any
   */
  public static ByteBufferInputStream mapToMemory(File file) throws IOException {

    RandomAccessFile aFile = new RandomAccessFile(file, "r");
    FileChannel inChannel = aFile.getChannel();
    ByteBufferInputStream is = ByteBufferInputStream
        .map(inChannel, FileChannel.MapMode.READ_ONLY);
    aFile.close();

    return is;
  }
}
