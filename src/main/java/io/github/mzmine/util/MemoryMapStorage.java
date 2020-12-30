/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.DoubleBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * This class defines a memory-mapped temporary file storage for large double[] arrays. The
 * storeData() function stores a double[] array into the underlying file and returns a DoubleBuffer.
 * The buffer is directly bound to the memory-mapped portion of the file so the data can be directly
 * accessed without loading it into another intermediate double[] array.
 *
 * The size of each temporary file is STORAGE_FILE_CAPACITY doubles (e.g., 1 GB for 125 million
 * doubles). When the file is full, a new file is automatically created using the
 * createNewMappedFile() function. The size of each temporary file in the filesystem may show as
 * 1GB, but actually only a portion of that is occupied on the disk, depending on the amount of
 * stored data (can be examined using the 'du -hs' unix command.
 *
 * There is no support for removing data from the file - we assume that such operation is rare and
 * therefore the data can be left on the disk until the whole file is discarded.
 *
 * There is a limit on the number of open file descriptors (e.g. 1024 by default on Linux). With 1
 * GB per temporary file, this would give us about 1 TB of storage space, so perhaps it is okay.
 * There is no way to remove the memory-mapped file, it is removed automatically when the
 * MappedByteBuffer is garbage-collected.
 *
 * The total amount of space is also limited by the amount of addressable virtual memory (e.g.,
 * 128TB on Linux). For this reason, this approach requires a 64-bit system - the limit would be
 * only 2GB on a 32-bit system.
 *
 *
 */
public class MemoryMapStorage {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * One file can store STORAGE_FILE_CAPACITY doubles, each double takes 8 bytes. We need to fit
   * within 2GB limit for a single MappedByteBuffer. 1 GB per file seems like a good start.
   */
  private static final long STORAGE_FILE_CAPACITY = 125_000_000L;

  /**
   * The file that we are currently writing into.
   */
  private DoubleBuffer currentMappedFile = null;

  /**
   * Creates a new temporary file, maps it into memory, and returns the corresponding DoubleBuffer.
   * The capacity of the buffer is STORAGE_FILE_CAPACITY doubles.
   *
   * @return a DoubleBuffer corresponding to the memory-mapped temporary file
   * @throws IOException
   */
  private DoubleBuffer createNewMappedFile() throws IOException {

    // Create the temporary storage file
    File storageFileName = File.createTempFile("mzmine", ".tmp");
    logger.finest("Created a temporary file " + storageFileName);

    // Open the file for writing
    RandomAccessFile storageFile = new RandomAccessFile(storageFileName, "rw");

    // Map the file into memory
    MappedByteBuffer mappedFileBuffer = storageFile.getChannel().map(FileChannel.MapMode.READ_WRITE,
        0, STORAGE_FILE_CAPACITY * Double.BYTES);

    // Create a double view of the memory-mapped byte buffer
    DoubleBuffer mappedDoubleBuffer = mappedFileBuffer.asDoubleBuffer();

    // Close the tepomrary file, the memory mapping will remain.
    storageFile.close();

    // Unfortunately, deleteOnExit() doesn't work on Windows, see JDK
    // bug #4171239. We will try to remove the temporary files in a
    // shutdown hook registered in the main.ShutDownHook class.
    storageFileName.deleteOnExit();

    return mappedDoubleBuffer;

  }

  /**
   * Store the given double[] array in a memory-mapped temporary file and return a read-only
   * DoubleBuffer that can access the data.
   *
   * @param data the double[] array with the data
   * @return a read-only DoubleBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */
  public synchronized @Nonnull DoubleBuffer storeData(@Nonnull final double data[])
      throws IOException {
    return storeData(data, 0, data.length);
  }

  /**
   * Store the given double[] array in a memory-mapped temporary file and return a read-only
   * DoubleBuffer that can access the data.
   *
   * @param data the double[] array with the data
   * @param offset offset of the stored portion of the data[] array
   * @param length size of the stored portion of the data[] array
   * @return a read-only DoubleBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */
  public synchronized @Nonnull DoubleBuffer storeData(@Nonnull final double data[], int offset,
      int length) throws IOException {

    // If we have no storage file or if the current file is full, create a new one
    if ((currentMappedFile == null)
        || (currentMappedFile.position() + length > STORAGE_FILE_CAPACITY)) {
      currentMappedFile = createNewMappedFile();
    }

    // Save the current position in the storage file
    final int savedPosition = currentMappedFile.position();

    // Copy the data to the memory mapped storage
    currentMappedFile.put(data, offset, length);

    // Move the position to the beginning of the array we just added, set the limit to the end of
    // the array,
    // and create a slice
    currentMappedFile.position(savedPosition);
    currentMappedFile.limit(savedPosition + length);
    final DoubleBuffer slice = currentMappedFile.slice();

    // Return the position and limit of the buffer back to the original values
    currentMappedFile.position(savedPosition + length);
    currentMappedFile.limit(currentMappedFile.capacity());

    // Create a read-only version of the new buffer slice
    final DoubleBuffer readOnlySlice = slice.asReadOnlyBuffer();

    return readOnlySlice;

  }

}
