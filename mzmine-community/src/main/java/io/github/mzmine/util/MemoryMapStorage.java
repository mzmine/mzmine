/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MemoryMapStorage {

  private static final Logger logger = Logger.getLogger(MemoryMapStorage.class.getName());

  //  private static final long STORAGE_FILE_CAPACITY = 4_294_967_296L;
  private static final long STORAGE_FILE_CAPACITY = 1_000_000_000L;

  private static boolean storeFeaturesInRam = false;
  private static boolean storeRawFilesInRam = false;
  private static boolean storeMassListsInRam = false;

  private static AtomicLong tempFileCounter = new AtomicLong(0L);

  private final CloseableReentrantReadWriteLock lock = new CloseableReentrantReadWriteLock();

  private SegmentAllocator allocator;

  private MemoryMapStorage() {
  }

  private SegmentAllocator createMappedFile(long capacity) throws IOException {
    try (var l = lock.lockWrite()) {
      final MemorySegment currentSegment = FileAndPathUtil.memoryMapSparseTempFile(Arena.ofAuto(),
          capacity);

      return SegmentAllocator.slicingAllocator(currentSegment);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error creating temp file. %s".formatted(e.getMessage()), e);
      throw e;
    }
  }

  @NotNull
  private MemorySegment __storeData(@NotNull Object data, ValueLayout layout, int length) {
    if (((long) length * layout.byteSize()) > STORAGE_FILE_CAPACITY) {
      throw new RuntimeException(
          STR."Cannot store double array of length \{length} in mapped file of \{STORAGE_FILE_CAPACITY}. File too small.");
    }

    try (var l = lock.lockWrite()) {
      // check if the allocator exists
      if (allocator == null) {
        try {
          allocator = createMappedFile(STORAGE_FILE_CAPACITY);
        } catch (IOException e) {
          logger.log(Level.SEVERE,
              "Error creating temp file. %s, storing data into RAM.".formatted(e.getMessage()), e);

          final MemorySegment memorySegment = Arena.ofAuto().allocate(layout, length);
          MemorySegment.copy(data, 0, memorySegment, layout, 0, length);
          return memorySegment;
        }
      }

      try {
        // success, store in actual mapped file
        final MemorySegment memorySegment = allocator.allocate(layout, length);
        MemorySegment.copy(data, 0, memorySegment, layout, 0, length);
        return memorySegment;
      } catch (IndexOutOfBoundsException e) {
        logger.log(Level.FINE, "Cannot memory map array of length %d".formatted(length), e);

        // current mapped file is full
        try {
          allocator = createMappedFile(STORAGE_FILE_CAPACITY);
          // success, store in actual mapped file
          final MemorySegment memorySegment = allocator.allocate(layout, length);
          MemorySegment.copy(data, 0, memorySegment, layout, 0, length);
          return memorySegment;

        } catch (IOException ex) {
          logger.log(Level.SEVERE, "Error creating temp file. %s".formatted(e.getMessage()), ex);

          final MemorySegment memorySegment = Arena.ofAuto().allocate(layout, length);
          MemorySegment.copy(data, 0, memorySegment, layout, 0, length);
          return memorySegment;
        }
      }
    }
  }

  public DoubleBuffer storeData(@NotNull final double[] data) {
    if ((long) data.length * Double.BYTES > STORAGE_FILE_CAPACITY) {
      throw new RuntimeException(
          STR."Cannot store double array of length \{data.length} in mapped file of \{STORAGE_FILE_CAPACITY}. File too small.");
    }

    return __storeData(data, ValueLayout.JAVA_DOUBLE, data.length).asByteBuffer()
        .order(ByteOrder.nativeOrder()).asDoubleBuffer();
  }

  public FloatBuffer storeData(@NotNull final float[] data) {
    if ((long) data.length * Float.BYTES > STORAGE_FILE_CAPACITY) {
      throw new RuntimeException(
          STR."Cannot store float array of length \{data.length} in mapped file of \{STORAGE_FILE_CAPACITY}. File too small.");
    }

    return __storeData(data, ValueLayout.JAVA_FLOAT, data.length).asByteBuffer()
        .order(ByteOrder.nativeOrder()).asFloatBuffer();
  }

  public IntBuffer storeData(@NotNull final int[] data) {
    if ((long) data.length * Integer.BYTES > STORAGE_FILE_CAPACITY) {
      throw new RuntimeException(
          STR."Cannot store double array of length \{data.length} in mapped file of \{STORAGE_FILE_CAPACITY}. File too small.");
    }

    return __storeData(data, ValueLayout.JAVA_INT, data.length).asByteBuffer()
        .order(ByteOrder.nativeOrder()).asIntBuffer();
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
}
