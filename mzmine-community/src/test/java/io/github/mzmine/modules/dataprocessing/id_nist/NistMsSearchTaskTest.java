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

package io.github.mzmine.modules.dataprocessing.id_nist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the SRCRESLT.TXT block splitting of {@link NistMsSearchTask}.
 */
class NistMsSearchTaskTest {

  /**
   * A results file as MS Search writes it with {@link ImportOption#APPEND}: every spectrum still in
   * the spec list is reported again, oldest first. This used to make the task throw
   * "Search results are for a different peak" for every row after the first.
   */
  private static final String APPEND_MULTI_BLOCK = """
      Unknown: Row 1 (unknown) of feature list
      Hit 1  : <<Caffeine>>;<<C8H10N4O2>>;  CAS:58-08-2;  Mw:194;  Id:12345.  Lib: <<mainlib>>;  MF: 912;  RMF: 930;
      Hit 2  : <<Theobromine>>;<<C7H8N4O2>>;  CAS:83-67-0;  Mw:180;  Id:12346.  Lib: <<mainlib>>;  MF: 640;  RMF: 700;
      Unknown: Row 2 (unknown) of feature list
      Hit 1  : <<Naphthalene>>;<<C10H8>>;  CAS:91-20-3;  Mw:128;  Id:22222.  Lib: <<mainlib>>;  MF: 880;  RMF: 900;
      Unknown: Row 3 (unknown) of feature list
      Hit 1  : <<Toluene>>;<<C7H8>>;  CAS:108-88-3;  Mw:92;  Id:33333.  Lib: <<replib>>;  MF: 800;  RMF: 850;
      Hit 2  : <<Benzene>>;<<C6H6>>;  CAS:71-43-2;  Mw:78;  Id:33334.  Lib: <<replib>>;  MF: 700;  RMF: 760;
      Hit 3  : <<Xylene>>;<<C8H10>>;  CAS:1330-20-7;  Mw:106;  Id:33335.  Lib: <<replib>>;  MF: 600;  RMF: 650;
      """;

  private static @Nullable List<String> hitLines(final String content, final int rowId)
      throws IOException {
    try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
      return NistMsSearchTask.readHitLinesForRow(reader, rowId);
    }
  }

  @Test
  @DisplayName("A results file with blocks for several rows no longer fails - regression for "
      + "'Search results are for a different peak'")
  void appendModeMultipleBlocksAreDispatchedByRowId() throws IOException {

    // the last block: previously threw on the very first Unknown: line
    final List<String> row3 = hitLines(APPEND_MULTI_BLOCK, 3);
    assertNotNull(row3);
    assertEquals(3, row3.size());
    assertTrue(row3.get(0).contains("<<Toluene>>"));
    assertTrue(row3.get(2).contains("<<Xylene>>"));

    // a block in the middle
    final List<String> row2 = hitLines(APPEND_MULTI_BLOCK, 2);
    assertNotNull(row2);
    assertEquals(1, row2.size());
    assertTrue(row2.get(0).contains("<<Naphthalene>>"));

    // the first block still works
    final List<String> row1 = hitLines(APPEND_MULTI_BLOCK, 1);
    assertNotNull(row1);
    assertEquals(2, row1.size());
    assertTrue(row1.get(0).contains("<<Caffeine>>"));
  }

  @Test
  @DisplayName("A row without a block returns null instead of throwing")
  void missingBlockReturnsNull() throws IOException {
    assertNull(hitLines(APPEND_MULTI_BLOCK, 99));
  }

  @Test
  @DisplayName("Blank lines and unrecognised text are ignored, not fatal")
  void unrecognisedTextIsIgnored() throws IOException {

    final String content = """
        NIST MS Search results

        Unknown: Row 7 (unknown) of feature list

        Hit 1  : <<Caffeine>>;<<C8H10N4O2>>;  CAS:58-08-2;  Mw:194;  Id:12345.  Lib: <<mainlib>>;  MF: 912;  RMF: 930;
        some trailing note that is not a hit
        """;

    final List<String> hits = hitLines(content, 7);
    assertNotNull(hits);
    assertEquals(1, hits.size());
    assertTrue(hits.get(0).contains("<<Caffeine>>"));
  }

  @Test
  @DisplayName("A block with no hits returns an empty list, not null")
  void emptyBlockReturnsEmptyList() throws IOException {

    final List<String> hits = hitLines("Unknown: Row 5 (unknown) of feature list\n", 5);
    assertNotNull(hits);
    assertTrue(hits.isEmpty());
  }
}
