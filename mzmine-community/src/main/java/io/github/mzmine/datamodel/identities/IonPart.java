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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.maths.Precision;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public sealed interface IonPart permits IonPartDefinition, IonPartSilentCharge, IonPartFullCounted,
    IonPartUnknown {

  /**
   * Empty name is reserved for {@link IonParts#SILENT_CHARGE}
   */
  @NotNull String name();

  /**
   * @return The uncharged single formula (count not applied)
   */
  @Nullable String singleFormula();

  /**
   * Mass of a single instance
   */
  double absSingleMass();

  /**
   * Charge of a single instance
   */
  int singleCharge();

  /**
   * Multiplier for this ion part
   */
  int count();


  /**
   * @return A merged IonPart if both ions match completely, excluding their count field. Otherwise,
   * null or null if both a and b are null. The result may have a count of 0.
   */
  @Nullable
  static IonPart merge(@Nullable IonPart a, @Nullable IonPart b) {
    if (a == null && b == null) {
      return null;
    }
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    // disregard count for matching to only rely on mass etc
    if (!a.equalsWithoutCount(b)) {
      throw new IllegalArgumentException(
          "A and B define IonParts with different properties and cannot be merged %s and %s".formatted(
              a, b));
    }
    final int total = a.count() + b.count();
    return a.withCount(total);
  }

  /**
   * Exclude count from equals and hash so that duplicate elements can be more easily merged
   *
   * @param o the reference object with which to compare.
   * @return true if equals
   */
  default boolean equalsWithoutCount(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final IonPart ionPart)) {
      return false;
    }

    return singleCharge() == ionPart.singleCharge() && Precision.equals(absSingleMass(),
        ionPart.absSingleMass(), 0.0000001) && name().equals(ionPart.name()) && Objects.equals(
        singleFormula(), ionPart.singleFormula());
  }


  /**
   *
   * @return true if ion part is undefined by mass
   */
  default boolean isUndefinedMass() {
    // name is blank for silent charge - so it is reserved
    // for example for [M]+ (already charged and not -e-)
    // do not treat silent charge as unknown
    return !name().isBlank() && singleFormula() == null && Double.compare(absSingleMass(), 0d) == 0;
  }

  /**
   *
   * @return true if the name is ? like for {@link IonParts#UNKNOWN}
   */
  default boolean isUnknownPart() {
    // name is blank for silent charge - so it is reserved
    // for example for [M]+ (already charged and not -e-)
    // do not treat silent charge as unknown
    return name().equals("?") && singleFormula() == null;
  }


  default String toString(IonPartStringFlavor flavor) {
    if (name().isBlank()) {
      // e,g, {@link IonParts#}
      return "";
    }
    String base = IonUtils.getSignedNumberOmit1(count());
    return switch (flavor) {
      case SIMPLE_NO_CHARGE -> base + name();
      case SIMPLE_WITH_CHARGE ->
        // use single charge here to allow saving loading in json
        // use charge as in +2 or - with trailing number
          "%s(%s%s)".formatted(base, name(), IonUtils.getSignedNumberOmit1(singleCharge()));
      case FULL_WITH_MASS ->
        // use single charge here
          "%s(%s%s) (%s Da)".formatted(base, name(), IonUtils.getSignedNumberOmit1(singleCharge()),
              ConfigService.getExportFormats().mz(totalMass()));
    };
  }

  default IonPart withCount(int count) {
    if (count == this.count()) {
      return this;
    }
    return new IonPartFullCounted(name(), singleFormula(), absSingleMass(), singleCharge(), count);
  }

  /**
   * Will change mass by the number of electrons
   *
   * @return a new instance if charge is different or the same instance if not
   */
  default IonPart withSingleCharge(Integer singleCharge) {
    if (singleCharge == null) {
      return this;
    }
    if (isUndefinedMass()) {
      throw new IllegalStateException(
          "Cannot change charge of unknown ion part without mass definition");
    }

    if (this.singleCharge() == singleCharge) {
      return this;
    }
    final double actualMass;
    final IMolecularFormula formula = unchargedSingleCDKFormula();
    if (formula != null) {
      actualMass = FormulaUtils.getMonoisotopicMass(formula, singleCharge);
    } else {
      int chargeDiff = this.singleCharge() - singleCharge;
      actualMass = absSingleMass() + chargeDiff * FormulaUtils.electronMass;
    }
    return new IonPartFullCounted(name(), singleFormula(), actualMass, singleCharge, count());
  }

  default int totalCharge() {
    return singleCharge() * count();
  }

  default int absTotalCharge() {
    return Math.abs(totalCharge());
  }

  default boolean isCharged() {
    return singleCharge() != 0;
  }

  /**
   * Polarity of total charge so charge * count which may flip sign of singleCharge
   */
  default PolarityType totalChargePolarity() {
    return switch (singleCharge()) {
      case int c when c < 0 -> PolarityType.NEGATIVE;
      case int c when c > 0 -> PolarityType.POSITIVE;
      default -> PolarityType.NEUTRAL;
    };
  }

  default double totalMass() {
    return absSingleMass() * count();
  }

  default double absTotalMass() {
    return Math.abs(totalMass());
  }

  default String partSign() {
    return isLoss() ? "-" : "+";
  }

  default boolean isLoss() {
    return count() < 0;
  }

  default boolean isAddition() {
    return count() >= 0;
  }

  default boolean isNeutralModification() {
    return !isCharged();
  }

  /**
   * @return the type of this ion part
   */
  default Type type() {
    if (isCharged()) {
      return Type.ADDUCT;
    }
    return isLoss() ? Type.IN_SOURCE_FRAGMENT : Type.CLUSTER;
  }

  /**
   * @return the charged formula object of a single count
   */
  @Nullable
  default IMolecularFormula chargedSingleCDKFormula() {
    if (singleFormula() == null) {
      return null;
    }
    // could also introduce this as a variable but then it musst not go into the equals and hashcode
    return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(singleFormula(), singleCharge());
  }

  /**
   * @return the charged formula object of a single count
   */
  @Nullable
  default IMolecularFormula unchargedSingleCDKFormula() {
    if (singleFormula() == null) {
      return null;
    }
    // could also introduce this as a variable but then it musst not go into the equals and hashcode
    return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(singleFormula(), 0);
  }

  /**
   * @return silent charge is the only blank name
   */
  default boolean isSilentCharge() {
    return name().isBlank() && singleFormula() == null && absSingleMass() == 0d;
  }

  /**
   * @param formula changed in place
   * @param ionize  ionize formula if part has charge
   */
  default void addToFormula(@NotNull IMolecularFormula formula, boolean ionize) {
    final int formulaCharge = requireNonNullElse(formula.getCharge(), 0);
    if (ionize) {
      formula.setCharge(formulaCharge + totalCharge());
    }
    if (singleFormula() == null) {
      return;
    }

    final IMolecularFormula chargedSingleFormula = chargedSingleCDKFormula();
    if (chargedSingleFormula == null) {
      return; // cannot subtract if formula unknown or failed to parse formula
    }
    final int absCount = Math.abs(count());
    if (isLoss()) {
      FormulaUtils.subtractFormula(formula, chargedSingleFormula, absCount);
    } else {
      FormulaUtils.addFormula(formula, chargedSingleFormula, absCount);
    }
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
    CLUSTER;

    @Override
    public String toString() {
      return switch (this) {
        case ADDUCT -> "Adduct";
        case IN_SOURCE_FRAGMENT -> "In-source fragment";
        case CLUSTER -> "Cluster";
      };
    }
  }

  public enum IonPartStringFlavor {

    /**
     * including count, name, charge, mass: +2(Na+) (totalMass Da)
     */
    FULL_WITH_MASS,
    /**
     * count and name: +2Na
     */
    SIMPLE_NO_CHARGE,
    /**
     * count, name, charge: +2(Na+)
     */
    SIMPLE_WITH_CHARGE

  }
}
