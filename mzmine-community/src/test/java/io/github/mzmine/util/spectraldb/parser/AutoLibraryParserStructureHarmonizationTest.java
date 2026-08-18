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

import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.datamodel.structures.StructureParser;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AutoLibraryParserStructureHarmonizationTest {

  private static final String LIBRARY_RESOURCE = "spectral_libraries/integration_tests/matches_for_tims-full.json";
  private static final String SOURCE_ISOMERIC_SMILES = "CCCCCCCC\\C=C/CCCCCCCCCCCC(N)=O";
  private static final String EXPECTED_CANONICAL_SMILES = "CCCCCCCCC=CCCCCCCCCCCCC(N)=O";
  private static final String EXPECTED_ISOMERIC_SMILES = "CCCCCCCC/C=C\\CCCCCCCCCCCC(=O)N";
  private static final String EXPECTED_INCHI = "InChI=1S/C22H43NO/c1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20-21-22(23)24/h9-10H,2-8,11-21H2,1H3,(H2,23,24)/b10-9-";
  private static final String INCHI_DERIVED_CANONICAL_SMILES = "CCCCCCCCC=CCCCCCCCCCCCC(=N)O";
  private static final String INCHI_DERIVED_ISOMERIC_SMILES = "CCCCCCCC/C=C\\CCCCCCCCCCCC(=N)O";

  @Test
  void erucamideCanonicalAndIsomericSmilesDependOnCachedInchi()
      throws IOException, UnsupportedFormatException, URISyntaxException {
    // assumption: another pipeline step parsed an InChI-only representation first.
    final MolecularStructure cachedInchiStructure = StructureParser.silent()
        .parseStructure(null, EXPECTED_INCHI);
    Assertions.assertNotNull(cachedInchiStructure);
    Assertions.assertAll(() -> Assertions.assertEquals(INCHI_DERIVED_CANONICAL_SMILES,
            cachedInchiStructure.canonicalSmiles()),
        () -> Assertions.assertEquals(INCHI_DERIVED_ISOMERIC_SMILES,
            cachedInchiStructure.isomericSmiles()));

    final SpectralLibraryEntry erucamide = parseLastEntry();
    Assertions.assertEquals(SOURCE_ISOMERIC_SMILES,
        erucamide.getAsString(DBEntryField.SMILES).orElse(null));
    Assertions.assertTrue(erucamide.getAsString(DBEntryField.ISOMERIC_SMILES).isEmpty());
    Assertions.assertEquals(EXPECTED_INCHI, erucamide.getAsString(DBEntryField.INCHI).orElse(null));
    Assertions.assertNotNull(erucamide.getStructure());

    Assertions.assertAll(() -> Assertions.assertEquals(EXPECTED_CANONICAL_SMILES,
            erucamide.getAsString(DBEntryField.SMILES).orElse(null)),
        () -> Assertions.assertEquals(EXPECTED_ISOMERIC_SMILES,
            erucamide.getAsString(DBEntryField.ISOMERIC_SMILES).orElse(null)));
  }

  @NotNull
  private static SpectralLibraryEntry parseLastEntry()
      throws URISyntaxException, IOException, UnsupportedFormatException {
    final URL resource = Assertions.assertDoesNotThrow(
        () -> AutoLibraryParserStructureHarmonizationTest.class.getClassLoader()
            .getResource(LIBRARY_RESOURCE));
    Assertions.assertNotNull(resource);
    final File file = new File(resource.toURI());
    final SpectralLibrary library = new SpectralLibrary(null, file);
    final List<SpectralLibraryEntry> entries = new ArrayList<>();
    final AutoLibraryParser parser = new AutoLibraryParser(100,
        (list, alreadyProcessed) -> entries.addAll(list));

    Assertions.assertTrue(parser.parse(null, file, library));
    Assertions.assertEquals(61, entries.size());
    return entries.getLast();
  }
}
