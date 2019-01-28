/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */package net.sf.mzmine.datamodel.identities.iontype;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.mzmine.datamodel.identities.NeutralMolecule;
import net.sf.mzmine.main.MZmineCore;

public class IonModification extends NeutralMolecule implements Comparable<IonModification> {

  // use combinations of X adducts (2H++; -H+Na2+) and modifications
  public static final IonModification M_MINUS =
      new IonModification(IonModificationType.ADDUCT, "e", +0.00054858, -1);
  public static final IonModification H_NEG =
      new IonModification(IonModificationType.ADDUCT, "H", "H", -1.007276, -1);
  public static final IonModification M_PLUS =
      new IonModification(IonModificationType.ADDUCT, "e", -0.00054858, 1);
  public static final IonModification H =
      new IonModification(IonModificationType.ADDUCT, "H", "H", 1.007276, 1);
  //
  public static final IonModification NA =
      new IonModification(IonModificationType.ADDUCT, "Na", "Na", 22.989218, 1, 1);
  public static final IonModification NH4 =
      new IonModification(IonModificationType.ADDUCT, "NH4", "NH4", 18.033823, 1);
  public static final IonModification K =
      new IonModification(IonModificationType.ADDUCT, "K", "K", 38.963158, 1, 1);
  public static final IonModification FE =
      new IonModification(IonModificationType.ADDUCT, "Fe", "Fe", 55.933840, 2, 1);
  public static final IonModification CA =
      new IonModification(IonModificationType.ADDUCT, "Ca", "Ca", 39.961493820, 2, 1);
  public static final IonModification MG =
      new IonModification(IonModificationType.ADDUCT, "Mg", "Mg", 47.96953482, 2, 1);
  // combined
  public static final IonModification H2plus =
      new CombinedIonModification(new IonModification[] {H, H});
  public static final IonModification NA_H =
      new CombinedIonModification(new IonModification[] {NA, H});
  public static final IonModification K_H =
      new CombinedIonModification(new IonModification[] {K, H});
  public static final IonModification NH4_H =
      new CombinedIonModification(new IonModification[] {NH4, H});
  public static final IonModification Hneg_NA2 =
      new CombinedIonModification(new IonModification[] {NA, NA, H_NEG});
  public static final IonModification Hneg_CA =
      new CombinedIonModification(new IonModification[] {CA, H_NEG});
  public static final IonModification Hneg_FE =
      new CombinedIonModification(new IonModification[] {FE, H_NEG});
  public static final IonModification Hneg_MG =
      new CombinedIonModification(new IonModification[] {MG, H_NEG});

  // NEGATIVE
  public static final IonModification CL =
      new IonModification(IonModificationType.ADDUCT, "Cl", "Cl", 34.969401, -1);
  public static final IonModification BR =
      new IonModification(IonModificationType.ADDUCT, "Br", "Br", 78.918886, -1);
  public static final IonModification FA =
      new IonModification(IonModificationType.ADDUCT, "FA", "HCO2", 44.99820285, -1);
  // combined
  // +Na -2H+]-
  public static final IonModification NA_2H =
      new CombinedIonModification(new IonModification[] {NA, H_NEG, H_NEG});

  // modifications
  public static final IonModification H2 =
      new IonModification(IonModificationType.NEUTRAL_LOSS, "H2", "H2", -2.015650, 0);
  public static final IonModification C2H4 =
      new IonModification(IonModificationType.NEUTRAL_LOSS, "C2H4", "C2H4", -28.031301, 0);
  public static final IonModification H2O =
      new IonModification(IonModificationType.NEUTRAL_LOSS, "H2O", "H2O", -18.010565, 0);
  public static final IonModification H2O_2 =
      new CombinedIonModification(new IonModification[] {H2O, H2O});

  public static final IonModification NH3 =
      new IonModification(IonModificationType.NEUTRAL_LOSS, "NH3", "NH3", -17.026549, 0);
  public static final IonModification CO =
      new IonModification(IonModificationType.NEUTRAL_LOSS, "CO", "CO", -27.994915, 0);
  public static final IonModification CO2 =
      new IonModification(IonModificationType.NEUTRAL_LOSS, "CO2", "CO2", -43.989829, 0);
  // cluster
  public static final IonModification MEOH =
      new IonModification(IonModificationType.CLUSTER, "MeOH", "CH3OH", 32.026215, 0);
  public static final IonModification HFA =
      new IonModification(IonModificationType.CLUSTER, "HFA", "CHOOH", 46.005479, 0);
  public static final IonModification HAc =
      new IonModification(IonModificationType.CLUSTER, "HAc", "CH3COOH", 60.021129, 0);
  public static final IonModification ACN =
      new IonModification(IonModificationType.CLUSTER, "ACN", "CH3CN", 41.026549, 0);
  public static final IonModification O =
      new IonModification(IonModificationType.CLUSTER, "O", "O", 15.99491462, 0);
  public static final IonModification ISOPROP =
      new IonModification(IonModificationType.CLUSTER, "IsoProp", "C3H8O", 60.058064, 0);
  // isotopes
  public static final IonModification C13 =
      new IonModification(IonModificationType.ISOTOPE, "(13C)", 1.003354838, 0);

  // default values
  public static final IonModification[] DEFAULT_VALUES_POSITIVE = {H_NEG, M_PLUS, H, NA, K, NH4,
      H2plus, CA, FE, MG, NA_H, NH4_H, K_H, Hneg_NA2, Hneg_CA, Hneg_FE, Hneg_MG};
  public static final IonModification[] DEFAULT_VALUES_NEGATIVE =
      {M_MINUS, H_NEG, NA_2H, NA, CL, BR, FA};
  // default modifications
  public static final IonModification[] DEFAULT_VALUES_MODIFICATIONS =
      {H2O, H2O_2, NH3, O, CO, CO2, C2H4, HFA, HAc, MEOH, ACN, ISOPROP};
  // isotopes
  public static final IonModification[] DEFAULT_VALUES_ISOTOPES = {C13};

  // charge
  protected IonModificationType type;
  protected String parsedName = "";
  protected int charge;
  private int maxModification;


  /**
   * 
   * @param name
   * @param massDifference mass difference (for single charge, Molecule) for example M to M+H+
   *        (1.0072) M to M+Na+ (22.9892)
   * @param charge negative for negatives
   * @param molecules count of molecules in a cluster
   */
  /**
   * copy of adduct
   * 
   * @param a
   */
  public IonModification(IonModification a) {
    this(a.type, a.getName(), a.getMolFormula(), a.getMass(), a.getCharge());
  }

  /**
   * new raw adduct
   * 
   * @param name
   * @param massDifference
   * @param charge
   * @param molecules
   */
  public IonModification(IonModificationType type, String name, String molFormula,
      double massDifference, int charge) {
    this(type, name, molFormula, massDifference, charge, -1);
  }

  /**
   * 
   * @param type
   * @param name
   * @param molFormula
   * @param massDifference
   * @param charge
   * @param maxModification -1 if no limit
   */
  public IonModification(IonModificationType type, String name, String molFormula,
      double massDifference, int charge, int maxModification) {
    super(name, molFormula, massDifference);
    this.name = name;
    this.mass = massDifference;
    this.charge = charge;
    this.molFormula = molFormula;
    this.maxModification = maxModification;
    parsedName = parseName();
    this.type = type;
  }

  /**
   * new raw adduct
   * 
   * @param name
   * @param massDifference
   * @param charge
   */
  public IonModification(IonModificationType type, String name, double massDifference, int charge) {
    this(type, name, massDifference, charge, -1);
  }

  public IonModification(IonModificationType type, String name, double massDifference, int charge,
      int maxModification) {
    this(type, name, "", massDifference, charge, maxModification);
  }

  public IonModification() {
    this(IonModificationType.UNKNOWN, "", 0, 0);
  }

  public IonModificationType getType() {
    return type;
  }

  /**
   * Specifies whether this object limits further modification
   * 
   * @return
   */
  public boolean hasModificationLimit() {
    return maxModification != -1;
  }

  public int getModificationLimit() {
    return maxModification;
  }

  /**
   * 
   * @return array of names
   */
  public String[] getRawNames() {
    return new String[] {getName()};
  }

  /**
   * 
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
   * @param a
   * @return
   */
  public boolean nameEquals(IonModification a) {
    return parsedName.equals(a.parsedName);
  }

  @Override
  public String toString() {
    return toString(true);
  }

  public String toString(boolean showMass) {
    int absCharge = Math.abs(charge);
    String z = absCharge > 1 ? absCharge + "" : "";
    z += (charge < 0 ? "-" : "+");
    if (charge == 0)
      z = "";
    // molecules
    if (showMass)
      return MessageFormat.format("[M{0}]{1} ({2})", parsedName, z,
          MZmineCore.getConfiguration().getMZFormat().format(getMass()));
    else
      return MessageFormat.format("[M{0}]{1}", parsedName, z);
  }

  public String getMassDiffString() {
    return MZmineCore.getConfiguration().getMZFormat().format(mass) + " m/z";
  }

  /**
   * Checks mass diff, charge and mol equality
   * 
   * @param adduct
   * @return
   */
  public boolean sameMathDifference(IonModification adduct) {
    return sameMassDifference(adduct) && charge == adduct.charge;
  }

  /**
   * Checks mass diff
   * 
   * @param adduct
   * @return
   */
  public boolean sameMassDifference(IonModification adduct) {
    return Double.compare(mass, adduct.mass) == 0;
  }

  public int getAbsCharge() {
    return Math.abs(charge);
  }

  public @Nonnull IonModification[] getAdducts() {
    return new IonModification[] {this};
  }

  public int getNumberOfAdducts() {
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
      if (i == 0)
        i = Double.compare(getCharge(), a.getCharge());
    }
    return i;
  }

  /**
   * ((mz * charge) - deltaMass) / numberOfMolecules
   * 
   * @param mz
   * @return
   */
  public double getMass(double mz) {
    return ((mz * this.getAbsCharge()) - this.getMass());
  }


  /**
   * neutral mass of M to mz of yM+X]charge
   * 
   * (mass*mol + deltaMass) /charge
   * 
   * @param mz
   * @return
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
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!obj.getClass().equals(getClass()))
      return false;
    if (!(obj instanceof IonModification))
      return false;
    IonModification other = (IonModification) obj;
    if (charge != other.charge)
      return false;
    if (parsedName == null) {
      if (other.parsedName != null)
        return false;
    } else if (!parsedName.equals(other.parsedName))
      return false;

    if (!Objects.equals(mass, other.getMass()))
      return false;
    return true;
  }

  /**
   * Creates the opposite modification: -H2O --> +H2O
   * 
   * @return
   */
  public IonModification createOpposite() {
    return new IonModification(getType(), name, molFormula, -mass, charge, maxModification);
  }


  /**
   * 
   * @param b
   * @return true if no adduct is a duplicate
   */
  public boolean uniqueAdductsTo(IonModification adduct) {
    IonModification[] a = getAdducts();
    IonModification[] b = adduct.getAdducts();
    return Arrays.stream(a).noneMatch(adda -> Arrays.stream(b).anyMatch(addb -> adda.equals(addb)));
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
   * @return
   */
  public static IonModification getUndefinedforCharge(int charge) {
    double mass = IonModification.M_PLUS.getMass() * charge;
    return new IonModification(IonModificationType.UNDEFINED_ADDUCT, "?", mass, charge);
  }

  /**
   * All sub types of this need to be in parameter b
   * 
   * @param b
   * @return
   */
  public boolean isSubsetOf(IonModification b) {
    if (b instanceof CombinedIonModification) {
      // ion modifications all need to be in the mod array of this
      IonModification[] full = b.getAdducts();
      IonModification[] subset = this.getAdducts();
      boolean[] used = new boolean[full.length];

      for (int i = 0; i < subset.length; i++) {
        boolean found = false;
        for (int tm = 0; tm < used.length && !found; tm++) {
          if (!used[tm] && full[tm].equals(subset[i])) {
            used[tm] = true;
            found = true;
          }
        }
        if (!found)
          return false;
      }
      return true;
    } else
      return false;
  }

  /**
   * Removes all sub types of parameter from this type
   * 
   * @param type
   * @return
   */
  public @Nullable IonModification remove(IonModification type) {
    if (this.isSubsetOf(type) || this.equals(type))
      return null;
    else
      return this;
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
   * @return
   */
  public int getAdductsCount() {
    return 1;
  }

}
