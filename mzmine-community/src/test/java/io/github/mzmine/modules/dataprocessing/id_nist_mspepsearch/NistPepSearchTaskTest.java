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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link NistPepSearchTask#resolveQueryPosition(int, String, int, Map)}, which decides on
 * which row a hit lands.
 */
class NistPepSearchTaskTest {

  /**
   * Three queries submitted to one run, as NistPepSearchTask#applyHits builds the lookup.
   */
  private static final Map<String, Integer> POSITIONS = Map.of("mzmine_11_0", 0, "mzmine_11_1", 1,
      "mzmine_47_0", 2);

  private static final int QUERY_COUNT = 3;

  @Test
  @DisplayName("Num is a position within the submitted list, counted from 1")
  void specNumIsAPositionInTheRun() {

    assertEquals(0,
        NistPepSearchTask.resolveQueryPosition(1, "mzmine_11_0", QUERY_COUNT, POSITIONS));
    assertEquals(1,
        NistPepSearchTask.resolveQueryPosition(2, "mzmine_11_1", QUERY_COUNT, POSITIONS));
    assertEquals(2,
        NistPepSearchTask.resolveQueryPosition(3, "mzmine_47_0", QUERY_COUNT, POSITIONS));
  }

  @Test
  @DisplayName("Every run restarts at 1, so a grouped search maps by position and not by ordinal")
  void groupedSearchMapsByPositionWithinTheRun() {

    // second run of a grouped (hybrid) search: these two queries are number 4 and 5 of the feature
    // list but MSPepSearch calls them 1 and 2, and both have to resolve to this run's own list
    final Map<String, Integer> secondRun = Map.of("mzmine_90_0", 0, "mzmine_91_0", 1);

    assertEquals(0, NistPepSearchTask.resolveQueryPosition(1, "mzmine_90_0", 2, secondRun));
    assertEquals(1, NistPepSearchTask.resolveQueryPosition(2, "mzmine_91_0", 2, secondRun));
  }

  @Test
  @DisplayName("A Num beyond the submitted list falls back to the Unknown name")
  void outOfRangeSpecNumFallsBackToTheName() {

    assertEquals(2,
        NistPepSearchTask.resolveQueryPosition(99, "mzmine_47_0", QUERY_COUNT, POSITIONS));
    // -1 is what the parser reports when the output has no Num column at all
    assertEquals(1,
        NistPepSearchTask.resolveQueryPosition(-1, "mzmine_11_1", QUERY_COUNT, POSITIONS));
  }

  @Test
  @DisplayName("The name fallback is case insensitive, as NIST may change the case")
  void theNameFallbackIsCaseInsensitive() {
    assertEquals(0,
        NistPepSearchTask.resolveQueryPosition(-1, "MZMINE_11_0", QUERY_COUNT, POSITIONS));
  }

  @Test
  @DisplayName("A hit that matches neither key is reported as unmapped")
  void unmappableHit() {

    assertEquals(-1,
        NistPepSearchTask.resolveQueryPosition(-1, "Scan 1467 of some other file", QUERY_COUNT,
            POSITIONS));
    assertEquals(-1, NistPepSearchTask.resolveQueryPosition(0, null, QUERY_COUNT, POSITIONS));
    assertEquals(-1, NistPepSearchTask.resolveQueryPosition(4, null, QUERY_COUNT, POSITIONS));
  }
}
