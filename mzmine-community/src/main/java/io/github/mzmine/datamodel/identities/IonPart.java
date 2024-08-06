/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.maths.Precision;
import java.util.Comparator;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * A single part in an IonType: like 2Na or -H
 *
 * @param name                   clear name - often derived from formula or from alternative names
 * @param unchargedSingleFormula uncharged formula without multiplier formula may be null if
 *                               unknown. Formula of a single item - so the count multiplier is not
 *                               added
 * @param singleMass             mass of a single item of this type which is multiplied by count to
 *                               get total mass. Loss if singleMass is negative and addition if mass
 *                               is positive
 * @param singleCharge           this defines the charge of a single item which is multiplied by
 *                               count to get total charge
 * @param count                  the multiplier of this single item, positive non-zero. e.g., 2 for
 *                               2Na
 */
public record IonPart(@NotNull String name, @Nullable IMolecularFormula unchargedSingleFormula,
                      double singleMass, int singleCharge, int count) {

  /**
   * losses first then additions. Each sorted by name
   */
  public static final Comparator<IonPart> DEFAULT_ION_ADDUCT_SORTER = Comparator.comparing(
      IonPart::isAddition).thenComparing(IonPart::name);

  public static final Pattern PART_PATTERN = Pattern.compile("([+-]?\\d*)(\\w+)");

  private static final Logger logger = Logger.getLogger(IonPart.class.getName());

  public IonPart(@NotNull final String name,
      @Nullable final IMolecularFormula unchargedSingleFormula, final double singleMass,
      final int singleCharge, final int count) {
    this.name = name;
    this.unchargedSingleFormula = unchargedSingleFormula;
    this.singleMass = singleMass;
    this.singleCharge = singleCharge;
    this.count = Math.abs(count);
  }

  /**
   * Formula constructor
   *
   * @param formula used to calculate other fields
   */
  public IonPart(@NotNull final String formula, final int singleCharge) {
    this(formula, singleCharge, 1);
  }

  /**
   * Formula constructor
   *
   * @param formula used to calculate other fields
   */
  public IonPart(@NotNull final String formula, final int singleCharge, final int count) {
    this(Objects.requireNonNull(
        FormulaUtils.createMajorIsotopeMolFormulaWithCharge(formula, singleCharge)), count);
  }

  public IonPart(@NotNull final IMolecularFormula formula, final int count) {
    this(FormulaUtils.getFormulaString(formula, false), formula,
        FormulaUtils.getMonoisotopicMass(formula, formula.getCharge()), formula.getCharge(), count);
  }

  /**
   * No formula constructor
   */
  public IonPart(@NotNull String name, final double singleMass, final int singleCharge) {
    this(name, singleMass, singleCharge, 1);
  }

  /**
   * No formula constructor
   */
  public IonPart(@NotNull String name, final double singleMass, final int singleCharge,
      final int count) {
    this(name, null, singleMass, singleCharge, count);
  }

  @Nullable
  public static IonPart parse(@NotNull String part) {
    // mod is +Na or -H so with sign and multiplier -2H
    var matcher = PART_PATTERN.matcher(part);
    if (!matcher.matches()) {
      return null;
    }

    // need +H or -H2O to get the correct part
    String sign = StringUtils.orDefault(matcher.group(1), "+").trim();
    int count = sign.length() == 1 ? (sign.equals("-") ? -1 : 1) : Integer.parseInt(sign);

    String name = StringUtils.orDefault(matcher.group(2), "").trim();
    // try to find predefined parts by name

    // otherwise create new ion trying to parse formula
    var formula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(name);
    if (formula == null) {
      logger.warning("Formula for " + name
                     + " not found during parsing of ion type. This means that the mass remains unknown. Please provide proper formulas or add this name to the mzmine code base");
      // just create with unknown values
      return IonPart.unknown(name, count);
    }

    return new IonPart(formula, count);
  }

  /**
   * @param signedCount - or + count here to see if this is a loss or addition
   */
  public static IonPart unknown(final String name, final int signedCount) {
    // need to add a tiny mass difference to allow - or + in toString
    //
    return new IonPart(name, null, Double.MIN_VALUE * signedCount, 0, signedCount);
  }

  /**
   * Creates the final part string with mass and charge see {@link #toString(IonStringFlavor)} with
   * {@link IonStringFlavor#FULL}
   *
   * @return sign count name charge (mass)
   */
  @Override
  public String toString() {
    return toString(IonStringFlavor.FULL);
  }

  public String toString(IonStringFlavor flavor) {
    String base = getPartSign() + count + name;
    return switch (flavor) {
      case SIMPLE_NO_CHARGE -> base;
      case SIMPLE_WITH_CHARGE -> base + IonUtils.getChargeString(getTotalCharge());
      case FULL -> base + IonUtils.getChargeString(getTotalCharge()) + " ("
                   + ConfigService.getExportFormats().mz(getTotalMass()) + ")";
    };
  }

  /**
   * @return A merged IonPart if both ions match completely, excluding their count field. Otherwise,
   * null or null if both a and b are null.
   */
  @Nullable
  public static IonPart merge(@Nullable IonPart a, @Nullable IonPart b) {
    if (a == null && b == null) {
      return null;
    }
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    if (!a.equals(b)) {
      throw new IllegalArgumentException(
          "A and B define IonParts with different properties and cannot be merged %s and %s".formatted(
              a, b));
    }
    return a.withCount(a.count + b.count);
  }

  public IonPart withCount(final int count) {
    return new IonPart(name, unchargedSingleFormula, singleMass, singleCharge, count);
  }


  public int getTotalCharge() {
    return singleCharge * count;
  }

  public int getTotalChargeAbs() {
    return Math.abs(getTotalCharge());
  }

  private boolean isCharged() {
    return singleCharge != 0;
  }

  public PolarityType getPolarity() {
    return singleCharge < 0 ? PolarityType.NEGATIVE : PolarityType.POSITIVE;
  }

  public double getTotalMass() {
    return singleMass * count;
  }

  public double getTotalMassAbs() {
    return Math.abs(getTotalMass());
  }

  public String getPartSign() {
    return singleMass < 0 ? "-" : "+";
  }

  public boolean isLoss() {
    return singleMass < 0;
  }

  public boolean isAddition() {
    return singleMass >= 0;
  }

  public Type getType() {
    if (isCharged()) {
      return Type.ADDUCT;
    }
    return singleMass < 0 ? Type.IN_SOURCE_FRAGMENT : Type.CLUSTER;
  }


  /**
   * Exclude count from equals and hash so that duplicate elements can be more easily merged
   *
   * @param o the reference object with which to compare.
   * @return true if equals
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final IonPart ionPart)) {
      return false;
    }

    return singleCharge == ionPart.singleCharge && Precision.equals(singleMass, ionPart.singleMass,
        0.0000000) && name.equals(ionPart.name) && Objects.equals(unchargedSingleFormula,
        ionPart.unchargedSingleFormula);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + Objects.hashCode(unchargedSingleFormula);
    result = 31 * result + Double.hashCode(singleMass);
    result = 31 * result + singleCharge;
    return result;
  }

  public enum Type {
    /**
     * has charge, positive or negative mass
     */
    ADDUCT,
    /**
     * negative mass, no charge
     */
    IN_SOURCE_FRAGMENT,
    /**
     * positive mass, no charge
     */
    CLUSTER

  }

  public enum IonStringFlavor {

    FULL, SIMPLE_NO_CHARGE, SIMPLE_WITH_CHARGE

  }
}
