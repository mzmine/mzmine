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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.identities.IonPart;
import io.github.mzmine.datamodel.identities.IonParts;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.StringMapParser;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Deprecated
class LegacyIonModification extends LegacyNeutralMolecule implements
    Comparable<LegacyIonModification>, StringMapParser<LegacyIonModification> {

  public static Comparator<LegacyIonModification> POLARITY_MASS_SORTER = Comparator.comparing(
          LegacyIonModification::getPolarity).thenComparingInt(LegacyIonModification::getAbsCharge)
      .thenComparing(LegacyNeutralMolecule::getMass);

  // use combinations of X adducts (2H++; -H+Na2+) and modifications
  public static final LegacyIonModification M_MINUS = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "e", +0.00054858, -1);
  // NR4+ is already charged, mass might also be charged already
  public static final LegacyIonModification M_MINUS_ALREADY_CHARGED = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "e", 0, -1);
  public static final LegacyIonModification H_NEG = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "H", "H", -1.007276, -1);
  public static final LegacyIonModification M_PLUS = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "e", -0.00054858, 1);
  // NR4+ is already charged, mass might also be charged already
  public static final LegacyIonModification M_PLUS_ALREADY_CHARGED = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "e", 0, 1);
  public static final LegacyIonModification H = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "H", "H", 1.007276, 1);
  // water loss
  public static final LegacyIonModification H2O = new LegacyIonModification(
      LegacyIonModificationType.NEUTRAL_LOSS, "H2O", "H2O", -18.010565, 0);
  //
  public static final LegacyIonModification NA = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "Na", "Na", 22.989218, 1);
  public static final LegacyIonModification NH4 = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "NH4", "NH4", 18.033823, 1);
  public static final LegacyIonModification K = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "K", "K", 38.963158, 1);
  public static final LegacyIonModification FE = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "Fe", "Fe", 55.933840, 2);
  public static final LegacyIonModification CA = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "Ca", "Ca", 39.961493820, 2);
  public static final LegacyIonModification MG = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "Mg", "Mg", 47.96953482, 2);
  // combined
  public static final LegacyIonModification M_PLUS_H2O = LegacyCombinedIonModification.create(
      M_PLUS, H2O);
  public static final LegacyIonModification H_H2O_1 = LegacyCombinedIonModification.create(H, H2O);
  public static final LegacyIonModification H_H2O_2 = LegacyCombinedIonModification.create(H, H2O,
      H2O);
  public static final LegacyIonModification H_H2O_3 = LegacyCombinedIonModification.create(H, H2O,
      H2O, H2O);
  public static final LegacyIonModification H2plus = LegacyCombinedIonModification.create(H, H);
  public static final LegacyIonModification M2plus = LegacyCombinedIonModification.create(M_PLUS,
      M_PLUS);
  public static final LegacyIonModification NA_H = LegacyCombinedIonModification.create(NA, H);
  public static final LegacyIonModification K_H = LegacyCombinedIonModification.create(K, H);
  public static final LegacyIonModification NH4_H = LegacyCombinedIonModification.create(NH4, H);
  public static final LegacyIonModification Hneg_NA2 = LegacyCombinedIonModification.create(NA, NA,
      H_NEG);
  public static final LegacyIonModification Hneg_CA = LegacyCombinedIonModification.create(CA,
      H_NEG);
  public static final LegacyIonModification Hneg_FE = LegacyCombinedIonModification.create(FE,
      H_NEG);
  public static final LegacyIonModification Hneg_MG = LegacyCombinedIonModification.create(MG,
      H_NEG);

  // NEGATIVE
  public static final LegacyIonModification CL = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "Cl", "Cl", 34.969401, -1);
  public static final LegacyIonModification BR = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "Br", "Br", 78.918886, -1);
  public static final LegacyIonModification FA = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "FA", "HCO2", 44.99820285, -1);
  public static final LegacyIonModification ACETATE = new LegacyIonModification(
      LegacyIonModificationType.ADDUCT, "Acetate", "C2H3O2", 59.013304, -1);
  // combined
  // +Na -2H+]-
  public static final LegacyIonModification NA_2H = LegacyCombinedIonModification.create(NA, H_NEG,
      H_NEG);

  // modifications
  public static final LegacyIonModification H2 = new LegacyIonModification(
      LegacyIonModificationType.NEUTRAL_LOSS, "H2", "H2", -2.015650, 0);
  public static final LegacyIonModification C2H4 = new LegacyIonModification(
      LegacyIonModificationType.NEUTRAL_LOSS, "C2H4", "C2H4", -28.031301, 0);
  public static final LegacyIonModification H2O_2 = LegacyCombinedIonModification.create(H2O, H2O);
  public static final LegacyIonModification H2O_3 = LegacyCombinedIonModification.create(H2O, H2O,
      H2O);
  public static final LegacyIonModification H2O_4 = LegacyCombinedIonModification.create(H2O, H2O,
      H2O, H2O);
  public static final LegacyIonModification H2O_5 = LegacyCombinedIonModification.create(H2O, H2O,
      H2O, H2O, H2O);

  public static final LegacyIonModification NH3 = new LegacyIonModification(
      LegacyIonModificationType.NEUTRAL_LOSS, "NH3", "NH3", -17.026549, 0);
  public static final LegacyIonModification CO = new LegacyIonModification(
      LegacyIonModificationType.NEUTRAL_LOSS, "CO", "CO", -27.994915, 0);
  public static final LegacyIonModification CO2 = new LegacyIonModification(
      LegacyIonModificationType.NEUTRAL_LOSS, "CO2", "CO2", -43.989829, 0);
  // cluster
  public static final LegacyIonModification MEOH = new LegacyIonModification(
      LegacyIonModificationType.CLUSTER, "MeOH", "CH3OH", 32.026215, 0);
  public static final LegacyIonModification HFA = new LegacyIonModification(
      LegacyIonModificationType.CLUSTER, "HFA", "CHOOH", 46.005479, 0);
  public static final LegacyIonModification HAc = new LegacyIonModification(
      LegacyIonModificationType.CLUSTER, "HAc", "CH3COOH", 60.021129, 0);
  public static final LegacyIonModification ACN = new LegacyIonModification(
      LegacyIonModificationType.CLUSTER, "ACN", "CH3CN", 41.026549, 0);
  public static final LegacyIonModification O = new LegacyIonModification(
      LegacyIonModificationType.CLUSTER, "O", "O", 15.99491462, 0);
  public static final LegacyIonModification ISOPROP = new LegacyIonModification(
      LegacyIonModificationType.CLUSTER, "IsoProp", "C3H8O", 60.058064, 0);
  // isotopes
  public static final LegacyIonModification C13 = new LegacyIonModification(
      LegacyIonModificationType.ISOTOPE, "(13C)", 1.003354838, 0);

  // default values
  public static final LegacyIonModification[] DEFAULT_VALUES_POSITIVE = {M_PLUS, M_PLUS_H2O, H,
      H_H2O_1, H_H2O_2, H_H2O_3, NA, K, NH4, M2plus, H2plus, CA, FE, MG, NA_H, NH4_H, K_H, Hneg_NA2,
      Hneg_CA, Hneg_FE, Hneg_MG, M_PLUS_ALREADY_CHARGED, H2O, H_NEG};
  public static final LegacyIonModification[] DEFAULT_VALUES_NEGATIVE = {M_MINUS, H_NEG, NA_2H, CL,
      BR, FA, ACETATE, M_MINUS_ALREADY_CHARGED, H2O, NA};
  // default modifications
  public static final LegacyIonModification[] DEFAULT_VALUES_MODIFICATIONS = {H2O, H2O_2, H2O_3,
      H2O_4, H2O_5, NH3, O, CO, CO2, C2H4, HFA, HAc, MEOH, ACN, ISOPROP};
  // isotopes
  public static final LegacyIonModification[] DEFAULT_VALUES_ISOTOPES = {C13};
  public static final String XML_ELEMENT = "ionmodification";
  protected final LegacyIonModificationType type;
  protected final int charge;
  // charge
  protected String parsedName;

  /**
   * new raw adduct
   *
   * @param type
   * @param name
   * @param massDifference
   * @param charge
   */
  public LegacyIonModification(LegacyIonModificationType type, String name, double massDifference,
      int charge) {
    this(type, name, null, massDifference, charge);
  }

  /**
   * @param type
   * @param name
   * @param molFormula
   * @param massDifference
   * @param charge
   */
  public LegacyIonModification(LegacyIonModificationType type, String name, String molFormula,
      double massDifference, int charge) {
    super(name, molFormula, massDifference);
    this.charge = charge;
    this.type = type;
    parsedName = parseName();
  }

  /**
   * Only for super classes that need to parse their own name (see
   * {@link LegacyCombinedIonModification})
   *
   * @param type
   * @param massDifference
   * @param charge
   */
  protected LegacyIonModification(LegacyIonModificationType type, double massDifference,
      int charge) {
    super("", null, massDifference);
    this.charge = charge;
    this.type = type;
  }

  public void saveToXML(@NotNull final XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute("name", name);
    writer.writeAttribute("formula", molFormula != null ? molFormula : CONST.XML_NULL_VALUE);
    writer.writeAttribute("massdifference", String.valueOf(mass));
    writer.writeAttribute("type", type.name());
    writer.writeAttribute("charge", String.valueOf(charge));
    writer.writeEndElement();
  }

  public static LegacyIonModification loadFromXML(@NotNull final XMLStreamReader reader) {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Current element is not an ion modification element.");
    }

    String name = reader.getAttributeValue(null, "name");
    String formula = ParsingUtils.readNullableString(reader.getAttributeValue(null, "formula"));
    String massDiff = reader.getAttributeValue(null, "massdifference");
    String type = reader.getAttributeValue(null, "type");
    String charge = reader.getAttributeValue(null, "charge");

    return new LegacyIonModification(LegacyIonModificationType.valueOf(type), name, formula,
        Double.parseDouble(massDiff), Integer.parseInt(charge));
  }


  /**
   * Get the default adducts.
   *
   * @return the list of default adducts.
   */
  public static LegacyIonModification[] getDefaultValuesPos() {
    return Arrays.copyOf(DEFAULT_VALUES_POSITIVE, DEFAULT_VALUES_POSITIVE.length);
  }

  public static LegacyIonModification[] getDefaultValuesNeg() {
    return Arrays.copyOf(DEFAULT_VALUES_NEGATIVE, DEFAULT_VALUES_NEGATIVE.length);
  }

  public static LegacyIonModification[] getDefaultModifications() {
    return Arrays.copyOf(DEFAULT_VALUES_MODIFICATIONS, DEFAULT_VALUES_MODIFICATIONS.length);
  }

  public static LegacyIonModification[] getDefaultIsotopes() {
    return Arrays.copyOf(DEFAULT_VALUES_MODIFICATIONS, DEFAULT_VALUES_ISOTOPES.length);
  }

  /**
   * Undefined adduct for charge
   *
   * @param charge
   * @return [M+?]+-charge
   */
  public static LegacyIonModification getUndefinedforCharge(int charge) {
    double mass = LegacyIonModification.M_PLUS.getMass() * charge;
    return new LegacyIonModification(LegacyIonModificationType.UNDEFINED_ADDUCT, "?", mass, charge);
  }

  @Override
  public String parseName() {
    if ("e".equals(name)) {
      return "";
    }
    String sign = this.getMass() < 0 ? "-" : "+";
    // always +?
    if (type.equals(LegacyIonModificationType.UNDEFINED_ADDUCT)) {
      sign = "+";
    }
    return sign + getName();
  }

  public LegacyIonModificationType getType() {
    return type;
  }

  /**
   * @return array of names
   */
  public String[] getRawNames() {
    return new String[]{getName()};
  }

  /**
   * @return parsed name (f.e. -2H+Na)
   */
  public String getParsedName() {
    return parsedName;
  }

  public int getCharge() {
    return charge;
  }

  /**
   * checks all sub/raw ESIAdductTypes
   *
   * @param a other
   * @return true if parsedName equals
   */
  public boolean nameEquals(LegacyIonModification a) {
    return parsedName.equals(a.parsedName);
  }


  /**
   * @param part formula or predefined single part (modification or adduct)
   * @return modification or null
   */
  public static @NotNull LegacyIonModification parseFromString(String part) {
    return Stream.of(DEFAULT_VALUES_POSITIVE, DEFAULT_VALUES_NEGATIVE, DEFAULT_VALUES_MODIFICATIONS)
        .flatMap(Arrays::stream).filter(m -> {
          String sign = m.getAddRemovePartSign();
          return part.equals(sign + m.getName()) || part.equals(sign + m.getMolFormula()) ||
              // positive part can also be without sign
              (sign.equals("+") && (part.equals(m.getName()) || part.equals(m.getMolFormula())));
        }).findFirst().orElseGet(() -> {
          // if formula fails - cannot know the charge and massDiff - so just default to zero
          // parser will add charges later
          return requireNonNullElse(fromFormula(part),
              new LegacyIonModification(LegacyIonModificationType.UNKNOWN, part, 0, 0));
        });
  }

  @NotNull
  public String getAddRemovePartSign() {
    return switch (Double.compare(mass, 0d)) {
      case -1 -> "-";
      case 1 -> "+";
      default -> "";
    };
  }

  @NotNull
  public String getChargeSign() {
    return switch (charge) {
      case -1 -> "-";
      case 1 -> "+";
      default -> "";
    };
  }

  /**
   * @param part formula with sign or without -Na is negative while +Na or Na are positive
   * @return null if formula cannot be parsed
   */
  @Nullable
  public static LegacyIonModification fromFormula(String part) {
    if (part == null || part.isBlank()) {
      return null;
    }
    char first = part.charAt(0);
    int multiplier = 1;
    if (first == '+' || first == '-') {
      part = part.substring(1);
      if (first == '-') {
        multiplier = -1;
      }
    }
    var formula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(part);
    if (formula == null) {
      return null;
    }
    return new LegacyIonModification(LegacyIonModificationType.UNKNOWN, part, part,
        multiplier * FormulaUtils.getMonoisotopicMass(formula), 0);
  }


  @Override
  public String toString() {
    return toString(true);
  }

  public String toString(boolean showMass) {
    int absCharge = Math.abs(charge);
    String z = absCharge > 1 ? String.valueOf(absCharge) : "";
    z += (charge < 0 ? "-" : "+");
    if (charge == 0) {
      z = "";
    }

    // molecules
    if (showMass) {
      return MessageFormat.format("[M{0}]{1} ({2})", parsedName, z,
          MZmineCore.getConfiguration().getMZFormat().format(getMass()));
    } else {
      return MessageFormat.format("[M{0}]{1}", parsedName, z);
    }
  }

  public String getMassDiffString() {
    return MZmineCore.getConfiguration().getMZFormat().format(mass) + " m/z";
  }

  /**
   * Checks mass diff, charge and mol equality
   *
   * @param modification
   * @return true if the mass difference and the same charge
   */
  public boolean sameMathDifference(LegacyIonModification modification) {
    return sameMassDifference(modification) && charge == modification.charge;
  }

  /**
   * Checks mass diff
   *
   * @param modification
   * @return true if equal mass difference
   */
  public boolean sameMassDifference(LegacyIonModification modification) {
    return Double.compare(mass, modification.mass) == 0;
  }

  /**
   * @return the absolute charge
   */
  public int getAbsCharge() {
    return Math.abs(charge);
  }

  /**
   * @return array of modifications ({@link LegacyIonModification} has one;
   * {@link LegacyCombinedIonModification} has n)
   */
  @NotNull
  public LegacyIonModification[] getModifications() {
    return new LegacyIonModification[]{this};
  }

  /**
   * Stream all modifications
   *
   * @return
   */
  public Stream<LegacyIonModification> streamModifications() {
    return Stream.of(getModifications());
  }

  /**
   * The number of modifications (see {@link LegacyCombinedIonModification})
   *
   * @return
   */
  public int getNumberOfModifications() {
    return 1;
  }

  /**
   * sorting
   */
  @Override
  public int compareTo(LegacyIonModification a) {
    // electrons always last needed for name generation later
    if (isElectron() && a.isElectron()) {
      return 0;
    }
    if (isElectron()) {
      return 1;
    }
    if (a.isElectron()) {
      return -1;
    }

    //
    int i = this.getName().compareTo(a.getName());
    if (i == 0) {
      i = Double.compare(getMass(), a.getMass());
      if (i == 0) {
        i = Double.compare(getCharge(), a.getCharge());
      }
    }
    return i;
  }

  public boolean isElectron() {
    return getName().equals("e");
  }

  /**
   * (mz * absolute charge) - deltaMass
   *
   * @param mz the mass to charge ratio
   * @return the neutral mass for a specific m/z value
   */
  public double getMass(double mz) {
    return ((mz * this.getAbsCharge()) - this.getMass());
  }

  /**
   * neutral mass of M to mz of yM+X]charge
   * <p>
   * (mass + deltaMass) / absolute charge
   *
   * @return the mass to charge ratio for a neutral mass
   */
  public double getMZ(double neutralmass) {
    return (neutralmass + getMass()) / getAbsCharge();
  }

  @Override
  public int hashCode() {
    return Objects.hash(parsedName, charge, mass);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!obj.getClass().equals(getClass())) {
      return false;
    }
    if (!(obj instanceof LegacyIonModification other)) {
      return false;
    }
    if (charge != other.charge) {
      return false;
    }
    if (parsedName == null) {
      if (other.parsedName != null) {
        return false;
      }
    } else if (!parsedName.equals(other.parsedName)) {
      return false;
    }

    return Objects.equals(mass, other.getMass());
  }

  /**
   * Creates the opposite modification: -H2O --> +H2O
   *
   * @return creates opposite modification by flipping the mass difference
   */
  public LegacyIonModification createOpposite() {
    return new LegacyIonModification(getType(), name, molFormula, -mass, charge);
  }

  /**
   * @return true if no adduct is a duplicate
   */
  public boolean uniqueAdductsTo(LegacyIonModification adduct) {
    LegacyIonModification[] a = getModifications();
    LegacyIonModification[] b = adduct.getModifications();
    return Arrays.stream(a).noneMatch(adda -> Arrays.stream(b).anyMatch(addb -> adda.equals(addb)));
  }

  /**
   * All sub types of this need to be in argument parent
   *
   * @param parent
   * @return
   */
  public boolean isSubsetOf(LegacyIonModification parent) {
    if (parent instanceof LegacyCombinedIonModification) {
      // ion modifications all need to be in the mod array of this
      LegacyIonModification[] full = parent.getModifications();
      LegacyIonModification[] subset = this.getModifications();
      boolean[] used = new boolean[full.length];

      for (int i = 0; i < subset.length; i++) {
        boolean found = false;
        for (int tm = 0; tm < used.length && !found; tm++) {
          if (!used[tm] && full[tm].equals(subset[i])) {
            used[tm] = true;
            found = true;
          }
        }
        if (!found) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Removes all sub types of parameter from this type. See also
   * {@link LegacyCombinedIonModification#remove(LegacyIonModification)}
   *
   * @param type
   * @return
   */
  @Nullable
  public LegacyIonModification remove(LegacyIonModification type) {
    if (this.isSubsetOf(type) || this.equals(type)) {
      return null;
    } else {
      return this;
    }
  }

  /**
   * this or any sub modification (for combined) equals to mod?
   *
   * @param mod
   * @return
   */
  public boolean contains(LegacyIonModification mod) {
    return this.equals(mod);
  }

  /**
   * Number of sub IonModifications
   *
   * @return the number of sub modifications
   */
  public int getModCount() {
    return 1;
  }


  @Override
  public Map<String, String> getDataMap() {
    Map<String, String> map = new TreeMap<>();
    map.put("Name", getName());
    map.put("Mass Diff", String.valueOf(getMass()));
    map.put("Type", getType().name());
    map.put("Charge", String.valueOf(getCharge()));
    map.put("Formula", getMolFormula());
    return map;
  }

  @Override
  public LegacyIonModification parseDataMap(Map<String, String> map) {
    String name = map.get("Name");
    // is combined
    if (name.split(";").length > 1) {
      try {
        List<LegacyIonModification> mods = new ArrayList<>();

        String[] names = name.split(";");
        String[] massdiffs = map.get("Mass Diff").split(";");
        String[] charges = map.get("Charge").split(";");
        String[] formulas = map.get("Formula").split(";");
        String[] types = map.get("Type").split(";");
        for (int i = 0; i < names.length; i++) {
          double massdiff = Double.parseDouble(massdiffs[i]);
          int charge = Integer.parseInt(charges[i]);
          String formula = formulas[i];
          LegacyIonModificationType type = LegacyIonModificationType.valueOf(types[i]);

          LegacyIonModification ion = new LegacyIonModification(type, names[i], formula, massdiff,
              charge);
          mods.add(ion);
        }
        return LegacyCombinedIonModification.create(mods);
      } catch (Exception ex) {
        return null;
      }
    } else {
      new LegacyIonModification(LegacyIonModificationType.ADDUCT, "NH4", "NH4", 18.033823, 1);
      try {
        double massdiff = Double.parseDouble(map.get("Mass Diff"));
        int charge = Integer.parseInt(map.get("Charge"));
        int mod = Integer.parseInt(map.getOrDefault("Max Modification", "-1"));
        String formula = map.getOrDefault("Formula", "");
        LegacyIonModificationType type = LegacyIonModificationType.valueOf(
            map.getOrDefault("Type", ""));
        return new LegacyIonModification(type, name, formula, massdiff, charge);
      } catch (Exception ex) {
        return null;
      }
    }
  }

  /**
   * @return checks for charge smaller / greater than 0
   */
  public PolarityType getPolarity() {
    return PolarityType.fromInt(charge);
  }

  /**
   * Create new modification with changed charge
   *
   * @param newCharge new charge overrides only the old charge. nothing else
   * @return copy of this ion modification with other charge
   */
  public LegacyIonModification withCharge(final int newCharge) {
    return new LegacyIonModification(type, name, molFormula, mass, newCharge);
  }

  /**
   * Attempts to find the best fitting ion modification for the differnce of the neutral mass and
   * the mz within the given tolerance.
   *
   * @param tol The mz tolerance to look for. The tolerance is applied to (neutralMass + adduct) vs
   *            mz, not the (mz - neutralMass) vs adduct.
   * @return The best fitting modification or null.
   */
  public static @Nullable LegacyIonModification getBestIonModification(double neutralMass,
      double mz, @NotNull MZTolerance tol, @Nullable PolarityType polarity) {

    // select the appropriate modifications to search
    final Stream<LegacyIonModification> modifications = switch (polarity) {
      case POSITIVE -> Stream.of(LegacyIonModification.DEFAULT_VALUES_POSITIVE);
      case NEGATIVE -> Stream.of(LegacyIonModification.DEFAULT_VALUES_NEGATIVE);
      case NEUTRAL -> Stream.of();
      case ANY, UNKNOWN ->
          Stream.of(DEFAULT_VALUES_POSITIVE, DEFAULT_VALUES_NEGATIVE).flatMap(Arrays::stream);
      case null ->
          Stream.of(DEFAULT_VALUES_POSITIVE, DEFAULT_VALUES_NEGATIVE).flatMap(Arrays::stream);
    };

    return modifications.filter(m -> m.getCharge() != 0)
        .filter(m -> tol.checkWithinTolerance(m.getMZ(neutralMass), mz))
        .min(Comparator.comparingDouble(m -> Math.abs(m.getMZ(neutralMass) - mz))).orElse(null);
  }

  @NotNull
  public Stream<? extends IonPart> toNewParts() {
    return Stream.of(IonParts.create(name, molFormula, mass, charge, getModCount()));
  }
}
