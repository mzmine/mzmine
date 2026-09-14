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

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * MoNA nests the interesting values in metaData name/value arrays and in the compound array, so
 * this covers those lookups as well as the colon separated spectrum string.
 */
class MonaJsonParserTest {

  @Test
  void testParse() throws IOException {
    final File file = new File(MonaJsonParserTest.class.getClassLoader()
        .getResource("spectral_libraries/integration_tests/MoNA-export-LC-MS-MS_Spectra.json")
        .getFile());
    final SpectralLibrary library = new SpectralLibrary(null, file);

    final List<SpectralLibraryEntry> entries = new ArrayList<>();
    Assertions.assertTrue(
        new MonaJsonParser(0, (l, done) -> entries.addAll(l), true).parse(null, file, library));
    Assertions.assertEquals(2, entries.size());

    final SpectralLibraryEntry first = entries.getFirst();
    // joined from the names array of the first compound
    Assertions.assertEquals(
        "Myristamidopropyl betaine, carboxymethyl-dimethyl-[3-(tetradecanoylamino)propyl]azanium",
        first.<String>getOrElse(DBEntryField.NAME, null));
    // straight from the compound object
    Assertions.assertEquals("QGCUAFIULMNFPJ-UHFFFAOYSA-O",
        first.<String>getOrElse(DBEntryField.INCHIKEY, null));
    // from the compound metaData array
    Assertions.assertEquals("[C21H43N2O3]+", first.<String>getOrElse(DBEntryField.FORMULA, null));
    Assertions.assertEquals("91247", first.<String>getOrElse(DBEntryField.CHEMSPIDER, null));
    // from the top level metaData array
    Assertions.assertEquals("LTQ Orbitrap XL Thermo Scientific",
        first.<String>getOrElse(DBEntryField.INSTRUMENT, null));
    Assertions.assertEquals("15000", first.<String>getOrElse(DBEntryField.RESOLUTION, null));
    // numeric metaData
    Assertions.assertEquals(371.3268, first.getAsDouble(DBEntryField.PRECURSOR_MZ).orElseThrow());
    Assertions.assertEquals(371.3274, first.getAsDouble(DBEntryField.EXACT_MASS).orElseThrow());

    // every value goes through DBEntryField.convertValue, so the fields carry the type they
    // declare instead of the text MoNA wrote. "MS2" becomes the level, "positive" is harmonized
    Assertions.assertEquals(Integer.valueOf(2),
        first.<Integer>getOrElse(DBEntryField.MS_LEVEL, null));
    Assertions.assertEquals(Optional.of(2), first.getMsLevel());
    Assertions.assertEquals(PolarityType.POSITIVE, first.getPolarity());
    // "50 % (nominal)" keeps only the number and becomes the declared FloatArrayList
    Assertions.assertEquals(new FloatArrayList(new float[]{50f}),
        first.<Object>getOrElse(DBEntryField.COLLISION_ENERGY, null));
    // "13.601 min" used to be dropped because the unit made the parse fail
    Assertions.assertEquals(13.601f, first.getAsFloat(DBEntryField.RT).orElseThrow(), 1e-4f);

    // the "mz:intensity mz:intensity" spectrum string
    Assertions.assertEquals(12, first.getNumberOfDataPoints());
    Assertions.assertEquals(55.0541, first.getMzValue(0));
    Assertions.assertEquals(1.379018, first.getIntensityValue(0));
    Assertions.assertEquals(268.2637, first.getMzValue(11));
    Assertions.assertEquals(100.0, first.getIntensityValue(11));

    Assertions.assertEquals("2-(undec-1-en-1-yl)quinolin-4-ol:Series 2 HAQ C11:1",
        entries.get(1).<String>getOrElse(DBEntryField.NAME, null));
  }
}
