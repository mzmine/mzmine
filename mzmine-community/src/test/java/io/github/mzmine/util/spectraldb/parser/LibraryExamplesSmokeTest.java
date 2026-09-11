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

import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.parser.gnps.GNPSJsonParser;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every example library that ships with the tests has to be detected as the right format and to
 * yield entries with signals. Guards the whole set at once so a new example, or a change to the
 * format detection, cannot silently stop working.
 */
class LibraryExamplesSmokeTest {

  private static final String PARSER_DIR = "io/github/mzmine/util/spectraldb/parser/";

  private static Stream<Arguments> examples() {
    return Stream.of(
        // mgf
        Arguments.of(PARSER_DIR + "gnps2.mgf", GnpsMgfParser.class, 2),
        Arguments.of(PARSER_DIR + "gnps_clean_different.mgf", GnpsMgfParser.class, 2),
        Arguments.of(PARSER_DIR + "gnps_cleaned.mgf", GnpsMgfParser.class, 3),
        Arguments.of(PARSER_DIR + "gnps_raw.mgf", GnpsMgfParser.class, 3),
        Arguments.of(PARSER_DIR + "matchms_cleaned.mgf", GnpsMgfParser.class, 3),
        Arguments.of(PARSER_DIR + "mzmine.mgf", GnpsMgfParser.class, 3),
        Arguments.of(PARSER_DIR + "mzmine_propranolol.mgf", GnpsMgfParser.class, 8),
        // msp
        Arguments.of(PARSER_DIR + "gnps_raw.msp", NistMspParser.class, 2),
        Arguments.of(PARSER_DIR + "massbank.msp_NIST", NistMspParser.class, 3),
        Arguments.of(PARSER_DIR + "massbank.msp_RIKEN", NistMspParser.class, 3),
        Arguments.of(PARSER_DIR + "massbank_eu.msp_NIST", NistMspParser.class, 12),
        Arguments.of(PARSER_DIR + "massbank_eu.msp_RIKEN", NistMspParser.class, 7),
        Arguments.of(PARSER_DIR + "mona.msp", NistMspParser.class, 3),
        Arguments.of(PARSER_DIR + "mzmine.msp", NistMspParser.class, 2),
        Arguments.of(PARSER_DIR + "riken.msp", NistMspParser.class, 3),
        Arguments.of("spectral_libraries/fraghub.msp", NistMspParser.class, 1),
        // json
        Arguments.of("json/gnps.json", GNPSJsonParser.class, 4),
        Arguments.of("json/GNPS-FAULKNERLEGACY.json", GNPSJsonParser.class, 127),
        Arguments.of("json/gnps_cleaned.json", GNPSJsonParser.class, 7),
        Arguments.of("json/gnps2.json", GNPSJsonParser.class, 3),
        Arguments.of("json/mzmine.json", MZmineJsonParser.class, 53),
        Arguments.of("spectral_libraries/integration_tests/MoNA-export-LC-MS-MS_Spectra.json",
            MonaJsonParser.class, 2),
        // libraries the integration test batches import, mzmine json wrapped in a json array
        Arguments.of("spectral_libraries/integration_tests/GC_HRMS_Archeology.json",
            MZmineJsonParser.class, 2),
        Arguments.of("spectral_libraries/integration_tests/lib_to_flist.json",
            MZmineJsonParser.class, 3));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("examples")
  void testExampleLibrary(final String resource, final Class<?> expectedParser,
      final int expectedEntries) throws Exception {
    final File file = new File(
        LibraryExamplesSmokeTest.class.getClassLoader().getResource(resource).getFile());

    final List<SpectralLibraryEntry> entries = new ArrayList<>();
    final SpectralDBParser parser = SpectralLibraryFormatChecker.getParser(file, 0,
        (l, done) -> entries.addAll(l), false);
    Assertions.assertEquals(expectedParser, parser.getClass(),
        resource + " is detected as the wrong format");

    Assertions.assertTrue(parser.parse(null, file, new SpectralLibrary(null, file)),
        resource + " was not parsed");
    Assertions.assertEquals(expectedEntries, entries.size(), resource + " entry count");

    for (final SpectralLibraryEntry entry : entries) {
      Assertions.assertTrue(entry.getNumberOfDataPoints() > 0,
          resource + " has an entry without signals");
    }
    // progress has to end at the full file for every format
    Assertions.assertEquals(1d, parser.getProgress(), 1e-9, resource + " progress");
  }
}
