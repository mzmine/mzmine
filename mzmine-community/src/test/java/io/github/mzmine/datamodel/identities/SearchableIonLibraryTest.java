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

package io.github.mzmine.datamodel.identities;

import static io.github.mzmine.datamodel.identities.iontype.IonTypes.ACETATE_AC;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.BR;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.CA;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.CA_H_MINUS;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.CL;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.FEII;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.FEIII_2H_MINUS;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.FEIII_H_MINUS;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.FEII_MINUS_H;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.FORMATE_FA;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.H;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.H2_MINUS;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.H2_PLUS;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.H_2H2O;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.H_3H2O;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.H_4H2O;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.H_H2O;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.H_MINUS;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.K;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.K_H;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M2_2H_PLUS;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M2_CL;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M2_H;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M2_H_H2O;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M2_H_MINUS;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M2_NA;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M2_NA2;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M2_NA_H;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M2_NH4;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M3_H;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M3_NA;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M4_H;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M_2PLUS;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M_MINUS;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M_PLUS;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.M_PLUS_H2O;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.NA;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.NA2;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.NA2_MINUS_H;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.NA_H;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.NA_H2O;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.NH4;
import static io.github.mzmine.datamodel.identities.iontype.IonTypes.NH4_H;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.mzmine.datamodel.identities.iontype.IonSearchRow;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypePair;
import io.github.mzmine.datamodel.identities.iontype.IonTypeSorting;
import io.github.mzmine.datamodel.identities.iontype.IonTypes;
import io.github.mzmine.datamodel.identities.iontype.SearchableIonLibrary;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.collections.CollectionUtils;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SearchableIonLibraryTest {

  private static SearchableIonLibrary library;
  // one adduct that is never used and stands as a negative test that it is not found
  private static IonType negative = IonTypes.FEIII.asIonType();
  private static final MZTolerance tolerance = new MZTolerance(1.5, 20);
  private static final MZTolerance narrow = new MZTolerance(0.001, 2);

  @BeforeAll
  static void setUp() {

    // default values
    List<IonType> ions = Stream.of(M_PLUS, M_PLUS_H2O, H, H_H2O, H_2H2O, H_3H2O, H_4H2O, NA, NA_H2O,
            K, NH4, M_2PLUS, H2_PLUS, CA, CA_H_MINUS, FEII, FEII_MINUS_H, FEIII_H_MINUS, FEIII_2H_MINUS,
            NA_H, NH4_H, K_H, NA2_MINUS_H, M2_H, M2_NA, NA2, M2_NA2, M2_NH4, M2_NA_H, M2_H_H2O, M_MINUS,
            H_MINUS, CL, BR, FORMATE_FA, ACETATE_AC, H2_MINUS, M2_H_MINUS, M2_CL, M2_2H_PLUS, M3_H,
            M4_H, M3_NA).map(IonTypes::asIonType)
        .sorted(IonTypeSorting.getIonTypeDefault().getComparator())
        .collect(CollectionUtils.toArrayList());

    int[] mol = {4, 5};
    final int initialSize = ions.size();
    for (int m : mol) {
      for (int i = 0; i < initialSize; i++) {
        IonType ion = ions.get(i);
        if (ion.molecules() == 1) {
          // add copies with more molecules
          final IonType molCopy = ion.withMolecules(m);
          ions.add(molCopy);
        }
      }
    }
    library = new SearchableIonLibrary(ions, true);
  }

  @Test
  void testMultimerToMono() {
    // 2M to 1M always works
    testPair(200, narrow, true, M2_H, H, 1);
    testPair(200, narrow, true, M2_H, H_H2O, 1);
    testPair(200, narrow, true, M2_H_H2O, H, 1);
    testPair(200, narrow, true, M3_H, H, 1);
    testPair(200, narrow, true, M3_H, M2_H, 1);
  }

  @Test
  void testMultimersWithCommonDivisor() {
    // will never allow to match M4 to M2 as this is the same as 1M to 2M
    testPair(200, narrow, true, M2_H, M4_H, H, M2_H, 1);
  }

  @Test
  void testMultiChargeMultimers() {
    // only difference is H2O.
    testPair(200, narrow, true, M2_2H_PLUS, H_H2O, 0);

    testPair(200, narrow, true, M2_2H_PLUS, NA, 2);
    // same mz should be empty
    testPair(200, narrow, true, M2_2H_PLUS, H, 0);

  }

  @Test
  void testMultiChargeMultimersWithoutChargeFilter() {
    // should not match as this is multiples where 2M has 2H and 1M has 1H
    testPair(200, narrow, false, M2_2H_PLUS, H_H2O, 0);
    // special case where it matches also reverse because mz is the same
    testPair(200, narrow, true, M2_NA_H, NA, 1);
    // multiple matches because charge filter off so 2M+2H is the same as M+H difference is just H to NA
    testPair(200, narrow, false, M2_2H_PLUS, NA, 5);
  }

  @Test
  void testBothMultiCharge() {
    testPair(200, narrow, true, M2_2H_PLUS, M2_NA2, H2_PLUS, NA2, 1);

    // matches the distance of (H to NA)/2 so other NA distances also match
    testPair(200, narrow, true, M2_2H_PLUS, M2_NA_H, 0);
    // no results because the distance is not specific enough.
    testPair(200, narrow, true, H2_PLUS, NA_H, 0);
  }

  @Test
  void testUnderdefinedCases() {
    //matches anything that is different by H like M+2H and M+H and M+Fe+2 and [M-H+Fe]+
    testPair(200, narrow, true, NA_H, NA, 0);
  }

  @Test
  void testEdgeCasesMayChange() {
    // only more results if charge filter is off
    testPair(200, narrow, false, H2_PLUS, NA_H, M2_NA_H, NA, 2);
    // not sure how to improve this and filter this out
    testPair(200, narrow, true, M2_NA_H, NA, 1);
  }

  @Test
  void searchRows() {
    // here we have different charge states so this works
    testPair(200, narrow, true, H2_PLUS, NA, 1);
    testPair(200, narrow, true, M2_2H_PLUS, M2_NA, H2_PLUS, NA, 1);
    testPair(200, narrow, true, H2_PLUS, M2_CL, 1);
    testPair(200, narrow, true, H, M2_CL, 1);
    testPair(200, narrow, true, H, M2_H, 1);
    testPair(200, narrow, true, H, NA, 1);
    testPair(200, narrow, true, H_H2O, H, 0);
    testPair(200, narrow, true, H_2H2O, NA_H2O, H_H2O, NA, 1);
    // both 2M with single charge will just match to single M version as this is the only thing know from delta mass
    testPair(200, narrow, true, M2_NA, M2_H, NA, H, 1);

    // special case where both are modified which is not allowed to not match too many pairs
    testPair(200, narrow, true, M2_H_H2O, H_H2O, 0);
  }

  private void testPair(double mass, MZTolerance tolerance, boolean useChargeFilter, IonTypes ta,
      IonTypes tb, int expectedResults) {
    testPair(mass, tolerance, useChargeFilter, ta, tb, ta, tb, expectedResults);
  }

  private void testPair(double mass, MZTolerance tolerance, boolean useChargeFilter, IonTypes ta,
      IonTypes tb, IonTypes expectedA, IonTypes expectedB, int expectedResults) {

    final IonType a = ta.asIonType();
    final IonType b = tb.asIonType();
    final double mzA = a.getMZ(mass);
    final double mzB = b.getMZ(mass);

    // use charge as filter or not
    final IonSearchRow sa = new IonSearchRow(mzA, useChargeFilter ? a.totalCharge() : null);
    final IonSearchRow sb = new IonSearchRow(mzB, useChargeFilter ? b.totalCharge() : null);
    var pairs = library.searchRows(sa, sb, tolerance);
    final String pairsString = StringUtils.join(pairs, ", ", Objects::toString);
    assertEquals(expectedResults, pairs.size(),
        "Number of pairs did not match (matches: %s)".formatted(pairsString));
    if (expectedResults > 0) {
      final IonTypePair targetPair = new IonTypePair(expectedA.asIonType(), expectedB.asIonType());
      Assertions.assertTrue(pairs.contains(targetPair),
          "Pair was not found: %s (actual: %s)".formatted(targetPair, pairsString));
    }

    // reverse should give same
    pairs = library.searchRows(sb, sa, tolerance);
    assertEquals(expectedResults, pairs.size(),
        "Number of pairs did not match (matches: %s)".formatted(pairsString));
    if (expectedResults > 0) {
      final IonTypePair targetPair = new IonTypePair(expectedB.asIonType(), expectedA.asIonType());
      Assertions.assertTrue(pairs.contains(targetPair),
          "Pair was not found in reverse search. This should never happen: " + targetPair);
    }

    // no match to negative
    assertFalse(pairs.stream().<IonType>mapMulti((pair, consumer) -> {
      consumer.accept(pair.a());
      consumer.accept(pair.b());
    }).anyMatch(ion -> negative.equals(ion)));
  }
}