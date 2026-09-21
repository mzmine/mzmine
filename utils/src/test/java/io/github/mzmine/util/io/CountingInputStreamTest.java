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

package io.github.mzmine.util.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CountingInputStreamTest {

  private static CountingInputStream of(final String content) {
    return new CountingInputStream(
        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void testCountsSingleByteReads() throws IOException {
    try (CountingInputStream in = of("abc")) {
      Assertions.assertEquals(0, in.getCount());
      Assertions.assertEquals('a', in.read());
      Assertions.assertEquals(1, in.getCount());
      Assertions.assertEquals('b', in.read());
      Assertions.assertEquals('c', in.read());
      Assertions.assertEquals(3, in.getCount());
      // end of stream must not be counted
      Assertions.assertEquals(-1, in.read());
      Assertions.assertEquals(3, in.getCount());
    }
  }

  @Test
  void testCountsBulkReads() throws IOException {
    try (CountingInputStream in = of("0123456789")) {
      final byte[] buffer = new byte[4];
      Assertions.assertEquals(4, in.read(buffer));
      Assertions.assertEquals(4, in.getCount());
      Assertions.assertEquals(3, in.read(buffer, 0, 3));
      Assertions.assertEquals(7, in.getCount());
      Assertions.assertEquals(3, in.readAllBytes().length);
      Assertions.assertEquals(10, in.getCount());
    }
  }

  @Test
  void testCountsSkip() throws IOException {
    try (CountingInputStream in = of("0123456789")) {
      Assertions.assertEquals(4, in.skip(4));
      Assertions.assertEquals(4, in.getCount());
      Assertions.assertEquals('4', in.read());
      Assertions.assertEquals(5, in.getCount());
    }
  }

  @Test
  void testMarkAndResetRewindTheCount() throws IOException {
    try (CountingInputStream in = of("0123456789")) {
      Assertions.assertEquals(2, in.read(new byte[2]));
      in.mark(8);
      Assertions.assertEquals(3, in.read(new byte[3]));
      Assertions.assertEquals(5, in.getCount());
      in.reset();
      Assertions.assertEquals(2, in.getCount());
    }
  }

  /**
   * The count has to end at the file size, which is what progress is measured against.
   */
  @Test
  void testCountMatchesContentWhenReadThroughABuffer() throws IOException {
    final String content = "some text\n".repeat(5000);
    final long bytes = content.getBytes(StandardCharsets.UTF_8).length;
    try (CountingInputStream counting = of(content); InputStream in = new BufferedInputStream(
        counting, 64)) {
      Assertions.assertEquals(bytes, in.readAllBytes().length);
      Assertions.assertEquals(bytes, counting.getCount());
    }
  }
}
