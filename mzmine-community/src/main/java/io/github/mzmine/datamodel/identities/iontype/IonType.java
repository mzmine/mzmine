/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.identities.NeutralMolecule;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.ParsingUtils;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
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

  public static final String XML_ELEMENT = "iontype";
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

    return mod != null ? new IonType(molecules, adduct, mod) : new IonType(molecules, adduct);
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement("iontype");
    writer.writeAttribute("molecules", String.valueOf(molecules));
    writer.writeAttribute("charge", String.valueOf(charge));

    writer.writeStartElement("adduct");
    adduct.saveToXML(writer);
    writer.writeEndElement();

    if (mod != null) {
      writer.writeStartElement("modification");
      mod.saveToXML(writer);
      writer.writeEndElement();
    }

    writer.writeEndElement();
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
      // do not check the ? unknown adduct as this does not count as an adduct overlap
      if (aa.getType() == IonModificationType.UNDEFINED_ADDUCT) {
        continue;
      }
      if (Arrays.stream(b).anyMatch(ab -> aa.equals(ab))) {
        return true;
      }
    }
    return false;
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
      Collections.addAll(allMods, this.mod.getModifications());
    }

    IonModification combinedIonModification = CombinedIonModification.create(allMods);
    return new IonType(this.molecules, this.adduct, combinedIonModification);
  }

  public String toString(boolean showMass) {
    int absCharge = Math.abs(charge);
    String z = charge < 0 ? "-" : "+";
    if (absCharge > 1) {
      z += absCharge;
    }
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
   * @return count of adducts
   */
  public int getAdductCount() {
    return adduct == null ? 0 : adduct.getModCount();
  }

  /**
   * @return sum of modification and adducts, molecules, charge
   */
  public int getTotalPartsCount() {
    return molecules + charge + getModCount() + getAdductCount();
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
  public String toString() {
    return toString(false);
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
   * @param formula the formula. The formula is cloned and the parameter is not altered.
   */
  public IMolecularFormula addToFormula(IMolecularFormula formula)
      throws CloneNotSupportedException {
    return addToFormula(formula, true);
  }

  /**
   * Is adding or removing all sub adducts / modifications from the molecular formula.
   *
   * @param formula the formula. The formula is cloned and the parameter is not altered.
   * @param ionize  if the formula shall be ionised.
   * @return The resulting molecule may be neutral if the charge of the molecule and the charge of
   * this adduct are opposite.
   */
  public IMolecularFormula addToFormula(IMolecularFormula formula, boolean ionize)
      throws CloneNotSupportedException {
    final int formulaCharge = Objects.requireNonNullElse(formula.getCharge(), 0);

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

    if (ionize) {
      final int ionTypeCharge = getCharge();
      result.setCharge(formulaCharge + ionTypeCharge);
    }

    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null || !obj.getClass().equals(this.getClass())
        || !(obj instanceof final IonType a)) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }

    return (sameMathDifference(a) && adductsEqual(a) && modsEqual(a));
  }
}
