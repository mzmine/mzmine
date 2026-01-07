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

package io.github.mzmine.datamodel.identities.iontype;

import io.github.mzmine.datamodel.identities.iontype.IonType.IonTypeStringFlavor;
import java.util.Comparator;

public enum IonTypeSorting {
  ALPHABETICAL( //
      Comparator.comparingInt(IonType::molecules)
          .thenComparing(ion -> ion.toString(IonTypeStringFlavor.FOR_ALPHABETICAL_SORTING))
          .thenComparingInt(IonType::totalCharge).thenComparingDouble(IonType::totalMass)),
  /**
   *
   */
  CHARGE_THEN_MASS(//
      Comparator.comparingInt(IonType::totalCharge).thenComparingDouble(IonType::totalMass)),
  /**
   *
   */
  MOLECULES_THEN_CHARGE_THEN_MASS( //
      Comparator.comparingInt(IonType::molecules).thenComparingInt(IonType::totalCharge)
          .thenComparingDouble(IonType::totalMass)),
  /**
   *
   */
  MASS(Comparator.comparingDouble(IonType::totalMass).thenComparing(IonType::totalCharge));

  /**
   * By molecules then charge then mass
   *
   */
  public static IonTypeSorting getIonTypeDefault() {
    return MOLECULES_THEN_CHARGE_THEN_MASS;
  }

  private final Comparator<IonType> comparator;

  IonTypeSorting(Comparator<IonType> comparator) {
    this.comparator = comparator;
  }

  @Override
  public String toString() {
    return switch (this) {
      case ALPHABETICAL -> "alphabetic";
      case CHARGE_THEN_MASS -> "charge, Δmass";
      case MOLECULES_THEN_CHARGE_THEN_MASS -> "molecules, charge, Δmass";
      case MASS -> "Δmass";
    };
  }

  public Comparator<IonType> getComparator() {
    return comparator;
  }

}
