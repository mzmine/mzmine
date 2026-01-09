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
 * A single part in an IonType: like 2Na or -H.
 * <p>
 * Is class no record to remain in control of construction
 */
final class IonPartFullCounted implements IonPart {

  private final @NotNull String name;
  private final @Nullable String singleFormula;
  private final double absSingleMass;
  private final int singleCharge;
  private final int count;


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
   * @param count         the singed multiplier of this single item, non-zero. e.g., 2 for +2Na and
   *                      -1 for -H
   */
  IonPartFullCounted(@NotNull String name, @Nullable String singleFormula, double absSingleMass,
      int singleCharge, final int count) {
    // checks were already done in IonParts
    this.name = name.trim();
    // SILENT_CHARGE has empty name check that mass is null
    if (this.name.isEmpty()) {
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
    this.count = count;
  }


  /**
   * Creates the final part string with mass and charge see {@link #toString(IonPartStringFlavor)}
   * with {@link IonPartStringFlavor#FULL_WITH_MASS}
   *
   * @return sign count name charge (mass)
   */
  @Override
  public String toString() {
    return toString(IonPartStringFlavor.FULL_WITH_MASS);
  }


  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final IonPartFullCounted ionPart)) {
      return false;
    }

    return equalsWithoutCount(o) && Objects.equals(count, ionPart.count);
  }

  /**
   * Hash does not include the count - idea is to find the same adduct in maps
   *
   */
  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + Objects.hashCode(singleFormula);
    result = 31 * result + Double.hashCode(absSingleMass);
    result = 31 * result + count;
    result = 31 * result + singleCharge;
    return result;
  }


  @Override
  public @NotNull String name() {
    return name;
  }

  @Override
  public @Nullable String singleFormula() {
    return singleFormula;
  }

  @Override
  public double absSingleMass() {
    return absSingleMass;
  }

  @Override
  public int singleCharge() {
    return singleCharge;
  }

  @Override
  public int count() {
    return count;
  }


}
