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

package io.github.mzmine.datamodel.identities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonPartFrequency;
import io.github.mzmine.datamodel.identities.iontype.IonPartReference;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeRanking;
import io.github.mzmine.datamodel.identities.iontype.IonTypes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * The order of ion identities on a row must only depend on the ion types, their network sizes and
 * the user defined {@link IonTypeRanking} - never on the order in which they were found. See
 * {@link IonNetworkLogic#compareIonIdentitiesLikelyhood(IonTypeRanking, IonIdentity,
 * IonIdentity)}.
 */
class IonIdentitySortingTest {

  private static final IonTypeRanking RANKING = IonTypeRanking.createDefault();

  /**
   * Sorts the given ion types best first. All identities are without a network, so all network
   * sizes are equal and only the ranking applies.
   */
  private static List<IonType> sortBestFirst(final IonType... types) {
    return Arrays.stream(types).map(IonIdentity::new)
        .sorted(IonNetworkLogic.bestFirstSorter(RANKING)).map(IonIdentity::getIonType).toList();
  }

  @Test
  void prefersSimpleAdducts() {
    assertEquals(List.of(IonTypes.H.asIonType(), IonTypes.NA.asIonType(), IonTypes.NH4.asIonType(),
            IonTypes.K.asIonType()),
        sortBestFirst(IonTypes.NA.asIonType(), IonTypes.K.asIonType(), IonTypes.H.asIonType(),
            IonTypes.NH4.asIonType()));
  }

  @Test
  void prefersFewerNeutralModifications() {
    assertEquals(
        List.of(IonTypes.H.asIonType(), IonTypes.H_H2O.asIonType(), IonTypes.H_2H2O.asIonType()),
        sortBestFirst(IonTypes.H_2H2O.asIonType(), IonTypes.H.asIonType(),
            IonTypes.H_H2O.asIonType()));
  }

  @Test
  void prefersMonomerOverMultimer() {
    assertEquals(
        List.of(IonTypes.H.asIonType(), IonTypes.M2_H.asIonType(), IonTypes.M3_H.asIonType()),
        sortBestFirst(IonTypes.M3_H.asIonType(), IonTypes.M2_H.asIonType(),
            IonTypes.H.asIonType()));
  }

  /**
   * The charge state is not scored, prefilters take care of it. [M+H]+ and [M+2H]2+ therefore score
   * the same and only the smaller mass difference decides.
   */
  @Test
  void prefersLowerCharge() {
    assertEquals(List.of(IonTypes.H.asIonType(), IonTypes.H2_PLUS.asIonType()),
        sortBestFirst(IonTypes.H2_PLUS.asIonType(), IonTypes.H.asIonType()));
  }

  /**
   * An in-source water loss of a monomer beats a dimer of half the mass. This decides workshop
   * dataset row 121 in the integration tests.
   */
  @Test
  void prefersWaterLossOverDimer() {
    assertEquals(List.of(IonTypes.H_H2O.asIonType(), IonTypes.M2_H.asIonType()),
        sortBestFirst(IonTypes.M2_H.asIonType(), IonTypes.H_H2O.asIonType()));
  }

  @Test
  void appliesRankingInNegativeMode() {
    assertEquals(
        List.of(IonTypes.H_MINUS.asIonType(), IonTypes.CL.asIonType(), IonTypes.F.asIonType()),
        sortBestFirst(IonTypes.F.asIonType(), IonTypes.CL.asIonType(),
            IonTypes.H_MINUS.asIonType()));
  }

  /**
   * The comparator has to be a total order, otherwise the result depends on the insertion order of
   * the ion identities, which in turn depends on the iteration order of the correlation map.
   */
  @Test
  void orderIsIndependentOfInputOrder() {
    final List<IonType> all = IonTypes.valuesAsIonType();
    final List<IonType> expected = sortBestFirst(all.toArray(IonType[]::new));

    final Random random = new Random(42);
    for (int i = 0; i < 20; i++) {
      final List<IonType> shuffled = new ArrayList<>(all);
      Collections.shuffle(shuffled, random);
      assertEquals(expected, sortBestFirst(shuffled.toArray(IonType[]::new)),
          "Sorting is not stable for shuffle " + i);
    }
  }

  @Test
  void scoresByMeanFrequencyMinusMultimerPenalty() {
    // [M+H]+ is the reference with frequency 1 and no penalty
    assertEquals(1d, RANKING.score(IonTypes.H.asIonType()), 1e-6);
    // [2M+H]+ loses one multimer penalty
    assertEquals(1d - IonTypeRanking.MULTIMER_PENALTY, RANKING.score(IonTypes.M2_H.asIonType()),
        1e-6);
    // [M+2H]2+ is a single part with count 2 and the charge is not scored, so it ties with [M+H]+
    assertEquals(1d, RANKING.score(IonTypes.H2_PLUS.asIonType()), 1e-6);
    // mean of H2O and H
    final double h2o = RANKING.frequency(IonParts.H2O);
    assertEquals((h2o + 1d) / 2d, RANKING.score(IonTypes.H_H2O.asIonType()), 1e-6);
  }

  /**
   * Deprotonation reuses the H part with a negative count, so the count direction is part of the
   * reference. The count magnitude is not, therefore +2H matches the +H entry.
   */
  @Test
  void referenceMatchesCountDirectionAndIgnoresMass() {
    final IonPartReference protonation = IonPartReference.of(IonParts.H);
    assertTrue(protonation.matches(IonParts.H2_PLUS));
    assertFalse(protonation.matches(IonParts.H_MINUS));

    final IonPartReference deprotonation = IonPartReference.of(IonParts.H_MINUS);
    assertTrue(deprotonation.matches(IonParts.H_MINUS));
    assertFalse(deprotonation.matches(IonParts.H));
    assertEquals(protonation, deprotonation.withCountSign(1));

    // several losses of the same part share one entry
    assertTrue(IonPartReference.of(IonParts.H2O).matches(IonParts.H2O_2));

    assertEquals("+H+", protonation.toString());
    assertEquals("-H+", deprotonation.toString());
    assertEquals("+Fe+3", IonPartReference.of(IonParts.FEIII).toString());
    assertEquals("+Cl-", IonPartReference.of(IonParts.CL).toString());
    assertEquals("-H2O", IonPartReference.of(IonParts.H2O).toString());
    assertEquals("+H2O", IonPartReference.of(IonParts.H2O).withCountSign(1).toString());
  }

  /**
   * The ranking dialog lets the user type a building block, so the parsed part has to end up as the
   * reference the user typed.
   */
  @Test
  void typedDefinitionBecomesReference() {
    final IonPart calcium = IonParts.parseSilent("+Ca+2");
    assertNotNull(calcium);
    assertEquals("+Ca+2", IonPartReference.of(calcium).toString());

    final IonPart waterLoss = IonParts.parseSilent("-H2O");
    assertNotNull(waterLoss);
    assertEquals(IonPartReference.of(IonParts.H2O), IonPartReference.of(waterLoss));

    // nothing parsable, the dialog rejects the input
    assertNull(IonParts.parseSilent("not an ion"));
  }

  /**
   * The one merged ranking keeps the count directions apart: the direction {@link IonParts} defines
   * is listed, the opposite one stays unranked.
   */
  @Test
  void countDirectionSelectsTheFrequency() {
    // protonation and deprotonation are separate entries, both with frequency 1
    assertEquals(1f, RANKING.frequency(IonParts.H));
    assertEquals(1f, RANKING.frequency(IonParts.H_MINUS));
    // water is only ranked as a loss
    assertEquals(0.7f, RANKING.frequency(IonParts.H2O));
    assertEquals(IonTypeRanking.UNRANKED_FREQUENCY, RANKING.frequency(IonParts.H2O.withCount(1)));
    // acetonitrile only as an addition
    assertEquals(0.2f, RANKING.frequency(IonParts.ACN));
    assertEquals(IonTypeRanking.UNRANKED_FREQUENCY, RANKING.frequency(IonParts.ACN.withCount(-1)));
  }

  @Test
  void unlistedPartsCountAsZero() {
    final IonTypeRanking onlyProton = new IonTypeRanking(
        List.of(IonPartFrequency.of(IonParts.H, 1f)));
    // Na is not listed, so [M+Na]+ scores 0 while [M+H]+ still scores 1
    assertEquals(0d, onlyProton.score(IonTypes.NA.asIonType()), 1e-6);
    assertEquals(1d, onlyProton.score(IonTypes.H.asIonType()), 1e-6);
  }

}
