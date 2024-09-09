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

import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout.OfDouble;
import java.lang.foreign.ValueLayout.OfFloat;
import java.lang.foreign.ValueLayout.OfInt;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;
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

  private SegmentAllocator allocator;

  private MemoryMapStorage() {

  }

  private static synchronized SegmentAllocator createMappedFile(long capacity) {
    try {
      var file = FileAndPathUtil.createTempFile("mzmine", ".tmp");
      logger.info(() -> "Creating mapped file at " + file.getAbsolutePath());
      FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ,
          StandardOpenOption.WRITE, StandardOpenOption.CREATE);

      final MemorySegment currentSegment = channel.map(MapMode.READ_WRITE, 0, capacity,
          Arena.ofAuto());

      file.deleteOnExit();

      return SegmentAllocator.slicingAllocator(currentSegment);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized DoubleBuffer storeData(@NotNull final double[] data) {
    if ((long) data.length * Double.BYTES > STORAGE_FILE_CAPACITY) {
      throw new RuntimeException(
          STR."Cannot store double array of length \{data.length} in mapped file of \{STORAGE_FILE_CAPACITY}. File too small.");
    }

    if (allocator == null) {
      allocator = createMappedFile(STORAGE_FILE_CAPACITY);
    }

    try {
      final MemorySegment memorySegment = allocator.allocate(OfDouble.JAVA_DOUBLE, data.length);
      MemorySegment.copy(data, 0, memorySegment, OfDouble.JAVA_DOUBLE, 0, data.length);
      return memorySegment.asByteBuffer().order(ByteOrder.nativeOrder()).asDoubleBuffer();
    } catch (IndexOutOfBoundsException e) {
      allocator = createMappedFile(STORAGE_FILE_CAPACITY);
      final MemorySegment memorySegment = allocator.allocate(OfDouble.JAVA_DOUBLE, data.length);
      MemorySegment.copy(data, 0, memorySegment, OfDouble.JAVA_DOUBLE, 0, data.length);
      return memorySegment.asByteBuffer().order(ByteOrder.nativeOrder()).asDoubleBuffer();
    }
  }

  public synchronized FloatBuffer storeData(@NotNull final float[] data) {
    if ((long) data.length * Double.BYTES > STORAGE_FILE_CAPACITY) {
      throw new RuntimeException(
          STR."Cannot store double array of length \{data.length} in mapped file of \{STORAGE_FILE_CAPACITY}. File too small.");
    }

    if (allocator == null) {
      allocator = createMappedFile(STORAGE_FILE_CAPACITY);
    }

    try {
      final MemorySegment memorySegment = allocator.allocate(OfFloat.JAVA_FLOAT, data.length);
      MemorySegment.copy(data, 0, memorySegment, OfFloat.JAVA_FLOAT, 0, data.length);
      return memorySegment.asByteBuffer().order(ByteOrder.nativeOrder()).asFloatBuffer();
    } catch (IndexOutOfBoundsException e) {
      allocator = createMappedFile(STORAGE_FILE_CAPACITY);
      final MemorySegment memorySegment = allocator.allocate(OfFloat.JAVA_FLOAT, data.length);
      MemorySegment.copy(data, 0, memorySegment, OfFloat.JAVA_FLOAT, 0, data.length);
      return memorySegment.asByteBuffer().order(ByteOrder.nativeOrder()).asFloatBuffer();
    }
  }

  public synchronized IntBuffer storeData(@NotNull final int[] data) {
    if ((long) data.length * Integer.BYTES > STORAGE_FILE_CAPACITY) {
      throw new RuntimeException(
          STR."Cannot store double array of length \{data.length} in mapped file of \{STORAGE_FILE_CAPACITY}. File too small.");
    }

    if (allocator == null) {
      allocator = createMappedFile(STORAGE_FILE_CAPACITY);
    }

    try {
      final MemorySegment memorySegment = allocator.allocate(OfInt.JAVA_INT, data.length);
      MemorySegment.copy(data, 0, memorySegment, OfInt.JAVA_INT, 0, data.length);
      return memorySegment.asByteBuffer().order(ByteOrder.nativeOrder()).asIntBuffer();
    } catch (IndexOutOfBoundsException e) {
      allocator = createMappedFile(STORAGE_FILE_CAPACITY);
      final MemorySegment memorySegment = allocator.allocate(OfInt.JAVA_INT, data.length);
      MemorySegment.copy(data, 0, memorySegment, OfInt.JAVA_INT, 0, data.length);
      return memorySegment.asByteBuffer().order(ByteOrder.nativeOrder()).asIntBuffer();
    }
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
