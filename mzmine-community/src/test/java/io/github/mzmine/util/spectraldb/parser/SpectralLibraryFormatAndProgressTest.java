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

import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.parser.gnps.GNPSJsonParser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Format detection reads the head of a json library to pick the parser, and every json parser
 * reports progress from the bytes it consumed rather than from a line count.
 */
class SpectralLibraryFormatAndProgressTest {

  private static final String MONA = "spectral_libraries/integration_tests/MoNA-export-LC-MS-MS_Spectra.json";
  private static final String MSP_NIST = "io/github/mzmine/util/spectraldb/parser/massbank_eu.msp_NIST";
  private static final String MSP_RIKEN = "io/github/mzmine/util/spectraldb/parser/massbank_eu.msp_RIKEN";
  private static final String MGF = "io/github/mzmine/util/spectraldb/parser/mzmine_propranolol.mgf";

  @TempDir
  Path tempDir;

  private static File resource(final String name) {
    return new File(
        SpectralLibraryFormatAndProgressTest.class.getClassLoader().getResource(name).getFile());
  }

  private static SpectralDBParser parserFor(final File file) throws Exception {
    return SpectralLibraryFormatChecker.getParser(file, 0, (l, done) -> {
    }, false);
  }

  @Test
  void testDetectsEveryJsonFlavour() throws Exception {
    Assertions.assertInstanceOf(GNPSJsonParser.class, parserFor(resource("json/gnps.json")));
    Assertions.assertInstanceOf(GNPSJsonParser.class,
        parserFor(resource("json/GNPS-FAULKNERLEGACY.json")));
    Assertions.assertInstanceOf(GNPSJsonParser.class,
        parserFor(resource("json/gnps_cleaned.json")));
    Assertions.assertInstanceOf(MonaJsonParser.class, parserFor(resource(MONA)));
    Assertions.assertInstanceOf(MZmineJsonParser.class, parserFor(resource("json/mzmine.json")));
    Assertions.assertInstanceOf(NistMspParser.class,
        parserFor(resource("spectral_libraries/fraghub.msp")));
    Assertions.assertInstanceOf(NistMspParser.class, parserFor(resource(MSP_NIST)));
    Assertions.assertInstanceOf(NistMspParser.class, parserFor(resource(MSP_RIKEN)));
    Assertions.assertInstanceOf(GnpsMgfParser.class, parserFor(resource(MGF)));
  }

  /**
   * The marker can sit far into the first entry when it carries long structure fields. Detection
   * used to look at the first 4 kB only and would fall back to the mzmine parser here.
   */
  @Test
  void testDetectsMarkerFarIntoTheFirstEntry() throws Exception {
    final String filler = "A".repeat(200_000);
    final Path file = tempDir.resolve("late_marker.json");
    Files.writeString(file, """
        [{"spectrum_id": "CCMSLIB00000001547", "Compound_Name": "test", "Smiles": "%s",\
        "Precursor_MZ": "100.5", "Charge": "1",\
        "peaks_json": "[[100.0,1.0],[200.0,2.0]]"}]""".formatted(filler));

    Assertions.assertTrue(Files.readString(file).indexOf("peaks_json") > 4048,
        "test file does not actually push the marker past the old detection window");
    Assertions.assertInstanceOf(GNPSJsonParser.class, parserFor(file.toFile()));
  }

  @Test
  void testEmptyFileFallsBackWithoutThrowing() throws Exception {
    final Path file = tempDir.resolve("empty.json");
    Files.writeString(file, "");
    Assertions.assertInstanceOf(MZmineJsonParser.class, parserFor(file.toFile()));
  }

  /**
   * Byte progress has to end at 1 and has to advance while parsing, for every json parser.
   */
  @Test
  void testByteProgress() throws Exception {
    assertProgressRunsToOne(resource("json/GNPS-FAULKNERLEGACY.json"), true);
    assertProgressRunsToOne(resource("json/gnps_cleaned.json"), true);
    assertProgressRunsToOne(resource("json/mzmine.json"), true);
    // the shipped MoNA file has two entries, both consumed by the format check before any entry
    // is handed over, so there is nothing to sample in between
    assertProgressRunsToOne(resource(MONA), false);
    // text formats, previously line count based. A file below the 8 kB the decoder pulls at once
    // is fully read before the first entry is handed over, so it can only report 0 and then 1
    assertProgressRunsToOne(resource("spectral_libraries/fraghub.msp"), false);
    assertProgressRunsToOne(resource(MSP_RIKEN), false);
    assertProgressRunsToOne(resource(MSP_NIST), true);
    assertProgressRunsToOne(resource(MGF), true);
  }

  /**
   * MoNA parses its lines in parallel batches. This runs enough entries through it to cross the
   * batch size several times, which the two entry file in the resources cannot do.
   */
  @Test
  void testMonaParsesManyEntriesInOrder() throws Exception {
    final List<String> sourceLines = Files.readAllLines(resource(MONA).toPath()).stream()
        .filter(line -> line.length() > 2).toList();
    Assertions.assertEquals(2, sourceLines.size(), "unexpected MoNA test resource");

    final int repeats = 550;
    final Path file = tempDir.resolve("many.json");
    try (var writer = Files.newBufferedWriter(file)) {
      writer.write("[");
      writer.newLine();
      for (int i = 0; i < repeats; i++) {
        for (final String line : sourceLines) {
          writer.write(line);
          writer.newLine();
        }
      }
    }

    final List<SpectralLibraryEntry> entries = new ArrayList<>();
    final List<Double> sampled = new ArrayList<>();
    final SpectralDBParser[] self = new SpectralDBParser[1];
    final SpectralDBParser parser = SpectralLibraryFormatChecker.getParser(file.toFile(), 1,
        (l, done) -> {
          entries.addAll(l);
          if (self[0] != null) {
            sampled.add(self[0].getProgress());
          }
        }, false);
    self[0] = parser;
    Assertions.assertInstanceOf(MonaJsonParser.class, parser);
    Assertions.assertTrue(
        parser.parse(null, file.toFile(), new SpectralLibrary(null, file.toFile())));

    Assertions.assertEquals(repeats * 2, entries.size());
    Assertions.assertEquals(1d, parser.getProgress(), 1e-9);
    Assertions.assertTrue(sampled.stream().anyMatch(p -> p > 0 && p < 1),
        "progress never reported a partial value while batching");

    // batches are parsed in parallel but must be handed over in file order
    final String firstName = entries.getFirst().getOrElse(DBEntryField.NAME, "");
    final String secondName = entries.get(1).getOrElse(DBEntryField.NAME, "");
    Assertions.assertNotEquals(firstName, secondName);
    for (int i = 0; i < entries.size(); i++) {
      Assertions.assertEquals(i % 2 == 0 ? firstName : secondName,
          entries.get(i).getOrElse(DBEntryField.NAME, ""), "entry " + i + " is out of order");
    }
  }

  private void assertProgressRunsToOne(final File file, final boolean expectPartialProgress)
      throws Exception {
    final List<SpectralLibraryEntry> entries = new ArrayList<>();
    final List<Double> sampled = new ArrayList<>();
    final SpectralDBParser[] self = new SpectralDBParser[1];
    // buffer of 1 so the processor is called per entry and can sample the progress
    final SpectralDBParser parser = SpectralLibraryFormatChecker.getParser(file, 1, (l, done) -> {
      entries.addAll(l);
      if (self[0] != null) {
        sampled.add(self[0].getProgress());
      }
    }, false);
    self[0] = parser;

    Assertions.assertEquals(0d, parser.getProgress(), "progress before parsing " + file.getName());
    Assertions.assertTrue(parser.parse(null, file, new SpectralLibrary(null, file)));
    Assertions.assertFalse(entries.isEmpty());

    Assertions.assertEquals(1d, parser.getProgress(), 1e-9,
        "progress after parsing " + file.getName());
    double previous = 0;
    for (final double progress : sampled) {
      Assertions.assertTrue(progress >= 0 && progress <= 1,
          "progress out of range in " + file.getName() + ": " + progress);
      Assertions.assertTrue(progress >= previous,
          "progress went backwards in " + file.getName() + ": " + previous + " -> " + progress);
      previous = progress;
    }
    if (expectPartialProgress) {
      Assertions.assertTrue(sampled.stream().anyMatch(p -> p > 0 && p < 1),
          "progress never reported a partial value for " + file.getName());
    }
  }
}
