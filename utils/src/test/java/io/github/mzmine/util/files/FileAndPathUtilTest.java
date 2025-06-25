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
  void existingFile() {

    logger.info("Testing existing file");
    Path tmp = Path.of("D:\\mzminetemp2\\test.tmp");
    tmp.toFile().getParentFile().mkdirs();
    try (var writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
        WriterOptions.REPLACE.toOpenOption())) {

      try {
        MemorySegment segment = FileAndPathUtil.memoryMapSparseTempFile("test", ".tmp",
            Path.of("D:\\mzminetemp2"), Arena.ofAuto(), 1L << 20);
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
}
