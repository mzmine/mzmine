/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import io.github.mzmine.datamodel.identities.IonPart.IonStringFlavor;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.ParsingUtils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Prefer creation with static factories {@link #create(IonPart...)}. Whole definition of an adduct,
 * insource fragment, insource cluster of 2M etc. e.g., [2M+Na]+, [M-H2O+2H]+2.
 * <p>
 * Parse by {@link IonTypeParser}.
 * <p>
 * If undefined {@link IonPart} are used the following will happen. One undefined part will take up
 * all the remaining charge. If there are two undefined parts the charge will be distributed
 * randomly. Try adding common names to {@link CompoundsByNames}.
 *
 * @param name        parsed name from all parts
 * @param parts       unmodifiable list of ion parts
 * @param totalMass   total singed mass difference from ion parts
 * @param totalCharge total charge from all ion parts
 * @param molecules   number of M molecules in cluster, e.g., M or 2M
 */
public record IonType(@NotNull String name, @NotNull List<@NotNull IonPart> parts, double totalMass,
                      int totalCharge, int molecules) {

  public static final String XML_ELEMENT = "iontype";

  public static final Comparator<IonType> DEFAULT_ION_ADDUCT_SORTER = Comparator.comparingDouble(
      IonType::totalCharge).thenComparingDouble(IonType::totalMass);

  public static IonType create(@NotNull IonPart... parts) {
    return create(List.of(parts), 1);
  }

  public static IonType create(@NotNull List<@NotNull IonPart> parts, int molecules) {
    // requires to merge all the same IonParts into single objects by adding up their count multiplier
    HashMap<IonPart, IonPart> unique = HashMap.newHashMap(parts.size());
    for (final IonPart part : parts) {
      unique.merge(part, part, IonPart::merge);
    }

    // sort so that name will be correct
    parts = unique.values().stream().sorted(IonPart.DEFAULT_ION_ADDUCT_SORTER).toList();

    double totalMass = 0;
    int totalCharge = 0;
    for (final IonPart part : parts) {
      totalMass += part.totalMass();
      totalCharge += part.totalCharge();
    }

    // generate name
    String ionParts = parts.stream().map(p -> p.toString(IonStringFlavor.SIMPLE_NO_CHARGE))
        .collect(Collectors.joining());
    String name = "[" + molecules + ionParts + "]" + IonUtils.getChargeString(totalCharge);
    return new IonType(name, parts, totalMass, totalCharge, molecules);
  }

  @Override
  public String toString() {
    return name;
  }

  public double absTotalMass() {
    return Math.abs(totalMass);
  }

  public int absTotalCharge() {
    return Math.abs(totalCharge);
  }


  /**
   * ((mz * charge) - deltaMass) / numberOfMolecules
   *
   * @param mz mass to charge ratio
   * @return the neutral mass for this m/z calculated for this IonType
   */
  public double getMass(double mz) {
    return ((mz * this.absTotalCharge()) - this.totalMass()) / this.molecules();
  }


  /**
   * neutral mass of M to mz of yM+X]charge
   * <p>
   * (mass*mol + deltaMass) /charge
   *
   * @return the m/z for this neutral ionized by IonType
   */
  public double getMZ(double neutralmass) {
    return (neutralmass * molecules() + totalMass()) / absTotalCharge();
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement("iontype");
    writer.writeAttribute("molecules", String.valueOf(molecules));
    writer.writeAttribute("charge", String.valueOf(totalCharge));
    writer.writeAttribute("mass", String.valueOf(totalMass));
    writer.writeStartElement("adduct");
    parts.saveToXML(writer);
    writer.writeEndElement();
    writer.writeEndElement();
  }

  public static IonType loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Current element is not an iontype");
    }

    final int molecules = Integer.parseInt(reader.getAttributeValue(null, "molecules"));
    final int charge = Integer.parseInt(reader.getAttributeValue(null, "charge"));

    IonModification adduct = null;
    IonModification mod = null;

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals("iontype"))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals("adduct")) {
        if (ParsingUtils.progressToStartElement(reader, IonModification.XML_ELEMENT,
            CONST.XML_DATA_TYPE_ELEMENT)) {
          adduct = IonModification.loadFromXML(reader);
        } else {
          return null;
        }
      }
      if (reader.getLocalName().equals("modification")) {
        if (ParsingUtils.progressToStartElement(reader, IonModification.XML_ELEMENT,
            CONST.XML_DATA_TYPE_ELEMENT)) {
          mod = IonModification.loadFromXML(reader);
        }
      }
    }

    assert adduct != null;

    return mod != null ? new io.github.mzmine.datamodel.identities.iontype.IonType(molecules,
        adduct, mod) : new io.github.mzmine.datamodel.identities.iontype.IonType(molecules, adduct);
  }

  public boolean hasPartsOverlap(@Nullable IonType a) {
    // usually very low number of parts so can just use list operations
    return streamPartsOverlap(a).findAny().isPresent();
  }

  public Stream<IonPart> streamPartsOverlap(@Nullable IonType a) {
    if (a == null) {
      return Stream.empty();
    }
    // usually very low number of parts so can just use list operations
    return a.parts.stream().filter(this.parts::contains);
  }

  /**
   * @param a
   * @return true if all charged parts are the same
   */
  public boolean hasSameAdducts(@Nullable IonType a) {
    if (a == null) {
      return false;
    }
    var ap = a.streamChargedAdducts().collect(Collectors.toSet());
    var bp = this.streamChargedAdducts().toList();
    return ap.size() == bp.size() && ap.containsAll(bp);
  }

  @NotNull
  public Stream<@NotNull IonPart> stream() {
    return parts.stream();
  }

  @NotNull
  public Stream<@NotNull IonPart> streamChargedAdducts() {
    return stream().filter(IonPart::isCharged);
  }

  @NotNull
  public Stream<@NotNull IonPart> streamNeutralMods() {
    return stream().filter(p -> !p.isCharged());
  }

  /**
   * Checks mass diff, charge and mol equality
   *
   * @return true if charge, mass difference, and molecules factor is the same
   */
  public boolean sameMathDifference(IonType adduct) {
    return sameMassDifference(adduct) && totalCharge == adduct.totalCharge
           && molecules == adduct.molecules;
  }

  /**
   * Checks mass diff
   */
  public boolean sameMassDifference(IonType adduct) {
    return Double.compare(totalMass, adduct.totalMass) == 0;
  }


  public PolarityType getPolarity() {
    if (totalCharge() == 0) {
      return PolarityType.NEUTRAL;
    }
    if (totalCharge() < 0) {
      return PolarityType.NEGATIVE;
    }
    return PolarityType.POSITIVE;
  }

  /**
   * Is adding or removing all sub adducts / modifications from the molecular formula.
   *
   * @param formula the formula.
   * @param ionize  if the formula shall be ionised.
   * @return The resulting molecule may be neutral if the charge of the molecule and the charge of
   * this adduct are opposite.
   */
  public IMolecularFormula addToFormula(IMolecularFormula formula, boolean ionize)
      throws CloneNotSupportedException {
    IMolecularFormula result = (IMolecularFormula) formula.clone();
    // add for n molecules the M formula
    for (int i = 2; i <= molecules; i++) {
      FormulaUtils.addFormula(result, formula);
    }

    // add first then remove
    parts.stream().filter(IonPart::isAddition).forEach(ion -> ion.addToFormula(formula, ionize));
    parts.stream().filter(IonPart::isLoss).forEach(ion -> ion.addToFormula(formula, ionize));

    return result;
  }

}
