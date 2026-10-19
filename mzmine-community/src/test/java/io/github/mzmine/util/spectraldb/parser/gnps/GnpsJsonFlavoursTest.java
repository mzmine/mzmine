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

package io.github.mzmine.util.spectraldb.parser.gnps;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import io.github.mzmine.util.spectraldb.parser.SpectralDBParser;
import io.github.mzmine.util.spectraldb.parser.SpectralLibraryFormatChecker;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * GNPS publishes three json flavours. The cleaned libraries rename keys, write every value as a
 * string and store the signals as python tuples instead of json arrays. GNPS2 moves the metadata
 * into a nested object with mgf style keys and writes the signals as a real json array. All go
 * through the same parser, which is picked by the format checker, so these parse the way an import
 * does.
 */
class GnpsJsonFlavoursTest {

  private static File resource(final String name) {
    return new File(GnpsJsonFlavoursTest.class.getClassLoader().getResource(name).getFile());
  }

  /**
   * Parses through the format checker, so every case here also asserts that the flavour is detected
   * and routed to this parser.
   */
  private static List<SpectralLibraryEntry> parse(final String resource) throws Exception {
    final File file = resource(resource);
    final List<SpectralLibraryEntry> entries = new ArrayList<>();
    final SpectralDBParser parser = SpectralLibraryFormatChecker.getParser(file, 0,
        (l, done) -> entries.addAll(l), true);
    Assertions.assertInstanceOf(GNPSJsonParser.class, parser,
        resource + " is not detected as a GNPS json library");
    Assertions.assertTrue(parser.parse(null, file, new SpectralLibrary(null, file)));
    return entries;
  }

  @Test
  void testClassicExport() throws Exception {
    final List<SpectralLibraryEntry> entries = parse("json/gnps.json");
    Assertions.assertEquals(4, entries.size());

    final SpectralLibraryEntry first = entries.getFirst();
    Assertions.assertEquals("CCMSLIB00000081017",
        first.<String>getOrElse(DBEntryField.ENTRY_ID, null));
    Assertions.assertEquals("B03A18", first.<String>getOrElse(DBEntryField.NAME, null));
    Assertions.assertEquals(438.324, first.getAsDouble(DBEntryField.PRECURSOR_MZ).orElseThrow());
    Assertions.assertEquals(Integer.valueOf(1),
        first.<Integer>getOrElse(DBEntryField.CHARGE, null));
    Assertions.assertEquals(Integer.valueOf(2),
        first.<Integer>getOrElse(DBEntryField.MS_LEVEL, null));
    Assertions.assertEquals("qTof", first.<String>getOrElse(DBEntryField.INSTRUMENT_TYPE, null));
    Assertions.assertEquals(73, first.getNumberOfDataPoints());
    Assertions.assertEquals(105.068466, first.getMzValue(0));
    Assertions.assertEquals(458.062897, first.getIntensityValue(0));

    // the classic export writes the placeholder "N/A" into INCHI_AUX and "None" as the collision
    // energy, neither of which is a value
    Assertions.assertNull(first.<String>getOrElse(DBEntryField.INCHIKEY, null));
    Assertions.assertNull(first.<String>getOrElse(DBEntryField.COLLISION_ENERGY, null));
  }

  @Test
  void testClassicExportUsesInchiKeyFromSmiles() throws Exception {
    // INCHI_AUX is always "N/A" in this flavour, the usable key sits in InChIKey_smiles
    final String expected = "QPTVAYUFYIWGFW-QRDIJWGLSA-N";
    final boolean found = parse("json/GNPS-FAULKNERLEGACY.json").stream()
        .anyMatch(e -> expected.equals(e.getOrElse(DBEntryField.INCHIKEY, null)));
    Assertions.assertTrue(found, "no entry picked up the InChIKey from InChIKey_smiles");
  }

  @Test
  void testCleanedLibrary() throws Exception {
    final List<SpectralLibraryEntry> entries = parse("json/gnps_cleaned.json");
    Assertions.assertEquals(7, entries.size());
    // 1128 signals in the file, 5 of them zero intensity and dropped by
    // SpectralDBParser.addLibraryEntry for every format
    Assertions.assertEquals(1123,
        entries.stream().mapToInt(SpectralLibraryEntry::getNumberOfDataPoints).sum());

    final SpectralLibraryEntry first = entries.getFirst();
    Assertions.assertEquals("CCMSLIB00000001547",
        first.<String>getOrElse(DBEntryField.ENTRY_ID, null));
    Assertions.assertEquals("3-Des-Microcystein_LR",
        first.<String>getOrElse(DBEntryField.NAME, null));
    Assertions.assertEquals("[M+H]1+", first.<String>getOrElse(DBEntryField.ION_TYPE, null));
    Assertions.assertEquals(981.54, first.getAsDouble(DBEntryField.PRECURSOR_MZ).orElseThrow());
    Assertions.assertEquals(980.533117744,
        first.getAsDouble(DBEntryField.EXACT_MASS).orElseThrow());
    // written as "1.0" in this flavour
    Assertions.assertEquals(Integer.valueOf(1),
        first.<Integer>getOrElse(DBEntryField.CHARGE, null));

    // renamed instrument keys
    Assertions.assertEquals("qtof", first.<String>getOrElse(DBEntryField.INSTRUMENT_TYPE, null));
    Assertions.assertEquals("ESI", first.<String>getOrElse(DBEntryField.ION_SOURCE, null));
    // InChIKey_smiles is the only key here
    Assertions.assertEquals("UYJGHPVHCMVZPP-UHFFFAOYSA-N",
        first.<String>getOrElse(DBEntryField.INCHIKEY, null));
    // compound classes only this flavour carries
    Assertions.assertEquals("Organic acids and derivatives",
        first.<String>getOrElse(DBEntryField.CLASSYFIRE_SUPERCLASS, null));
    Assertions.assertEquals("Oligopeptides",
        first.<String>getOrElse(DBEntryField.CLASSYFIRE_PARENT, null));
    // "nan" is a placeholder, not a value
    Assertions.assertNull(first.<String>getOrElse(DBEntryField.FRAGMENTATION_METHOD, null));
    Assertions.assertNull(first.<String>getOrElse(DBEntryField.COLLISION_ENERGY, null));

    // signals come from python tuples, "[(mz, intensity), ...]"
    Assertions.assertEquals(218, first.getNumberOfDataPoints());
    Assertions.assertEquals(289.286377, first.getMzValue(0));
    Assertions.assertEquals(8068.0, first.getIntensityValue(0));
    Assertions.assertEquals(982.221924, first.getMzValue(217));
    Assertions.assertEquals(27147.0, first.getIntensityValue(217));

    // 336 signals in the file, one of them zero intensity
    final SpectralLibraryEntry second = entries.get(1);
    Assertions.assertEquals(335, second.getNumberOfDataPoints());
    Assertions.assertEquals(278.049927, second.getMzValue(0));
    Assertions.assertEquals(940.88324, second.getMzValue(334));
    Assertions.assertEquals(2.0, second.getIntensityValue(334));

    // an entry whose structure fields are all "nan" must not store those words
    for (final SpectralLibraryEntry entry : entries) {
      Assertions.assertNotEquals("nan", entry.getOrElse(DBEntryField.SMILES, null));
      Assertions.assertNotEquals("nan", entry.getOrElse(DBEntryField.INCHI, null));
      Assertions.assertNotEquals("nan", entry.getOrElse(DBEntryField.INCHIKEY, null));
      Assertions.assertTrue(entry.getNumberOfDataPoints() > 0);
    }
  }

  /**
   * GNPS2 keeps the metadata in a nested object under mgf style keys and the signals in a plain
   * json array, so it needs its own entry mapping but goes through the same parser.
   */
  @Test
  void testGnps2Library() throws Exception {
    final List<SpectralLibraryEntry> entries = parse("json/gnps2.json");
    Assertions.assertEquals(3, entries.size());
    Assertions.assertEquals(150,
        entries.stream().mapToInt(SpectralLibraryEntry::getNumberOfDataPoints).sum());

    final SpectralLibraryEntry first = entries.getFirst();
    Assertions.assertEquals("10(11)-EpDPE", first.<String>getOrElse(DBEntryField.NAME, null));
    // SYS_NAME, only known to this flavour
    Assertions.assertEquals("10(11)-epoxydocosapentaenoic acid",
        first.<String>getOrElse(DBEntryField.IUPAC_NAME, null));
    // mgf style keys: PEPMASS, INCHIAUX, MSLEVEL, ADDUCT, IONMODE, SCANS
    Assertions.assertEquals(343.2279, first.getAsDouble(DBEntryField.PRECURSOR_MZ).orElseThrow());
    Assertions.assertEquals(344.235176467,
        first.getAsDouble(DBEntryField.EXACT_MASS).orElseThrow());
    Assertions.assertEquals("YYZNJWZRJUGQCW-UQZHZJRSSA-N",
        first.<String>getOrElse(DBEntryField.INCHIKEY, null));
    Assertions.assertEquals(Integer.valueOf(2),
        first.<Integer>getOrElse(DBEntryField.MS_LEVEL, null));
    Assertions.assertEquals(Integer.valueOf(1),
        first.<Integer>getOrElse(DBEntryField.CHARGE, null));
    Assertions.assertEquals(Integer.valueOf(1),
        first.<Integer>getOrElse(DBEntryField.SCAN_NUMBER, null));
    Assertions.assertEquals("[M-H]-", first.<String>getOrElse(DBEntryField.ION_TYPE, null));
    Assertions.assertEquals("C22H32O3", first.<String>getOrElse(DBEntryField.FORMULA, null));
    Assertions.assertEquals("ESI(-)-QTOF Agilent 6546",
        first.<String>getOrElse(DBEntryField.INSTRUMENT, null));
    Assertions.assertEquals("splash10-0uk9-0900000000-615c5e09ca5e5186b79b",
        first.<String>getOrElse(DBEntryField.SPLASH, null));
    // values are converted, not stored as the raw strings
    Assertions.assertEquals(new FloatArrayList(new float[]{20.0f}),
        first.<Object>getOrElse(DBEntryField.COLLISION_ENERGY, null));
    Assertions.assertEquals(PolarityType.NEGATIVE,
        PolarityType.parseFromString(first.getOrElse(DBEntryField.POLARITY, "")));

    // peaks are a real json array here, not json inside a string
    Assertions.assertEquals(50, first.getNumberOfDataPoints());
    Assertions.assertEquals(105.07069492649084, first.getMzValue(0));
    Assertions.assertEquals(15.020489552052531, first.getIntensityValue(0));
    Assertions.assertEquals(343.2261957870904, first.getMzValue(49));

    Assertions.assertEquals("11(12)-EpETE",
        entries.get(1).<String>getOrElse(DBEntryField.NAME, null));
    Assertions.assertEquals("11(12)-EpETrE",
        entries.get(2).<String>getOrElse(DBEntryField.NAME, null));
  }
}
