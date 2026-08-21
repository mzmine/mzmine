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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.util.RIColumn;
import io.github.mzmine.util.RIRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the translation of the MSPepSearch RI column into an {@link RIRecord}.
 * <p>
 * MSPepSearch writes the retention index as value and column class, e.g. {@code 2480-S}, which
 * {@link RIRecord#fromString(String)} does not understand on its own - it expects the {@code s=},
 * {@code n=}, {@code p=} or {@code a=} prefixes of the spectral library RI field.
 */
class NistPepSearchRetentionIndexTest {

  @Test
  @DisplayName("The NIST column class letters map to the RI column types")
  void columnClasses() {

    // S = semi standard non polar, the column most NIST retention indices were measured on
    final RIRecord semiPolar = NistPepSearchTask.parseRetentionIndex("2480-S");
    assertNotNull(semiPolar);
    assertEquals(2480f, semiPolar.getRI(RIColumn.SEMIPOLAR));

    final RIRecord nonPolar = NistPepSearchTask.parseRetentionIndex("1300-N");
    assertNotNull(nonPolar);
    assertEquals(1300f, nonPolar.getRI(RIColumn.NONPOLAR));

    final RIRecord polar = NistPepSearchTask.parseRetentionIndex("1750-P");
    assertNotNull(polar);
    assertEquals(1750f, polar.getRI(RIColumn.POLAR));
  }

  @Test
  @DisplayName("Any, unspecified and AI predicted retention indices become the default column")
  void defaultColumnClasses() {

    // V is an AI predicted retention index, as seen in the captured output
    for (final String value : new String[]{"2464-V", "1000-A", "900-U"}) {

      final RIRecord record = NistPepSearchTask.parseRetentionIndex(value);
      assertNotNull(record, () -> "failed to parse " + value);
      // the default column answers any request
      assertEquals(Float.parseFloat(value.substring(0, value.indexOf('-'))),
          record.getRI(RIColumn.SEMIPOLAR));
      assertEquals(Float.parseFloat(value.substring(0, value.indexOf('-'))),
          record.getRI(RIColumn.DEFAULT));
    }
  }

  @Test
  @DisplayName("A bare number is accepted as the default column")
  void bareNumber() {

    final RIRecord record = NistPepSearchTask.parseRetentionIndex("1234");
    assertNotNull(record);
    assertEquals(1234f, record.getRI(RIColumn.DEFAULT));
  }

  @Test
  @DisplayName("Absent and unparsable retention indices yield null instead of throwing")
  void absentValues() {

    assertNull(NistPepSearchTask.parseRetentionIndex(null));
    assertNull(NistPepSearchTask.parseRetentionIndex(""));
    assertNull(NistPepSearchTask.parseRetentionIndex("   "));
    assertNull(NistPepSearchTask.parseRetentionIndex("not-a-number"));
  }

  @Test
  @DisplayName("A negative retention index keeps its sign rather than splitting on the minus")
  void negativeSignIsNotAColumnClass() {

    // the column class is taken from the last '-', so "-500" has no class and stays a plain number
    final RIRecord record = NistPepSearchTask.parseRetentionIndex("-500");
    assertNull(record, "a value without a column class and without digits before the dash is "
        + "not a usable retention index");
  }
}
