/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.identities.NeutralMolecule;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.StringMapParser;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

public class IonModification extends NeutralMolecule implements Comparable<IonModification>,
    StringMapParser<IonModification> {

  // use combinations of X adducts (2H++; -H+Na2+) and modifications
  public static final IonModification M_MINUS = new IonModification(IonModificationType.ADDUCT, "e",
      +0.00054858, -1);
  // NR4+ is already charged, mass might also be charged already
  public static final IonModification M_MINUS_ALREADY_CHARGED = new IonModification(
      IonModificationType.ADDUCT, "e", 0, -1);
  public static final IonModification H_NEG = new IonModification(IonModificationType.ADDUCT, "H",
      "H", -1.007276, -1);
  public static final IonModification M_PLUS = new IonModification(IonModificationType.ADDUCT, "e",
      -0.00054858, 1);
  // NR4+ is already charged, mass might also be charged already
  public static final IonModification M_PLUS_ALREADY_CHARGED = new IonModification(
      IonModificationType.ADDUCT, "e", 0, 1);
  public static final IonModification H = new IonModification(IonModificationType.ADDUCT, "H", "H",
      1.007276, 1);
  //
  public static final IonModification NA = new IonModification(IonModificationType.ADDUCT, "Na",
      "Na", 22.989218, 1);
  public static final IonModification NH4 = new IonModification(IonModificationType.ADDUCT, "NH4",
      "NH4", 18.033823, 1);
  public static final IonModification K = new IonModification(IonModificationType.ADDUCT, "K", "K",
      38.963158, 1);
  public static final IonModification FE = new IonModification(IonModificationType.ADDUCT, "Fe",
      "Fe", 55.933840, 2);
  public static final IonModification CA = new IonModification(IonModificationType.ADDUCT, "Ca",
      "Ca", 39.961493820, 2);
  public static final IonModification MG = new IonModification(IonModificationType.ADDUCT, "Mg",
      "Mg", 47.96953482, 2);
  // combined
  public static final IonModification H2plus = CombinedIonModification.create(H, H);
  public static final IonModification M2plus = CombinedIonModification.create(M_PLUS, M_PLUS);
  public static final IonModification NA_H = CombinedIonModification.create(NA, H);
  public static final IonModification K_H = CombinedIonModification.create(K, H);
  public static final IonModification NH4_H = CombinedIonModification.create(NH4, H);
  public static final IonModification Hneg_NA2 = CombinedIonModification.create(NA, NA, H_NEG);
  public static final IonModification Hneg_CA = CombinedIonModification.create(CA, H_NEG);
  public static final IonModification Hneg_FE = CombinedIonModification.create(FE, H_NEG);
  public static final IonModification Hneg_MG = CombinedIonModification.create(MG, H_NEG);

  // NEGATIVE
  public static final IonModification CL = new IonModification(IonModificationType.ADDUCT, "Cl",
      "Cl", 34.969401, -1);
  public static final IonModification BR = new IonModification(IonModificationType.ADDUCT, "Br",
      "Br", 78.918886, -1);
  public static final IonModification FA = new IonModification(IonModificationType.ADDUCT, "FA",
      "HCO2", 44.99820285, -1);
  // combined
  // +Na -2H+]-
  public static final IonModification NA_2H = CombinedIonModification.create(NA, H_NEG, H_NEG);

  // modifications
  public static final IonModification H2 = new IonModification(IonModificationType.NEUTRAL_LOSS,
      "H2", "H2", -2.015650, 0);
  public static final IonModification C2H4 = new IonModification(IonModificationType.NEUTRAL_LOSS,
      "C2H4", "C2H4", -28.031301, 0);
  public static final IonModification H2O = new IonModification(IonModificationType.NEUTRAL_LOSS,
      "H2O", "H2O", -18.010565, 0);
  public static final IonModification H2O_2 = CombinedIonModification.create(H2O, H2O);
  public static final IonModification H2O_3 = CombinedIonModification.create(H2O, H2O, H2O);
  public static final IonModification H2O_4 = CombinedIonModification.create(H2O, H2O, H2O, H2O);
  public static final IonModification H2O_5 = CombinedIonModification.create(H2O, H2O, H2O, H2O,
      H2O);

  public static final IonModification NH3 = new IonModification(IonModificationType.NEUTRAL_LOSS,
      "NH3", "NH3", -17.026549, 0);
  public static final IonModification CO = new IonModification(IonModificationType.NEUTRAL_LOSS,
      "CO", "CO", -27.994915, 0);
  public static final IonModification CO2 = new IonModification(IonModificationType.NEUTRAL_LOSS,
      "CO2", "CO2", -43.989829, 0);
  // cluster
  public static final IonModification MEOH = new IonModification(IonModificationType.CLUSTER,
      "MeOH", "CH3OH", 32.026215, 0);
  public static final IonModification HFA = new IonModification(IonModificationType.CLUSTER, "HFA",
      "CHOOH", 46.005479, 0);
  public static final IonModification HAc = new IonModification(IonModificationType.CLUSTER, "HAc",
      "CH3COOH", 60.021129, 0);
  public static final IonModification ACN = new IonModification(IonModificationType.CLUSTER, "ACN",
      "CH3CN", 41.026549, 0);
  public static final IonModification O = new IonModification(IonModificationType.CLUSTER, "O", "O",
      15.99491462, 0);
  public static final IonModification ISOPROP = new IonModification(IonModificationType.CLUSTER,
      "IsoProp", "C3H8O", 60.058064, 0);
  // isotopes
  public static final IonModification C13 = new IonModification(IonModificationType.ISOTOPE,
      "(13C)", 1.003354838, 0);

  // default values
  public static final IonModification[] DEFAULT_VALUES_POSITIVE = {H_NEG, M_PLUS, H, NA, K, NH4,
      M2plus, H2plus, CA, FE, MG, NA_H, NH4_H, K_H, Hneg_NA2, Hneg_CA, Hneg_FE,
      M_PLUS_ALREADY_CHARGED, Hneg_MG};
  public static final IonModification[] DEFAULT_VALUES_NEGATIVE = {M_MINUS, H_NEG, NA_2H, NA, CL,
      BR, FA, M_MINUS_ALREADY_CHARGED};
  // default modifications
  public static final IonModification[] DEFAULT_VALUES_MODIFICATIONS = {H2O, H2O_2, H2O_3, H2O_4,
      H2O_5, NH3, O, CO, CO2, C2H4, HFA, HAc, MEOH, ACN, ISOPROP};
  // isotopes
  public static final IonModification[] DEFAULT_VALUES_ISOTOPES = {C13};
  public static final String XML_ELEMENT = "ionmodification";
  protected final IonModificationType type;
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
  public IonModification(IonModificationType type, String name, double massDifference, int charge) {
    this(type, name, null, massDifference, charge);
  }

  /**
   * @param type
   * @param name
   * @param molFormula
   * @param massDifference
   * @param charge
   */
  public IonModification(IonModificationType type, String name, String molFormula,
      double massDifference, int charge) {
    super(name, molFormula, massDifference);
    this.charge = charge;
    this.type = type;
    parsedName = parseName();
  }

  /**
   * Only for super classes that need to parse their own name (see {@link CombinedIonModification})
   *
   * @param type
   * @param massDifference
   * @param charge
   */
  protected IonModification(IonModificationType type, double massDifference, int charge) {
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

  public static IonModification loadFromXML(@NotNull final XMLStreamReader reader) {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Current element is not an ion modification element.");
    }

    String name = reader.getAttributeValue(null, "name");
    String formula = reader.getAttributeValue(null, "formula");
    String massDiff = reader.getAttributeValue(null, "massdifference");
    String type = reader.getAttributeValue(null, "type");
    String charge = reader.getAttributeValue(null, "charge");

    return new IonModification(IonModificationType.valueOf(type), name, formula,
        Double.parseDouble(massDiff), Integer.parseInt(charge));
  }


  /**
   * Get the default adducts.
   *
   * @return the list of default adducts.
   */
  public static IonModification[] getDefaultValuesPos() {
    return Arrays.copyOf(DEFAULT_VALUES_POSITIVE, DEFAULT_VALUES_POSITIVE.length);
  }

  public static IonModification[] getDefaultValuesNeg() {
    return Arrays.copyOf(DEFAULT_VALUES_NEGATIVE, DEFAULT_VALUES_NEGATIVE.length);
  }

  public static IonModification[] getDefaultModifications() {
    return Arrays.copyOf(DEFAULT_VALUES_MODIFICATIONS, DEFAULT_VALUES_MODIFICATIONS.length);
  }

  public static IonModification[] getDefaultIsotopes() {
    return Arrays.copyOf(DEFAULT_VALUES_MODIFICATIONS, DEFAULT_VALUES_ISOTOPES.length);
  }

  /**
   * Undefined adduct for charge
   *
   * @param charge
   * @return [M+?]+-charge
   */
  public static IonModification getUndefinedforCharge(int charge) {
    double mass = IonModification.M_PLUS.getMass() * charge;
    return new IonModification(IonModificationType.UNDEFINED_ADDUCT, "?", mass, charge);
  }

  @Override
  public String parseName() {
    if ("e".equals(name)) {
      return "";
    }
    String sign = this.getMass() < 0 ? "-" : "+";
    // always +?
    if (type.equals(IonModificationType.UNDEFINED_ADDUCT)) {
      sign = "+";
    }
    return sign + getName();
  }

  public IonModificationType getType() {
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
  public boolean nameEquals(IonModification a) {
    return parsedName.equals(a.parsedName);
  }


  /**
   * @param part formula or predefined single part (modification or adduct)
   * @return modification or null
   */
  public static @NotNull IonModification parseFromString(String part) {
    return Stream.of(DEFAULT_VALUES_POSITIVE, DEFAULT_VALUES_NEGATIVE, DEFAULT_VALUES_MODIFICATIONS)
        .flatMap(Arrays::stream).filter(m -> {
          String sign = m.getSign();
          return part.equals(sign + m.getName()) || part.equals(sign + m.getMolFormula()) ||
                 // positive part can also be without sign
                 (sign.equals("+") && (part.equals(m.getName()) || part.equals(m.getMolFormula())));
        }).findFirst().orElseGet(() -> {
          // if formula fails - cannot know the charge and massDiff - so just default to zero
          // parser will add charges later
          return requireNonNullElse(fromFormula(part),
              new IonModification(IonModificationType.UNKNOWN, part, 0, 0));
        });
  }

  @NotNull
  public String getSign() {
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
  public static IonModification fromFormula(String part) {
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
    var formula = FormulaUtils.createMajorIsotopeMolFormula(part);
    if (formula == null) {
      return null;
    }
    return new IonModification(IonModificationType.UNKNOWN, part, part,
        multiplier * FormulaUtils.getMonoisotopicMass(formula), 0);
  }


  @Override
  public String toString() {
    return toString(true);
  }

  public String toString(boolean showMass) {
    int absCharge = Math.abs(charge);
    String z = absCharge > 1 ? absCharge + "" : "";
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
  public boolean sameMathDifference(IonModification modification) {
    return sameMassDifference(modification) && charge == modification.charge;
  }

  /**
   * Checks mass diff
   *
   * @param modification
   * @return true if equal mass difference
   */
  public boolean sameMassDifference(IonModification modification) {
    return Double.compare(mass, modification.mass) == 0;
  }

  /**
   * @return the absolute charge
   */
  public int getAbsCharge() {
    return Math.abs(charge);
  }

  /**
   * @return array of modifications ({@link IonModification} has one;
   * {@link CombinedIonModification} has n)
   */
  @NotNull
  public IonModification[] getModifications() {
    return new IonModification[]{this};
  }

  /**
   * Stream all modifications
   *
   * @return
   */
  public Stream<IonModification> streamModifications() {
    return Stream.of(getModifications());
  }

  /**
   * The number of modifications (see {@link CombinedIonModification})
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
  public int compareTo(IonModification a) {
    int i = this.getName().compareTo(a.getName());
    if (i == 0) {
      i = Double.compare(getMass(), a.getMass());
      if (i == 0) {
        i = Double.compare(getCharge(), a.getCharge());
      }
    }
    return i;
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
    if (!(obj instanceof IonModification other)) {
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
  public IonModification createOpposite() {
    return new IonModification(getType(), name, molFormula, -mass, charge);
  }

  /**
   * @return true if no adduct is a duplicate
   */
  public boolean uniqueAdductsTo(IonModification adduct) {
    IonModification[] a = getModifications();
    IonModification[] b = adduct.getModifications();
    return Arrays.stream(a).noneMatch(adda -> Arrays.stream(b).anyMatch(addb -> adda.equals(addb)));
  }

  /**
   * All sub types of this need to be in argument parent
   *
   * @param parent
   * @return
   */
  public boolean isSubsetOf(IonModification parent) {
    if (parent instanceof CombinedIonModification) {
      // ion modifications all need to be in the mod array of this
      IonModification[] full = parent.getModifications();
      IonModification[] subset = this.getModifications();
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
   * {@link CombinedIonModification#remove(IonModification)}
   *
   * @param type
   * @return
   */
  @Nullable
  public IonModification remove(IonModification type) {
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
  public boolean contains(IonModification mod) {
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
  public IonModification parseDataMap(Map<String, String> map) {
    String name = map.get("Name");
    // is combined
    if (name.split(";").length > 1) {
      try {
        List<IonModification> mods = new ArrayList<>();

        String[] names = name.split(";");
        String[] massdiffs = map.get("Mass Diff").split(";");
        String[] charges = map.get("Charge").split(";");
        String[] formulas = map.get("Formula").split(";");
        String[] types = map.get("Type").split(";");
        for (int i = 0; i < names.length; i++) {
          double massdiff = Double.parseDouble(massdiffs[i]);
          int charge = Integer.parseInt(charges[i]);
          String formula = formulas[i];
          IonModificationType type = IonModificationType.valueOf(types[i]);

          IonModification ion = new IonModification(type, names[i], formula, massdiff, charge);
          mods.add(ion);
        }
        return CombinedIonModification.create(mods);
      } catch (Exception ex) {
        return null;
      }
    } else {
      new IonModification(IonModificationType.ADDUCT, "NH4", "NH4", 18.033823, 1);
      try {
        double massdiff = Double.parseDouble(map.get("Mass Diff"));
        int charge = Integer.parseInt(map.get("Charge"));
        int mod = Integer.parseInt(map.getOrDefault("Max Modification", "-1"));
        String formula = map.getOrDefault("Formula", "");
        IonModificationType type = IonModificationType.valueOf(map.getOrDefault("Type", ""));
        return new IonModification(type, name, formula, massdiff, charge);
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
  public IonModification withCharge(final int newCharge) {
    return new IonModification(type, name, molFormula, mass, newCharge);
  }
}
