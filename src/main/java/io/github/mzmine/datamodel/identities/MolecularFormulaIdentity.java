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

package io.github.mzmine.datamodel.identities;

import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.ParsingUtils;
import java.util.Objects;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class MolecularFormulaIdentity {

  private static final Logger logger = Logger.getLogger(MolecularFormulaIdentity.class.getName());

  @NotNull
  protected final IMolecularFormula cdkFormula;
  protected final Float rdbe;
  protected final double searchedNeutralMass;

  public static final String XML_ELEMENT = "molecular_formula_identity";
  public static final String SEARCHED_NEUTRAL_MASS_ATTR = "searched_neutral_mass";

  public MolecularFormulaIdentity(IMolecularFormula cdkFormula, double searchedNeutralMass) {
    this.cdkFormula = cdkFormula;
    Double rdbe = RDBERestrictionChecker.calculateRDBE(cdkFormula);
    this.rdbe = rdbe == null ? null : rdbe.floatValue();
    this.searchedNeutralMass = searchedNeutralMass;
  }

  public MolecularFormulaIdentity(String formula, double searchedNeutralMass) {
    this(FormulaUtils.createMajorIsotopeMolFormula(formula), searchedNeutralMass);
  }

  public String getFormulaAsString() {
    return MolecularFormulaManipulator.getString(cdkFormula);
  }

  public String getFormulaAsHTML() {
    return MolecularFormulaManipulator.getHTML(cdkFormula);
  }

  public IMolecularFormula getFormulaAsObject() {
    return cdkFormula;
  }

  public double getExactMass() {
    return MolecularFormulaManipulator.getTotalExactMass(cdkFormula);
  }

  public float getPpmDiff(double neutralMass) {
    double exact = getExactMass();
    return (float) ((neutralMass - exact) / exact * 1E6);
  }

  @Override
  public String toString() {
    return getFormulaAsString();
  }

  /**
   * The initially searched neutral mass
   *
   * @return the neutral mass
   */
  public double getSearchedNeutralMass() {
    return searchedNeutralMass;
  }

  /**
   * Merged score
   *
   * @param ppmMax
   * @return
   */
  public float getScore(float ppmMax) {
    return getScore(ppmMax, 1, 1);
  }

  /**
   * Merged score with weights. Uses the initial set neutral mass
   *
   * @param ppmMax        weight for ppm distance
   * @param fIsotopeScore
   * @param fMSMSscore
   * @return
   */
  public float getScore(float ppmMax, float fIsotopeScore, float fMSMSscore) {
    return this.getScore(searchedNeutralMass, ppmMax, fIsotopeScore, fMSMSscore);
  }

  /**
   * Merged score with weights. {@link ResultFormula}
   *
   * @param neutralMass   overrides the initally set (searched) neutral mass
   * @param ppmMax        weight for ppm distance
   * @param fIsotopeScore
   * @param fMSMSscore
   * @return
   */
  public float getScore(double neutralMass, float ppmMax, float fIsotopeScore, float fMSMSscore) {
    return getPPMScore(neutralMass, ppmMax);
  }

  /**
   * Score for ppm distance
   *
   * @param ppmMax
   * @return
   */
  public float getPPMScore(double neutralMass, float ppmMax) {
    if (ppmMax <= 0) {
      ppmMax = 50f;
    }
    return (float) (ppmMax - Math.abs(getPpmDiff(neutralMass))) / ppmMax;
  }

  /**
   * Only checks molecular formula as with toString method
   *
   * @param f
   * @return
   */
  public boolean equalFormula(MolecularFormulaIdentity f) {
    return this.toString().equals(f.toString());
  }

  public Float getRDBE() {
    return rdbe;
  }

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(SEARCHED_NEUTRAL_MASS_ATTR,
        ParsingUtils.numberToString(searchedNeutralMass));
    writer.writeCharacters(getFormulaAsString());
    writer.writeEndElement();
  }

  public static MolecularFormulaIdentity loadFromXML(XMLStreamReader reader)
      throws XMLStreamException {
    if (!reader.isStartElement() || !reader.getLocalName().equals(XML_ELEMENT)) {
      throw new IllegalStateException(
          "Unexpected xml element for MolecularFormulaIdentity: " + reader.getLocalName());
    }
    Double mass = ParsingUtils.stringToDouble(
        reader.getAttributeValue(null, SEARCHED_NEUTRAL_MASS_ATTR));
    final String formula = reader.getElementText();
    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    var molFormula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(formula, builder);


    return new MolecularFormulaIdentity(molFormula, Objects.requireNonNull(mass));
  }
}
