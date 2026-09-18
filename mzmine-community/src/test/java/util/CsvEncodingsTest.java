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

package util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.opencsv.exceptions.CsvException;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.io.CharsetUtils;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Real files in different encodings and with different separators, see
 * test/resources/csv/encodings. All of them contain the same little table, so every file must parse
 * into {@link #EXPECTED} no matter how it was written.
 * <p>
 * The files are marked as binary in .gitattributes, otherwise git would normalize their line
 * endings and break the utf-16 files.
 */
public class CsvEncodingsTest {

  private static final String RESOURCE_DIR = "csv/encodings/";

  private static final String[][] EXPECTED = new String[][]{ //
      {"name", "mz", "note"}, //
      {"Öl", "195.0877", "25 °C"}, // Öl, 25 °C
      {"Glucose", "180.0634", "30 µm"}}; // 30 µm

  /**
   * @param file             resource name
   * @param charset          the encoding the file was written in
   * @param separator        the separator that must be detected
   * @param firstBytes       start of the file, guards against a tool or git changing the encoding
   */
  private record EncodedFile(String file, Charset charset, char separator, String firstBytes) {

    @Override
    public String toString() {
      return "%s (%s)".formatted(file, charset.name());
    }
  }

  private static Stream<EncodedFile> files() {
    final Charset utf8 = StandardCharsets.UTF_8;
    final Charset cp1252 = CharsetUtils.LEGACY_FALLBACK;
    return Stream.of( //
        new EncodedFile("utf8.csv", utf8, ',', "6e616d65"), // name
        new EncodedFile("utf8.tsv", utf8, '\t', "6e616d65"), //
        new EncodedFile("utf8-bom.csv", utf8, ',', "efbbbf"), // byte order mark
        new EncodedFile("windows1252.csv", cp1252, ',', "6e616d65"), //
        new EncodedFile("windows1252-semicolon.csv", cp1252, ';', "6e616d65"), //
        new EncodedFile("excel-sep-directive.csv", cp1252, ';', "7365703d3b"), // sep=;
        new EncodedFile("utf16le-bom.tsv", StandardCharsets.UTF_16LE, '\t', "fffe"), //
        new EncodedFile("utf16be-bom.csv", StandardCharsets.UTF_16BE, ',', "feff"), //
        new EncodedFile("utf16le-nobom.tsv", StandardCharsets.UTF_16LE, '\t', "6e006100"));
  }

  private static File resource(String name) {
    try {
      return Path.of(CsvEncodingsTest.class.getClassLoader().getResource(RESOURCE_DIR + name).toURI())
          .toFile();
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Cannot find test resource " + name, e);
    }
  }

  @ParameterizedTest
  @MethodSource("files")
  void testFileIsStillEncodedAsExpected(EncodedFile expected) throws IOException {
    final byte[] bytes = Files.readAllBytes(resource(expected.file()).toPath());
    final int length = expected.firstBytes().length() / 2;

    assertEquals(expected.firstBytes(),
        HexFormat.of().formatHex(Arrays.copyOf(bytes, length)),
        "%s does not start with the expected bytes any more. Was the file re-encoded or did git normalize it?".formatted(
            expected.file()));
  }

  @ParameterizedTest
  @MethodSource("files")
  void testDetectCharset(EncodedFile expected) {
    assertEquals(expected.charset(), CharsetUtils.detectCharset(resource(expected.file())));
  }

  @ParameterizedTest
  @MethodSource("files")
  void testDetectSeparator(EncodedFile expected) {
    assertEquals(expected.separator(),
        CSVParsingUtils.autoDetermineSeparator(resource(expected.file())));
  }

  @ParameterizedTest
  @MethodSource("files")
  void testReadWithAutoDetection(EncodedFile expected) throws IOException, CsvException {
    final List<String[]> rows = CSVParsingUtils.readDataAutoSeparator(resource(expected.file()));

    assertEquals(EXPECTED.length, rows.size(), expected.file());
    for (int i = 0; i < EXPECTED.length; i++) {
      assertArrayEquals(EXPECTED[i], rows.get(i), "row %d of %s".formatted(i, expected.file()));
    }
  }

  @Test
  void testSemicolonWithDecimalComma() throws IOException, CsvException {
    // the values contain commas, so comma splits more columns than the semicolon. Only the
    // semicolon splits every line into the same number of columns
    final File file = resource("utf8-decimal-comma.csv");

    assertEquals(';', CSVParsingUtils.autoDetermineSeparator(file));

    final List<String[]> rows = CSVParsingUtils.readDataAutoSeparator(file);
    assertEquals(3, rows.size());
    assertArrayEquals(new String[]{"Öl", "195,0877", "25 °C"}, rows.get(1));
  }
}
