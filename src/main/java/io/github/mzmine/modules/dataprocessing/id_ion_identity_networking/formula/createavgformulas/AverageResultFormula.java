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
 */

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.createavgformulas;

import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import java.util.ArrayList;
import java.util.List;

public class AverageResultFormula extends ResultFormula {

  private List<ResultFormula> formulas = new ArrayList<>();

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
  public Double getIsotopeScore() {
    double avg = formulas.stream().filter(f -> f.getIsotopeScore() != null)
        .mapToDouble(ResultFormula::getIsotopeScore).average().orElse(-1);
    return avg == -1 ? null : avg;
  }

  @Override
  public Double getMSMSScore() {
    double avg = formulas.stream().filter(f -> f.getMSMSScore() != null)
        .mapToDouble(ResultFormula::getMSMSScore).average().orElse(-1);
    return avg == -1 ? null : avg;
  }

  @Override
  public double getPPMScore(double neutralMass, double ppmMax) {
    return formulas.stream().mapToDouble(f -> f.getPPMScore(neutralMass, ppmMax)).average()
        .orElse(0);
  }

}
