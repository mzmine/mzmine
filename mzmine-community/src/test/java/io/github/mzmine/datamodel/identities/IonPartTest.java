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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.util.FormulaUtils;
import org.junit.jupiter.api.Test;

class IonPartTest {

  @Test
  void equalsWithoutCount() {
    assertTrue(IonParts.H.equalsWithoutCount(new IonPart("H", 1, 2)));
  }

  @Test
  void testEquals() {
    assertEquals(IonParts.H, new IonPart("H", 1));
    assertEquals(IonParts.H2_PLUS, new IonPart("H", 1, 2));
  }

  @Test
  void testWithCharge() {
    final IonPart h = IonParts.H;
    IonPart changed = h.withSingleCharge(-1);
    assertEquals(h.absSingleMass() + 2 * FormulaUtils.electronMass, changed.absSingleMass(),
        0.000001);

    changed = h.withSingleCharge(2);
    assertEquals(h.absSingleMass() - 1 * FormulaUtils.electronMass, changed.absSingleMass(),
        0.000001);

    // undefined formula
    IonPart named = IonPart.ofNamed("TE", 50d, 0);
    changed = named.withSingleCharge(-1);

    assertEquals(named.absSingleMass() + 1 * FormulaUtils.electronMass, changed.absSingleMass(),
        0.000001);

    changed = named.withSingleCharge(2);
    assertEquals(Math.abs(named.absSingleMass() - 2 * FormulaUtils.electronMass),
        changed.absSingleMass(), 0.000001);

    // illegal to change charge state of unknown where formula and mass is undefined
    assertThrows(IllegalStateException.class,
        () -> IonPart.ofNamed("unknown", 0d, 0).withSingleCharge(-1));
  }
}