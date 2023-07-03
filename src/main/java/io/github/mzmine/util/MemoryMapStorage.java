/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

/**
 * This class defines a memory-mapped temporary file storage for large primitive type arrays. The
 * storeData() function stores an array into the underlying temporary file and returns a buffer. The
 * buffer is directly bound to the memory-mapped portion of the file so the data can be directly
 * accessed without loading it into another intermediate primitive type array.
 * <p>
 * The size of each temporary file is STORAGE_FILE_CAPACITY bytes. When the file is full, a new file
 * is automatically created using the createNewMappedFile() function. The size of each temporary
 * file in the filesystem may show as 1GB, but actually only a portion of that space is occupied on
 * the disk, depending on the amount of stored data (this can be examined using the 'du -hs' Linux
 * command.
 * <p>
 * There is no support for removing data from the file - we assume such operation is rare and
 * therefore the data can be left on the disk until the whole temporary file is discarded.
 * <p>
 * There is a limit on the number of open file descriptors (e.g. 1024 by default on Linux). With 1
 * GB per temporary file, this would give us about 1 TB of storage space, so perhaps it is okay.
 * There is no way to remove the memory-mapped file, it is removed automatically when the
 * MappedByteBuffer is garbage-collected.
 * <p>
 * The total amount of storage space is also limited by the amount of addressable virtual memory
 * (e.g., 128TB on Linux). For this reason, this approach requires a 64-bit system - the limit would
 * be only 2GB on a 32-bit system.
 */
public class MemoryMapStorage {

  /**
   * One temporary file can store STORAGE_FILE_CAPACITY bytes. We need to fit within 2GB limit for a
   * single MappedByteBuffer. 1 GB per file seems like a good start.
   */
  private static final long STORAGE_FILE_CAPACITY = 1_000_000_000L;
  private static boolean storeFeaturesInRam = false;
  private static boolean storeRawFilesInRam = false;
  private static boolean storeMassListsInRam = false;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final Set<File> temporaryFiles = new HashSet<>();
  private final List<MappedByteBuffer> mappedByteBufferList = new ArrayList<>();
  /**
   * The file that we are currently writing into.
   */
  private MappedByteBuffer currentMappedFile = null;

  private MemoryMapStorage() {
    // register this storage to MZmineCore, so we can delete all temp files later.
    MZmineCore.registerStorage(this);
  }

  /**
   * @return The {@link MemoryMapStorage} or null, if the data shall be stored in ram.
   */
  @Nullable
  public static MemoryMapStorage forFeatureList() {
    return storeFeaturesInRam ? null : new MemoryMapStorage();
  }

  /**
   * @return The {@link MemoryMapStorage} or null, if the data shall be stored in ram.
   */
  @Nullable
  public static MemoryMapStorage forRawDataFile() {
    return storeRawFilesInRam ? null : new MemoryMapStorage();
  }

  /**
   * @return The {@link MemoryMapStorage} or null, if the data shall be stored in ram.
   */
  @Nullable
  public static MemoryMapStorage forMassList() {
    return storeMassListsInRam ? null : new MemoryMapStorage();
  }

  @NotNull
  public static MemoryMapStorage create() {
    return new MemoryMapStorage();
  }

  public static boolean isStoreFeaturesInRam() {
    return storeFeaturesInRam;
  }

  public static void setStoreFeaturesInRam(boolean storeFeaturesInRam) {
    MemoryMapStorage.storeFeaturesInRam = storeFeaturesInRam;
  }

  public static boolean isStoreRawFilesInRam() {
    return storeRawFilesInRam;
  }

  public static void setStoreRawFilesInRam(boolean storeRawFilesInRam) {
    MemoryMapStorage.storeRawFilesInRam = storeRawFilesInRam;
  }

  public static boolean isStoreMassListsInRam() {
    return storeMassListsInRam;
  }

  public static void setStoreMassListsInRam(boolean storeMassListsInRam) {
    MemoryMapStorage.storeMassListsInRam = storeMassListsInRam;
  }

  /**
   * Store everything in RAM instead of using MemoryMapStorage
   *
   * @param state true to keep all objects in RAM
   */
  public static void setStoreAllInRam(boolean state) {
    storeFeaturesInRam = state;
    storeMassListsInRam = state;
    storeRawFilesInRam = state;
  }

  /**
   * Creates a new temporary file, maps it into memory, and returns the corresponding
   * MappedByteBuffer. The capacity of the buffer is STORAGE_FILE_CAPACITY bytes.
   *
   * @return a MappedByteBuffer corresponding to the memory-mapped temporary file
   * @throws IOException
   */
  private MappedByteBuffer createNewMappedFile() throws IOException {

    // Create the temporary storage file
    File storageFileName = FileAndPathUtil.createTempFile("mzmine", ".tmp");
    temporaryFiles.add(storageFileName);
    logger.finest("Created a temporary file " + storageFileName);

    // Open the file for writing
    RandomAccessFile storageFile = new RandomAccessFile(storageFileName, "rw");

    // Map the file into memory
    MappedByteBuffer mappedFileBuffer =
        storageFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, STORAGE_FILE_CAPACITY);
    mappedByteBufferList.add(mappedFileBuffer);

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
  @NotNull
  public synchronized DoubleBuffer storeData(@NotNull final double data[])
      throws IOException {
    return storeData(data, 0, data.length);
  }

  /**
   * Store the given double[] array in a memory-mapped temporary file and return a read-only
   * DoubleBuffer that can access the data.
   *
   * @param data   the double[] array with the data
   * @param offset offset of the stored portion of the data[] array
   * @param length size of the stored portion of the data[] array
   * @return a read-only DoubleBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */
  @NotNull
  public synchronized DoubleBuffer storeData(@NotNull final double data[], int offset,
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
  @NotNull
  public synchronized FloatBuffer storeData(@NotNull final float data[])
      throws IOException {
    return storeData(data, 0, data.length);
  }

  /**
   * Store the given float[] array in a memory-mapped temporary file and return a read-only
   * FloatBuffer that can access the data.
   *
   * @param data   the float[] array with the data
   * @param offset offset of the stored portion of the data[] array
   * @param length size of the stored portion of the data[] array
   * @return a read-only FloatBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */
  @NotNull
  public synchronized FloatBuffer storeData(@NotNull final float data[], int offset,
      int length) throws IOException {

    // If we have no storage file or if the current file is full, create a new one
    if ((currentMappedFile == null)
        || (currentMappedFile.position() + (length * Float.BYTES) > STORAGE_FILE_CAPACITY)) {
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
  @NotNull
  public synchronized IntBuffer storeData(@NotNull final int data[]) throws IOException {
    return storeData(data, 0, data.length);
  }

  /**
   * Store the given int[] array in a memory-mapped temporary file and return a read-only IntBuffer
   * that can access the data.
   *
   * @param data   the int[] array with the data
   * @param offset offset of the stored portion of the data[] array
   * @param length size of the stored portion of the data[] array
   * @return a read-only IntBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */
  @NotNull
  public synchronized IntBuffer storeData(@NotNull final int data[], int offset,
      int length) throws IOException {

    // If we have no storage file or if the current file is full, create a new one
    if ((currentMappedFile == null)
        || (currentMappedFile.position() + (length * Integer.BYTES) > STORAGE_FILE_CAPACITY)) {
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
  public synchronized void discard(Unsafe theUnsafe) throws IOException {

    if (theUnsafe != null) {
      for (MappedByteBuffer mappedByteBuffer : mappedByteBufferList) {
        theUnsafe.invokeCleaner(mappedByteBuffer);
      }
    }

    for (File tmpFile : temporaryFiles) {
      if (!tmpFile.delete()) {
        logger.warning("Could not delete temporary file " + tmpFile.getAbsolutePath());
      }
    }

    temporaryFiles.clear();
    currentMappedFile = null;
  }
}
