/*
 * Copyright 2006-2020 The MZmine Development Team
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
 */

package io.github.mzmine.datamodel.identities;

import java.util.Map;
import javax.annotation.Nonnull;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.impl.SimplePeakIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.util.FormulaUtils;

public class MolecularFormulaIdentity extends SimplePeakIdentity {

  private final @Nonnull IMolecularFormula cdkFormula;
  private Double rdbe;
  private Double isotopeScore;
  private Double msmsScore;
  private double neutralMass;

  public MolecularFormulaIdentity(IMolecularFormula cdkFormula, double neutralMass,
      Double isotopeScore, Double msmsScore) {
    super("");
    this.cdkFormula = cdkFormula;
    this.neutralMass = neutralMass;
    this.isotopeScore = isotopeScore;
    this.msmsScore = msmsScore;
    rdbe = RDBERestrictionChecker.calculateRDBE(cdkFormula);
    setPropertyValue(PROPERTY_NAME, getFormulaAsString());
    setPropertyValue(PROPERTY_FORMULA, getFormulaAsString());
  }

  public MolecularFormulaIdentity(String formula, double neutralMass, Double isotopeScore,
      Double msmsScore) {
    this(FormulaUtils.createMajorIsotopeMolFormula(formula), neutralMass, isotopeScore, msmsScore);
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

  public double getPpmDiff(double neutralMass) {
    double exact = getExactMass();
    return (neutralMass - exact) / exact * 1E6;
  }

  @Override
  public String toString() {
    return getFormulaAsString();
  }

  /**
   * Merged score
   * 
   * @param neutralMass
   * @param ppmMax
   * @return
   */
  public double getScore(double ppmMax) {
    return getScore(ppmMax, 0, 0);
  }

  /**
   * Merged score with weights
   * 
   * @param ppmMax weight for ppm distance
   * @param fIsotopeScore
   * @param fMSMSscore
   * @return
   */
  public double getScore(double ppmMax, double fIsotopeScore, double fMSMSscore) {
    return getPPMScore(neutralMass, ppmMax) + fIsotopeScore * getIsotopeScore()
        + fMSMSscore * getMSMSScore();
  }

  /**
   * Score for ppm distance
   * 
   * @param neutralMass
   * @param ppmMax
   * @return
   */
  public double getPPMScore(double neutralMass, double ppmMax) {
    if (ppmMax <= 0)
      ppmMax = 50;
    return (ppmMax - Math.abs(getPpmDiff(neutralMass))) / ppmMax;
  }

  /**
   * 
   * @return The isotope score or null
   */
  public Double getIsotopeScore() {
    return isotopeScore == null ? 0 : isotopeScore;
  }

  /**
   * 
   * @return the msms score or null
   */
  public Double getMSMSScore() {
    return msmsScore == null ? 0 : msmsScore;
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

  public Double getRDBE() {
    return rdbe;
  }

  /**
   * MSMS annotations of MS/MS scans (sub molecular formulas)
   * 
   * @return
   */
  public Map<DataPoint, String> getMSMSannotation() {
    return null;
  }

  /**
   * The predicted isotopes pattern
   * 
   * @return
   */
  public IsotopePattern getPredictedIsotopes() {
    return null;
  }
}
