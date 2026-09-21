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

package io.github.mzmine.modules.io.spectraldbsubmit.formats;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizer;
import io.github.mzmine.util.io.JsonUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntryFactory;
import io.github.mzmine.util.spectraldb.parser.SpectralDBParser;
import io.github.mzmine.util.spectraldb.parser.SpectralLibraryFormatChecker;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The generator writes what {@link io.github.mzmine.util.spectraldb.parser.MZmineJsonParser} reads,
 * so exporting a library and reading it back has to preserve the entries.
 */
class MZmineJsonGeneratorTest {

  private static List<SpectralLibraryEntry> read(final File file) throws Exception {
    final List<SpectralLibraryEntry> entries = new ArrayList<>();
    final SpectralDBParser parser = SpectralLibraryFormatChecker.getParser(file, 0,
        (l, done) -> entries.addAll(l), false);
    parser.parse(null, file, new SpectralLibrary(null, file));
    return entries;
  }

  private static List<SpectralLibraryEntry> read(final String resource) throws Exception {
    return read(
        new File(MZmineJsonGeneratorTest.class.getClassLoader().getResource(resource).getFile()));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"json/mzmine.json", "json/gnps.json", "json/gnps2.json",
      "json/gnps_cleaned.json", "io/github/mzmine/util/spectraldb/parser/massbank_eu.msp_NIST",
      "io/github/mzmine/util/spectraldb/parser/mzmine_propranolol.mgf",
      "io/github/mzmine/util/spectraldb/parser/riken.msp"})
  void testRoundTripKeepsSignalsAndMetadata(final String resource) throws Exception {
    final List<SpectralLibraryEntry> entries = read(resource);
    Assertions.assertFalse(entries.isEmpty(), resource + " has no entries");

    final IntensityNormalizer normalizer = IntensityNormalizer.createDefault();
    final File exported = File.createTempFile("mzmine_json_generator", ".json");
    exported.deleteOnExit();
    try (var writer = java.nio.file.Files.newBufferedWriter(exported.toPath())) {
      for (final SpectralLibraryEntry entry : entries) {
        writer.write(MZmineJsonGenerator.generateJSON(entry, normalizer));
        writer.write("\n");
      }
    }

    final List<SpectralLibraryEntry> reread = read(exported);
    Assertions.assertEquals(entries.size(), reread.size(), "entry count changed");

    for (int i = 0; i < entries.size(); i++) {
      final SpectralLibraryEntry before = entries.get(i);
      final SpectralLibraryEntry after = reread.get(i);
      final String at = resource + " entry " + i;

      Assertions.assertEquals(before.getNumberOfDataPoints(), after.getNumberOfDataPoints(),
          at + " signal count");
      for (int s = 0; s < before.getNumberOfDataPoints(); s++) {
        // the mz is rounded to 6 digits on export
        Assertions.assertEquals(before.getMzValue(s), after.getMzValue(s), 1e-6, at + " mz " + s);
        Assertions.assertEquals(before.getIntensityValue(s), after.getIntensityValue(s), 0d,
            at + " intensity " + s);
      }

      // a float field has to survive as the same float, not as the wider double
      assertSameField(before, after, DBEntryField.RT, at);
      assertSameField(before, after, DBEntryField.CCS, at);
      assertSameField(before, after, DBEntryField.PRECURSOR_MZ, at);
      assertSameField(before, after, DBEntryField.CHARGE, at);
      assertSameField(before, after, DBEntryField.FORMULA, at);
      assertSameField(before, after, DBEntryField.INCHIKEY, at);
      assertSameField(before, after, DBEntryField.NAME, at);
      assertSameField(before, after, DBEntryField.COLLISION_ENERGY, at);
      Assertions.assertEquals(before.getPolarity(), after.getPolarity(), at + " polarity");
    }
  }

  private static void assertSameField(final SpectralLibraryEntry before,
      final SpectralLibraryEntry after, final DBEntryField field, final String at) {
    Assertions.assertEquals(before.getField(field).orElse(null), after.getField(field).orElse(null),
        at + " " + field);
  }

  /**
   * m/z is rounded to 6 digits on export, which must not cut high masses.
   */
  @Test
  void testHighMzIsNotTruncated() throws Exception {
    final SpectralLibraryEntry entry = SpectralLibraryEntryFactory.create(null,
        Map.of(DBEntryField.PRECURSOR_MZ, 2500.5),
        new double[]{100.123456789, 2500.987654321, 5000.5}, new double[]{1, 2, 3});

    final String json = MZmineJsonGenerator.generateJSON(entry,
        IntensityNormalizer.createDefault());
    Assertions.assertTrue(json.contains("2500.987654"), "high m/z was cut: " + json);
    Assertions.assertTrue(json.contains("5000.5"), "high m/z was cut: " + json);
  }

  /**
   * The entry carries some of the exported keys itself, so they must not end up in the object
   * twice.
   */
  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"json/mzmine.json", "json/gnps_cleaned.json",
      "io/github/mzmine/util/spectraldb/parser/mzmine_propranolol.mgf"})
  void testNoDuplicateKeys(final String resource) throws Exception {
    for (final SpectralLibraryEntry entry : read(resource)) {
      final String json = MZmineJsonGenerator.generateJSON(entry,
          IntensityNormalizer.createDefault());
      final Set<String> keys = new HashSet<>();
      try (JsonParser p = JsonUtils.FACTORY.createParser(json)) {
        Assertions.assertEquals(JsonToken.START_OBJECT, p.nextToken());
        while (p.nextToken() == JsonToken.FIELD_NAME) {
          final String name = p.currentName();
          Assertions.assertTrue(keys.add(name), resource + " writes " + name + " twice");
          p.nextToken();
          p.skipChildren();
        }
      }
      // written from the spectrum, not from the stored field
      Assertions.assertTrue(keys.contains(DBEntryField.NUM_PEAKS.getMZmineJsonID()));
      Assertions.assertTrue(keys.contains("peaks"));
    }
  }
}
