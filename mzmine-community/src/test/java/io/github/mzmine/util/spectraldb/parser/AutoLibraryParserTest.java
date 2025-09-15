/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AutoLibraryParserTest {

  @Test
  void testMspMassBankMZMINE() throws UnsupportedFormatException, IOException {
    File file = new File(AutoLibraryParserTest.class.getClassLoader()
        .getResource("io/github/mzmine/util/spectraldb/parser/mzmine.msp").getFile());

    SpectralLibrary library = new SpectralLibrary(null, file);
    List<SpectralLibraryEntry> entries = new ArrayList<>();

    final AutoLibraryParser parser = new AutoLibraryParser(100,
        (list, alreadyProcessed) -> entries.addAll(list));
    final boolean result = parser.parse(null, file, library);

    Assertions.assertTrue(result);
    assertEquals(2, entries.size());

    // First entry - quercetin (positive mode)
    final SpectralLibraryEntry firstEntry = entries.get(0);
    assertEquals("quercetin", firstEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(303.04992866, firstEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null),
        0.00000001);
    assertEquals(1.9112366f, firstEntry.getAsFloat(DBEntryField.RT).orElse(null), 0.0000001f);
    assertEquals("[M+H]+", firstEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals(302.04265266, firstEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null),
        0.00000001);
    assertEquals(2, firstEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(1, firstEntry.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals("O=c1c(O)c(-c2cc(O)c(O)cc2)oc2cc(O)cc(O)c12",
        firstEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals(
        "InChI=1S/C15H10O7/c16-7-4-10(19)12-11(5-7)22-15(14(21)13(12)20)6-1-2-8(17)9(18)3-6/h1-5,16-19,21H",
        firstEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals("REFJWTPEDVJJIY-UHFFFAOYSA-N",
        firstEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals("HCD", firstEntry.getAsString(DBEntryField.FRAGMENTATION_METHOD).orElse(null));
    assertEquals(List.of(30.0f), firstEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    assertEquals(0.7999999999999545,
        firstEntry.getAsDouble(DBEntryField.ISOLATION_WINDOW).orElse(null), 0.0000000000000001);
    assertEquals(PolarityType.POSITIVE, firstEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals(50, firstEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));

    // Second entry - Isoquercitrin (fake negative mode)
    final SpectralLibraryEntry secondEntry = entries.get(1);
    assertEquals("Isoquercitrin (fake negative mode)",
        secondEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(465.10275207999996,
        secondEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null), 0.00000000000001);
    assertEquals(1.9112366f, secondEntry.getAsFloat(DBEntryField.RT).orElse(null), 0.0000001f);
    assertEquals("[M-H]-", secondEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals(464.09547607999997, secondEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null),
        0.00000000000001);
    assertEquals(2, secondEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(1, secondEntry.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals("O=c1c(OC2OC(CO)C(O)C(O)C2O)c(-c2cc(O)c(O)cc2)oc2cc(O)cc(O)c12",
        secondEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals(
        "InChI=1S/C21H20O12/c22-6-13-15(27)17(29)18(30)21(32-13)33-20-16(28)14-11(26)4-8(23)5-12(14)31-19(20)7-1-2-9(24)10(25)3-7/h1-5,13,15,17-18,21-27,29-30H,6H2/t13-,15-,17+,18-,21+/m1/s1",
        secondEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals("OVSQVDMCBVZWGM-QSOFNFLRSA-N",
        secondEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals("HCD", secondEntry.getAsString(DBEntryField.FRAGMENTATION_METHOD).orElse(null));
    assertEquals(List.of(30.0f), secondEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    assertEquals(0.7999999999999545,
        secondEntry.getAsDouble(DBEntryField.ISOLATION_WINDOW).orElse(null), 0.0000000000000001);
    assertEquals(PolarityType.NEGATIVE, secondEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals(18, secondEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));

    // Verify that all entries have valid mass spectra data
    Assertions.assertTrue(firstEntry.getNumberOfDataPoints() > 0);
    Assertions.assertTrue(secondEntry.getNumberOfDataPoints() > 0);

    // Verify exact number of data points matches num peaks
    assertEquals(50, firstEntry.getNumberOfDataPoints());
    assertEquals(18, secondEntry.getNumberOfDataPoints());

    // Verify polarity parsing
    assertEquals(PolarityType.POSITIVE, firstEntry.getPolarity());
    assertEquals(PolarityType.NEGATIVE, secondEntry.getPolarity());

    // Verify that collision energy is properly parsed as FloatArrayList
    Assertions.assertInstanceOf(it.unimi.dsi.fastutil.floats.FloatArrayList.class,
        firstEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    Assertions.assertInstanceOf(it.unimi.dsi.fastutil.floats.FloatArrayList.class,
        secondEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));

    // Verify fragmentation method and isolation window are properly parsed
    Assertions.assertNotNull(
        firstEntry.getAsString(DBEntryField.FRAGMENTATION_METHOD).orElse(null));
    Assertions.assertNotNull(
        secondEntry.getAsString(DBEntryField.FRAGMENTATION_METHOD).orElse(null));
    Assertions.assertNotNull(firstEntry.getAsDouble(DBEntryField.ISOLATION_WINDOW).orElse(null));
    Assertions.assertNotNull(secondEntry.getAsDouble(DBEntryField.ISOLATION_WINDOW).orElse(null));
  }

  @Test
  void testMspMassBankNIST() throws UnsupportedFormatException, IOException {
    File file = new File(AutoLibraryParserTest.class.getClassLoader()
        .getResource("io/github/mzmine/util/spectraldb/parser/massbank.msp_NIST").getFile());

    SpectralLibrary library = new SpectralLibrary(null, file);
    List<SpectralLibraryEntry> entries = new ArrayList<>();

    final AutoLibraryParser parser = new AutoLibraryParser(100,
        (list, alreadyProcessed) -> entries.addAll(list));
    final boolean result = parser.parse(null, file, library);

    Assertions.assertTrue(result);
    assertEquals(3, entries.size());

    // First entry - Tryptamine (GC-EI-TOF)
    final SpectralLibraryEntry firstEntry = entries.get(0);
    assertEquals("Tryptamine", firstEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals("MSBNK-Kazusa-KZ000271",
        firstEntry.getAsString(DBEntryField.ENTRY_ID).orElse(null));
    assertEquals("APJYDQYYACXCRM-UHFFFAOYSA-N",
        firstEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals("InChI=1S/C10H12N2/c11-6-5-8-7-12-10-4-2-1-3-9(8)10/h1-4,7,12H,5-6,11H2",
        firstEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals("C1=CC=C2C(=C1)C(=CN2)CCN",
        firstEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals(1, firstEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals("GC-EI-TOF", firstEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("Pegasus III TOF-MS system, Leco; GC 6890, Agilent Technologies",
        firstEntry.getAsString(DBEntryField.INSTRUMENT).orElse(null));
    assertEquals(PolarityType.POSITIVE, firstEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals("C10H12N2", firstEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals(160.0, firstEntry.getAsDouble(DBEntryField.MOLWEIGHT).orElse(null), 0.001);
    assertEquals(160.10005, firstEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null), 0.00001);
    assertEquals("Parent=-1", firstEntry.getAsString(DBEntryField.COMMENT).orElse(null));
    assertEquals("splash10-00dr-2900000000-037af42e76613b924496",
        firstEntry.getAsString(DBEntryField.SPLASH).orElse(null));
    assertEquals(98, firstEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));

    // Second entry - Pyrophen (LC-ESI-ITFT MS2)
    final SpectralLibraryEntry secondEntry = entries.get(1);
    assertEquals("Pyrophen", secondEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals("N-[(1S)-1-(4-methoxy-6-oxopyran-2-yl)-2-phenylethyl]acetamide",
        secondEntry.getAsString(DBEntryField.SYNONYMS).orElse(null));
    assertEquals("MSBNK-AAFC-AC000854",
        secondEntry.getAsString(DBEntryField.ENTRY_ID).orElse(null));
    assertEquals("VFMQMACUYWGDOJ-AWEZNQCLSA-N",
        secondEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals(
        "InChI=1S/C16H17NO4/c1-11(18)17-14(8-12-6-4-3-5-7-12)15-9-13(20-2)10-16(19)21-15/h3-7,9-10,14H,8H2,1-2H3,(H,17,18)/t14-/m0/s1",
        secondEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals("CC(=O)N[C@@H](CC1=CC=CC=C1)C2=CC(=CC(=O)O2)OC",
        secondEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals("[M+H]+", secondEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals(2, secondEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(288.1225, secondEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null), 0.0001);
    assertEquals("LC-ESI-ITFT", secondEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("Q-Exactive Orbitrap Thermo Scientific",
        secondEntry.getAsString(DBEntryField.INSTRUMENT).orElse(null));
    assertEquals(PolarityType.POSITIVE, secondEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals(List.of(30f), secondEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    assertEquals("C16H17NO4", secondEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals(287.0, secondEntry.getAsDouble(DBEntryField.MOLWEIGHT).orElse(null), 0.001);
    assertEquals(287.11575, secondEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null), 0.00001);
    assertEquals("Parent=288.1225", secondEntry.getAsString(DBEntryField.COMMENT).orElse(null));
    assertEquals("splash10-004j-1940000000-b1bd14eb30f6afd2739e",
        secondEntry.getAsString(DBEntryField.SPLASH).orElse(null));
    assertEquals(8, secondEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));

    // Third entry - Benzyl butyl phthalate (LC-ESI-QFT MS2)
    final SpectralLibraryEntry thirdEntry = entries.get(2);
    assertEquals("Benzyl butyl phthalate", thirdEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals("2-O-benzyl 1-O-butyl benzene-1,2-dicarboxylate",
        thirdEntry.getAsString(DBEntryField.SYNONYMS).orElse(null));
    assertEquals("MSBNK-CASMI_2016-SM836901",
        thirdEntry.getAsString(DBEntryField.ENTRY_ID).orElse(null));
    assertEquals("IRIAEXORFWYRCZ-UHFFFAOYSA-N",
        thirdEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals(
        "InChI=1S/C19H20O4/c1-2-3-13-22-18(20)16-11-7-8-12-17(16)19(21)23-14-15-9-5-4-6-10-15/h4-12H,2-3,13-14H2,1H3",
        thirdEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals("CCCCOC(=O)C1=CC=CC=C1C(=O)OCC1=CC=CC=C1",
        thirdEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals("[M+H]+", thirdEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals(2, thirdEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(313.1434, thirdEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null), 0.0001);
    assertEquals("LC-ESI-QFT", thirdEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("Q Exactive Plus Orbitrap Thermo Scientific",
        thirdEntry.getAsString(DBEntryField.INSTRUMENT).orElse(null));
    assertEquals(PolarityType.POSITIVE, thirdEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals(List.of(35f), thirdEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    assertEquals("C19H20O4", thirdEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals(312.0, thirdEntry.getAsDouble(DBEntryField.MOLWEIGHT).orElse(null), 0.001);
    assertEquals(312.13616, thirdEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null), 0.00001);
    assertEquals("Parent=313.1434", thirdEntry.getAsString(DBEntryField.COMMENT).orElse(null));
    assertEquals("splash10-0006-9300000000-595d7be3b0c3da1a203e",
        thirdEntry.getAsString(DBEntryField.SPLASH).orElse(null));
    assertEquals(8, thirdEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));

    // Verify that all entries have valid mass spectra data
    Assertions.assertTrue(firstEntry.getNumberOfDataPoints() > 0);
    Assertions.assertTrue(secondEntry.getNumberOfDataPoints() > 0);
    Assertions.assertTrue(thirdEntry.getNumberOfDataPoints() > 0);
  }

  @Test
  void testMspMassBankRiken() throws UnsupportedFormatException, IOException {
    File file = new File(AutoLibraryParserTest.class.getClassLoader()
        .getResource("io/github/mzmine/util/spectraldb/parser/massbank.msp_RIKEN").getFile());

    SpectralLibrary library = new SpectralLibrary(null, file);
    List<SpectralLibraryEntry> entries = new ArrayList<>();

    final AutoLibraryParser parser = new AutoLibraryParser(100,
        (list, alreadyProcessed) -> entries.addAll(list));
    final boolean result = parser.parse(null, file, library);

    Assertions.assertTrue(result);
    assertEquals(3, entries.size());

    // First entry - Androstenedione
    final SpectralLibraryEntry firstEntry = entries.get(0);
    assertEquals("Androstenedione", firstEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(287.3, firstEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null), 0.001);
    assertEquals("[M+H]+", firstEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals("C19H26O2", firstEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals("Androstane steroids",
        firstEntry.getAsString(DBEntryField.CLASSYFIRE_CLASS).orElse(null));
    assertEquals("AEMFNILZOJDQLW-WFZCBACDSA-N",
        firstEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals(
        "InChI=1S/C19H26O2/c1-18-9-7-13(20)11-12(18)3-4-14-15-5-6-17(21)19(15,2)10-8-16(14)18/h11,14-16H,3-10H2,1-2H3/t14?,15?,16?,18-,19-/m0/s1",
        firstEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals("O=C(C4)C=C(C3)C(C)(C4)C(C2)C(C3)C(C1)C(C)(C2)C(=O)C1",
        firstEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals(0.820833f, firstEntry.getAsFloat(DBEntryField.RT).orElse(null), 0.0001f);
    assertEquals("LC-APPI-QQ", firstEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("API2000", firstEntry.getAsString(DBEntryField.INSTRUMENT).orElse(null));
    assertEquals(PolarityType.POSITIVE, firstEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals("splash10-052b-9840000000-a89309de36b355d58785",
        firstEntry.getAsString(DBEntryField.SPLASH).orElse(null));
    assertEquals(249, firstEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));
    Assertions.assertTrue(firstEntry.getAsString(DBEntryField.COMMENT).orElse("")
        .contains("DB#=MSBNK-PFOS_research_group-FFF00261"));

    // Second entry - Daidzein
    final SpectralLibraryEntry secondEntry = entries.get(1);
    assertEquals("Daidzein", secondEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(255.4, secondEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null), 0.001);
    assertEquals("[M+H]+", secondEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals("C15H10O4", secondEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals("Isoflav-2-enes",
        secondEntry.getAsString(DBEntryField.CLASSYFIRE_CLASS).orElse(null));
    assertEquals("ZQSIJRDFPHDXIC-UHFFFAOYSA-N",
        secondEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals(
        "InChI=1S/C15H10O4/c16-10-3-1-9(2-4-10)13-8-19-14-7-11(17)5-6-12(14)15(13)18/h1-8,16-17H",
        secondEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals("Oc(c3)ccc(c3)C(=C1)C(=O)c(c2)c(cc(O)c2)O1",
        secondEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals(0.82085f, secondEntry.getAsFloat(DBEntryField.RT).orElse(null), 0.0001f);
    assertEquals("LC-APPI-QQ", secondEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("API2000", secondEntry.getAsString(DBEntryField.INSTRUMENT).orElse(null));
    assertEquals(PolarityType.POSITIVE, secondEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals("splash10-0f7x-4900000000-fd7f271816fa0d6e6138",
        secondEntry.getAsString(DBEntryField.SPLASH).orElse(null));
    assertEquals(194, secondEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));
    Assertions.assertTrue(secondEntry.getAsString(DBEntryField.COMMENT).orElse("")
        .contains("DB#=MSBNK-PFOS_research_group-FFF00071"));

    // Third entry - Androstenedione (duplicate)
    final SpectralLibraryEntry thirdEntry = entries.get(2);
    assertEquals("Androstenedione", thirdEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(287.3, thirdEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null), 0.001);
    assertEquals("[M+H]+", thirdEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals("C19H26O2", thirdEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals("Androstane steroids",
        thirdEntry.getAsString(DBEntryField.CLASSYFIRE_CLASS).orElse(null));
    assertEquals("AEMFNILZOJDQLW-WFZCBACDSA-N",
        thirdEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals(
        "InChI=1S/C19H26O2/c1-18-9-7-13(20)11-12(18)3-4-14-15-5-6-17(21)19(15,2)10-8-16(14)18/h11,14-16H,3-10H2,1-2H3/t14?,15?,16?,18-,19-/m0/s1",
        thirdEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals("O=C(C4)C=C(C3)C(C)(C4)C(C2)C(C3)C(C1)C(C)(C2)C(=O)C1",
        thirdEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals(0.820833f, thirdEntry.getAsFloat(DBEntryField.RT).orElse(null), 0.0001f);
    assertEquals("LC-APPI-QQ", thirdEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("API2000", thirdEntry.getAsString(DBEntryField.INSTRUMENT).orElse(null));
    assertEquals(PolarityType.POSITIVE, thirdEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals("splash10-052b-9840000000-a89309de36b355d58785",
        thirdEntry.getAsString(DBEntryField.SPLASH).orElse(null));
    assertEquals(249, thirdEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));
    Assertions.assertTrue(thirdEntry.getAsString(DBEntryField.COMMENT).orElse("")
        .contains("DB#=MSBNK-PFOS_research_group-FFF00261"));

    // Verify that all entries have valid mass spectra data
    Assertions.assertTrue(firstEntry.getNumberOfDataPoints() > 0);
    Assertions.assertTrue(secondEntry.getNumberOfDataPoints() > 0);
    Assertions.assertTrue(thirdEntry.getNumberOfDataPoints() > 0);
  }

}