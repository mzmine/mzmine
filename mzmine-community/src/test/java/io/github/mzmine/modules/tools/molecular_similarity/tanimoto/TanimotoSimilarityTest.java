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

package io.github.mzmine.modules.tools.molecular_similarity.tanimoto;

import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.datamodel.structures.StructureInputType;
import io.github.mzmine.datamodel.structures.StructureParser;
import java.util.BitSet;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openscience.cdk.interfaces.IAtomContainer;

class TanimotoSimilarityTest {

  // caffeine and theobromine differ only by a methyl group -> high but not identical similarity
  private static final String CAFFEINE = "Cn1cnc2c1c(=O)n(C)c(=O)n2C";
  private static final String THEOBROMINE = "Cn1cnc2c1c(=O)[nH]c(=O)n2C";
  private static final String ETHANOL = "CCO";

  private static final StructureParser PARSER = new StructureParser(true);

  private static IAtomContainer parse(final String smiles) {
    final MolecularStructure structure = PARSER.parseStructure(smiles, StructureInputType.SMILES);
    Assertions.assertNotNull(structure, "could not parse " + smiles);
    return structure.structure();
  }

  @ParameterizedTest
  @EnumSource(FingerprintType.class)
  void everyFingerprintTypeProducesAFingerprint(final FingerprintType type) {
    final TanimotoSimilarity tanimoto = new TanimotoSimilarity(type);
    final BitSet fp = tanimoto.getFingerprint(parse(CAFFEINE));
    Assertions.assertNotNull(fp, "fingerprint was null for " + type);
    Assertions.assertTrue(fp.cardinality() > 0, "no bits set for " + type);
  }

  @ParameterizedTest
  @EnumSource(FingerprintType.class)
  void identicalStructuresAreFullySimilar(final FingerprintType type) {
    final TanimotoSimilarity tanimoto = new TanimotoSimilarity(type);
    final BitSet a = tanimoto.getFingerprint(parse(CAFFEINE));
    final BitSet b = tanimoto.getFingerprint(parse(CAFFEINE));
    Assertions.assertNotNull(a);
    Assertions.assertNotNull(b);
    Assertions.assertEquals(1.0f, TanimotoSimilarity.maxTanimoto(List.of(a), List.of(b)), 1e-4f,
        "identical structures should score 1.0 for " + type);
  }

  @ParameterizedTest
  @EnumSource(FingerprintType.class)
  void differentStructuresScoreBelowOne(final FingerprintType type) {
    final TanimotoSimilarity tanimoto = new TanimotoSimilarity(type);
    final BitSet caffeine = tanimoto.getFingerprint(parse(CAFFEINE));
    final BitSet ethanol = tanimoto.getFingerprint(parse(ETHANOL));
    Assertions.assertNotNull(caffeine);
    Assertions.assertNotNull(ethanol);
    final float sim = TanimotoSimilarity.maxTanimoto(List.of(caffeine), List.of(ethanol));
    Assertions.assertTrue(sim >= 0.0f && sim < 1.0f,
        "expected 0 <= sim < 1 for " + type + " but was " + sim);
  }

  @ParameterizedTest
  @EnumSource(FingerprintType.class)
  void similarStructuresScoreHigherThanDissimilar(final FingerprintType type) {
    final TanimotoSimilarity tanimoto = new TanimotoSimilarity(type);
    final BitSet caffeine = tanimoto.getFingerprint(parse(CAFFEINE));
    final BitSet theobromine = tanimoto.getFingerprint(parse(THEOBROMINE));
    final BitSet ethanol = tanimoto.getFingerprint(parse(ETHANOL));
    final float similar = TanimotoSimilarity.maxTanimoto(List.of(caffeine), List.of(theobromine));
    final float dissimilar = TanimotoSimilarity.maxTanimoto(List.of(caffeine), List.of(ethanol));
    Assertions.assertTrue(similar > dissimilar,
        type + ": caffeine~theobromine (" + similar + ") should exceed caffeine~ethanol ("
            + dissimilar + ")");
  }

  @ParameterizedTest
  @EnumSource(FingerprintType.class)
  void maxTanimotoTakesTheBestPairAcrossLists(final FingerprintType type) {
    final TanimotoSimilarity tanimoto = new TanimotoSimilarity(type);
    final BitSet caffeine = tanimoto.getFingerprint(parse(CAFFEINE));
    final BitSet ethanol = tanimoto.getFingerprint(parse(ETHANOL));
    // list A holds caffeine + ethanol, list B holds only ethanol -> best pair is ethanol vs ethanol
    final float max = TanimotoSimilarity.maxTanimoto(List.of(caffeine, ethanol), List.of(ethanol));
    Assertions.assertEquals(1.0f, max, 1e-4f,
        "max should pick the identical ethanol pair for " + type);
  }

  @ParameterizedTest
  @EnumSource(FingerprintType.class)
  void emptyListYieldsZero(final FingerprintType type) {
    final TanimotoSimilarity tanimoto = new TanimotoSimilarity(type);
    final BitSet caffeine = tanimoto.getFingerprint(parse(CAFFEINE));
    Assertions.assertEquals(0.0f, TanimotoSimilarity.maxTanimoto(List.of(caffeine), List.of()),
        "empty list should yield 0 for " + type);
  }
}
