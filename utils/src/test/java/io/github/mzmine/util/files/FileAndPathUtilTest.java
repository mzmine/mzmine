/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.files;

import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileAndPathUtilTest {

  private static final Logger logger = Logger.getLogger(FileAndPathUtilTest.class.getName());
  @TempDir
  static Path tempDir;

  @Test
  void configuredCreationMethodsFailOver() throws IOException {
    File[] previousDirectories = FileAndPathUtil.getTempDirs();
    List<TempDirectoryManager.TempFileOperation<?>> operations = List.of(
        _ -> FileAndPathUtil.createTempFile("failover", ".tmp"),
        _ -> FileAndPathUtil.createTempDirectory("failover"), _ -> {
          try (var channel = FileAndPathUtil.openTempFileChannel("failover", ".tmp")) {
            return channel.size();
          }
        }, _ -> {
          try (var arena = Arena.ofConfined()) {
            var mapped = FileAndPathUtil.memoryMapSparseTempFile(arena, 1024);
            mapped.set(ValueLayout.JAVA_BYTE, 0, (byte) 42);
            return mapped.get(ValueLayout.JAVA_BYTE, 0);
          }
        });

    try {
      for (var operation : operations) {
        Path first = Files.createTempDirectory(tempDir, "first");
        Path second = Files.createTempDirectory(tempDir, "second");
        FileAndPathUtil.setTempDirs(first.toFile(), second.toFile());
        // Make the current directory unusable after configuration, without requiring a full disk.
        Files.delete(first);
        Files.createFile(first);

        operation.create(second);

        Assertions.assertEquals(second.toFile(), FileAndPathUtil.getTempDir());
      }
    } finally {
      FileAndPathUtil.setTempDirs(previousDirectories);
    }
  }

  @Test
  void detectsMzmineDirAndItsSubdirectories() {
    File mzmineDir = FileAndPathUtil.getMzmineDir();
    File userDir = mzmineDir.getParentFile();

    Assertions.assertTrue(FileAndPathUtil.isInMzmineDir(mzmineDir));
    Assertions.assertTrue(FileAndPathUtil.isInMzmineDir(new File(mzmineDir, "temp/nested")));
    Assertions.assertTrue(
        FileAndPathUtil.isInMzmineDir(new File(userDir, "other/../.mzmine/temp")));
    Assertions.assertFalse(FileAndPathUtil.isInMzmineDir(userDir));
    Assertions.assertFalse(FileAndPathUtil.isInMzmineDir(new File(userDir, ".mzmine_temp")));
    Assertions.assertFalse(FileAndPathUtil.isInMzmineDir(tempDir.toFile()));
  }

  @Test
  void mzmineDirIsNeverConfiguredAsTempDir() {
    File[] previousDirectories = FileAndPathUtil.getTempDirs();
    try {
      File mzmineDir = FileAndPathUtil.getMzmineDir();
      File subdirectory = new File(mzmineDir, "temp-" + System.nanoTime());
      File allowed = tempDir.resolve("allowed").toFile();

      FileAndPathUtil.setTempDirs(mzmineDir, subdirectory, allowed);

      Assertions.assertArrayEquals(new File[]{allowed}, FileAndPathUtil.getTempDirs());
      Assertions.assertFalse(subdirectory.exists());
    } finally {
      FileAndPathUtil.setTempDirs(previousDirectories);
    }
  }

  @Test
  void existingFile() {

    logger.info("Testing existing file");
    Path tmp = tempDir.resolve("test.tmp");
    tmp.toFile().getParentFile().mkdirs();
    try (var writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
        WriterOptions.REPLACE.toOpenOption())) {

      try {
        MemorySegment segment = FileAndPathUtil.memoryMapSparseTempFile("test", ".tmp", tempDir,
            Arena.ofAuto(), 1L << 20);
        Assertions.assertNotNull(segment);
      } catch (Exception e) {
        logger.warning("Issue memory mapping new file");
      }
      // write something to make sure writer is used
      writer.newLine();
    } catch (Exception e) {
      logger.warning("Issue with initial file creation");
      throw new RuntimeException(e);
    }
  }

  @Test
  void memoryMapSparseTempFile() throws IOException, InterruptedException {
    try (final var arena = Arena.ofConfined()) {
      var mapped = FileAndPathUtil.memoryMapSparseTempFile("test", ".tmp", tempDir, arena,
          1L << 20);
      SegmentAllocator alloc = SegmentAllocator.slicingAllocator(mapped);
//
      final double[] stored = new double[]{15.345, 7.231, 123.34234};

      for (int i = 0; i < 100; i++) {
        final MemorySegment allocated = alloc.allocateFrom(ValueLayout.JAVA_DOUBLE, stored);

        // ok
        Assertions.assertEquals(stored[0], allocated.getAtIndex(ValueLayout.JAVA_DOUBLE, 0));

        final ByteBuffer byteBuffer = allocated.asByteBuffer().order(ByteOrder.nativeOrder());
        final double fromByteBuffer = byteBuffer.getDouble(0);
        Assertions.assertEquals(stored[0], fromByteBuffer);

        final DoubleBuffer doubleBuffer = allocated.asByteBuffer().order(ByteOrder.nativeOrder())
            .asDoubleBuffer();
        final double fromDoubleBuffer = doubleBuffer.get(0);
        Assertions.assertEquals(stored[0], fromDoubleBuffer);
      }
      TimeUnit.SECONDS.sleep(1);
    }
  }

  final String allCorrect = "_-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'~!@#$%&()[]{}=.,+";

  @Test
  void getPathIllegalChars() {
    Assertions.assertEquals(List.of(),
        List.copyOf(FileAndPathUtil.getPathIllegalChars(allCorrect)));
    Assertions.assertEquals(List.of("*", "\"", "?", "|", "\\", "\t"),
        List.copyOf(FileAndPathUtil.getPathIllegalChars("*na\"me??te|st\\tab\t")));
  }

  @Test
  void safePathEncode() {
    Assertions.assertEquals(allCorrect, FileAndPathUtil.safePathEncode(allCorrect));

    Assertions.assertEquals("_name_test_", FileAndPathUtil.safePathEncode("\"name?test*"));
    Assertions.assertEquals("_na_me__test_", FileAndPathUtil.safePathEncode("*na\tme??test\\"));
  }
}
