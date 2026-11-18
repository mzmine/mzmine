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

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistPepSearchTask.QuerySpectrum;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistPepSearchTask.SearchRun;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link NistPepSearchTask#resolveQueryPosition(int, String, int, Map)}, which decides on
 * which row a hit lands, and how the queries are split into MSPepSearch runs.
 */
class NistPepSearchTaskTest {

  private static final PolarityType POS = PolarityType.POSITIVE;
  private static final PolarityType NEG = PolarityType.NEGATIVE;
  private static final PolarityType UNKNOWN = PolarityType.UNKNOWN;

  /**
   * A query as far as the grouping looks at it: the row and scan are not needed.
   */
  private static QuerySpectrum query(final String name, final Integer nominalMw,
      final PolarityType polarity) {
    return new QuerySpectrum(name, null, null, new DataPoint[0], nominalMw, polarity);
  }

  /**
   * The names of the queries per run, in run order.
   */
  private static Map<SearchRun, List<String>> names(
      final Map<SearchRun, List<QuerySpectrum>> runs) {

    final Map<SearchRun, List<String>> names = new LinkedHashMap<>();
    runs.forEach(
        (run, queries) -> names.put(run, queries.stream().map(QuerySpectrum::name).toList()));
    return names;
  }

  @Test
  @DisplayName("MS/MS queries are searched once per polarity, unknown polarity in a run of its own")
  void msmsRunsPerPolarity() {

    final List<QuerySpectrum> queries = List.of(query("p1", 194, POS), query("n1", 194, NEG),
        query("p2", 300, POS), query("u1", 120, UNKNOWN), query("n2", 88, NEG));

    final Map<SearchRun, List<QuerySpectrum>> runs = NistPepSearchTask.groupQueries(queries,
        NistSearchMode.MSMS_HIRES);

    // first seen order, and the query order within each run; no /MwForLoss outside the hybrid
    assertEquals(
        List.of(new SearchRun(null, POS), new SearchRun(null, NEG), new SearchRun(null, UNKNOWN)),
        List.copyOf(runs.keySet()));
    assertEquals(Map.of(new SearchRun(null, POS), List.of("p1", "p2"), new SearchRun(null, NEG),
        List.of("n1", "n2"), new SearchRun(null, UNKNOWN), List.of("u1")), names(runs));
  }

  @Test
  @DisplayName("Queries of one polarity are a single run")
  void singlePolarityIsOneRun() {

    final List<QuerySpectrum> queries = List.of(query("n1", null, NEG), query("n2", null, NEG));

    assertEquals(Map.of(new SearchRun(null, NEG), List.of("n1", "n2")),
        names(NistPepSearchTask.groupQueries(queries, NistSearchMode.MSMS_HIRES)));
  }

  @Test
  @DisplayName("The hybrid search groups by molecular weight and skips queries without one")
  void hybridRunsPerMolecularWeight() {

    final List<QuerySpectrum> queries = List.of(query("a", 278, UNKNOWN), query("b", 150, UNKNOWN),
        query("none", null, UNKNOWN), query("c", 278, UNKNOWN));

    assertEquals(Map.of(new SearchRun(278, UNKNOWN), List.of("a", "c"), new SearchRun(150, UNKNOWN),
        List.of("b")), names(NistPepSearchTask.groupQueries(queries, NistSearchMode.GC_EI_HYBRID)));
  }

  @Test
  @DisplayName("The other EI searches run once per polarity without /MwForLoss")
  void eiRunsPerPolarity() {

    final List<QuerySpectrum> queries = List.of(query("a", 278, POS), query("b", 150, POS),
        query("c", 150, NEG));

    assertEquals(
        Map.of(new SearchRun(null, POS), List.of("a", "b"), new SearchRun(null, NEG), List.of("c")),
        names(NistPepSearchTask.groupQueries(queries, NistSearchMode.GC_EI_IDENTITY)));
  }

  @Test
  @DisplayName("Only a positive or negative scan gives a query a polarity")
  void queryPolarity() {

    assertEquals(POS, NistPepSearchTask.queryPolarity(POS));
    assertEquals(NEG, NistPepSearchTask.queryPolarity(NEG));
    for (final PolarityType undefined : Arrays.asList(PolarityType.NEUTRAL, PolarityType.ANY,
        UNKNOWN, null)) {
      assertEquals(UNKNOWN, NistPepSearchTask.queryPolarity(undefined));
    }
  }

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
