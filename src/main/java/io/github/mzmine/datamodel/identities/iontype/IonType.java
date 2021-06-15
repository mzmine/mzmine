/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.identities.iontype;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.identities.NeutralMolecule;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.FormulaUtils;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * The IonType defines the adduct, neutral in source modifications (e.g., -H2O / +ACN), molecules
 * multiplier for multimiers (e.g, [2M+H]+), and the charge
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class IonType extends NeutralMolecule implements Comparable<IonType> {

  @NotNull
  protected final IonModification adduct;
  @Nullable
  protected final IonModification mod;
  protected final int molecules;
  protected final int charge;

  public IonType(IonModification adduct) {
    this(adduct, null);
  }

  public IonType(IonModification adduct, IonModification mod) {
    this(1, adduct, mod);
  }

  public IonType(int molecules, IonModification adduct) {
    this(molecules, adduct, null);
  }

  public IonType(int molecules, IonModification adduct, IonModification mod) {
    super("", mod != null ? adduct.getMass() + mod.getMass() : adduct.getMass());
    this.adduct = adduct;
    this.mod = mod;
    this.charge = adduct.charge;
    this.molecules = molecules;
    name = parseName();
  }

  /**
   * New ion type with different molecules count
   *
   * @param molecules
   * @param ion
   */
  public IonType(int molecules, IonType ion) {
    this(molecules, ion.adduct, ion.mod);
  }

  /**
   * Create a new modified ion type by adding all newMods
   *
   * @return modified IonType
   */
  public IonType createModified(final @NotNull IonModification... newMod) {
    List<IonModification> allMods = new ArrayList<>();
    Collections.addAll(allMods, newMod);

    if (this.mod != null) {
      for (IonModification m : this.mod.getModifications()) {
        allMods.add(m);
      }
    }

    IonModification combinedIonModification =
        CombinedIonModification.create(allMods);
    return new IonType(this.molecules, this.adduct, combinedIonModification);
  }

  /**
   * All modifications
   *
   * @return all modifications
   */
  public IonModification getModification() {
    return mod;
  }

  @Override
  public String parseName() {
    StringBuilder sb = new StringBuilder();
    // modification first
    if (mod != null) {
      sb.append(mod.getParsedName());
    }
    // adducts
    sb.append(adduct.getParsedName());

    return sb.toString();
  }

  public double getMassDifference() {
    return mass;
  }

  public int getCharge() {
    return charge;
  }

  public int getMolecules() {
    return molecules;
  }

  /**
   * checks all sub/raw types
   *
   * @param a
   * @return
   */
  public boolean nameEquals(IonType a) {
    return name.equals(a.name);
  }

  /**
   * checks if all modification are equal
   *
   * @param a
   * @return
   */
  public boolean modsEqual(IonType a) {
    if (this.mod == a.mod || (mod == null && a.mod == null)) {
      return true;
    }
    if (this.mod == null ^ a.mod == null) {
      return false;
    }

    return mod.equals(a.mod);
  }

  /**
   * checks if at least one modification is shared
   *
   * @return true if at least one modification is shared
   */
  public boolean hasModificationOverlap(IonType ion) {
    if (!hasMods() || !ion.hasMods()) {
      return false;
    }
    IonModification[] a = mod.getModifications();
    IonModification[] b = ion.mod.getModifications();
    if (a == b) {
      return true;
    }

    for (final IonModification aa : a) {
      if (Arrays.stream(b).anyMatch(ab -> aa.equals(ab))) {
        return true;
      }
    }
    return false;
  }

  /**
   * checks if at least one adduct is shared
   *
   * @return true if at least one adduct type is shared
   */
  public boolean hasAdductOverlap(IonType ion) {
    IonModification[] a = adduct.getModifications();
    IonModification[] b = ion.adduct.getModifications();
    if (a == b) {
      return true;
    }

    for (final IonModification aa : a) {
      if (Arrays.stream(b).anyMatch(ab -> aa.equals(ab))) {
        return true;
      }
    }
    return false;
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
    String mol = molecules > 1 ? String.valueOf(molecules) : "";
    if (showMass) {
      return MessageFormat.format("[{0}M{1}]{2} ({3})", mol, name, z,
          MZmineCore.getConfiguration().getMZFormat().format(getMassDifference()));
    } else {
      return MessageFormat.format("[{0}M{1}]{2}", mol, name, z);
    }
  }

  public String getMassDiffString() {
    return "m/z " + MZmineCore.getConfiguration().getMZFormat().format(mass);
  }

  /**
   * Checks mass diff, charge and mol equality
   *
   * @return true if charge, mass difference, and molecules factor is the same
   */
  public boolean sameMathDifference(IonType adduct) {
    return sameMassDifference(adduct) && charge == adduct.charge && molecules == adduct.molecules;
  }

  /**
   * Checks mass diff
   *
   * @param adduct
   * @return
   */
  public boolean sameMassDifference(IonType adduct) {
    return Double.compare(mass, adduct.mass) == 0;
  }

  /**
   * @return the absolute charge
   */
  public int getAbsCharge() {
    return Math.abs(charge);
  }

  /**
   * @return The adduct part of this IonType
   */
  public IonModification getAdduct() {
    return adduct;
  }

  /**
   * @return true if ion source modifications are available
   */
  public boolean hasMods() {
    return mod != null;
  }

  /**
   * sorting
   */
  @Override
  public int compareTo(IonType a) {
    int i = this.getName().compareTo(a.getName());
    if (i == 0) {
      double md1 = getMassDifference();
      double md2 = a.getMassDifference();
      i = Double.compare(md1, md2);
      if (i == 0) {
        i = Integer.compare(getMolecules(), a.getMolecules());
      }
    }
    return i;
  }

  /**
   * is a modification of parent? only if all adducts are the same, mass difference must be
   * different ONLY if this is a mod of parent
   *
   * @param parent the potential parent ion
   * @return true if this is a modification of the parent argument (e.g., this=[M-H2O+H]+; parent=
   * [M+H]+)
   */
  public boolean isModificationOf(IonType parent) {
    if (!hasMods() || !(parent.getModCount() < getModCount() && mass != parent.mass
                        && adduct.equals(parent.adduct) && molecules == parent.molecules
                        && charge == parent.charge)) {
      return false;
    } else if (!parent.hasMods()) {
      return true;
    } else {
      return parent.mod.isSubsetOf(mod);
    }
  }

  /**
   * subtracts the mods of the argument ion from this ion
   *
   * @return
   */
  @NotNull
  public IonType subtractMods(IonType ion) {
    // return an identity with only the modifications
    if (hasMods() && ion.hasMods()) {
      IonModification na = this.mod.remove(ion.mod);
      // na can be null
      return new IonType(this.molecules, this.adduct, na);
    } else {
      return this;
    }
  }

  /**
   * Undefined adduct with 1 molecule and all modifications
   *
   * @return modifications only or null
   */
  public IonType getModifiedOnly() {
    return new IonType(1, IonModification.getUndefinedforCharge(this.charge), mod);
  }

  /**
   * @return count of modification
   */
  public int getModCount() {
    return mod == null ? 0 : mod.getModCount();
  }

  /**
   * ((mz * charge) - deltaMass) / numberOfMolecules
   *
   * @param mz mass to charge ratio
   * @return the neutral mass for this m/z calculated for this IonType
   */
  public double getMass(double mz) {
    return ((mz * this.getAbsCharge()) - this.getMassDifference()) / this.getMolecules();
  }


  /**
   * neutral mass of M to mz of yM+X]charge
   * <p>
   * (mass*mol + deltaMass) /charge
   *
   * @return the m/z for this neutral ionized by IonType
   */
  public double getMZ(double neutralmass) {
    return (neutralmass * getMolecules() + getMassDifference()) / getAbsCharge();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null || !obj.getClass().equals(this.getClass()) || !(obj instanceof IonType)) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }

    final IonType a = (IonType) obj;
    return (sameMathDifference(a) && adductsEqual(a) && modsEqual(a));
  }

  @Override
  public int hashCode() {
    return Objects.hash(adduct, mod == null ? "" : mod, charge, molecules, mass, name);
  }

  /**
   * @param b
   * @return true if no adduct is a duplicate
   */
  public boolean adductsEqual(IonType b) {
    return adduct.equals(b.adduct);
  }

  /**
   * Has modifications and the adduct type is undefined (this should be target to refinement)
   *
   * @return
   */
  public boolean isModifiedUndefinedAdduct() {
    return isUndefinedAdduct() && getModCount() > 0;
  }

  /**
   * Undefined adduct [M+?]c+
   *
   * @return
   */
  public boolean isUndefinedAdduct() {
    return adduct.getType().equals(IonModificationType.UNDEFINED_ADDUCT);
  }

  /**
   * Undefined adduct [M+?]c+ but not modified
   *
   * @return
   */
  public boolean isUndefinedAdductParent() {
    return adduct.getType().equals(IonModificationType.UNDEFINED_ADDUCT) && getModCount() == 0;
  }

  public PolarityType getPolarity() {
    if (getCharge() == 0) {
      return PolarityType.NEUTRAL;
    }
    if (getCharge() > 0) {
      return PolarityType.POSITIVE;
    }
    if (getCharge() < 0) {
      return PolarityType.NEGATIVE;
    }
    return PolarityType.UNKNOWN;
  }

  /**
   * Is adding or removing all sub adducts / modifications from the molecular formula
   *
   * @param formula
   * @return
   * @throws CloneNotSupportedException
   */
  public IMolecularFormula addToFormula(IMolecularFormula formula)
      throws CloneNotSupportedException {
    IMolecularFormula result = (IMolecularFormula) formula.clone();
    // add for n molecules the M formula
    for (int i = 2; i <= molecules; i++) {
      FormulaUtils.addFormula(result, formula);
    }

    // add
    Arrays.stream(adduct.getModifications())
        .filter(m -> m.getMass() >= 0 && m.getCDKFormula() != null)
        .forEach(m -> FormulaUtils.addFormula(result, m.getCDKFormula()));
    if (mod != null) {
      Arrays.stream(mod.getModifications())
          .filter(m -> m.getMass() >= 0 && m.getCDKFormula() != null)
          .forEach(m -> FormulaUtils.addFormula(result, m.getCDKFormula()));
    }

    // subtract
    Arrays.stream(adduct.getModifications())
        .filter(m -> m.getMass() < 0 && m.getCDKFormula() != null)
        .forEach(m -> FormulaUtils.subtractFormula(result, m.getCDKFormula()));
    if (mod != null) {
      Arrays.stream(mod.getModifications())
          .filter(m -> m.getMass() < 0 && m.getCDKFormula() != null)
          .forEach(m -> FormulaUtils.subtractFormula(result, m.getCDKFormula()));
    }
    return result;
  }
}
