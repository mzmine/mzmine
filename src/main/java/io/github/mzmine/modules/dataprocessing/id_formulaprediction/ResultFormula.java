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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_formulaprediction;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import java.util.Map;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class ResultFormula extends MolecularFormulaIdentity {

  private Double isotopeScore, msmsScore;
  private IsotopePattern predictedIsotopePattern;
  private Map<Double, String> msmsAnnotation;

  protected ResultFormula(ResultFormula f) {
    this(f.cdkFormula, f.predictedIsotopePattern, f.getIsotopeScore(), f.getMSMSScore(), f.getMSMSannotation(),
        f.getSearchedNeutralMass());
  }
  public ResultFormula(IMolecularFormula cdkFormula, IsotopePattern predictedIsotopePattern,
      Double isotopeScore, Double msmsScore,
      Map<Double, String> msmsAnnotation, double searchedNeutralMass) {
    super(cdkFormula, searchedNeutralMass);
    this.predictedIsotopePattern = predictedIsotopePattern;
    this.isotopeScore = isotopeScore;
    this.msmsScore = msmsScore;
    this.msmsAnnotation = msmsAnnotation;
    this.searchedNeutralMass = searchedNeutralMass;
  }

  public Map<Double, String> getMSMSannotation() {
    return msmsAnnotation;
  }

  public IsotopePattern getPredictedIsotopes() {
    return predictedIsotopePattern;
  }

  public Double getIsotopeScore() {
    return isotopeScore;
  }

  public Double getMSMSScore() {
    return msmsScore;
  }

  @Override
  public double getScore(double neutralMass, double ppmMax, double fIsotopeScore,
      double fMSMSscore) {
    double ppmScore = super.getPPMScore(neutralMass, ppmMax);
    double totalScore = ppmScore;
    double div = 1;
    Double isoScore = getIsotopeScore();
    if (isoScore != null) {
      totalScore += isoScore * fIsotopeScore;
      div += fIsotopeScore;
    }
    Double msmsScore = getMSMSScore();
    if (msmsScore != null) {
      totalScore += msmsScore * fMSMSscore;
      div += fMSMSscore;
    }

    return totalScore / div;
  }

}
