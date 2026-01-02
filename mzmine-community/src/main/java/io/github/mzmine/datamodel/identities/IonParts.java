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

package io.github.mzmine.datamodel.identities;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.StringUtils;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class IonParts {

  public static final String XML_ELEMENT = "ionpart";

  /**
   * The only part allowed with empty name
   */
  public static final IonPart SILENT_CHARGE = new IonPartSilentCharge(1);
  /**
   * An unknown part
   */
  public static final IonPart UNKNOWN = new IonPartUnknown("?", 0, 1);

  public static final IonPart M_MINUS = ofNamed("e", IonUtils.ELECTRON_MASS, -1);
  public static final IonPart M_PLUS = M_MINUS.withCount(-1);

  public static final IonPart H = ofFormula("H", 1);
  public static final IonPart H2_PLUS = H.withCount(2);
  public static final IonPart H3_PLUS = H.withCount(3);
  public static final IonPart NA = ofFormula("Na", 1);
  public static final IonPart NA2_PLUS = NA.withCount(2);
  public static final IonPart NH4 = ofFormula("NH4", "NH4", 1);
  public static final IonPart K = ofFormula("K", 1);
  public static final IonPart CA = ofFormula("Ca", 2);
  public static final IonPart MG = ofFormula("Mg", 2);
  public static final IonPart FEIII = ofFormula("Fe", 3);
  public static final IonPart FEII = ofFormula("Fe", 2);

  // negative
  public static final IonPart H_MINUS = H.withCount(-1);
  public static final IonPart F = ofFormula("F", -1);
  public static final IonPart CL = ofFormula("Cl", -1);
  public static final IonPart BR = ofFormula("Br", -1);
  public static final IonPart I = ofFormula("I", -1);
  public static final IonPart FORMATE_FA = ofFormula("CHO2", -1);
  public static final IonPart ACETATE_AC = ofFormula("C2H3O2", -1);
  // complex
  public static final IonPart METHANOL = ofFormula("CH2OH", 0);
  public static final IonPart ETHANOL = ofFormula("C2H6O", 0);
  public static final IonPart ACN = ofFormula("CH3CN", 0);
  public static final IonPart FORMIC_ACID = ofFormula("CH2O2", 0);
  public static final IonPart ACETIC_ACID = ofFormula("CH3COOH", 0);
  public static final IonPart ISO_PROPANOL = ofFormula("C3H8O", 0);
  // loss;
  public static final IonPart CO = ofFormula("CO", 0, -1);
  public static final IonPart CO2 = ofFormula("CO2", 0, -1);
  public static final IonPart NH3 = ofFormula("NH3", "NH3", 0, -1);
  public static final IonPart H2 = ofFormula("H2", 0, -1);
  public static final IonPart C2H4 = ofFormula("C2H4", 0, -1);
  public static final IonPart HCL = ofFormula("HCl", "HCl", 0, -1);
  public static final IonPart H2O = ofFormula("H2O", 0, -1);
  public static final IonPart H2O_2 = H2O.withCount(-2);
  public static final IonPart H2O_3 = H2O.withCount(-3);
  public static final IonPart H2O_4 = H2O.withCount(-4);
  public static final IonPart H2O_5 = H2O.withCount(-5);

  // default modifications
  public static final List<IonPart> DEFAULT_VALUES_NEUTRAL_MODIFICATIONS = List.of(H2O, H2O_2,
      H2O_3, H2O_4, H2O_5, HCL, NH3, CO, CO2, H2, C2H4, FORMIC_ACID, ACETIC_ACID, ACN, METHANOL,
      ETHANOL, ISO_PROPANOL);

  public static final List<IonPart> PREDEFINED_PARTS = List.of(H2O, H2O_2, H2O_3, H2O_4, H2O_5, HCL,
      C2H4, H2, NH3, CO, CO2, FORMIC_ACID, ACETIC_ACID, ACN, METHANOL, ETHANOL, ISO_PROPANOL,
      ACETATE_AC, FORMATE_FA, I, BR, CL, F, H_MINUS,
      // adding FEIII first makes it the default charge state for Fe
      FEIII, FEII, MG, CA, K, NH4, NA2_PLUS, NA, H, H2_PLUS, H3_PLUS, M_MINUS, M_PLUS);


  /**
   * @param nameOrFormula structure or common name
   * @param charge
   * @return an IonPart either predefined by name, common name {@link CompoundsByNames}, by
   * structure. Otherwise, {@link IonParts#unknown(String, Integer)}
   */
  @NotNull
  public static IonPart findPartByNameOrFormula(@NotNull String nameOrFormula, int count,
      Integer charge) {

    nameOrFormula = nameOrFormula.trim();
    // search predefined by mzmine and by user
    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    final List<IonPartDefinition> definitionsForName = global.findPartsByName(nameOrFormula);

    IonPartDefinition best = null;
    for (final IonPartDefinition predefined : definitionsForName) {
      if (predefined.name().equals(nameOrFormula)) {
        // directly found the correct part
        if (charge == null || Objects.equals(charge, predefined.singleCharge())) {
          return predefined.withCount(count);
        }
        if (best == null) {
          best = predefined;
        }
      }
    }
    if (best != null) {
      // mismatch in charge
      // like when the current list of ion parts defines Fe+2 but not Fe+3 which was loaded
      // create a new instance with that charge
      return best.withCount(count).withSingleCharge(charge);
    }

    // map names or structure by internal known names
    Optional<IonPart> part = CompoundsByNames.getIonPartByName(nameOrFormula);
    if (part.isPresent()) {
      return part.get().withCount(count).withSingleCharge(charge);
    }

    // try with formula or return unknown
    return ofFormula(nameOrFormula, charge, count);
  }


  /**
   * merges duplicate parts (all matching properties excluding the count). If after merging a part
   * has count==0 it is removed from the list. Like when adding and removing a 'Na'.
   *
   * @param parts may contain duplicates
   * @return unmodifiable list of ion parts
   */
  public static List<IonPart> mergeDuplicates(@NotNull IonPart... parts) {
    return mergeDuplicates(List.of(parts));
  }

  /**
   * merges duplicate parts (all matching properties excluding the count). If after merging a part
   * has count==0 it is removed from the list. Like when adding and removing a 'Na'.
   *
   * @param parts may contain duplicates
   * @return unmodifiable list of ion parts
   */
  @NotNull
  public static List<IonPart> mergeDuplicates(@Nullable Collection<IonPart> parts) {
    if (parts == null) {
      return List.of();
    }
    // use trick to create a copy with count 1 to group all parts based on the other properties but not count
    final Collection<List<IonPart>> groupedDuplicates = parts.stream().filter(Objects::nonNull)
        .collect(Collectors.groupingBy(type -> type.withCount(1))).values();
    // merge duplicates into one and return new list
    //noinspection DataFlowIssue,OptionalGetWithoutIsPresent
    return groupedDuplicates.stream()
        .map(duplicates -> duplicates.stream().reduce(IonPart::merge).get())
        // require count !=0 to only keep parts that matter
        .filter(part -> part.count() != 0).toList();
  }


  /**
   * @param name          clear name - often derived from formula or from alternative names. Empty
   *                      name is only supported for {@link IonParts#SILENT_CHARGE}
   * @param singleFormula uncharged formula without multiplier formula may be null if unknown.
   *                      Formula of a single item - so the count multiplier is not added. Using a
   *                      String here instead of CDK formula as CDK formula does not implement
   *                      equals. If formula does not parse correctly, this will result in unknown
   *                      part definition with formula as name and no mass definition.
   * @param absSingleMass absolute (positive) mass of a single item of this type which is multiplied
   *                      by count to get total mass.
   * @param singleCharge  signed charge of a single item which is multiplied by count to get total
   *                      charge. Both H+ and 2H+ would be single charge +1. See count.
   * @param count         the singed multiplier of this single item, non-zero. e.g., 2 for +2Na and
   *                      -1 for -H
   */
  @NotNull
  public static IonPart create(@Nullable String name, @Nullable String singleFormula,
      @Nullable Double absSingleMass, @Nullable Integer singleCharge,
      final @Nullable Integer count) {

    if (StringUtils.isBlank(singleFormula)) {
      singleFormula = null;
    }

    if (singleFormula != null) {
      singleFormula = StringUtils.removeAllWhiteSpace(singleFormula);
      // try parse formula
      final IMolecularFormula parsedFormula =
          singleCharge == null ? FormulaUtils.createMajorIsotopeMolFormulaWithCharge(singleFormula)
              : FormulaUtils.createMajorIsotopeMolFormulaWithCharge(singleFormula, singleCharge);

      if (parsedFormula == null) {
        if (name == null) {
          name = singleFormula;
        }
        // remain unknown as formula was not parsed - mass 0 and formula null
        return unknown(name, requireNonNullElse(singleCharge, 0), count);
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

    if (name == null && StringUtils.isBlank(singleFormula)) {
      throw new IllegalArgumentException("name or singleFormula must be defined");
    }

    name = requireNonNullElse(name, singleFormula).trim();

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
      return new IonPartSilentCharge(requireNonNullElse(count, 0));
    }

    // always positive and then multiplied with count
    absSingleMass = Math.abs(absSingleMass);
    singleCharge = requireNonNullElse(singleCharge, 0);

    if (singleFormula == null && Double.compare(absSingleMass, 0d) == 0) {
      return unknown(name, singleCharge, count);
    }

    if (count == null) {
      // return only definition to skip count for hashmaps comparison
      return new IonPartDefinition(name, singleFormula, absSingleMass, singleCharge);
    }
    return new IonPartFullCounted(name, singleFormula, absSingleMass, singleCharge, count);
  }


  /**
   * Formula constructor with count 1
   *
   * @param formula used to calculate other fields
   */
  @NotNull
  public static IonPart ofFormula(@NotNull final String formula,
      final @Nullable Integer singleCharge) {
    return ofFormula(formula, singleCharge, 1);
  }

  /**
   * Name overwriting formula name
   */
  @NotNull
  public static IonPart ofFormula(@Nullable String name, @NotNull String formula,
      final @Nullable Integer singleCharge) {
    return ofFormula(name, formula, singleCharge, 1);
  }

  /**
   * Formula constructor
   *
   * @param formula used to calculate other fields
   */
  @NotNull
  public static IonPart ofFormula(@NotNull String formula, @Nullable Integer singleCharge,
      final @Nullable Integer count) {
    return ofFormula(null, formula, singleCharge, count);
  }

  @NotNull
  public static IonPart ofFormula(@Nullable String name, @NotNull String formula,
      @Nullable Integer singleCharge, final @Nullable Integer count) {

    // formula as name
    return create(name, formula, null, singleCharge, count);
  }

  /**
   * No formula constructor
   */
  @NotNull
  public static IonPart ofNamed(@NotNull String name, final double singleMass,
      final @Nullable Integer singleCharge) {
    return ofNamed(name, singleMass, singleCharge, 1);
  }

  /**
   * No formula constructor
   */
  @NotNull
  public static IonPart ofNamed(@NotNull String name, final double singleMass,
      final @Nullable Integer singleCharge, final @Nullable Integer count) {
    return create(name, null, singleMass, singleCharge, count);
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
  public static IonPart unknown(final String name, final @Nullable Integer signedCount) {
    return unknown(name, 0, signedCount);
  }

  /**
   * @param signedCount - or + count here to see if this is a loss or addition
   */
  public static IonPart unknown(@NotNull final String name, final Integer singleCharge,
      final @Nullable Integer signedCount) {
    if (signedCount == null) {
      return new IonPartDefinition(name, null, 0d, singleCharge);
    }

    return new IonPartUnknown(name, requireNonNullElse(singleCharge, 0), signedCount);
  }

  @NotNull
  public static IonPart loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals("ionpart"))) {
      throw new IllegalStateException("Current element is not an ionpart");
    }
    final Integer silentCharge = ParsingUtils.stringToInteger(
        reader.getAttributeValue(null, "silentCharge"));
    if (silentCharge != null) {
      // unknown silent charge instance if it was set
      return new IonPartSilentCharge(silentCharge);
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

    return create(name, formula, mass, charge, count);
  }


  public static void saveToXML(@NotNull XMLStreamWriter writer, @Nullable IonPart p)
      throws XMLStreamException {
    if (p == null) {
      return;
    }
    writer.writeStartElement(IonParts.XML_ELEMENT);

    if (p.isSilentCharge()) {
      // silent charge of type {@link IonPartSilentCharge}
      writer.writeAttribute("silentCharge", String.valueOf(p.count()));
    } else {
      // regular
      writer.writeAttribute("name", p.name());
      writer.writeAttribute("mass", String.valueOf(p.absSingleMass()));
      writer.writeAttribute("charge", String.valueOf(p.singleCharge()));
      writer.writeAttribute("count", String.valueOf(p.count()));
      if (p.singleFormula() != null) {
        writer.writeAttribute("formula", p.singleFormula());
      }
    }
    writer.writeEndElement();
  }
}
