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

package io.github.mzmine.datamodel.identities.fx.sub;

import io.github.mzmine.datamodel.identities.IonPart;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.IonType.IonTypeStringFlavor;
import java.util.Comparator;
import java.util.List;

public enum IonSorting {
  ALPHABETICAL, CHARGE_THEN_MASS, MOLECULES_THEN_CHARGE_THEN_MASS, MASS;

  public static IonSorting getIonPartDefault() {
    return CHARGE_THEN_MASS;
  }

  public static IonSorting getIonTypeDefault() {
    return MOLECULES_THEN_CHARGE_THEN_MASS;
  }


  public static List<IonSorting> valuesForIonTypes() {
    return List.of(ALPHABETICAL, CHARGE_THEN_MASS, MASS);
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

  public Comparator<IonPart> createIonPartComparator() {
    return switch (this) {
      case ALPHABETICAL ->
          Comparator.comparing(IonPart::name).thenComparingInt(IonPart::totalCharge)
              .thenComparingDouble(IonPart::totalMass);
      case CHARGE_THEN_MASS, MOLECULES_THEN_CHARGE_THEN_MASS ->
          Comparator.comparingInt(IonPart::totalCharge).thenComparingDouble(IonPart::totalMass);
      case MASS ->
          Comparator.comparingDouble(IonPart::totalMass).thenComparing(IonPart::totalCharge);
    };
  }

  public Comparator<IonType> createIonTypeComparator() {
    return switch (this) {
      case ALPHABETICAL -> Comparator.comparingInt(IonType::molecules)
          .thenComparing(ion -> ion.toString(IonTypeStringFlavor.FOR_ALPHABETICAL_SORTING))
          .thenComparingInt(IonType::totalCharge).thenComparingDouble(IonType::totalMass);
      case CHARGE_THEN_MASS ->
          Comparator.comparingInt(IonType::totalCharge).thenComparingDouble(IonType::totalMass);
      case MOLECULES_THEN_CHARGE_THEN_MASS ->
          Comparator.comparingInt(IonType::molecules).thenComparingInt(IonType::totalCharge)
              .thenComparingDouble(IonType::totalMass);
      case MASS ->
          Comparator.comparingDouble(IonType::totalMass).thenComparing(IonType::totalCharge);
    };
  }


}
