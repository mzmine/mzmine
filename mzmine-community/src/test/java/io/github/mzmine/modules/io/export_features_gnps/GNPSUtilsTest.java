/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_features_gnps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class GNPSUtilsTest {

  @Test
  void accessLibraryOrUSISpectrum() throws IOException {
    // Phenylalanine conjugated deoxycholic acid
    String input = "mzspec:GNPS:GNPS-LIBRARY:accession:CCMSLIB00005716807";
    SpectralLibraryEntry data = GNPSUtils.accessLibraryOrUSISpectrum(input);
    assertNotNull(data);
    assertEquals(155, data.getNumberOfDataPoints());
    assertEquals(1, data.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals(540.368, data.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null));

//    tauro cholic acid
    input = "CCMSLIB00005435561";
    data = GNPSUtils.accessLibrarySpectrum(input);
    assertEquals(516.297, data.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null));
    assertEquals(542, data.getNumberOfDataPoints());
    assertNotNull(data);

    // wrong usi
    data = GNPSUtils.accessLibraryOrUSISpectrum(input.substring(2));
    assertNull(data);

    assertThrows(IllegalArgumentException.class, () -> GNPSUtils.accessUSISpectrum("should fail"));
  }

  @Test
  void accessLibrarySpectrum() throws IOException {
//    tauro cholic acid
    String input = "CCMSLIB00005435561";
    SpectralLibraryEntry data = GNPSUtils.accessLibrarySpectrum(input);
    assertNotNull(data);
    // wrong libid
    data = GNPSUtils.accessLibraryOrUSISpectrum(input.substring(2));
    assertNull(data);

    assertThrows(IllegalArgumentException.class, () -> GNPSUtils.accessUSISpectrum("should fail"));
  }

  @Test
  void accessUSISpectrum() throws IOException {
    // Phenylalanine conjugated deoxycholic acid
    String input = "mzspec:GNPS:GNPS-LIBRARY:accession:CCMSLIB00005716807";
    SpectralLibraryEntry data = GNPSUtils.accessUSISpectrum(input);
    assertNotNull(data);
    assertEquals(155, data.getNumberOfDataPoints());
    assertEquals(1, data.getAsInteger(DBEntryField.CHARGE).orElse(null));
    assertEquals(540.368, data.getAsDouble(DBEntryField.PRECURSOR_MZ).orElse(null));
    // wrong usi
    data = GNPSUtils.accessLibraryOrUSISpectrum(input.substring(2));
    assertNull(data);

    assertThrows(IllegalArgumentException.class, () -> GNPSUtils.accessUSISpectrum("should fail"));
  }
}