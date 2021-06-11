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
package io.github.mzmine.modules.dataprocessing.id_formula_sort;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FormulaSortTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private ModularFeatureList featureList;
  private String message;
  private int totalRows;
  private int finishedRows = 0;
  private Double weightIsotopeScore;
  private Double ppmMaxWeight;
  private Double weightMSMSscore;
  private final ParameterSet parameterSet;

  /**
   * @param parameters
   */
  public FormulaSortTask(ParameterSet parameters) {
    super(null); // no new data stored -> null
    weightIsotopeScore =
        parameters.getParameter(FormulaSortParameters.ISOTOPE_SCORE_WEIGHT).getValue();
    ppmMaxWeight = parameters.getParameter(FormulaSortParameters.MAX_PPM_WEIGHT).getValue();
    weightMSMSscore = parameters.getParameter(FormulaSortParameters.MSMS_SCORE_WEIGHT).getValue();
    parameterSet = parameters;
  }

  public FormulaSortTask(ModularFeatureList featureList, ParameterSet parameters) {
    this(parameters);
    this.featureList = featureList;
    message = "Sorting formula lists of feature list " + featureList.getName();
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0.0;
    }
    return finishedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return message;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    for (FeatureListRow row : featureList.getRows()) {
      // all formulas
      List<ResultFormula> formulas = row.getFormulas();
      if (formulas == null || formulas.isEmpty()) {
        continue;
      }

      sort(formulas);

      // replace
      row.setFormulas(formulas);

      finishedRows++;
    }

    logger.finest("Finished formula search for all networks");
    featureList.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
        FormulaSortModule.class, parameterSet));
    setStatus(TaskStatus.FINISHED);
  }

  public void sort(List<ResultFormula> list) {
    FormulaUtils.sortFormulaList(list, ppmMaxWeight, weightIsotopeScore, weightMSMSscore);
  }
  public void sort(List<ResultFormula> list, double neutralMass) {
    FormulaUtils.sortFormulaList(list, neutralMass, ppmMaxWeight, weightIsotopeScore, weightMSMSscore);
  }

}
