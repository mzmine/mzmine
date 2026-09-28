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

import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Guards the streaming parse of the nested peaks_json string in {@link SpectrumDeserializer}.
 */
class SpectrumDeserializerTest {

  @Test
  void testPeaksJson() throws IOException {
    final File file = new File(
        SpectrumDeserializerTest.class.getClassLoader().getResource("json/GNPS-FAULKNERLEGACY.json")
            .getFile());
    final SpectralLibrary library = new SpectralLibrary(null, file);

    final List<SpectralLibraryEntry> entries = new ArrayList<>();
    new GNPSJsonParser(0, (newList, done) -> entries.addAll(newList), false).parse(null, file,
        library);

    Assertions.assertEquals(127, entries.size());
    Assertions.assertEquals(7547,
        entries.stream().mapToInt(SpectralLibraryEntry::getNumberOfDataPoints).sum());

    final SpectralLibraryEntry first = entries.getFirst();
    Assertions.assertEquals("CCMSLIB00000081017",
        first.<String>getOrElse(DBEntryField.ENTRY_ID, null));
    Assertions.assertEquals(73, first.getNumberOfDataPoints());

    // exact values, the fast double parser must agree with Double.parseDouble bit for bit
    Assertions.assertEquals(105.068466, first.getMzValue(0));
    Assertions.assertEquals(458.062897, first.getIntensityValue(0));
    Assertions.assertEquals(440.063141, first.getMzValue(72));
    Assertions.assertEquals(25.894171, first.getIntensityValue(72));

    // mz values must stay sorted, which only holds if pairs are not shifted
    for (final SpectralLibraryEntry entry : entries) {
      final double[] mzs = entry.getMzValues(new double[0]);
      for (int i = 1; i < mzs.length; i++) {
        Assertions.assertTrue(mzs[i - 1] <= mzs[i],
            "mz values not sorted in " + entry.getOrElse(DBEntryField.ENTRY_ID, "?"));
      }
    }
  }
}
