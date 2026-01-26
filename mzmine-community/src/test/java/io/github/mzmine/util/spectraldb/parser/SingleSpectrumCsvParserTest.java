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

package io.github.mzmine.util.spectraldb.parser;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mzmine.datamodel.MassList;
import java.io.File;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;

class SingleSpectrumCsvParserTest {

  @Test
  void readFile() throws URISyntaxException {
    final File file = new File(
        getClass().getClassLoader().getResource("rawdatafiles/single_scan.tsv").toURI());
    final MassList scan = SingleSpectrumCsvParser.readFile(file);
    assertNotNull(scan);
    assertEquals(632, scan.getNumberOfDataPoints());
    assertEquals(478, scan.getBasePeakIndex());
    assertEquals(1.1485100298027015E8, scan.getTIC(), 0.000001);
  }
}