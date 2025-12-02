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
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.maths.Precision;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * A single part in an IonType: like 2Na or -H.
 * <p>
 * Is class no record to remain in control of construction
 */
public final class IonPart {

  private static final Logger logger = Logger.getLogger(IonPart.class.getName());
  public static final String XML_ELEMENT = "ionpart";

  private final @NotNull String name;
  private final @Nullable String singleFormula;
  private final double absSingleMass;
  private final int singleCharge;
  private final int count;

  /**
   * @param singleFormula uncharged formula without multiplier formula may be null if unknown.
   *                      Formula of a single item - so the count multiplier is not added. Using a
   *                      String here instead of CDK formula as CDK formula does not implement
   *                      equals.
   * @param singleCharge  signed charge of a single item which is multiplied by count to get total
   *                      charge. Both H+ and 2H+ would be single charge +1. See count.
   */
  public IonPart(@NotNull String singleFormula, @Nullable Integer singleCharge) {
    this(null, singleFormula, null, singleCharge, 1);
  }


  /**
   * @param singleFormula uncharged formula without multiplier formula may be null if unknown.
   *                      Formula of a single item - so the count multiplier is not added. Using a
   *                      String here instead of CDK formula as CDK formula does not implement
   *                      equals.
   * @param singleCharge  signed charge of a single item which is multiplied by count to get total
   *                      charge. Both H+ and 2H+ would be single charge +1. See count.
   * @param count         the singed multiplier of this single item, non-zero. e.g., 2 for +2Na and
   *                      -1 for -H
   */
  public IonPart(@Nullable String singleFormula, @Nullable Integer singleCharge, final int count) {
    this(null, singleFormula, null, singleCharge, count);
  }

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
  public IonPart(@Nullable String name, @Nullable String singleFormula,
      @Nullable Double absSingleMass, @Nullable Integer singleCharge, final int count) {

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

    this.name = requireNonNullElse(name, singleFormula);
    this.singleFormula = singleFormula;
    // always positive and then multiplied with count
    this.absSingleMass = Math.abs(absSingleMass);
    this.singleCharge = requireNonNullElse(singleCharge, 0);
    this.count = count;
  }

  /**
   * Formula constructor with count 1
   *
   * @param formula used to calculate other fields
   */
  public static IonPart ofFormula(@NotNull final String formula, final int singleCharge) {
    return ofFormula(formula, singleCharge, 1);
  }


  /**
   * Name overwriting formula name
   */
  public static IonPart ofFormula(@Nullable String name, @NotNull String formula,
      final int singleCharge) {
    return ofFormula(name, formula, singleCharge, 1);
  }

  /**
   * Formula constructor
   *
   * @param formula used to calculate other fields
   */
  public static IonPart ofFormula(@NotNull String formula, @Nullable Integer singleCharge,
      final int count) {
    return ofFormula(null, formula, singleCharge, count);
  }

  public static IonPart ofFormula(@Nullable String name, @NotNull String formula,
      @Nullable Integer singleCharge, final int count) {

    // formula as name
    return new IonPart(name, formula, null, singleCharge, count);
  }

  /**
   * No formula constructor
   */
  public static IonPart ofNamed(@NotNull String name, final double singleMass,
      final int singleCharge) {
    return ofNamed(name, singleMass, singleCharge, 1);
  }

  /**
   * No formula constructor
   */
  public static IonPart ofNamed(@NotNull String name, final double singleMass,
      final int singleCharge, final int count) {
    return new IonPart(name, null, singleMass, singleCharge, count);
  }

  public static List<IonPart> parseMultiple(final String input) {
    return IonPartParser.parseMultiple(input);
  }

  @Nullable
  public static IonPart parse(@NotNull String part) {
    return IonPartParser.parse(part);
  }

  /**
   * @param signedCount - or + count here to see if this is a loss or addition
   */
  public static IonPart unknown(final String name, final int signedCount) {
    return unknown(name, signedCount, 0);
  }

  /**
   * @param signedCount - or + count here to see if this is a loss or addition
   */
  public static IonPart unknown(final String name, final int signedCount,
      final Integer singleCharge) {
    return new IonPart(name, null, 0d, requireNonNullElse(singleCharge, 0), signedCount);
  }

  public boolean isUnknown() {
    // name is blank for silent charge - so it is reserved
    // for example for [M]+ (already charged and not -e-)
    // do not treat silent charge as unknown
    return !name.isBlank() && singleFormula == null && absSingleMass == 0d;
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
    String base = IonUtils.getSignedNumberOmit1(count);
    return switch (flavor) {
      case SIMPLE_NO_CHARGE -> base + name;
      case SIMPLE_WITH_CHARGE ->
        // use single charge here to allow saving loading in json
        // use charge as in +2 or - with trailing number
          "%s(%s%s)".formatted(base, name, IonUtils.getSignedNumberOmit1(singleCharge()));
      case FULL_WITH_MASS ->
        // use single charge here
          "%s(%s%s) (%s Da)".formatted(base, name, IonUtils.getSignedNumberOmit1(singleCharge()),
              ConfigService.getExportFormats().mz(totalMass()));
    };
  }

  /**
   * @return A merged IonPart if both ions match completely, excluding their count field. Otherwise,
   * null or null if both a and b are null. The result may have a count of 0.
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
    // disregard count for matching to only rely on mass etc
    if (!a.equalsWithoutCount(b)) {
      throw new IllegalArgumentException(
          "A and B define IonParts with different properties and cannot be merged %s and %s".formatted(
              a, b));
    }
    final int total = a.count + b.count;
    return a.withCount(total);
  }

  /**
   * Flip count to flip the effective total mass and charge
   *
   * @return Same formula, e.g., conversion from +Na to -Na
   */
  public IonPart flipCount() {
    return new IonPart(name, singleFormula, absSingleMass, singleCharge, -count);
  }

  public IonPart withCount(final int count) {
    if (count == this.count) {
      return this;
    }
    return new IonPart(name, singleFormula, absSingleMass, singleCharge, count);
  }

  /**
   * Will change mass by the number of electrons
   *
   * @return a new instance if charge is different or the same instance if not
   */
  public IonPart withSingleCharge(final Integer singleCharge) {
    if (singleCharge == null) {
      return this;
    }
    if (isUnknown()) {
      throw new IllegalStateException(
          "Cannot change charge of unknown ion part without mass definition");
    }

    final int actualCharge = requireNonNullElse(singleCharge, 0);
    if (this.singleCharge == actualCharge) {
      return this;
    }
    final double actualMass;
    final IMolecularFormula formula = unchargedSingleCDKFormula();
    if (formula != null) {
      actualMass = FormulaUtils.getMonoisotopicMass(formula, actualCharge);
    } else {
      int chargeDiff = this.singleCharge - actualCharge;
      actualMass = absSingleMass + chargeDiff * FormulaUtils.electronMass;
    }
    return new IonPart(name, singleFormula, actualMass, singleCharge, count);
  }

  public int totalCharge() {
    return singleCharge * count;
  }

  public int absTotalCharge() {
    return Math.abs(totalCharge());
  }

  public boolean isCharged() {
    return singleCharge != 0;
  }

  /**
   * Polarity of total charge so charge * count which may flip sign of singleCharge
   */
  public PolarityType totalChargePolarity() {
    return switch (singleCharge) {
      case int c when c < 0 -> PolarityType.NEGATIVE;
      case int c when c > 0 -> PolarityType.POSITIVE;
      default -> PolarityType.NEUTRAL;
    };
  }

  public double totalMass() {
    return absSingleMass * count;
  }

  public double absTotalMass() {
    return Math.abs(totalMass());
  }

  public String partSign() {
    return isLoss() ? "-" : "+";
  }

  public boolean isLoss() {
    return count < 0;
  }

  public boolean isAddition() {
    return count >= 0;
  }

  public boolean isNeutralModification() {
    return !isCharged();
  }

  /**
   * @return the type of this ion part
   */
  public Type type() {
    if (isCharged()) {
      return Type.ADDUCT;
    }
    return isLoss() ? Type.IN_SOURCE_FRAGMENT : Type.CLUSTER;
  }

  /**
   * Exclude count from equals and hash so that duplicate elements can be more easily merged
   *
   * @param o the reference object with which to compare.
   * @return true if equals
   */
  public boolean equalsWithoutCount(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final IonPart ionPart)) {
      return false;
    }

    return singleCharge == ionPart.singleCharge && Precision.equals(absSingleMass,
        ionPart.absSingleMass, 0.0000001) && name.equals(ionPart.name) && Objects.equals(
        singleFormula, ionPart.singleFormula);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final IonPart ionPart)) {
      return false;
    }

    return singleCharge == ionPart.singleCharge && Precision.equals(absSingleMass,
        ionPart.absSingleMass, 0.0000001) && name.equals(ionPart.name) && Objects.equals(
        singleFormula, ionPart.singleFormula) && Objects.equals(count, ionPart.count);
  }

  /**
   * Hash does not include the count - idea is to find the same adduct in maps
   *
   * @return
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

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute("name", name);
    writer.writeAttribute("mass", String.valueOf(absSingleMass));
    writer.writeAttribute("charge", String.valueOf(singleCharge));
    writer.writeAttribute("count", String.valueOf(count));
    if (singleFormula != null) {
      writer.writeAttribute("formula", singleFormula);
    }
    writer.writeEndElement();
  }

  public static IonPart loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals("ionpart"))) {
      throw new IllegalStateException("Current element is not an ionpart");
    }

    final Integer charge = ParsingUtils.stringToInteger(reader.getAttributeValue(null, "charge"));
    final Integer count = ParsingUtils.stringToInteger(reader.getAttributeValue(null, "count"));
    final Double mass = ParsingUtils.stringToDouble(reader.getAttributeValue(null, "mass"));
    final String name = reader.getAttributeValue(null, "name");
    Objects.requireNonNull(charge);
    Objects.requireNonNull(count);
    Objects.requireNonNull(mass);
    Objects.requireNonNull(name);
    // may be null
    final String formula = reader.getAttributeValue(null, "formula");

    return new IonPart(name, formula, mass, charge, count);
  }

  /**
   * @param formula changed in place
   * @param ionize  ionize formula if part has charge
   */
  public void addToFormula(final IMolecularFormula formula, final boolean ionize) {
    final int formulaCharge = requireNonNullElse(formula.getCharge(), 0);
    if (ionize) {
      formula.setCharge(formulaCharge + totalCharge());
    }
    if (singleFormula == null) {
      return;
    }

    final IMolecularFormula chargedSingleFormula = chargedSingleCDKFormula();
    if (chargedSingleFormula == null) {
      return; // cannot subtract if formula unknown or failed to parse formula
    }
    final int absCount = Math.abs(count);
    if (isLoss()) {
      FormulaUtils.subtractFormula(formula, chargedSingleFormula, absCount);
    } else {
      FormulaUtils.addFormula(formula, chargedSingleFormula, absCount);
    }
  }

  /**
   * @return the charged formula object of a single count
   */
  @Nullable
  public IMolecularFormula chargedSingleCDKFormula() {
    if (singleFormula == null) {
      return null;
    }
    // could also introduce this as a variable but then it musst not go into the equals and hashcode
    return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(singleFormula, singleCharge);
  }

  /**
   * @return the charged formula object of a single count
   */
  @Nullable
  public IMolecularFormula unchargedSingleCDKFormula() {
    if (singleFormula == null) {
      return null;
    }
    // could also introduce this as a variable but then it musst not go into the equals and hashcode
    return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(singleFormula, 0);
  }


  /**
   * @return silent charge is the only blank name
   */
  public boolean isSilentCharge() {
    return name.isBlank() && singleFormula == null && absSingleMass == 0d;
  }

  public @NotNull String name() {
    return name;
  }

  /**
   * @return The uncharged single formula (count not applied)
   */
  public @Nullable String singleFormula() {
    return singleFormula;
  }

  public double absSingleMass() {
    return absSingleMass;
  }

  public int singleCharge() {
    return singleCharge;
  }

  public int count() {
    return count;
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
