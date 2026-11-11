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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.opencsv.exceptions.CsvException;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.io.CharsetUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Auto detection of the column separator and of the file encoding in
 * {@link CSVParsingUtils#readData(File, String)}.
 */
public class CsvSeparatorDetectionTest {

  @TempDir
  Path tempDir;

  private File write(String name, String content) throws IOException {
    return write(name, content, StandardCharsets.UTF_8);
  }

  private File write(String name, String content, Charset charset) throws IOException {
    final Path path = tempDir.resolve(name);
    Files.write(path, content.getBytes(charset));
    return path.toFile();
  }

  @Test
  void testSemicolonWithDecimalComma() throws IOException {
    // european excel export: comma splits more columns, but only semicolon splits all lines equally
    final File file = write("european.csv", """
        name;mz;rt
        Caffeine;195,0877;3,2
        Glucose;180,0634;1,1
        """);

    assertEquals(';', CSVParsingUtils.autoDetermineSeparator(file));
  }

  @Test
  void testPipeSeparated() throws IOException {
    final File file = write("pipe.csv", """
        name|mz|rt
        Caffeine|195.0877|3.2
        """);

    assertEquals('|', CSVParsingUtils.autoDetermineSeparator(file));
  }

  @Test
  void testHeaderIsNotTheFirstLine() throws IOException {
    // the first two lines have a different number of columns, the majority of lines decides
    final File file = write("comment.csv", """
        # exported by some tool
        name,mz,rt
        Caffeine,195.0877,3.2
        Glucose,180.0634,1.1
        """);

    assertEquals(',', CSVParsingUtils.autoDetermineSeparator(file));
  }

  @Test
  void testRaggedRows() throws IOException {
    final File file = write("ragged.csv", """
        name,mz,rt
        Caffeine,195.0877,3.2
        Glucose,180.0634
        Sucrose,342.1162,4.7
        """);

    assertEquals(',', CSVParsingUtils.autoDetermineSeparator(file));
  }

  @Test
  void testQuotedFieldsCountAsOneColumn() throws IOException, CsvException {
    // RFC4180: a quoted field may contain the separator, line breaks, and escaped quotes ("")
    final File file = write("quotes.csv", """
        name,description,rt
        a,"b with , comma",c
        "quoted ""name""\","multi
        line field",3.2
        """);

    assertEquals(',', CSVParsingUtils.autoDetermineSeparator(file));

    final List<String[]> rows = CSVParsingUtils.readDataAutoSeparator(file);
    assertEquals(3, rows.size());
    assertArrayEquals(new String[]{"a", "b with , comma", "c"}, rows.get(1));
    assertArrayEquals(new String[]{"quoted \"name\"", "multi\nline field", "3.2"}, rows.get(2));
  }

  @Test
  void testQuotedSeparators() throws IOException {
    // the tabs are inside quoted fields and must not be counted
    final File file = write("quoted.csv", """
        name,description
        Caffeine,"tab\tinside, quoted"
        Glucose,"another\ttab"
        """);

    assertEquals(',', CSVParsingUtils.autoDetermineSeparator(file));
  }

  @Test
  void testSingleColumnFallsBackToExtension() throws IOException {
    final File csv = write("single.csv", """
        name
        Caffeine
        Glucose
        """);

    assertNull(CSVParsingUtils.autoDetermineSeparator(csv));
    assertEquals(',', CSVParsingUtils.autoDetermineSeparatorDefaultFallback(csv));
    assertEquals('\t', CSVParsingUtils.autoDetermineSeparatorDefaultFallback(
        write("single.tsv", "name\nCaffeine\n")));
  }

  @Test
  void testSeparatorDirective() throws IOException, CsvException {
    // excel writes an optional sep=; line in front of the header to declare the separator
    final File file = write("directive.csv", """
        sep=;
        name;mz
        Caffeine,dimer;195.0877
        """);

    assertEquals(';', CSVParsingUtils.autoDetermineSeparator(file));

    // the directive is not data and must not show up as a row
    final List<String[]> rows = CSVParsingUtils.readDataAutoSeparator(file);
    assertEquals(2, rows.size());
    assertArrayEquals(new String[]{"name", "mz"}, rows.getFirst());
    assertArrayEquals(new String[]{"Caffeine,dimer", "195.0877"}, rows.getLast());
  }

  @Test
  void testTabDirective() throws IOException, CsvException {
    final File file = write("directive-tab.txt", "sep=\t\nname\tmz\nCaffeine\t195.0877\n");

    assertEquals('\t', CSVParsingUtils.autoDetermineSeparator(file));
    assertEquals(2, CSVParsingUtils.readDataAutoSeparator(file).size());
  }

  @Test
  void testReadAnyEncoding() throws IOException, CsvException {
    final String content = """
        name,note
        Öl,25 °C
        Glucose,30 µm
        """;
    final String[] expected = new String[]{"\u00d6l", "25 \u00b0C"};

    for (Charset charset : List.of(StandardCharsets.UTF_8, CharsetUtils.LEGACY_FALLBACK,
        StandardCharsets.UTF_16LE, StandardCharsets.UTF_16)) {
      final File file = write("encoding-%s.csv".formatted(charset.name()), content, charset);

      final List<String[]> rows = CSVParsingUtils.readDataAutoSeparator(file);
      assertEquals(3, rows.size(), charset.name());
      assertArrayEquals(expected, rows.get(1), charset.name());
    }
  }

  @Test
  void testUtf8ByteOrderMarkIsNotPartOfTheFirstColumn() throws IOException, CsvException {
    final Path path = tempDir.resolve("bom.csv");
    Files.write(path, "\uFEFFname,mz\nCaffeine,195.0877\n".getBytes(StandardCharsets.UTF_8));

    final List<String[]> rows = CSVParsingUtils.readDataAutoSeparator(path.toFile());
    assertArrayEquals(new String[]{"name", "mz"}, rows.getFirst());
  }

  @Test
  void testAutoAsSeparatorParameter() throws IOException, CsvException {
    final File file = write("auto.csv", """
        name;mz
        Caffeine;195,0877
        """);

    // modules pass the separator parameter through as a string
    for (String separator : new String[]{CSVParsingUtils.AUTO_SEPARATOR, "AUTO", ""}) {
      final List<String[]> rows = CSVParsingUtils.readData(file, separator);
      assertEquals(2, rows.size(), separator);
      assertArrayEquals(new String[]{"Caffeine", "195,0877"}, rows.getLast(), separator);
    }

    // whitespace is a separator and not a request for auto detection
    assertEquals('\t', CSVParsingUtils.toSeparatorChar("\t"));
    assertEquals('\t', CSVParsingUtils.toSeparatorChar("\\t"));
  }

  @Test
  void testUnbalancedQuotesAreRecovered() throws IOException, CsvException {
    // the quote in front of "unterminated is never closed, which is not valid RFC4180
    final File file = write("unbalanced.csv", """
        name,note
        Caffeine,"unterminated
        Glucose,ok
        """);

    final List<String[]> rows = CSVParsingUtils.readData(file, ",");
    assertEquals(3, rows.size());
    assertArrayEquals(new String[]{"Glucose", "ok"}, rows.getLast());
  }

  /**
   * Every candidate separator occurs in the values, but only inside quoted fields. The quotes must
   * hide them from the detection, whichever of them is the real separator.
   */
  @ParameterizedTest
  @ValueSource(chars = {',', ';', '\t', '|'})
  void testAllSeparatorsInsideQuotedValues(char separator) throws IOException, CsvException {
    final String sep = String.valueOf(separator);
    final String value = "a, b; c\td|e";
    final String content = String.join("\n", //
        "name" + sep + "note" + sep + "rt", //
        "Caffeine" + sep + inQuotes(value) + sep + "3.2", //
        "Glucose" + sep + inQuotes(value) + sep + "1.1", //
        "Sucrose" + sep + inQuotes(value) + sep + "4.7") + "\n";

    // named .csv on purpose, the extension must not decide this
    final File file = write("quoted-%d.csv".formatted((int) separator), content);

    assertEquals(separator, CSVParsingUtils.autoDetermineSeparator(file));

    final List<String[]> rows = CSVParsingUtils.readDataAutoSeparator(file);
    assertEquals(4, rows.size());
    assertArrayEquals(new String[]{"Caffeine", value, "3.2"}, rows.get(1));
  }

  @Test
  void testDecoySeparatorOnEveryLine() throws IOException, CsvException {
    // the semicolon is not quoted and splits every line into the same number of columns, just like
    // the tab. Of two equally consistent separators the one with more columns wins
    final File file = write("decoy.csv", """
        name\tnote; unit\trt
        Caffeine\t25 °C; dry\t3.2
        Glucose\t30 °C; wet\t1.1
        """);

    assertEquals('\t', CSVParsingUtils.autoDetermineSeparator(file));

    final List<String[]> rows = CSVParsingUtils.readDataAutoSeparator(file);
    assertArrayEquals(new String[]{"Caffeine", "25 °C; dry", "3.2"}, rows.get(1));
  }

  @Test
  void testQuotedValuesHideAMoreFrequentSeparator() throws IOException, CsvException {
    // the values contain more commas than the file has semicolons, but all of them are quoted, so
    // the comma does not split anything and is not even a candidate
    final File file = write("hidden.csv", """
        name;note
        Caffeine;"1,2,3,4,5"
        Glucose;"6,7,8,9,0"
        """);

    assertEquals(';', CSVParsingUtils.autoDetermineSeparator(file));

    final List<String[]> rows = CSVParsingUtils.readDataAutoSeparator(file);
    assertArrayEquals(new String[]{"Caffeine", "1,2,3,4,5"}, rows.get(1));
  }

  /**
   * @return the value in quotes, as a tool would write a field that contains a separator
   */
  private static String inQuotes(final String value) {
    return "\"" + value + "\"";
  }
}
