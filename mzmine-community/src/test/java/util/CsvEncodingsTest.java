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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.github.mzmine.parameters.parametertypes.combowithinput.FieldSeparator;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.io.CharsetUtils;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Real files in different encodings and with different separators, see
 * test/resources/csv/encodings. Two groups of files, each written in every format its tool
 * offers: the small table of {@link #EXPECTED} and the metadata sheet of
 * {@link #EXPECTED_METADATA_SHEET} that was saved with every entry of the excel save as dialog.
 * All files of a group must parse into the same rows, no matter how they were written.
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
   * Content of all micometa_ files, see {@link #excelExports()}.
   */
  private static final String[][] EXPECTED_METADATA_SHEET = new String[][]{ //
      {"filename", "species", "condition"}, //
      {"171103_PMA_TK_M1_01.mzML", "Pseudomonas aeruginosa", "M1"}, //
      {"171103_PMA_TK_M1_02.mzML", "Pseudomonas aeruginosa", "M1"}, //
      {"171103_PMA_TK_M1_03.mzML", "Pseudomonas aeruginosa", "M1"}, //
      {"171103_PMA_TK_M1_04.mzML", "Pseudomonas aeruginosa", "M1"}, //
      {"171103_PMA_TK_M1_05.mzML", "Pseudomonas aeruginosa", "M1"}, //
      {"171103_PMA_TK_M1_06.mzML", "Pseudomonas aeruginosa", "M1"}, //
      {"171103_PMA_TK_PA14_01.mzML", "Pseudomonas aeruginosa", "PA14"}, //
      {"171103_PMA_TK_PA14_02.mzML", "Pseudomonas aeruginosa", "PA14"}, //
      {"171103_PMA_TK_PA14_03.mzML", "Pseudomonas aeruginosa", "PA14"}, //
      {"171103_PMA_TK_PA14_04.mzML", "Pseudomonas aeruginosa", "PA14"}, //
      {"171103_PMA_TK_PA14_05.mzML", "Pseudomonas aeruginosa", "PA14"}, //
      {"171103_PMA_TK_PA14_06.mzML", "Pseudomonas aeruginosa", "PA14"}, //
      {"171103_PMA_TK_QC_01.mzML", "nd", "QC"}, //
      {"171103_PMA_TK_QC_02.mzML", "nd", "QC"}, //
      {"171103_PMA_TK_QC_03.mzML", "nd", "QC"}, //
      {"171103_PMA_TK_QC_04.mzML", "nd", "QC"}, //
      {"171103_PMA_TK_QC_05.mzML", "nd", "QC"}, //
      {"171103_PMA_TK_QC_06.mzML", "nd", "QC"}, //
      {"171103_PMA_TK_QC_07.mzML", "nd", "QC"}, //
      {"171103_PMA_TK_QC_08.mzML", "nd", "QC"}, //
      {"171103_PMA_TK_media_02.mzML", "nd", "media"}, //
      {"171103_PMA_TK_media_03.mzML", "nd", "media"}, //
      {"171103_PMA_TK_media_04.mzML", "nd", "media"}, //
      {"171103_PMA_TK_media_05.mzML", "nd", "media"}, //
      {"171103_PMA_TK_media_06.mzML", "nd", "media"}};

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

  /**
   * The same metadata sheet, saved once with every format that excel offers in its save as dialog.
   * The content is plain ascii, what differs is the encoding, the separator and the line ending,
   * which is a lone carriage return in the macintosh formats.
   *
   * @param savedAs    how the file was made, the entry in the excel save as dialog
   * @param lineEnding the line ending excel wrote, git would normalize it without .gitattributes
   */
  private record ExcelExport(String file, Charset charset, char separator, String lineEnding,
                             String savedAs) {

    @Override
    public String toString() {
      return "%s (%s)".formatted(file, savedAs);
    }
  }

  private static Stream<ExcelExport> excelExports() {
    // the ascii files are written as windows-1252 by excel, but plain ascii is valid utf-8 and is
    // decoded identically, so the detection reports utf-8
    final Charset utf8 = StandardCharsets.UTF_8;
    return Stream.of( //
        new ExcelExport("micometa_csv.csv", utf8, ',', "\r\n", "CSV (comma delimited)"), //
        new ExcelExport("micometa_doscsv.csv", utf8, ',', "\r\n", "CSV (MS-DOS)"), //
        new ExcelExport("micometa_maccsv.csv", utf8, ',', "\r", "CSV (Macintosh)"), //
        new ExcelExport("micometa_utf8.csv", utf8, ',', "\r\n", "CSV UTF-8"), //
        new ExcelExport("micometa_tab.txt", utf8, '\t', "\r\n", "Text (tab delimited)"), //
        new ExcelExport("micometa_dos.txt", utf8, '\t', "\r\n", "Text (MS-DOS)"), //
        new ExcelExport("micometa_mac.txt", utf8, '\t', "\r", "Text (Macintosh)"), //
        new ExcelExport("micometa_unicode.txt", StandardCharsets.UTF_16LE, '\t', "\r\n",
            "Unicode Text"), //
        // the extension promises a comma, the detection has to win over it
        new ExcelExport("micometa_semicolon.csv", utf8, ';', "\r\n",
            "CSV (comma delimited) of a european excel"), //
        new ExcelExport("micometa_faketab.csv", utf8, '\t', "\r\n",
            "Text (tab delimited), then renamed to .csv"));
  }

  @ParameterizedTest
  @MethodSource("excelExports")
  void testExcelExportStillHasItsLineEndings(ExcelExport expected) throws IOException {
    final String text = Files.readString(resource(expected.file()).toPath(), expected.charset());
    // the last line is terminated differently by some of the formats, e.g. the macintosh csv
    // separates its rows with a carriage return but ends the file with CRLF. Only the separators
    // between the rows are checked
    final String rows = text.stripTrailing();
    final String rest = rows.replace(expected.lineEnding(), "");

    assertTrue(rows.contains(expected.lineEnding()),
        "%s does not separate its rows with %s any more. Did git normalize it?".formatted(
            expected.file(),
            HexFormat.of().formatHex(expected.lineEnding().getBytes(StandardCharsets.US_ASCII))));
    assertFalse(rest.contains("\r") || rest.contains("\n"),
        "%s mixes line endings now. Did git normalize it?".formatted(expected.file()));
  }

  @ParameterizedTest
  @MethodSource("excelExports")
  void testDetectCharsetOfExcelExport(ExcelExport expected) {
    assertEquals(expected.charset(), CharsetUtils.detectCharset(resource(expected.file())));
  }

  @ParameterizedTest
  @MethodSource("excelExports")
  void testDetectSeparatorOfExcelExport(ExcelExport expected) {
    assertEquals(expected.separator(),
        CSVParsingUtils.autoDetermineSeparator(resource(expected.file())));
  }

  @ParameterizedTest
  @MethodSource("excelExports")
  void testReadExcelExportWithAutoDetection(ExcelExport expected) throws IOException, CsvException {
    assertRowsAreTheMetadataSheet(expected.file(),
        CSVParsingUtils.readDataAutoSeparator(resource(expected.file())));
  }

  @ParameterizedTest
  @MethodSource("excelExports")
  void testReadExcelExportWithCsvReader(ExcelExport expected) throws IOException, CsvException {
    // the streaming reader used by the importers, once with auto detection and once with the
    // separator defined in the parameters
    final FieldSeparator defined = FieldSeparator.parse(String.valueOf(expected.separator()));

    for (FieldSeparator separator : List.of(FieldSeparator.AUTO, defined)) {
      final List<String[]> rows = new ArrayList<>();
      try (CSVReader reader = CSVParsingUtils.createDefaultReader(resource(expected.file()),
          separator)) {
        String[] row;
        while ((row = reader.readNext()) != null) {
          rows.add(row);
        }
      }
      assertRowsAreTheMetadataSheet("%s (%s)".formatted(expected.file(), separator), rows);
    }
  }

  @Test
  void testDetectedSeparatorWinsOverTheFileExtension() {
    // both files are named .csv but are not comma separated. Without detection they would be read
    // as a single column, because the extension is all that is left to go by
    for (String name : List.of("micometa_semicolon.csv", "micometa_faketab.csv")) {
      final File file = resource(name);

      assertEquals(',', CSVParsingUtils.defaultSeparatorForExtension(file), name);
      assertNotEquals(',', CSVParsingUtils.autoDetermineSeparator(file), name);
    }
  }

  private static void assertRowsAreTheMetadataSheet(String file, List<String[]> rows) {
    assertEquals(EXPECTED_METADATA_SHEET.length, rows.size(), file);
    for (int i = 0; i < EXPECTED_METADATA_SHEET.length; i++) {
      assertArrayEquals(EXPECTED_METADATA_SHEET[i], rows.get(i), "row %d of %s".formatted(i, file));
    }
  }
}
