/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.io.CSVUtils;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class CSVUtilsTest {

  @Test
  void testCSVEscape() {
    assertEquals("test", CSVUtils.escape("test", ","));
    assertEquals("\"test, with comma\"", CSVUtils.escape("test, with comma", ","));
    assertEquals("\"\"\"test\"\" with quotes\"", CSVUtils.escape("\"test\" with quotes", ","));
    assertEquals("\"test\twith tab\"", CSVUtils.escape("test\twith tab", "\t"));
  }

  @Test
  void testAutoSeparator() {
    Assertions.assertEquals(',', CSVParsingUtils.autoDetermineSeparator(
        new File(getClass().getClassLoader().getResource("csv/comma-separated.txt").getFile())));

    Assertions.assertEquals('\t', CSVParsingUtils.autoDetermineSeparator(
        new File(getClass().getClassLoader().getResource("csv/tab-separated.txt").getFile())));

    Assertions.assertEquals(',', CSVParsingUtils.autoDetermineSeparatorDefaultFallback(new File(
        getClass().getClassLoader().getResource("csv/comma-separated-fallback.csv").getFile())));

    Assertions.assertEquals('\t', CSVParsingUtils.autoDetermineSeparatorDefaultFallback(new File(
        getClass().getClassLoader().getResource("csv/tab-separated-fallback.tsv").getFile())));

    Assertions.assertEquals('\t', CSVParsingUtils.autoDetermineSeparatorDefaultFallback(new File(
        getClass().getClassLoader().getResource("csv/tab-separated-fallback.txt").getFile())));

    Assertions.assertEquals('\t', CSVParsingUtils.autoDetermineSeparatorDefaultFallback(
        new File(getClass().getClassLoader().getResource("csv/one-line-only.tsv").getFile())));
  }
}
