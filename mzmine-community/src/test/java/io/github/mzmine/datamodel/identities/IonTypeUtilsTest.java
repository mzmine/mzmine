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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeUtils;
import io.github.mzmine.datamodel.identities.iontype.IonTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class IonTypeUtilsTest {

  enum OverlapTestCase {
    H_NA(IonTypes.H, IonTypes.NA, true), //
    M2NA_NA(IonTypes.M2_NA, IonTypes.NA, true), //
    M2H_H2O_H_H2O(IonTypes.M2_H_H2O, IonTypes.H_H2O, true), //
    // flipping count sign is ignored: H in both ions is ignored to allow +H -H
    H_ADDITION_TO_LOSS(IonTypes.H, IonTypes.H_MINUS, true), //
    // - and +H2O is not allowed
    H2O_LOSS_TO_H2O_ADDITION(IonTypes.H_H2O.asIonType(),
        IonType.create(IonParts.NA, IonParts.H2O.withCount(1)), true), //
    H_NA2_MINUS_H(IonTypes.H, IonTypes.NA2_MINUS_H, true), //
    // all false
    H2PLUS_H_H2O(IonTypes.H2_PLUS, IonTypes.H_H2O, false), //
    H_H_H2O(IonTypes.H, IonTypes.H_H2O, false), //
    // false because 2M2H is multiple of 1M1H
    M2_H2PLUS_H2O(IonTypes.M2_2H_PLUS, IonTypes.H_H2O, false), //
    ;

    final IonType a;
    final IonType b;
    final boolean expected;

    OverlapTestCase(IonType a, IonType b, boolean expected) {
      this.a = a;
      this.b = b;
      this.expected = expected;
    }

    OverlapTestCase(IonTypes a, IonTypes b, boolean expected) {
      this(a.asIonType(), b.asIonType(), expected);
    }

    void test() {
      assertEquals(expected, IonTypeUtils.restrictPartsOverlapToMultimers(a, b));
    }
  }

  @ParameterizedTest
  @EnumSource(OverlapTestCase.class)
  void testRestrictPartsOverlapToMultimers(OverlapTestCase testCase) {
    testCase.test();
  }

  @Test
  void checkMolCount() {
    assertTrue(IonTypeUtils.checkMolCount(1, 1));
    assertTrue(IonTypeUtils.checkMolCount(1, 2));
    assertTrue(IonTypeUtils.checkMolCount(1, 4));
    assertTrue(IonTypeUtils.checkMolCount(4, 1));
    assertTrue(IonTypeUtils.checkMolCount(2, 5));
    assertTrue(IonTypeUtils.checkMolCount(5, 2));
    // never allow multiples
    assertFalse(IonTypeUtils.checkMolCount(2, 6));
    assertFalse(IonTypeUtils.checkMolCount(2, 4));
    assertFalse(IonTypeUtils.checkMolCount(4, 2));
  }
}