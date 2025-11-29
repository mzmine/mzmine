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
import io.github.mzmine.datamodel.identities.IonPart.IonPartStringFlavor;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.maths.Precision;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * this class is used to define alternative names and unknown pseudonyms for formulas or just names
 * used in {@link IonPart} definitions while parsing {@link IonType}.
 * <p>
 * A single part definition in an IonType - but without the count. So +H and -H are both just H+ and
 * can easily be found in hashmaps. There should always only be one definition of the charge and
 * mass of H+ but there can be multiple versions with different charge Fe+2 and Fe3+
 *
 * @param name    clear name - often derived from formula or from alternative names. Empty name is
 *                only supported for {@link IonParts#SILENT_CHARGE}
 * @param formula uncharged formula without multiplier formula may be null if unknown. Formula of a
 *                single item
 * @param absMass absolute (positive) mass of a single item
 * @param charge  signed charge of a single item. Both H+ and H+1 would be single charge +1.
 */
public record IonPartDefinition(@NotNull String name, @Nullable String formula, double absMass,
                                int charge) {

  private static final Logger logger = Logger.getLogger(IonPartDefinition.class.getName());

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
  public IonPartDefinition(@Nullable String name, @Nullable String singleFormula,
      @Nullable Double absSingleMass, @Nullable Integer singleCharge) {

    if (singleFormula != null) {
      final IMolecularFormula parsedFormula =
          singleCharge == null ? FormulaUtils.createMajorIsotopeMolFormulaWithCharge(singleFormula)
              : FormulaUtils.createMajorIsotopeMolFormulaWithCharge(singleFormula, singleCharge);

      if (parsedFormula == null) {
        if (name == null) {
          name = singleFormula;
        }
        singleFormula = null; // formula was not parsed correctly
      } else {
        // parsing successful
        if (singleCharge == null) {
          singleCharge = requireNonNullElse(parsedFormula.getCharge(), 0);
        }
        singleFormula = FormulaUtils.getFormulaString(parsedFormula, false);

        if (absSingleMass == null) {
          absSingleMass = FormulaUtils.getMonoisotopicMass(parsedFormula, singleCharge);
        }
      }
    }

    if (absSingleMass == null) {
      absSingleMass = 0d;
    }

    if (name == null && singleFormula == null) {
      throw new IllegalArgumentException("name or singleFormula must be defined");
    }

    String nameOrFormula = requireNonNullElse(name, singleFormula);
    int charge = requireNonNullElse(singleCharge, 0);
    final double mass = Math.abs(absSingleMass);
    this(nameOrFormula, singleFormula, mass, charge);
  }

  public static IonPartDefinition of(final IonPart p) {
    return new IonPartDefinition(p.name(), p.singleFormula(), p.absSingleMass(), p.singleCharge());
  }

  public static IonPartDefinition ofFormula(@Nullable String name, @NotNull String formula,
      @Nullable Integer singleCharge) {
    // formula as name
    return new IonPartDefinition(name, formula, null, singleCharge);
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

  public String toString(IonPartStringFlavor flavor) {
    if (name.isBlank()) {
      // e,g, {@link IonParts#}
      return "";
    }
    String base = name;
    return switch (flavor) {
      case SIMPLE_NO_CHARGE -> base;
      case SIMPLE_WITH_CHARGE -> "[%s]%s".formatted(base, IonUtils.getChargeString(charge()));
      case FULL_WITH_MASS -> "[%s]%s (%s Da)".formatted(base, IonUtils.getChargeString(charge()),
          ConfigService.getExportFormats().mz(absMass()));
    };
  }


  public boolean isCharged() {
    return charge != 0;
  }

  /**
   * Polarity of total charge so charge * count which may flip sign of charge
   */

  public PolarityType chargePolarity() {
    return switch (charge) {
      case int c when c < 0 -> PolarityType.NEGATIVE;
      case int c when c > 0 -> PolarityType.POSITIVE;
      default -> PolarityType.NEUTRAL;
    };
  }


  public boolean isNeutralModification() {
    return !isCharged();
  }


  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final IonPartDefinition ionPart)) {
      return false;
    }

    return charge == ionPart.charge && Precision.equalFloatSignificance(absMass, ionPart.absMass)
        && name.equals(ionPart.name) && Objects.equals(formula, ionPart.formula);
  }

  /**
   * Hash does not include the count - idea is to find the same adduct in maps
   *
   * @return
   */
  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + Objects.hashCode(formula);
    result = 31 * result + Double.hashCode(absMass);
    result = 31 * result + charge;
    return result;
  }

}
