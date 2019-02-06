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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.identities.NeutralMolecule;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.FormulaUtils;

public class IonType extends NeutralMolecule implements Comparable<IonType> {

  protected final @Nonnull IonModification adduct;
  protected final @Nullable IonModification mod;
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
    super("", 0);
    this.adduct = adduct;
    this.mod = mod;
    this.charge = adduct.charge;
    this.molecules = molecules;
    name = parseName();
    //
    mass = adduct.getMass();
    if (mod != null)
      mass += mod.getMass();
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
   * for adding modifications
   * 
   * @param a
   * @param mod
   * @return
   */
  public IonType createModified(final @Nonnull IonModification... newMod) {
    List<IonModification> allMod = new ArrayList<>();
    for (IonModification m : newMod)
      allMod.add(m);
    if (this.mod != null)
      for (IonModification m : this.mod.getAdducts())
        allMod.add(m);

    IonModification nm =
        new CombinedIonModification(allMod.toArray(new IonModification[allMod.size()]));
    return new IonType(this.molecules, this.adduct, nm);
  }

  /**
   * All modifications
   * 
   * @return
   */
  public IonModification getModification() {
    return mod;
  }

  @Override
  public String parseName() {
    StringBuilder sb = new StringBuilder();
    // modification first
    if (mod != null)
      sb.append(mod.getParsedName());
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
   * checks all sub/raw ESIAdductTypes
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
    if (this.mod == a.mod || (mod == null && a.mod == null))
      return true;
    if (this.mod == null ^ a.mod == null)
      return false;

    return mod.equals(a.mod);
  }

  /**
   * checks if at least one modification is shared
   * 
   * @param a
   * @return
   */
  public boolean hasModificationOverlap(IonType ion) {
    if (!hasMods() || !ion.hasMods())
      return false;
    IonModification[] a = mod.getAdducts();
    IonModification[] b = ion.mod.getAdducts();
    if (a == b)
      return true;

    for (final IonModification aa : a)
      if (Arrays.stream(b).anyMatch(ab -> aa.equals(ab)))
        return true;
    return false;
  }

  /**
   * checks if at least one adduct is shared
   * 
   * @param a
   * @return
   */
  public boolean hasAdductOverlap(IonType ion) {
    IonModification[] a = adduct.getAdducts();
    IonModification[] b = ion.adduct.getAdducts();
    if (a == b)
      return true;

    for (final IonModification aa : a)
      if (Arrays.stream(b).anyMatch(ab -> aa.equals(ab)))
        return true;
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
    if (charge == 0)
      z = "";
    // molecules
    String mol = molecules > 1 ? String.valueOf(molecules) : "";
    if (showMass)
      return MessageFormat.format("[{0}M{1}]{2} ({3})", mol, name, z,
          MZmineCore.getConfiguration().getMZFormat().format(getMassDifference()));
    else
      return MessageFormat.format("[{0}M{1}]{2}", mol, name, z);
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

  public int getAbsCharge() {
    return Math.abs(charge);
  }

  public IonModification getAdduct() {
    return adduct;
  }

  /**
   * Is modified
   * 
   * @return
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
      if (i == 0)
        i = Integer.compare(getMolecules(), a.getMolecules());
    }
    return i;
  }

  /**
   * is a modification of parameter adduct? only if all adducts are the same, mass difference must
   * be different ONLY if this is a mod of parameter adduct
   * 
   * @param adduct
   * @return
   */
  public boolean isModificationOf(IonType ion) {
    if (!hasMods() || !(ion.getModCount() < getModCount() && mass != ion.mass
        && adduct.equals(ion.adduct) && molecules == ion.molecules && charge == ion.charge))
      return false;
    else if (!ion.hasMods())
      return true;
    else {
      return ion.mod.isSubsetOf(mod);
    }
  }

  /**
   * subtracts the mods of the parameter adduct from this adduct
   * 
   * @param adduct
   * @return
   */
  public @Nonnull IonType subtractMods(IonType ion) {
    // return an identity with only the modifications
    if (hasMods() && ion.hasMods()) {
      IonModification na = this.mod.remove(ion.mod);
      // na can be null
      return new IonType(this.molecules, this.adduct, na);
    } else
      return this;
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
   * 
   * @return count of modification
   */
  public int getModCount() {
    return mod == null ? 0 : mod.getAdductsCount();
  }

  /**
   * ((mz * charge) - deltaMass) / numberOfMolecules
   * 
   * @param mz
   * @return
   */
  public double getMass(double mz) {
    return ((mz * this.getAbsCharge()) - this.getMassDifference()) / this.getMolecules();
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
    return (neutralmass * getMolecules() + getMassDifference()) / getAbsCharge();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null || !obj.getClass().equals(this.getClass()) || !(obj instanceof IonType))
      return false;
    if (!super.equals(obj))
      return false;

    final IonType a = (IonType) obj;
    return (sameMathDifference(a) && adductsEqual(a) && modsEqual(a));
  }

  @Override
  public int hashCode() {
    return Objects.hash(adduct, mod == null ? "" : mod, charge, molecules, mass, name);
  }

  /**
   * 
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
    if (getCharge() == 0)
      return PolarityType.NEUTRAL;
    if (getCharge() > 0)
      return PolarityType.POSITIVE;
    if (getCharge() < 0)
      return PolarityType.NEGATIVE;
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
    for (int i = 2; i <= molecules; i++)
      FormulaUtils.addFormula(result, formula);

    // add
    Arrays.stream(adduct.getAdducts()).filter(m -> m.getMass() >= 0 && m.getCDKFormula() != null)
        .forEach(m -> FormulaUtils.addFormula(result, m.getCDKFormula()));
    if (mod != null)
      Arrays.stream(mod.getAdducts()).filter(m -> m.getMass() >= 0 && m.getCDKFormula() != null)
          .forEach(m -> FormulaUtils.addFormula(result, m.getCDKFormula()));

    // subtract
    Arrays.stream(adduct.getAdducts()).filter(m -> m.getMass() < 0 && m.getCDKFormula() != null)
        .forEach(m -> FormulaUtils.subtractFormula(result, m.getCDKFormula()));
    if (mod != null)
      Arrays.stream(mod.getAdducts()).filter(m -> m.getMass() < 0 && m.getCDKFormula() != null)
          .forEach(m -> FormulaUtils.subtractFormula(result, m.getCDKFormula()));
    return result;
  }
}
