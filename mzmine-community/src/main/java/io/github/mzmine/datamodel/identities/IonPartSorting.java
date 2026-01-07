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

import java.util.Comparator;
import java.util.List;

public enum IonPartSorting {
  /**
   * DEFAULT: losses first then additions as in M-H2O+H. Each sorted by name, totalCharge,
   * totalMass
   */
  DEFAULT_NEUTRAL_THEN_LOSSES_THEN_ADDED(
      // neutral first then charged in each: first losses then additions
      Comparator.comparing(IonPart::isCharged).thenComparing(IonPart::isAddition)
          .thenComparing(IonPart::name).thenComparingInt(IonPart::totalCharge)
          .thenComparingDouble(IonPart::totalMass)),
  /**
   * Used for sorting by name which is just the part name and then the charge and mass
   */
  ALPHABETICAL( //
      Comparator.comparing(IonPart::name).thenComparingInt(IonPart::totalCharge)
          .thenComparingDouble(IonPart::totalMass)),
  /**
   *
   */
  CHARGE_THEN_MASS( //
      Comparator.comparingInt(IonPart::totalCharge).thenComparingDouble(IonPart::totalMass)),
  /**
   *
   */
  MASS(Comparator.comparingDouble(IonPart::totalMass).thenComparing(IonPart::totalCharge));

  private final Comparator<IonPart> comparator;

  IonPartSorting(Comparator<IonPart> comparator) {
    this.comparator = comparator;
  }

  public static List<IonPartSorting> valuesForDefinitions() {
    return List.of(ALPHABETICAL, CHARGE_THEN_MASS, MASS);
  }

  @Override
  public String toString() {
    return switch (this) {
      case DEFAULT_NEUTRAL_THEN_LOSSES_THEN_ADDED -> "Losses then added";
      case ALPHABETICAL -> "alphabetic";
      case CHARGE_THEN_MASS -> "charge, Δmass";
      case MASS -> "Δmass";
    };
  }

  public Comparator<IonPart> getComparator() {
    return comparator;
  }

  public Comparator<IonPartDefinition> getDefinitionComparator() {
    final Comparator<IonPart> parentComparator = this.getComparator();
    return Comparator.comparing(part -> part, parentComparator);
  }
}
