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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.createavgformulas;

import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import java.util.ArrayList;
import java.util.List;

public class AverageResultFormula extends ResultFormula {

  private final List<ResultFormula> formulas = new ArrayList<>();

  public AverageResultFormula(ResultFormula f) {
    super(f);
    formulas.add(f);
  }

  public List<ResultFormula> getFormulas() {
    return formulas;
  }

  public boolean isMatching(MolecularFormulaIdentity f) {
    return f.equalFormula(formulas.get(0));
  }

  public boolean addFormula(ResultFormula f) {
    if (isMatching(f)) {
      formulas.add(f);
      return true;
    }
    return false;
  }

  public void removeFormula(ResultFormula f) {
    formulas.remove(f);
  }

  @Override
  public Float getIsotopeScore() {
    float mean = 0;
    int c = 0;
    for (var f : formulas) {
      Float iso = f.getIsotopeScore();
      if (iso != null) {
        mean += iso;
        c++;
      }
    }
    return c == 0 ? null : mean / c;
  }

  @Override
  public Float getMSMSScore() {
    float mean = 0;
    int c = 0;
    for (var f : formulas) {
      Float msmsScore = f.getMSMSScore();
      if (msmsScore != null) {
        mean += msmsScore;
        c++;
      }
    }
    return c == 0 ? null : mean / c;
  }

  @Override
  public float getPPMScore(double neutralMass, float ppmMax) {
    float mean = 0;
    int c = 0;
    for (var f : formulas) {
      mean += f.getPPMScore(neutralMass, ppmMax);
      c++;
    }
    return mean / c;
  }

}
