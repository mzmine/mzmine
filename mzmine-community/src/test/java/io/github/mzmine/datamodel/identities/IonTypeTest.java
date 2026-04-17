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

import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypes;
import io.github.mzmine.util.FormulaUtils;
import java.util.List;
import org.junit.jupiter.api.Test;

class IonTypeTest {

  @Test
  void hasPartsOverlap() {
    assertTrue(IonTypes.NA_H.asIonType().hasPartsOverlap(IonTypes.H.asIonType()));
    assertFalse(IonTypes.NA_H.asIonType().hasPartsOverlap(IonTypes.FEII.asIonType()));
    assertTrue(IonTypes.NA_H.asIonType().hasPartsOverlap(IonTypes.H.asIonType(), true));

    assertFalse(IonTypes.NA_H.asIonType().hasPartsOverlap(IonTypes.FEII.asIonType(), true));

    // different +H-H
    assertTrue(IonTypes.NA2_MINUS_H.asIonType().hasPartsOverlap(IonTypes.H.asIonType(), false));
    assertFalse(IonTypes.NA2_MINUS_H.asIonType().hasPartsOverlap(IonTypes.H.asIonType(), true));
  }

  @Test
  void streamPartsOverlap() {
    final IonType H = IonTypes.H.asIonType();
    assertEquals(List.of(IonParts.H),
        IonTypes.NA_H.asIonType().streamPartsOverlap(H).map(p -> p[0]).toList());
    assertEquals(List.of(IonParts.H),
        IonTypes.NA_H.asIonType().streamPartsOverlap(H, true).map(p -> p[0]).toList());
    assertEquals(List.of(IonParts.H),
        IonTypes.NA_H.asIonType().streamPartsOverlap(H, false).map(p -> p[0]).toList());
    assertEquals(List.of(IonParts.ofFormula("H", 1)),
        IonTypes.NA_H.asIonType().streamPartsOverlap(H).map(p -> p[0]).toList());
    assertEquals(List.of(),
        IonTypes.NA_H.asIonType().streamPartsOverlap(IonTypes.FEII.asIonType()).map(p -> p[0])
            .toList());
    // ignore
    assertEquals(List.of(),
        IonTypes.NA2_MINUS_H.asIonType().streamPartsOverlap(H, true).map(p -> p[0]).toList());
  }

  @Test
  void hasSameAdducts() {
    assertTrue(IonTypes.H_H2O.asIonType().hasSameAdducts(IonTypes.H.asIonType()));
    assertFalse(IonTypes.NA_H.asIonType().hasSameAdducts(IonTypes.H.asIonType()));
    assertFalse(IonTypes.NA_H.asIonType().hasSameAdducts(IonTypes.FEII.asIonType()));
  }

  @Test
  void streamChargedAdducts() {
    assertEquals(List.of(IonParts.H, IonParts.NA),
        IonTypes.NA_H.asIonType().streamChargedAdducts().toList());
    assertEquals(List.of(IonParts.H), IonTypes.H_H2O.asIonType().streamChargedAdducts().toList());
  }

  @Test
  void streamNeutralMods() {
    assertEquals(List.of(), IonTypes.NA_H.asIonType().streamNeutralMods().toList());
    assertEquals(List.of(IonParts.H2O), IonTypes.H_H2O.asIonType().streamNeutralMods().toList());
  }

  @Test
  void equals() {
    assertEquals(IonTypes.H.asIonType(), IonType.create(IonParts.H));
  }

  @Test
  void addToFormula_doesNotMutateInput() {
    var formula = FormulaUtils.parse("C6H12O6");
    String before = FormulaUtils.getFormulaString(formula);

    IonType mPlusH = IonTypes.H.asIonType();
    var ionized = mPlusH.addToFormula(formula, true);

    // input must be unchanged
    assertEquals(before, FormulaUtils.getFormulaString(formula),
        "addToFormula must not mutate the input formula");
    // returned formula has one extra H (charge shown by default)
    assertEquals("C6H13O6+", FormulaUtils.getFormulaString(ionized));
  }

  @Test
  void merge_deduplicatesSilentCharges() {
    // Both IonTypes carry a SILENT_CHARGE; merging must yield exactly one after deduplication
    IonType t1 = IonType.create(IonParts.H, IonParts.SILENT_CHARGE);
    IonType t2 = IonType.create(IonParts.NA, IonParts.SILENT_CHARGE);
    IonType merged = t1.merge(t2);
    long silentCount = merged.streamAll().filter(IonPart::isSilentCharge).count();
    assertEquals(1, silentCount, "merge() should deduplicate SILENT_CHARGE parts into one");
  }
}