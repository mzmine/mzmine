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
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.util.RIColumn;
import io.github.mzmine.util.RIRecord;
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
  void testMspRIKEN() throws UnsupportedFormatException, IOException {
    File file = new File(AutoLibraryParserTest.class.getClassLoader()
        .getResource("io/github/mzmine/util/spectraldb/parser/riken.msp").getFile());

    SpectralLibrary library = new SpectralLibrary(null, file);
    List<SpectralLibraryEntry> entries = new ArrayList<>();

    final AutoLibraryParser parser = new AutoLibraryParser(100,
        (list, alreadyProcessed) -> entries.addAll(list));
    final boolean result = parser.parse(null, file, library);

    Assertions.assertTrue(result);
    assertEquals(3, entries.size());

    // First entry - 1-NITROPYRENE
    final SpectralLibraryEntry firstEntry = entries.get(0);
    assertEquals("1-NITROPYRENE; EI-B; MS", firstEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(247.0633285, firstEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null),
        0.0000001);
    assertEquals("C16H9NO2", firstEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals("[O-1][N+1](=O)c(c4)c(c1)c(c3c4)c(c2cc3)c(ccc2)c1",
        firstEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals("ALRLPDGCPYIVHP-UHFFFAOYSA-N",
        firstEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals("Pyrenes", firstEntry.getAsString(DBEntryField.CLASSYFIRE_CLASS).orElse(null));
    assertNull(firstEntry.getAsFloat(DBEntryField.RT).orElse(null));
    RIRecord ri = firstEntry.getOrElse(DBEntryField.RETENTION_INDEX, null);
    assertEquals(1872.217f, ri.getRI(RIColumn.DEFAULT), 0.001f);
    assertEquals(PolarityType.POSITIVE.toString(),
        firstEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals(List.of(70.0f), firstEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    assertEquals("source=downloaded from msdial website",
        firstEntry.getAsString(DBEntryField.COMMENT).orElse(null));
    assertEquals(75, firstEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));

    // Second entry - 2,4-DINITROPHENOL
    final SpectralLibraryEntry secondEntry = entries.get(1);
    assertEquals("2,4-DINITROPHENOL; EI-B; MS",
        secondEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(184.0120212, secondEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null),
        0.0000001);
    assertEquals("C6H4N2O5", secondEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals("[O-1][N+1](=O)c(c1)cc([N+1]([O-1])=O)c(O)c1",
        secondEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals("UFBJCMHMOXMLKC-UHFFFAOYSA-N",
        secondEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals("Dinitrophenols",
        secondEntry.getAsString(DBEntryField.CLASSYFIRE_CLASS).orElse(null));
    assertNull(secondEntry.getAsFloat(DBEntryField.RT).orElse(null));
    ri = secondEntry.getOrElse(DBEntryField.RETENTION_INDEX, null);
    assertEquals(1547.829f, ri.getRI(RIColumn.DEFAULT), 0.001f);
    assertEquals(PolarityType.POSITIVE.toString(),
        secondEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals(List.of(70.0f), secondEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    assertEquals(64, secondEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));

    // Third entry - Glucolesquerellin
    final SpectralLibraryEntry thirdEntry = entries.get(2);
    assertEquals("Glucolesquerellin", thirdEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(448.0775180239999, thirdEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null),
        0.0000000000001);
    assertEquals("[M-H]-", thirdEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals("C14H27NO9S3", thirdEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals("CSCCCCCCC(=NOS(=O)(=O)O)S[C@H]1[C@@H]([C@H]([C@@H]([C@H](O1)CO)O)O)O",
        thirdEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals("ZAKICGFSIJSCSF-LPUQOGTASA-N",
        thirdEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals("Alkylglucosinolates",
        thirdEntry.getAsString(DBEntryField.CLASSYFIRE_CLASS).orElse(null));
    assertEquals(PolarityType.NEGATIVE.toString(),
        thirdEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals("ESI", thirdEntry.getAsString(DBEntryField.ION_SOURCE).orElse(null));
    assertEquals("LC-ESI-QTOF", thirdEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals(List.of(40.0f), thirdEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    assertEquals(4.0667f, thirdEntry.getAsFloat(DBEntryField.RT).orElse(null), 0.0001f);
    assertEquals(189.9914404f, thirdEntry.getAsFloat(DBEntryField.CCS).orElse(null), 0.0000001f);
    assertEquals(
        "DB#=SMI00034; origin=MassBank High Quality Mass Spectral Database; source=downloaded from msdial website",
        thirdEntry.getAsString(DBEntryField.COMMENT).orElse(null));
    assertEquals(2, thirdEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));

    // Verify that all entries have valid mass spectra data
    Assertions.assertTrue(firstEntry.getNumberOfDataPoints() > 0);
    Assertions.assertTrue(secondEntry.getNumberOfDataPoints() > 0);
    Assertions.assertTrue(thirdEntry.getNumberOfDataPoints() > 0);

    // Verify exact number of data points matches num peaks
    assertEquals(75, firstEntry.getNumberOfDataPoints());
    assertEquals(64, secondEntry.getNumberOfDataPoints());
    assertEquals(2, thirdEntry.getNumberOfDataPoints());

    // Verify polarity parsing
    assertEquals(PolarityType.POSITIVE, firstEntry.getPolarity());
    assertEquals(PolarityType.POSITIVE, secondEntry.getPolarity());
    assertEquals(PolarityType.NEGATIVE, thirdEntry.getPolarity());

    // Verify collision energy arrays are properly parsed as FloatArrayList
    Assertions.assertInstanceOf(it.unimi.dsi.fastutil.floats.FloatArrayList.class,
        firstEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    Assertions.assertInstanceOf(it.unimi.dsi.fastutil.floats.FloatArrayList.class,
        secondEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    Assertions.assertInstanceOf(it.unimi.dsi.fastutil.floats.FloatArrayList.class,
        thirdEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));

    // Verify different ionization methods
    Assertions.assertNull(firstEntry.getAsString(DBEntryField.ION_SOURCE)
        .orElse(null)); // EI method has no ion source
    Assertions.assertNull(secondEntry.getAsString(DBEntryField.ION_SOURCE)
        .orElse(null)); // EI method has no ion source
    assertEquals("ESI", thirdEntry.getAsString(DBEntryField.ION_SOURCE).orElse(null));

    // Verify instrument types
    Assertions.assertNull(
        firstEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null)); // EI method
    Assertions.assertNull(
        secondEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null)); // EI method
    assertEquals("LC-ESI-QTOF", thirdEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
  }


  @Test
  void testMgfMZMINE() throws UnsupportedFormatException, IOException {
    File file = new File(AutoLibraryParserTest.class.getClassLoader()
        .getResource("io/github/mzmine/util/spectraldb/parser/mzmine.mgf").getFile());

    SpectralLibrary library = new SpectralLibrary(null, file);
    List<SpectralLibraryEntry> entries = new ArrayList<>();

    final AutoLibraryParser parser = new AutoLibraryParser(100,
        (list, alreadyProcessed) -> entries.addAll(list));
    final boolean result = parser.parse(null, file, library);

    Assertions.assertTrue(result);
    assertEquals(3, entries.size());

    // First entry - (S)-quinacrine
    final SpectralLibraryEntry firstEntry = entries.get(0);
    assertEquals("(S)-quinacrine", firstEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(398.20046, firstEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null),
        0.00001);
    assertEquals(1.9305f, firstEntry.getAsFloat(DBEntryField.RT).orElse(null), 0.01f);
    assertEquals("[M-H]-", firstEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals(399.20774, firstEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null), 0.00001);
    assertEquals(2, firstEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(-1, firstEntry.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals("C23H30ClN3O", firstEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals("CCN(CC)CCC[C@H](C)Nc1c2ccc(Cl)cc2nc2ccc(OC)cc12",
        firstEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals(
        "InChI=1S/C23H30ClN3O/c1-5-27(6-2)13-7-8-16(3)25-23-19-11-9-17(24)14-22(19)26-21-12-10-18(28-4)15-20(21)23/h9-12,14-16H,5-8,13H2,1-4H3,(H,25,26)/t16-/m0/s1",
        firstEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals("GPKJTRJOBQGKQK-INIZCTEOSA-N",
        firstEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals("HCD", firstEntry.getAsString(DBEntryField.FRAGMENTATION_METHOD).orElse(null));
    assertEquals(List.of(20.0f, 30.0f, 60.0f, 40.0f),
        firstEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    assertEquals(1.2000000476839432,
        firstEntry.getAsDouble(DBEntryField.ISOLATION_WINDOW).orElse(null), 0.0000000000000001);
    assertEquals(PolarityType.NEGATIVE.toString(),
        firstEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals(8, firstEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));
    assertEquals("Orbitrap", firstEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("Orbitrap ID-X", firstEntry.getAsString(DBEntryField.INSTRUMENT).orElse(null));
    assertEquals("ESI", firstEntry.getAsString(DBEntryField.ION_SOURCE).orElse(null));

    // Second entry - Fraxin
    final SpectralLibraryEntry secondEntry = entries.get(1);
    assertEquals("Fraxin", secondEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(369.08272, secondEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null),
        0.00001);
    assertEquals(1.3115001f, secondEntry.getAsFloat(DBEntryField.RT).orElse(null), 0.01f);
    assertEquals("[M-H]-", secondEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals(370.09, secondEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null), 0.01);
    assertEquals(2, secondEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(-1, secondEntry.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals("C16H18O10", secondEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals("COc1c(O)c(O[C@@H]2O[C@H](CO)[C@@H](O)[C@H](O)[C@H]2O)c2oc(=O)ccc2c1",
        secondEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals(
        "InChI=1S/C16H18O10/c1-23-7-4-6-2-3-9(18)25-14(6)15(11(7)20)26-16-13(22)12(21)10(19)8(5-17)24-16/h2-4,8,10,12-13,16-17,19-22H,5H2,1H3/t8-,10-,12+,13-,16+/m1/s1",
        secondEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals("CRSFLLTWRCYNNX-QBNNUVSCSA-N",
        secondEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals("HCD", secondEntry.getAsString(DBEntryField.FRAGMENTATION_METHOD).orElse(null));
    assertEquals(List.of(20.0f, 30.0f, 60.0f),
        secondEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    assertEquals(1.2000000476839432,
        secondEntry.getAsDouble(DBEntryField.ISOLATION_WINDOW).orElse(null), 0.0000000000000001);
    assertEquals(PolarityType.NEGATIVE.toString(),
        secondEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals(30, secondEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));
    assertEquals("Orbitrap", secondEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("Orbitrap ID-X", secondEntry.getAsString(DBEntryField.INSTRUMENT).orElse(null));
    assertEquals("ESI", secondEntry.getAsString(DBEntryField.ION_SOURCE).orElse(null));

    // Third entry - Lck inhibitor 2
    final SpectralLibraryEntry thirdEntry = entries.get(2);
    assertEquals("Lck inhibitor 2", thirdEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(380.13643, thirdEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null),
        0.00001);
    assertEquals(1.8683333f, thirdEntry.getAsFloat(DBEntryField.RT).orElse(null), 0.01f);
    assertEquals("[M+FA]-", thirdEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals(335.13822, thirdEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null), 0.00001);
    assertEquals(2, thirdEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(-1, thirdEntry.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals("C18H17N5O2", thirdEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals("Cc1c(Nc2nc(Nc3cccc(C(N)=O)c3)ncc2)cc(O)cc1",
        thirdEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals(
        "InChI=1S/C18H17N5O2/c1-11-5-6-14(24)10-15(11)22-16-7-8-20-18(23-16)21-13-4-2-3-12(9-13)17(19)25/h2-10,24H,1H3,(H2,19,25)(H2,20,21,22,23)",
        thirdEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals("SFCBIFOAGRZJNX-UHFFFAOYSA-N",
        thirdEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals("HCD", thirdEntry.getAsString(DBEntryField.FRAGMENTATION_METHOD).orElse(null));
    assertEquals(List.of(20.0f, 15.0f, 60.0f, 40.0f),
        thirdEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    assertEquals(1.2000000476839432,
        thirdEntry.getAsDouble(DBEntryField.ISOLATION_WINDOW).orElse(null), 0.0000000000000001);
    assertEquals(PolarityType.NEGATIVE.toString(),
        thirdEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals(17, thirdEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));
    assertEquals("Orbitrap", thirdEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("Orbitrap ID-X", thirdEntry.getAsString(DBEntryField.INSTRUMENT).orElse(null));
    assertEquals("ESI", thirdEntry.getAsString(DBEntryField.ION_SOURCE).orElse(null));

    // Verify that all entries have valid mass spectra data
    Assertions.assertTrue(firstEntry.getNumberOfDataPoints() > 0);
    Assertions.assertTrue(secondEntry.getNumberOfDataPoints() > 0);
    Assertions.assertTrue(thirdEntry.getNumberOfDataPoints() > 0);

    // Verify exact number of data points matches num peaks
    assertEquals(8, firstEntry.getNumberOfDataPoints());
    assertEquals(30, secondEntry.getNumberOfDataPoints());
    assertEquals(17, thirdEntry.getNumberOfDataPoints());

    // Verify polarity parsing
    assertEquals(PolarityType.NEGATIVE, firstEntry.getPolarity());
    assertEquals(PolarityType.NEGATIVE, secondEntry.getPolarity());
    assertEquals(PolarityType.NEGATIVE, thirdEntry.getPolarity());

    // Verify collision energy arrays are properly parsed
    Assertions.assertInstanceOf(it.unimi.dsi.fastutil.floats.FloatArrayList.class,
        firstEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    Assertions.assertInstanceOf(it.unimi.dsi.fastutil.floats.FloatArrayList.class,
        secondEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    Assertions.assertInstanceOf(it.unimi.dsi.fastutil.floats.FloatArrayList.class,
        thirdEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
  }

  @Test
  void testMgfMatchmsCleaned() throws UnsupportedFormatException, IOException {
    File file = new File(AutoLibraryParserTest.class.getClassLoader()
        .getResource("io/github/mzmine/util/spectraldb/parser/matchms_cleaned.mgf").getFile());

    SpectralLibrary library = new SpectralLibrary(null, file);
    List<SpectralLibraryEntry> entries = new ArrayList<>();

    final AutoLibraryParser parser = new AutoLibraryParser(100,
        (list, alreadyProcessed) -> entries.addAll(list));
    final boolean result = parser.parse(null, file, library);

    Assertions.assertTrue(result);
    assertEquals(3, entries.size());

    // First entry - AKOS034072661
    final SpectralLibraryEntry firstEntry = entries.get(0);
    assertEquals("AKOS034072661", firstEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(277.101, firstEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null), 0.001);
    assertEquals("[M+H]1+", firstEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals("Cc1nc(CNC(C)(C(=O)O)c2ccccc2)cs1",
        firstEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals("RYTTVMXNOGCABD-UHFFFAOYSA-N",
        firstEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals(
        "InChI=1S/C14H16N2O2S/c1-10-16-12(9-19-10)8-15-14(2,13(17)18)11-6-4-3-5-7-11/h3-7,9,15H,8H2,1-2H3,(H,17,18)",
        firstEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals(2, firstEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(1, firstEntry.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals(PolarityType.POSITIVE.toString(),
        firstEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals("CCMSLIB00012739756", firstEntry.getAsString(DBEntryField.ENTRY_ID).orElse(null));
    assertEquals("orbitrap", firstEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("ESI", firstEntry.getAsString(DBEntryField.ION_SOURCE).orElse(null));
    assertEquals(276.09372354790924, firstEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null),
        0.00000000000001);

    // Second entry - (1R,7R)-7-ethenyl compound
    final SpectralLibraryEntry secondEntry = entries.get(1);
    assertEquals(
        "(1R,7R)-7-ethenyl-1,4a,7-trimethyl-3,4,4b,5,6,9,10,10a-octahydro-2H-phenanthrene-1-carboxylic acid",
        secondEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(325.214, secondEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null), 0.001);
    assertEquals("[M+Na]1+", secondEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals("C=CC1(C)C=C2CCC3C(C)(C(=O)O)CCCC3(C)C2CC1",
        secondEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals("MHVJRKBZMUDEEV-UHFFFAOYSA-N",
        secondEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals(
        "InChI=1S/C20H30O2/c1-5-18(2)12-9-15-14(13-18)7-8-16-19(15,3)10-6-11-20(16,4)17(21)22/h5,13,15-16H,1,6-12H2,2-4H3,(H,21,22)",
        secondEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals(2, secondEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(1, secondEntry.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals(PolarityType.POSITIVE.toString(),
        secondEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals("CCMSLIB00004700359", secondEntry.getAsString(DBEntryField.ENTRY_ID).orElse(null));
    assertEquals("ftms", secondEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals(302.22477929990924, secondEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null),
        0.00000000000001);

    // Third entry - Hectochlorin
    final SpectralLibraryEntry thirdEntry = entries.get(2);
    assertEquals("Hectochlorin", thirdEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(667.115, thirdEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null), 0.001);
    assertEquals("[M+H]1+", thirdEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals(
        "CC(=O)OC1c2nc(cs2)C(=O)OC(CCCC(C)(Cl)[37Cl])C(C)C(=O)OC(C(C)(C)O)c2nc(cs2)C(=O)OC1(C)C",
        thirdEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals("USXIYWCPCGVOKF-CRTVXBCISA-N",
        thirdEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals(
        "InChI=1S/C27H34Cl2N2O9S2/c1-13-17(9-8-10-27(7,28)29)38-23(34)15-11-42-21(30-15)19(37-14(2)32)26(5,6)40-24(35)16-12-41-20(31-16)18(25(3,4)36)39-22(13)33/h11-13,17-19,36H,8-10H2,1-7H3/i28+2",
        thirdEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals(2, thirdEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(1, thirdEntry.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals(PolarityType.POSITIVE.toString(),
        thirdEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals("CCMSLIB00000001552", thirdEntry.getAsString(DBEntryField.ENTRY_ID).orElse(null));
    assertEquals("qtof", thirdEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("ESI", thirdEntry.getAsString(DBEntryField.ION_SOURCE).orElse(null));
    assertEquals(666.1077235479092, thirdEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null),
        0.0000000000001);

    // Verify that all entries have valid mass spectra data
    Assertions.assertTrue(firstEntry.getNumberOfDataPoints() > 0);
    Assertions.assertTrue(secondEntry.getNumberOfDataPoints() > 0);
    Assertions.assertTrue(thirdEntry.getNumberOfDataPoints() > 0);

    // Verify polarity parsing
    assertEquals(PolarityType.POSITIVE, firstEntry.getPolarity());
    assertEquals(PolarityType.POSITIVE, secondEntry.getPolarity());
    assertEquals(PolarityType.POSITIVE, thirdEntry.getPolarity());
  }

  @Test
  void testMgfGnpsCleaned() throws UnsupportedFormatException, IOException {
    File file = new File(AutoLibraryParserTest.class.getClassLoader()
        .getResource("io/github/mzmine/util/spectraldb/parser/gnps_cleaned.mgf").getFile());

    SpectralLibrary library = new SpectralLibrary(null, file);
    List<SpectralLibraryEntry> entries = new ArrayList<>();

    final AutoLibraryParser parser = new AutoLibraryParser(100,
        (list, alreadyProcessed) -> entries.addAll(list));
    final boolean result = parser.parse(null, file, library);

    Assertions.assertTrue(result);
    assertEquals(3, entries.size());

    // First entry - sibiricaxanthone B
    final SpectralLibraryEntry firstEntry = entries.get(0);
    assertEquals("sibiricaxanthone B", firstEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(539.14, firstEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null), 0.01);
    assertEquals("[M+H]+", firstEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals("O=C1C2=CC(O)=CC=C2OC3=CC(O)=C(C(O)=C13)C4OC(CO)C(O)C(O)C4OC5OCC(O)(CO)C5O",
        firstEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals("LLMCZIBSELEBMK-UHFFFAOYSA-N",
        firstEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals(
        "InChI=1S/C24H26O14/c25-5-13-17(30)19(32)21(38-23-22(33)24(34,6-26)7-35-23)20(37-13)14-10(28)4-12-15(18(14)31)16(29)9-3-8(27)1-2-11(9)36-12/h1-4,13,17,19-23,25-28,30-34H,5-7H2",
        firstEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals(2, firstEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(1, firstEntry.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals(PolarityType.POSITIVE.toString(),
        firstEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals("CCMSLIB00006514547", firstEntry.getAsString(DBEntryField.ENTRY_ID).orElse(null));
    assertEquals("ESI-Orbitrap", firstEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("C24H26O14", firstEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals(538.1327239999999, firstEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null),
        0.0000000000001);
    assertEquals("BMDMS_mgf.mgf", firstEntry.getAsString(DBEntryField.FILENAME).orElse(null));
    assertEquals("BMDMS-NP",
        firstEntry.getAsString(DBEntryField.PRINCIPAL_INVESTIGATOR).orElse(null));
    assertEquals("BMDMS-NP", firstEntry.getAsString(DBEntryField.DATA_COLLECTOR).orElse(null));
    assertEquals("*..*", firstEntry.getAsString(DBEntryField.PEPTIDE_SEQ).orElse(null));
    assertEquals(160247, firstEntry.getField(DBEntryField.SCAN_NUMBER).orElse(null));

    // Second entry - IDH-305
    final SpectralLibraryEntry secondEntry = entries.get(1);
    assertEquals("IDH-305", secondEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(491.18131, secondEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null),
        0.00001);
    assertEquals("[M+H]+", secondEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals("Cc1cc([C@H](C)Nc2nccc(N3C(=O)OC[C@@H]3[C@H](C)F)n2)ncc1-c1cc(C(F)(F)F)ncc1",
        secondEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals("DCGDPJCUIKLTDU-SUNYJGFJSA-N",
        secondEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals(
        "InChI=1S/C23H22F4N6O2/c1-12-8-17(30-10-16(12)15-4-6-28-19(9-15)23(25,26)27)14(3)31-21-29-7-5-20(32-21)33-18(13(2)24)11-35-22(33)34/h4-10,13-14,18H,11H2,1-3H3,(H,29,31,32)/t13-,14-,18+/m0/s1",
        secondEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals(2, secondEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(1, secondEntry.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals(PolarityType.POSITIVE.toString(),
        secondEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals("C23H22F4N6O2", secondEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals("HCD", secondEntry.getAsString(DBEntryField.FRAGMENTATION_METHOD).orElse(null));
    assertEquals(1.2000000476840569,
        secondEntry.getAsDouble(DBEntryField.ISOLATION_WINDOW).orElse(null), 0.0000000000000001);
    assertEquals("Orbitrap", secondEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("ESI", secondEntry.getAsString(DBEntryField.ION_SOURCE).orElse(null));
    assertEquals(1.415333f, secondEntry.getAsFloat(DBEntryField.RT).orElse(null), 0.01f);
    assertEquals("Tomas Pluskal",
        secondEntry.getAsString(DBEntryField.PRINCIPAL_INVESTIGATOR).orElse(null));
    assertEquals("Corinna Brungs",
        secondEntry.getAsString(DBEntryField.DATA_COLLECTOR).orElse(null));
    assertEquals(List.of(20.0f), secondEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    assertEquals(490.174037, secondEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null),
        0.000001);
    assertEquals(4, secondEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));
    assertEquals("MSVPLACEHOLDERID", secondEntry.getAsString(DBEntryField.DATASET_ID).orElse(null));
    assertEquals("mzspec:MSVPLACEHOLDERID:20220613_100AGC_60000Res_pluskal_mce_1D1_D24_id.mzML:510",
        secondEntry.getAsString(DBEntryField.USI).orElse(null));
    assertEquals(510, secondEntry.getField(DBEntryField.SCAN_NUMBER).orElse(null));
    assertEquals(0.9735733046627605f,
        secondEntry.getAsFloat(DBEntryField.QUALITY_PRECURSOR_PURITY).orElse(null),
        0.0000000000000001f);
    assertEquals("PASSED", secondEntry.getAsString(DBEntryField.QUALITY_CHIMERIC).orElse(null));
    assertEquals(0.9841458f,
        secondEntry.getAsFloat(DBEntryField.QUALITY_EXPLAINED_INTENSITY).orElse(null), 0.0000001f);
    assertEquals(0.75f, secondEntry.getAsFloat(DBEntryField.QUALITY_EXPLAINED_SIGNALS).orElse(null),
        0.01f);
    assertEquals("Crude", secondEntry.getAsString(DBEntryField.ACQUISITION).orElse(null));
    assertEquals("none", secondEntry.getAsString(DBEntryField.IMS_TYPE).orElse(null));
    assertEquals("510", secondEntry.getAsString(DBEntryField.FEATURE_ID).orElse(null));

    // Third entry - complex compound with long name
    final SpectralLibraryEntry thirdEntry = entries.get(2);
    assertEquals(
        "2-[3-[2-(1,3-benzodioxol-5-yl)-7-methoxy-1-benzofuran-5-yl]-3-hydroxypropoxy]-6-(hydroxymethyl)oxane-3,4,5-triol",
        thirdEntry.getAsString(DBEntryField.NAME).orElse(null));
    assertEquals(543.1262817382812, thirdEntry.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null),
        0.0000000000001);
    assertEquals("[M+K]+", thirdEntry.getAsString(DBEntryField.ION_TYPE).orElse(null));
    assertEquals("COc1cc(C(O)CCOC2OC(CO)C(O)C(O)C2O)cc2cc(-c3ccc4c(c3)OCO4)oc12",
        thirdEntry.getAsString(DBEntryField.SMILES).orElse(null));
    assertEquals("ZLACPIJRVKLLJM-UHFFFAOYSA-N",
        thirdEntry.getAsString(DBEntryField.INCHIKEY).orElse(null));
    assertEquals(
        "InChI=1S/C25H28O11/c1-31-19-8-13(15(27)4-5-32-25-23(30)22(29)21(28)20(10-26)36-25)6-14-9-17(35-24(14)19)12-2-3-16-18(7-12)34-11-33-16/h2-3,6-9,15,20-23,25-30H,4-5,10-11H2,1H3",
        thirdEntry.getAsString(DBEntryField.INCHI).orElse(null));
    assertEquals(2, thirdEntry.getAsInteger(DBEntryField.MS_LEVEL).orElse(null));
    assertEquals(1, thirdEntry.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals(PolarityType.POSITIVE.toString(),
        thirdEntry.getField(DBEntryField.POLARITY).orElse(null));
    assertEquals("VF-NPL-QEHF017611", thirdEntry.getAsString(DBEntryField.ENTRY_ID).orElse(null));
    assertEquals("ESI-QFT", thirdEntry.getAsString(DBEntryField.INSTRUMENT_TYPE).orElse(null));
    assertEquals("Thermo Q Exactive HF",
        thirdEntry.getAsString(DBEntryField.INSTRUMENT).orElse(null));
    assertEquals("C25H28O11", thirdEntry.getAsString(DBEntryField.FORMULA).orElse(null));
    assertEquals(List.of(35f), thirdEntry.getField(DBEntryField.COLLISION_ENERGY).orElse(null));
    assertEquals(504.163161716, thirdEntry.getAsDouble(DBEntryField.EXACT_MASS).orElse(null),
        0.000000001);
    assertEquals(36, thirdEntry.getAsInteger(DBEntryField.NUM_PEAKS).orElse(null));
    assertEquals("splash10-056v-3398480000-8c045ed2e7369b866b6c",
        thirdEntry.getAsString(DBEntryField.SPLASH).orElse(null));
    assertEquals("Arpana Vaniya",
        thirdEntry.getAsString(DBEntryField.PRINCIPAL_INVESTIGATOR).orElse(null));
    assertEquals("MX_UC_1333_p17_E08_6.raw",
        thirdEntry.getAsString(DBEntryField.FILENAME).orElse(null));
    assertEquals("$:00in-source", thirdEntry.getAsString(DBEntryField.SYNONYMS).orElse(null));
    assertEquals("direct injection", thirdEntry.getAsString(DBEntryField.ION_SOURCE).orElse(null));

    // Verify exact number of data points matches num peaks
    assertEquals(1, firstEntry.getNumberOfDataPoints());
    assertEquals(4, secondEntry.getNumberOfDataPoints());
    assertEquals(36, thirdEntry.getNumberOfDataPoints());

    // Verify polarity parsing
    assertEquals(PolarityType.POSITIVE, firstEntry.getPolarity());
    assertEquals(PolarityType.POSITIVE, secondEntry.getPolarity());
    assertEquals(PolarityType.POSITIVE, thirdEntry.getPolarity());
  }


  @Test
  void testMspMZMINE() throws UnsupportedFormatException, IOException {
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
    assertEquals(PolarityType.POSITIVE.toString(),
        firstEntry.getField(DBEntryField.POLARITY).orElse(null));
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
    assertEquals(PolarityType.NEGATIVE.toString(),
        secondEntry.getField(DBEntryField.POLARITY).orElse(null));
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
    assertEquals(PolarityType.POSITIVE.toString(),
        firstEntry.getField(DBEntryField.POLARITY).orElse(null));
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
    assertEquals(PolarityType.POSITIVE.toString(),
        secondEntry.getField(DBEntryField.POLARITY).orElse(null));
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
    assertEquals(PolarityType.POSITIVE.toString(),
        thirdEntry.getField(DBEntryField.POLARITY).orElse(null));
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
    assertEquals(PolarityType.POSITIVE.toString(),
        firstEntry.getField(DBEntryField.POLARITY).orElse(null));
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
    assertEquals(PolarityType.POSITIVE.toString(),
        secondEntry.getField(DBEntryField.POLARITY).orElse(null));
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
    assertEquals(PolarityType.POSITIVE.toString(),
        thirdEntry.getField(DBEntryField.POLARITY).orElse(null));
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