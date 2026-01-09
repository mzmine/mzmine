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

import io.github.mzmine.util.StringUtils;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * this class is used to define alternative names and unknown pseudonyms for formulas or just names
 * used in {@link IonPart} definitions while parsing {@link IonType}.
 * <p>
 * A single part definition in an IonType - but without the count. So +H and -H are both just H+ and
 * can easily be found in hashmaps. There should always only be one definition of the charge and
 * mass of H+ but there can be multiple versions with different charge Fe+2 and Fe3+
 *
 * @param name          clear name - often derived from formula or from alternative names. Empty
 *                      name is only supported for {@link IonParts#SILENT_CHARGE}
 * @param singleFormula uncharged formula without multiplier formula may be null if unknown. Formula
 *                      of a single item
 * @param absSingleMass absolute (positive) mass of a single item
 * @param singleCharge  signed charge of a single item. Both H+ and H+1 would be single charge +1.
 */
public record IonPartDefinition(@NotNull String name, @Nullable String singleFormula,
                                double absSingleMass, int singleCharge) implements IonPart {

  /**
   * @param name          clear name - often derived from formula or from alternative names. Empty
   *                      name is only supported for {@link IonParts#SILENT_CHARGE}
   * @param singleFormula uncharged formula without multiplier formula may be null if unknown.
   *                      Formula of a single item - so the count multiplier is not added. Using a
   *                      String here instead of CDK formula as CDK formula does not implement
   *                      equals.
   * @param absSingleMass absolute (positive) mass of a single item of this type which is multiplied
   *                      by count to get total mass.
   * @param singleCharge  signed charge of a single item which is multiplied by count to get total
   *                      charge. Both H+ and 2H+ would be single charge +1. See count.
   */
  public IonPartDefinition(@NotNull String name, @Nullable String singleFormula,
      double absSingleMass, int singleCharge) {

    // checks were already done in IonParts
    this.name = name.trim();
    // SILENT_CHARGE has empty name check that mass is null
    if (name.isEmpty()) {
      if (Double.compare(absSingleMass, 0d) != 0) {
        throw new IllegalStateException(
            "Cannot use blank name for part that defines a mass. Blank name is reserved for silent charge");
      }
      if (singleFormula != null) {
        throw new IllegalStateException(
            "Cannot use formula in combination with empty name. Empty name is reserved for silent charge instance.");
      }
    }

    this.singleFormula = StringUtils.isBlank(singleFormula) ? null : singleFormula;
    // always positive and then multiplied with count
    this.absSingleMass = Math.abs(absSingleMass);
    this.singleCharge = singleCharge;
  }

  @NotNull
  public static IonPartDefinition of(@NotNull final IonPart p) {
    if (p instanceof IonPartDefinition def) {
      return def;
    }
    return new IonPartDefinition(p.name(), p.singleFormula(), p.absSingleMass(), p.singleCharge());
  }

  @NotNull
  public static IonPartDefinition ofFormula(@Nullable String name, @NotNull String formula,
      @Nullable Integer singleCharge) {
    // formula as name
    return of(IonParts.ofFormula(name, formula, singleCharge, null));
  }

  public static IonPartDefinition ofNamed(@NotNull String name, final double singleMass,
      final @Nullable Integer singleCharge) {
    return of(IonParts.ofNamed(name, singleMass, singleCharge, null));
  }

  public static IonPartDefinition parse(String ionPart) {
    // simulate the addition if +- is missing.
    // definition does not need the count but parsing through the IonPart is easiest
    if (!ionPart.startsWith("[+-]")) {
      ionPart = "+" + ionPart;
    }
    final IonPart parsed = IonParts.parse(ionPart);
    return parsed == null ? null : of(parsed);
  }

  /**
   * Creates the final part string with mass and charge see {@link #toString(IonPartStringFlavor)}
   * with {@link IonPartStringFlavor#FULL_WITH_MASS}
   *
   * @return sign count name charge (mass)
   */
  @Override
  public @NotNull String toString() {
    return toString(IonPartStringFlavor.FULL_WITH_MASS);
  }

  /**
   * Count always 0. Just here to conform to the interface
   */
  @Override
  public int count() {
    return 0;
  }

  @Override
  public double totalMass() {
    return absSingleMass;
  }

  /**
   * Use {@link #equalsWithoutCount(Object)} to check for any match between implementing classes
   * without count
   *
   * @param o the reference object with which to compare.
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    // needs to be ion part definition here otherwise its not equal
    if (!(o instanceof final IonPartDefinition ionPart)) {
      return false;
    }

    return equalsWithoutCount(o);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + Objects.hashCode(singleFormula);
    result = 31 * result + Double.hashCode(absSingleMass);
    result = 31 * result + singleCharge;
    return result;
  }

}
