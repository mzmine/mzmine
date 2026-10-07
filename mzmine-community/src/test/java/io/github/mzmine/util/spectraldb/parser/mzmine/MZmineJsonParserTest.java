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

package io.github.mzmine.util.spectraldb.parser.mzmine;

import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.parser.MZmineJsonParser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MZmineJsonParserTest {

  private static final File file = new File(
      MZmineJsonParserTest.class.getClassLoader().getResource("json/mzmine.json").getFile());

  @Test
  void testParse() throws IOException {
    final SpectralLibrary library = new SpectralLibrary(null, file);

    final List<SpectralLibraryEntry> entries = new ArrayList<>();
    final boolean parsed = new MZmineJsonParser(0,
        (newList, alreadyProcessed) -> entries.addAll(newList), true).parse(null, file, library);

    Assertions.assertTrue(parsed);
    Assertions.assertEquals(53, entries.size());

    // covers the different json value types: string, int, double, float, array, nested object
    final SpectralLibraryEntry first = entries.getFirst();
    Assertions.assertEquals(
        "N-cyclopropyl-4-(5,6,7,8-tetrahydroquinazolin-4-yl)morpholine-2-carboxamide",
        first.<String>getOrElse(DBEntryField.NAME, null));
    Assertions.assertEquals("C16H22N4O2", first.<String>getOrElse(DBEntryField.FORMULA, null));
    Assertions.assertEquals(Integer.valueOf(2),
        first.<Integer>getOrElse(DBEntryField.MS_LEVEL, null));
    Assertions.assertEquals(Integer.valueOf(1),
        first.<Integer>getOrElse(DBEntryField.CHARGE, null));
    Assertions.assertEquals(303.181552, first.getAsDouble(DBEntryField.PRECURSOR_MZ).orElseThrow(),
        1e-9);
    Assertions.assertEquals(1.7176243f, first.getAsFloat(DBEntryField.RT).orElseThrow(), 1e-6f);
    Assertions.assertEquals(List.of(20.0f, 60.0f, 40.0f),
        first.<Object>getOrElse(DBEntryField.COLLISION_ENERGY, null));
    Assertions.assertEquals(202, first.getNumberOfDataPoints());
    Assertions.assertEquals(40.525691, first.getMzValue(0), 1e-9);

    // every entry must carry a spectrum
    for (final SpectralLibraryEntry entry : entries) {
      Assertions.assertTrue(entry.getNumberOfDataPoints() > 0);
    }
  }
}
