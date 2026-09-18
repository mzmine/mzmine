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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.util.io.CharsetUtils.CharsetDetection;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CharsetUtilsTest {

  /**
   * umlauts, a degree sign and a micro sign: all of them are encoded differently in utf-8 and
   * windows-1252
   */
  private static final String CONTENT = """
      name;temperature
      Glucose;25 °C
      Öl;30 µm
      """;

  @TempDir
  Path tempDir;

  private Path write(String name, byte[] bytes) throws IOException {
    final Path path = tempDir.resolve(name);
    Files.write(path, bytes);
    return path;
  }

  private Path write(String name, String content, Charset charset) throws IOException {
    return write(name, content.getBytes(charset));
  }

  private static byte[] concat(byte[] a, byte[] b) {
    final byte[] result = new byte[a.length + b.length];
    System.arraycopy(a, 0, result, 0, a.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }

  private static List<String> readLines(Path path) throws IOException {
    try (BufferedReader reader = CharsetUtils.newBufferedReader(path)) {
      return reader.lines().toList();
    }
  }

  @Test
  void testPlainUtf8() throws IOException {
    final Path file = write("utf8.csv", CONTENT, StandardCharsets.UTF_8);

    final CharsetDetection detection = CharsetUtils.detect(file);
    assertEquals(StandardCharsets.UTF_8, detection.charset());
    assertEquals(0, detection.bomLength());
    assertEquals(CONTENT.lines().toList(), readLines(file));
  }

  @Test
  void testAsciiIsReadAsUtf8() throws IOException {
    final Path file = write("ascii.csv", "name,mz\nCaffeine,195.0877\n", StandardCharsets.US_ASCII);

    assertEquals(StandardCharsets.UTF_8, CharsetUtils.detectCharset(file.toFile()));
    assertEquals(List.of("name,mz", "Caffeine,195.0877"), readLines(file));
  }

  @Test
  void testUtf8WithByteOrderMark() throws IOException {
    // excel "CSV UTF-8" writes a byte order mark
    final Path file = write("utf8-bom.csv",
        concat(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
            CONTENT.getBytes(StandardCharsets.UTF_8)));

    final CharsetDetection detection = CharsetUtils.detect(file);
    assertEquals(StandardCharsets.UTF_8, detection.charset());
    assertEquals(3, detection.bomLength());
    assertTrue(detection.fromBom());
    // the mark must not end up in the first column title
    assertEquals(CONTENT.lines().toList(), readLines(file));
  }

  @Test
  void testUtf16WithByteOrderMark() throws IOException {
    // excel "Unicode Text (*.txt)" writes utf-16le with a byte order mark
    final Path littleEndian = write("utf16le.txt", CONTENT, StandardCharsets.UTF_16LE);
    Files.write(littleEndian,
        concat(new byte[]{(byte) 0xFF, (byte) 0xFE}, Files.readAllBytes(littleEndian)));

    assertEquals(StandardCharsets.UTF_16LE, CharsetUtils.detect(littleEndian).charset());
    assertEquals(2, CharsetUtils.detect(littleEndian).bomLength());
    assertEquals(CONTENT.lines().toList(), readLines(littleEndian));

    // "UTF-16" writes a big endian mark
    final Path bigEndian = write("utf16be.txt", CONTENT, StandardCharsets.UTF_16);
    assertEquals(StandardCharsets.UTF_16BE, CharsetUtils.detect(bigEndian).charset());
    assertEquals(CONTENT.lines().toList(), readLines(bigEndian));
  }

  @Test
  void testUtf16WithoutByteOrderMark() throws IOException {
    final Path littleEndian = write("utf16le-nobom.txt", CONTENT, StandardCharsets.UTF_16LE);
    assertEquals(StandardCharsets.UTF_16LE, CharsetUtils.detect(littleEndian).charset());
    assertEquals(CONTENT.lines().toList(), readLines(littleEndian));

    final Path bigEndian = write("utf16be-nobom.txt", CONTENT, StandardCharsets.UTF_16BE);
    assertEquals(StandardCharsets.UTF_16BE, CharsetUtils.detect(bigEndian).charset());
    assertEquals(CONTENT.lines().toList(), readLines(bigEndian));
  }

  @Test
  void testSingleByteEncoding() throws IOException {
    // excel "CSV (comma delimited)" writes the system code page on windows
    final Path file = write("windows1252.csv", CONTENT, CharsetUtils.LEGACY_FALLBACK);

    assertEquals(CharsetUtils.LEGACY_FALLBACK, CharsetUtils.detect(file).charset());
    // strict utf-8 would fail on the umlaut
    assertEquals(CONTENT.lines().toList(), readLines(file));
  }

  @Test
  void testEmptyFile() throws IOException {
    final Path file = write("empty.csv", new byte[0]);

    assertEquals(StandardCharsets.UTF_8, CharsetUtils.detect(file).charset());
    assertEquals(List.of(), readLines(file));
  }

  @Test
  void testBrokenBytesAreReplacedInsteadOfThrowing() throws IOException {
    // a single broken byte after the detection sample must not fail the whole import
    final StringBuilder ascii = new StringBuilder("name,mz\n");
    while (ascii.length() < CharsetUtils.DETECTION_SAMPLE_BYTES) {
      ascii.append("Caffeine,195.0877\n");
    }
    final Path file = write("broken.csv",
        concat(ascii.toString().getBytes(StandardCharsets.US_ASCII), new byte[]{(byte) 0xFF, '\n'}));

    assertEquals(StandardCharsets.UTF_8, CharsetUtils.detect(file).charset());
    final List<String> lines = readLines(file);
    assertEquals("\uFFFD", lines.getLast()); // replacement character
  }

  @Test
  void testStreamDetection() throws IOException {
    final byte[] bytes = concat(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
        CONTENT.getBytes(StandardCharsets.UTF_8));

    try (InputStream in = new ByteArrayInputStream(bytes);
        BufferedReader reader = CharsetUtils.newBufferedReader(in)) {
      assertEquals(CONTENT.lines().toList(), reader.lines().toList());
    }
  }

  @Test
  void testDetectFromBytes() {
    assertEquals(StandardCharsets.UTF_16LE,
        CharsetUtils.detect(new byte[]{(byte) 0xFF, (byte) 0xFE, 'a', 0}).charset());
    assertEquals(StandardCharsets.UTF_16BE,
        CharsetUtils.detect(new byte[]{(byte) 0xFE, (byte) 0xFF, 0, 'a'}).charset());
    assertEquals(StandardCharsets.UTF_8,
        CharsetUtils.detect("Öl".getBytes(StandardCharsets.UTF_8)).charset());
    assertEquals(CharsetUtils.LEGACY_FALLBACK,
        CharsetUtils.detect("Öl".getBytes(CharsetUtils.LEGACY_FALLBACK)).charset());
  }
}
