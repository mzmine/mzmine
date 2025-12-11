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

import static java.util.function.Predicate.not;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.identities.IonPart.IonPartStringFlavor;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.ParsingUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class IonType {

  /**
   * handles the sorting and everything for the parts
   *
   * @param parts
   * @param molecules
   */
  IonType(@NotNull List<@NotNull IonPart> parts, int molecules) {
    // requires to merge all the same IonParts into single objects by adding up their count multiplier
    // sort so that name will be correct
    parts = IonParts.mergeDuplicates(parts).stream()
        .sorted(IonPartSorting.DEFAULT_NEUTRAL_THEN_LOSSES_THEN_ADDED.getComparator()).toList();

    double totalMass = 0;
    int totalCharge = 0;
    for (final IonPart part : parts) {
      totalMass += part.totalMass();
      totalCharge += part.totalCharge();
    }

    // generate name
    String ionParts = parts.stream().map(p -> p.toString(IonPartStringFlavor.SIMPLE_NO_CHARGE))
        .collect(Collectors.joining());
    String M = molecules > 1 ? molecules + "M" : "M";
    String name = "[" + M + ionParts + "]" + IonUtils.getChargeString(totalCharge);

    this.name = name;
    this.parts = parts;
    this.totalMass = totalMass;
    this.totalCharge = totalCharge;
    this.molecules = molecules;
  }

  public static final String XML_ELEMENT = "iontype";

  public static final Comparator<IonType> DEFAULT_ION_ADDUCT_SORTER = Comparator.comparingDouble(
      IonType::totalCharge).thenComparingDouble(IonType::totalMass);
  private final @NotNull String name;
  private final @NotNull List<@NotNull IonPart> parts;
  private final double totalMass;
  private final int totalCharge;
  private final int molecules;


  public IonType withMolecules(final int molecules) {
    return IonType.create(parts, molecules);
  }

  public static IonType create(@NotNull IonPart... parts) {
    return create(List.of(parts), 1);
  }

  public static IonType create(@NotNull List<@NotNull IonPart> parts, int molecules) {
    return new IonType(parts, molecules);
  }

  /**
   * Creates the final part string with mass and charge see {@link #toString(IonTypeStringFlavor)}
   * with {@link IonTypeStringFlavor#FULL_WITH_MASS}
   *
   * @return sign count name charge (mass)
   */
  @Override
  public String toString() {
    return toString(IonTypeStringFlavor.SIMPLE_DEFAULT);
  }

  public String toString(IonTypeStringFlavor flavor) {
    return switch (flavor) {
      case SIMPLE_DEFAULT -> name;
      case FULL_WITH_MASS ->
          "%s (%s Da)".formatted(name, ConfigService.getExportFormats().mz(totalMass()));
      case FOR_ALPHABETICAL_SORTING -> "%dM%s".formatted(molecules,
          parts.stream().map(IonPart::name).collect(Collectors.joining()));
    };
  }

  @JsonIgnore
  public double absTotalMass() {
    return Math.abs(totalMass);
  }

  @JsonIgnore
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
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute("molecules", String.valueOf(molecules));
    // charge and mass is calculated from rest
//    writer.writeAttribute("charge", String.valueOf(totalCharge));
//    writer.writeAttribute("mass", String.valueOf(totalMass));
    {
      writer.writeStartElement("parts");
      for (final IonPart part : parts) {
        IonParts.saveToXML(writer, part);
      }
      writer.writeEndElement();
    }
    writer.writeEndElement();
  }

  public static IonType loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Current element is not an iontype");
    }

    final Integer molecules = ParsingUtils.stringToInteger(
        reader.getAttributeValue(null, "molecules"));
    Objects.requireNonNull(molecules);
    // charge and mass is calculated from rest
//    final Integer charge = ParsingUtils.stringToInteger(reader.getAttributeValue(null, "charge"));
//    Objects.requireNonNull(charge);
//    final Integer mass = ParsingUtils.stringToInteger(reader.getAttributeValue(null, "mass"));
//    Objects.requireNonNull(mass);

    final List<@NotNull IonPart> parts = new ArrayList<>();

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(IonParts.XML_ELEMENT)) {
//        if (ParsingUtils.progressToStartElement(reader, IonModification.XML_ELEMENT,
//            CONST.XML_DATA_TYPE_ELEMENT)) {
        var part = IonParts.loadFromXML(reader);
        Objects.requireNonNull(part);
        parts.add(part);
      }
    }
    if (parts.isEmpty()) {
      throw new IllegalStateException("No ion parts found in xml");
    }

    return IonType.create(parts, molecules);
  }

  /**
   *
   * @return true if there are parts overlaps
   */
  public boolean hasPartsOverlap(@Nullable IonType a) {
    return hasPartsOverlap(a, false);
  }

  /**
   * @param ignoreLossAdditionPairs if true then losses and addition (-H and +H) pairs are ignored
   *                                while parts with the same sign are filtered out.
   * @return true if there are parts overlaps
   */
  public boolean hasPartsOverlap(@Nullable IonType a, boolean ignoreLossAdditionPairs) {
    // usually very low number of parts so can just use list operations
    return streamPartsOverlap(a, ignoreLossAdditionPairs).findAny().isPresent();
  }

  /**
   * @return a stream of part pairs [this, a]
   */
  public Stream<IonPart[]> streamPartsOverlap(@Nullable IonType a) {
    return streamPartsOverlap(a, false);
  }

  /**
   * @param ignoreLossAdditionPairs if true then losses and addition (-H and +H) pairs are ignored
   *                                while parts with the same sign are filtered out.
   * @return a stream of part pairs [this, a]
   */
  public Stream<IonPart[]> streamPartsOverlap(@Nullable IonType a,
      boolean ignoreLossAdditionPairs) {
    if (a == null) {
      return Stream.empty();
    }

    // usually very low number of parts so can just use list operations
    return a.parts.stream().map(part -> {
      for (IonPart thisPart : this.parts) {
        if (ignoreLossAdditionPairs && thisPart.count() * part.count() < 0) {
          // both are different then multiplied is negative
          continue; // skip
        }

        if (thisPart.equalsWithoutCount(part)) {
          return new IonPart[]{thisPart, part};
        }
      }
      return null;
    }).filter(Objects::nonNull);
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


  @JsonIgnore
  public PolarityType getPolarity() {
    return PolarityType.fromInt(totalCharge, PolarityType.NEUTRAL);
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
    stream(false).filter(IonPart::isAddition).forEach(ion -> ion.addToFormula(formula, ionize));
    stream(false).filter(IonPart::isLoss).forEach(ion -> ion.addToFormula(formula, ionize));

    return result;
  }

  /**
   * @param includeSilentCharges include the silent charges that are used to add or remove charges
   *                             when it does not sum up
   * @return a stream of ion parts
   */
  public Stream<IonPart> stream(boolean includeSilentCharges) {
    return includeSilentCharges ? stream() : stream().filter(not(IonPart::isSilentCharge));
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final IonType ionType)) {
      return false;
    }

    return molecules == ionType.molecules && totalCharge == ionType.totalCharge
        && Double.compare(totalMass, ionType.totalMass) == 0 && name.equals(ionType.name)
        && parts.equals(ionType.parts);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + parts.hashCode();
    result = 31 * result + Double.hashCode(totalMass);
    result = 31 * result + totalCharge;
    result = 31 * result + molecules;
    return result;
  }

  public @NotNull String name() {
    return name;
  }

  public @NotNull List<@NotNull IonPart> parts() {
    return parts;
  }

  public double totalMass() {
    return totalMass;
  }

  public int totalCharge() {
    return totalCharge;
  }

  public int molecules() {
    return molecules;
  }


  public enum IonTypeStringFlavor {
    /**
     * [M-H2O+Na]+
     */
    SIMPLE_DEFAULT,
    /**
     * including mass: [M-H2O+Na]+ (totalMass Da)
     */
    FULL_WITH_MASS,
    /**
     * ONLY for alphabetic order
     */
    FOR_ALPHABETICAL_SORTING;


  }
}
