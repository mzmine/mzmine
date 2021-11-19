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

package io.github.mzmine.modules.dataprocessing.id_formulaprediction;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import java.util.Map;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class ResultFormula extends MolecularFormulaIdentity {

  private final Float isotopeScore;
  private final Float msmsScore;
  private final IsotopePattern predictedIsotopePattern;
  private Map<Double, String> msmsAnnotation;

  protected ResultFormula(ResultFormula f) {
    this(f.cdkFormula, f.predictedIsotopePattern, f.getIsotopeScore(), f.getMSMSScore(),
        f.getMSMSannotation(),
        f.getSearchedNeutralMass());
  }

  public ResultFormula(IMolecularFormula cdkFormula, IsotopePattern predictedIsotopePattern,
      Float isotopeScore, Float msmsScore,
      Map<Double, String> msmsAnnotation, double searchedNeutralMass) {
    super(cdkFormula, searchedNeutralMass);
    this.predictedIsotopePattern = predictedIsotopePattern;
    this.isotopeScore = isotopeScore;
    this.msmsScore = msmsScore;
    this.msmsAnnotation = msmsAnnotation;
  }

  public Map<Double, String> getMSMSannotation() {
    return msmsAnnotation;
  }

  public IsotopePattern getPredictedIsotopes() {
    return predictedIsotopePattern;
  }

  public Float getIsotopeScore() {
    return isotopeScore;
  }

  public Float getMSMSScore() {
    return msmsScore;
  }

  @Override
  public float getScore(double neutralMass, float ppmMax, float fIsotopeScore,
      float fMSMSscore) {
    float ppmScore = super.getPPMScore(neutralMass, ppmMax);
    float totalScore = ppmScore;
    float div = 1f;
    Float isoScore = getIsotopeScore();
    if (isoScore != null) {
      totalScore += isoScore * fIsotopeScore;
      div += fIsotopeScore;
    }
    Float msmsScore = getMSMSScore();
    if (msmsScore != null) {
      totalScore += msmsScore * fMSMSscore;
      div += fMSMSscore;
    }

    return totalScore / div;
  }

  public float getPpmDiff() {
    return getPpmDiff(searchedNeutralMass);
  }

  public double getAbsoluteMzDiff() {
    return searchedNeutralMass - getExactMass();
  }
}
