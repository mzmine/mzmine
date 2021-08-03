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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 * <p>
 * Used to load a {@link File File} onto a
 * {@link io.github.mzmine.datamodel.msdk.io.mzml.util.ByteBufferInputStream ByteBufferInputStream}
 * </p>
 */
public abstract class FileMemoryMapper {

  /**
   * <p>
   * Used to load a {@link File File} onto a
   * {@link io.github.mzmine.datamodel.msdk.io.mzml.util.ByteBufferInputStream ByteBufferInputStream} *
   * </p>
   *
   * @param file the {@link File File} to be mapped
   * @return a {@link io.github.mzmine.datamodel.msdk.io.mzml2.util.io.ByteBufferInputStream ByteBufferInputStream}
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
