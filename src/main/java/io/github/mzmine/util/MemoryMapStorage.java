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
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * This class defines a memory-mapped temporary file storage for large primitive type arrays. The
 * storeData() function stores an array into the underlying temporary file and returns a buffer. The
 * buffer is directly bound to the memory-mapped portion of the file so the data can be directly
 * accessed without loading it into another intermediate primitive type array.
 *
 * The size of each temporary file is STORAGE_FILE_CAPACITY bytes. When the file is full, a new file
 * is automatically created using the createNewMappedFile() function. The size of each temporary
 * file in the filesystem may show as 1GB, but actually only a portion of that space is occupied on
 * the disk, depending on the amount of stored data (this can be examined using the 'du -hs' Linux
 * command.
 *
 * There is no support for removing data from the file - we assume such operation is rare and
 * therefore the data can be left on the disk until the whole temporary file is discarded.
 *
 * There is a limit on the number of open file descriptors (e.g. 1024 by default on Linux). With 1
 * GB per temporary file, this would give us about 1 TB of storage space, so perhaps it is okay.
 * There is no way to remove the memory-mapped file, it is removed automatically when the
 * MappedByteBuffer is garbage-collected.
 *
 * The total amount of storage space is also limited by the amount of addressable virtual memory
 * (e.g., 128TB on Linux). For this reason, this approach requires a 64-bit system - the limit would
 * be only 2GB on a 32-bit system.
 *
 *
 */
public class MemoryMapStorage {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * One temporary file can store STORAGE_FILE_CAPACITY bytes. We need to fit within 2GB limit for a
   * single MappedByteBuffer. 1 GB per file seems like a good start.
   */
  private static final long STORAGE_FILE_CAPACITY = 1_000_000_000L;

  private final Set<File> temporaryFiles = new HashSet<>();

  /**
   * The file that we are currently writing into.
   */
  private MappedByteBuffer currentMappedFile = null;

  /**
   * Creates a new temporary file, maps it into memory, and returns the corresponding
   * MappedByteBuffer. The capacity of the buffer is STORAGE_FILE_CAPACITY bytes.
   *
   * @return a MappedByteBuffer corresponding to the memory-mapped temporary file
   * @throws IOException
   */
  private MappedByteBuffer createNewMappedFile() throws IOException {

    // Create the temporary storage file
    File storageFileName = File.createTempFile("mzmine", ".tmp");
    temporaryFiles.add(storageFileName);
    logger.finest("Created a temporary file " + storageFileName);

    // Open the file for writing
    RandomAccessFile storageFile = new RandomAccessFile(storageFileName, "rw");

    // Map the file into memory
    MappedByteBuffer mappedFileBuffer =
        storageFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, STORAGE_FILE_CAPACITY);

    // Close the temporary file, the memory mapping will remain
    storageFile.close();

    // Unfortunately, deleteOnExit() doesn't work on Windows, see JDK
    // bug #4171239. We will try to remove the temporary files in a
    // shutdown hook registered in the main.ShutDownHook class.
    storageFileName.deleteOnExit();

    return mappedFileBuffer;

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
        || (currentMappedFile.position() + (length * Double.BYTES) > STORAGE_FILE_CAPACITY)) {
      currentMappedFile = createNewMappedFile();
    }

    // Save the current position in the storage file
    final int savedPosition = currentMappedFile.position();

    // Set the limit to the end of the new array and create a buffer slice
    currentMappedFile.limit(savedPosition + length * Double.BYTES);
    final ByteBuffer slice = currentMappedFile.slice();

    // Create a double view of the memory-mapped byte buffer
    DoubleBuffer sliceDoubleView = slice.asDoubleBuffer();

    // Copy the data to the memory mapped storage
    sliceDoubleView.put(data, offset, length);

    // Update the position and the main buffer so we are ready to store the next array
    currentMappedFile.position(savedPosition + length * Double.BYTES);

    // Create a read-only version of the new buffer slice
    final DoubleBuffer readOnlySlice = sliceDoubleView.asReadOnlyBuffer();

    return readOnlySlice;

  }

  /**
   * Store the given float[] array in a memory-mapped temporary file and return a read-only
   * FloatBuffer that can access the data.
   *
   * @param data the float[] array with the data
   * @return a read-only FloatBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */
  public synchronized @Nonnull FloatBuffer storeData(@Nonnull final float data[])
      throws IOException {
    return storeData(data, 0, data.length);
  }

  /**
   * Store the given float[] array in a memory-mapped temporary file and return a read-only
   * FloatBuffer that can access the data.
   *
   * @param data the float[] array with the data
   * @param offset offset of the stored portion of the data[] array
   * @param length size of the stored portion of the data[] array
   * @return a read-only FloatBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */
  public synchronized @Nonnull FloatBuffer storeData(@Nonnull final float data[], int offset,
      int length) throws IOException {

    // If we have no storage file or if the current file is full, create a new one
    if ((currentMappedFile == null)
        || (currentMappedFile.position() + length > STORAGE_FILE_CAPACITY)) {
      currentMappedFile = createNewMappedFile();
    }

    // Save the current position in the storage file
    final int savedPosition = currentMappedFile.position();

    // Set the limit to the end of the new array and create a buffer slice
    currentMappedFile.limit(savedPosition + length * Float.BYTES);
    final ByteBuffer slice = currentMappedFile.slice();

    // Create a float view of the memory-mapped byte buffer
    FloatBuffer sliceFloatView = slice.asFloatBuffer();

    // Copy the data to the memory mapped storage
    sliceFloatView.put(data, offset, length);

    // Update the position and the main buffer so we are ready to store the next array
    currentMappedFile.position(savedPosition + length * Float.BYTES);

    // Create a read-only version of the new buffer slice
    final FloatBuffer readOnlySlice = sliceFloatView.asReadOnlyBuffer();

    return readOnlySlice;

  }

  /**
   * Store the given int[] array in a memory-mapped temporary file and return a read-only IntBuffer
   * that can access the data.
   *
   * @param data the int[] array with the data
   * @return a read-only IntBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */
  public synchronized @Nonnull IntBuffer storeData(@Nonnull final int data[]) throws IOException {
    return storeData(data, 0, data.length);
  }

  /**
   * Store the given int[] array in a memory-mapped temporary file and return a read-only IntBuffer
   * that can access the data.
   *
   * @param data the int[] array with the data
   * @param offset offset of the stored portion of the data[] array
   * @param length size of the stored portion of the data[] array
   * @return a read-only IntBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */
  public synchronized @Nonnull IntBuffer storeData(@Nonnull final int data[], int offset,
      int length) throws IOException {

    // If we have no storage file or if the current file is full, create a new one
    if ((currentMappedFile == null)
        || (currentMappedFile.position() + length > STORAGE_FILE_CAPACITY)) {
      currentMappedFile = createNewMappedFile();
    }

    // Save the current position in the storage file
    final int savedPosition = currentMappedFile.position();

    // Set the limit to the end of the new array and create a buffer slice
    currentMappedFile.limit(savedPosition + length * Integer.BYTES);
    final ByteBuffer slice = currentMappedFile.slice();

    // Create an int view of the memory-mapped byte buffer
    IntBuffer sliceIntView = slice.asIntBuffer();

    // Copy the data to the memory mapped storage
    sliceIntView.put(data, offset, length);

    // Update the position and the main buffer so we are ready to store the next array
    currentMappedFile.position(savedPosition + length * Integer.BYTES);

    // Create a read-only version of the new buffer slice
    final IntBuffer readOnlySlice = sliceIntView.asReadOnlyBuffer();

    return readOnlySlice;

  }

  /**
   * Discard this memory-mapped storage and remove all the associated temporary files.
   */
  public synchronized void discard() throws IOException {
    for (File tmpFile : temporaryFiles)
      tmpFile.delete();
    temporaryFiles.clear();
    currentMappedFile = null;
  }

}
