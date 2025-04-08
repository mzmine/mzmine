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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.identities.IonPart.IonPartStringFlavor;
import io.github.mzmine.datamodel.identities.io.IMolecularFormulaDeserializer;
import io.github.mzmine.datamodel.identities.io.IMolecularFormulaSerializer;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.maths.Precision;
import java.util.Objects;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
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
@JsonIgnoreProperties(ignoreUnknown = true)
public record IonPartNocount(@NotNull String name,
                             @JsonSerialize(using = IMolecularFormulaSerializer.class) //
                             @JsonDeserialize(using = IMolecularFormulaDeserializer.class) //
                             @Nullable IMolecularFormula formula, double absMass, int charge) {

  private static final Logger logger = Logger.getLogger(IonPartNocount.class.getName());
  public static final String XML_ELEMENT = "ionpart_definition";

  @JsonCreator
  public IonPartNocount(@NotNull final String name, @Nullable final IMolecularFormula formula,
      final double absMass, final int charge) {
    this.name = name;
    this.formula = formula;
    this.absMass = Math.abs(absMass); // allways positive and then multiplied with count
    this.charge = charge;
  }

  /**
   * Formula constructor
   *
   * @param formula used to calculate other fields
   */
  public IonPartNocount(@NotNull final String formula, final int charge) {
    this(Objects.requireNonNull(
        FormulaUtils.createMajorIsotopeMolFormulaWithCharge(formula, charge)));
  }

  public IonPartNocount(@NotNull final IMolecularFormula formula) {
    this(FormulaUtils.getFormulaString(formula, false), formula,
        requireNonNullElse(formula.getCharge(), 0));
  }

  public IonPartNocount(@NotNull final IMolecularFormula formula, final int charge) {
    this(FormulaUtils.getFormulaString(formula, false), formula, charge);
  }

  /**
   * No formula constructor
   */
  public IonPartNocount(@NotNull String name, final double mass, final int charge) {
    this(name, null, mass, charge);
  }

  public IonPartNocount(@NotNull String name, @NotNull String formula, final int charge) {
    this(name, Objects.requireNonNull(
        FormulaUtils.createMajorIsotopeMolFormulaWithCharge(formula, charge)), charge);
  }

  public IonPartNocount(@NotNull String name, @NotNull IMolecularFormula formula,
      final int charge) {
    this(name, formula, FormulaUtils.getMonoisotopicMass(formula, charge), charge);
  }


  public static IonPartNocount unknown(final String name) {
    return new IonPartNocount(name, null, 0d, 0);
  }

  public static IonPartNocount of(final IonPart p) {
    return new IonPartNocount(p.name(), p.singleFormula(), p.absSingleMass(), p.singleCharge());
  }

  @JsonIgnore
  public boolean isUnknown() {
    // name is blank for silent charge - so it is reserved
    // for example for [M]+ (already charged and not -e-)
    // do not treat silent charge as unknown
    return !name.isBlank() && formula == null && absMass == 0d;
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

  @JsonIgnore
  public boolean isCharged() {
    return charge != 0;
  }

  /**
   * Polarity of total charge so charge * count which may flip sign of charge
   */
  @JsonIgnore
  public PolarityType chargePolarity() {
    return switch (charge) {
      case int c when c < 0 -> PolarityType.NEGATIVE;
      case int c when c > 0 -> PolarityType.POSITIVE;
      default -> PolarityType.NEUTRAL;
    };
  }

  @JsonIgnore
  @Nullable
  public String singleFormulaUnchargedString() {
    return formula == null ? null : FormulaUtils.getFormulaString(formula, false);
  }

  @JsonIgnore
  public boolean isNeutralModification() {
    return !isCharged();
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
    if (!(o instanceof final IonPartNocount ionPart)) {
      return false;
    }

    return charge == ionPart.charge && Precision.equals(absMass, ionPart.absMass, 0.0000001)
           && name.equals(ionPart.name) && Objects.equals(formula, ionPart.formula);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final IonPartNocount ionPart)) {
      return false;
    }

    return charge == ionPart.charge && Precision.equals(absMass, ionPart.absMass, 0.0000001)
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


  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute("name", name);
    writer.writeAttribute("mass", String.valueOf(absMass));
    writer.writeAttribute("charge", String.valueOf(charge));
    if (formula != null) {
      writer.writeAttribute("formula", FormulaUtils.getFormulaString(formula, false));
    }
    writer.writeEndElement();
  }

  public static IonPartNocount loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals("ionpart"))) {
      throw new IllegalStateException("Current element is not an ionpart");
    }

    final Integer charge = ParsingUtils.stringToInteger(reader.getAttributeValue(null, "charge"));
    final Double mass = ParsingUtils.stringToDouble(reader.getAttributeValue(null, "mass"));
    final String name = reader.getAttributeValue(null, "name");
    Objects.requireNonNull(charge);
    Objects.requireNonNull(mass);
    Objects.requireNonNull(name);
    // may be null
    final String formula = reader.getAttributeValue(null, "formula");
    final IMolecularFormula parsedFormula =
        formula == null ? null : FormulaUtils.createMajorIsotopeMolFormulaWithCharge(formula);

    return new IonPartNocount(name, parsedFormula, mass, charge);
  }

  /**
   * @return silent charge is the only blank name
   */
  @JsonIgnore
  public boolean isSilentCharge() {
    return name.isBlank() && formula == null && absMass == 0d;
  }

}
