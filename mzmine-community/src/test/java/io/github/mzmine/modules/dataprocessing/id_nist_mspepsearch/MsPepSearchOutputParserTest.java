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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MsPepSearchOutputParser} against output captured from MSPepSearch 0.9.7.5 searching
 * the NIST 26 libraries.
 */
class MsPepSearchOutputParserTest {

  private static NistPepSearchResult parseFixture(final String name) throws Exception {
    final URL url = MsPepSearchOutputParserTest.class.getResource("/nist/" + name);
    assertNotNull(url, () -> "missing test fixture /nist/" + name);
    return MsPepSearchOutputParser.parse(Path.of(url.toURI()).toFile());
  }

  @Test
  @DisplayName("Low resolution EI identity output: MF is the score, NumMP the overlap")
  void eiIdentity() throws Exception {

    final NistPepSearchResult result = parseFixture("mspepsearch_ei_identity.tsv");

    assertTrue(result.warnings().isEmpty(), () -> "unexpected warnings: " + result.warnings());
    assertFalse(result.hits().isEmpty());

    final NistHit first = result.hits().getFirst();
    assertEquals(1, first.specNum());
    assertEquals(1, first.rank());
    assertEquals("Scan 1467 (28.976 min) of JAN310301003.d 10149", first.unknownName());
    assertEquals("replib", first.libraryName());
    assertEquals("21923", first.entryId());
    assertEquals("Spiro[furan-2(3H),1'(3'H)-isobenzofuran]-3,3'-dione, 4-phenyl-", first.name());
    assertEquals("C17H10O4", first.formula());
    assertEquals("38183129", first.cas());
    assertEquals("136172", first.nistNumber());
    assertEquals("ZFKJVJIDPQDDFY-UHFFFAOYSA-N", first.inChIKey());
    assertEquals(278.058, first.exactMass());
    assertEquals(278, first.nominalMw());

    // MF, not DotProd (999) and not R.Match
    assertEquals(980, first.matchFactor());
    assertEquals(980, first.revMatchFactor());
    assertEquals(999, first.dotProduct());
    assertEquals(93.8, first.probability());
    assertEquals(73, first.numMatchedPeaks());
    assertEquals(74, first.numPeaks());
    assertEquals(0.98, first.score0to1(), 1e-9);

    // no high resolution columns in this mode
    assertNull(first.precursorType());

    // 16 spectra were searched, the first one has 5 hits
    assertEquals(16, result.hits().stream().map(NistHit::specNum).distinct().count());
    assertEquals(5, result.hits().stream().filter(hit -> hit.specNum() == 1).count());
  }

  @Test
  @DisplayName("High resolution MS/MS output: Score is the score, Rev-Dot the reverse match")
  void msmsHiRes() throws Exception {

    final NistPepSearchResult result = parseFixture("mspepsearch_msms_hires.tsv");

    assertTrue(result.warnings().isEmpty(), () -> "unexpected warnings: " + result.warnings());
    assertEquals(2, result.hits().size());

    final NistHit first = result.hits().getFirst();
    assertEquals(1, first.specNum());
    assertEquals("Desipramine", first.unknownName());
    assertEquals("Desipramine", first.name());
    assertEquals("hr_msms_nist", first.libraryName());
    assertEquals("C18H22N2", first.formula());
    assertEquals("[M+H]+", first.precursorType());
    assertEquals(1, first.charge());
    assertEquals("30", first.collisionEnergy());
    assertEquals("Q-TOF", first.instrumentType());
    assertEquals("50475", first.cas());

    // Score (114), not Dot Product (254) and not Rev-Dot (999)
    assertEquals(114, first.matchFactor());
    assertEquals(254, first.dotProduct());
    assertEquals(999, first.revMatchFactor());
    assertEquals(1, first.numMatchedPeaks());

    // the library precursor m/z, not the search spectrum's 267.19
    assertEquals(267.1856, first.precursorMz());

    // free text collision energy on the second hit must not break anything
    assertEquals("NCE=65% 34eV", result.hits().get(1).collisionEnergy());
  }

  @Test
  @DisplayName("Unrecognised columns do not shift the recognised ones")
  void unknownColumnsAreIgnored() throws Exception {

    // this fixture carries RI and uRI columns, which mzmine does not use, between NumMP and
    // Num.Peaks - header driven parsing has to find Num.Peaks at its actual position
    final NistPepSearchResult result = parseFixture("mspepsearch_ei_extra_columns.tsv");

    assertTrue(result.warnings().isEmpty(), () -> "unexpected warnings: " + result.warnings());
    assertFalse(result.hits().isEmpty());

    final NistHit first = result.hits().getFirst();
    assertEquals("mzmine_101_0", first.unknownName());
    assertEquals(74, first.numPeaks());
    assertEquals(73, first.numMatchedPeaks());
    assertEquals(980, first.matchFactor());
  }

  @Test
  @DisplayName("A run without hits has no header at all and is not an error")
  void noHits() throws Exception {

    final NistPepSearchResult result = parseFixture("mspepsearch_no_hits.tsv");

    assertTrue(result.hits().isEmpty());
    assertTrue(result.warnings().isEmpty(), () -> "unexpected warnings: " + result.warnings());
  }

  @Test
  @DisplayName("The mzmine mapping key survives the round trip through the Unknown column")
  void mapsByUnknownName() throws Exception {

    final NistPepSearchResult result = parseFixture("mspepsearch_ei_extra_columns.tsv");

    // the task maps hits back to their query by the Unknown column when the Num column is absent,
    // so every hit of a query has to carry the name mzmine submitted
    assertEquals(3,
        result.hits().stream().filter(hit -> "mzmine_101_0".equals(hit.unknownName())).count());
  }

  @Test
  @DisplayName("A malformed row is a warning, not an exception")
  void malformedRowIsWarned() throws IOException {

    final String content = """
        > MSPepSearch banner
        Num\tUnknown\tRank\tLibrary\tId\tMF\tName\tFormula
        1\tmzmine_1_0\t1\tmainlib\t42\t900\tCaffeine\tC8H10N4O2
        1\tmzmine_1_0\t2\tmainlib\t43\t800
        \t\t\t\t\t\t\t
        2\tmzmine_2_0\t1\tmainlib\t44\t700\tNaphthalene\tC10H8
        > Completed. 2 spectra
        """;

    final NistPepSearchResult result = MsPepSearchOutputParser.parse(new StringReader(content));

    // the truncated row has no name, so it is dropped with a warning; the others survive
    assertEquals(2, result.hits().size());
    assertEquals(1, result.warnings().size());
    assertTrue(result.warnings().getFirst().contains("no compound name"));
    assertEquals("Caffeine", result.hits().getFirst().name());
    assertEquals("Naphthalene", result.hits().get(1).name());
  }

  @Test
  @DisplayName("Alternative column labels are recognised")
  void alternativeLabels() throws IOException {

    // an older/other build naming the reverse match RMF and the score MF
    final String content = """
        Num\tUnknown\tRank\tLib\tId\tMF\tRMF\tName\tCAS r.n.
        1\tq0\t1\tmainlib\t42\t900\t950\tCaffeine\t0
        """;

    final NistPepSearchResult result = MsPepSearchOutputParser.parse(new StringReader(content));

    assertEquals(1, result.hits().size());
    final NistHit hit = result.hits().getFirst();
    assertEquals(900, hit.matchFactor());
    assertEquals(950, hit.revMatchFactor());
    assertEquals("mainlib", hit.libraryName());
    // NIST writes 0 when it has no CAS number
    assertNull(hit.cas());
  }
}
