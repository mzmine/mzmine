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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link ProcessOutputBuffer} stays bounded while keeping both ends of the stream, which
 * is what makes an MSPepSearch failure diagnosable after a run that printed a /PROGRESS line per
 * spectrum.
 */
class ProcessOutputBufferTest {

  @Test
  @DisplayName("A short stream is kept as it is")
  void keepsShortStream() {

    final ProcessOutputBuffer buffer = new ProcessOutputBuffer();
    buffer.add("first");
    buffer.add("second");

    assertEquals(String.join(System.lineSeparator(), List.of("first", "second")),
        buffer.toString());
  }

  @Test
  @DisplayName("An empty stream produces an empty string")
  void handlesEmptyStream() {
    assertEquals("", new ProcessOutputBuffer().toString());
  }

  @Test
  @DisplayName("A long stream keeps both ends and reports the gap")
  void keepsBothEnds() {

    final ProcessOutputBuffer buffer = new ProcessOutputBuffer();
    buffer.add("MS Library initiation error -10");
    for (int i = 0; i < 100_000; i++) {
      buffer.add("Comparing 34 library spectra with submitted spectrum. 100% done.");
    }
    buffer.add("Completed.");

    final String captured = buffer.toString();

    // the startup error is at the head, the last line at the tail, and the size stays bounded
    assertTrue(captured.startsWith("MS Library initiation error -10"));
    assertTrue(captured.endsWith("Completed."));
    assertTrue(captured.contains("lines omitted"));
    assertTrue(captured.length() < 20_000, "buffer grew to " + captured.length() + " characters");
  }

  @Test
  @DisplayName("A single very long line is truncated")
  void truncatesLongLine() {

    final ProcessOutputBuffer buffer = new ProcessOutputBuffer();
    buffer.add("x".repeat(10_000));

    final String captured = buffer.toString();

    assertTrue(captured.length() < 1_000, "line grew to " + captured.length() + " characters");
    assertTrue(captured.endsWith("..."));
    assertFalse(captured.contains(System.lineSeparator()));
  }
}
