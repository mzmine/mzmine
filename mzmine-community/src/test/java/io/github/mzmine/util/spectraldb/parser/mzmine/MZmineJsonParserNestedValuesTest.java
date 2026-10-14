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
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The shipped example only contains arrays of numbers and strings. This covers the remaining value
 * shapes the parser has to hand to {@link DBEntryField#convertValue(String)}: nested objects, and
 * unknown keys with nested values that only have to be skipped.
 */
class MZmineJsonParserNestedValuesTest {

  @TempDir
  Path tempDir;

  private static final String LINE = """
      {"compound_name":"Test compound","precursor_mz":100.5,"charge":2,"ms_level":2,\
      "scan_number":[1,2,3],\
      "collision_energy":[10.0,20.5],\
      "source_scan_usi":["mzspec:A:B:1","mzspec:A:B:2"],\
      "quality":{"purity":0.9,"chimeric":"PASSED"},\
      "unknown_nested":{"a":[1,2,{"b":3}],"c":{"d":[4]}},\
      "smiles":"N/A",\
      "formula":"C6H12O6",\
      "peaks":[[10.0,1.0],[20.0,2.0],[30.0,3.0]]}""";

  @Test
  void testNestedValues() throws IOException {
    final Path file = tempDir.resolve("nested.json");
    Files.writeString(file, LINE);

    final SpectralLibrary library = new SpectralLibrary(null, file.toFile());
    final List<SpectralLibraryEntry> entries = new ArrayList<>();
    Assertions.assertTrue(
        new MZmineJsonParser(0, (l, done) -> entries.addAll(l), true).parse(null, file.toFile(),
            library));
    Assertions.assertEquals(1, entries.size());
    final SpectralLibraryEntry entry = entries.getFirst();

    Assertions.assertEquals("Test compound", entry.<String>getOrElse(DBEntryField.NAME, null));
    Assertions.assertEquals(100.5, entry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElseThrow());

    // array of ints
    Assertions.assertEquals(List.of(1, 2, 3),
        entry.<Object>getOrElse(DBEntryField.SCAN_NUMBER, null));
    // array of numbers into a FloatArrayList
    Assertions.assertEquals(new FloatArrayList(new float[]{10.0f, 20.5f}),
        entry.<Object>getOrElse(DBEntryField.COLLISION_ENERGY, null));
    // array of strings into a List
    Assertions.assertEquals(List.of("mzspec:A:B:1", "mzspec:A:B:2"),
        entry.<Object>getOrElse(DBEntryField.SOURCE_SCAN_USI, null));
    // nested object is kept as its json text
    Assertions.assertEquals("{\"purity\":0.9,\"chimeric\":\"PASSED\"}",
        entry.<String>getOrElse(DBEntryField.QUALITY, null));

    // N/A is dropped
    Assertions.assertNull(entry.<String>getOrElse(DBEntryField.SMILES, null));
    // a field after the unknown nested key still parses, so it was skipped as a whole
    Assertions.assertEquals("C6H12O6", entry.<String>getOrElse(DBEntryField.FORMULA, null));

    Assertions.assertEquals(3, entry.getNumberOfDataPoints());
    Assertions.assertEquals(30.0, entry.getMzValue(2));
    Assertions.assertEquals(3.0, entry.getIntensityValue(2));
  }
}
