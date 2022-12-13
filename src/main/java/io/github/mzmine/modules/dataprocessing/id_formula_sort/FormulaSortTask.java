/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
package io.github.mzmine.modules.dataprocessing.id_formula_sort;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class FormulaSortTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final float weightIsotopeScore;
  private final float ppmMaxWeight;
  private final float weightMSMSscore;
  private final ParameterSet parameterSet;
  private ModularFeatureList featureList;
  private String message;
  private int totalRows;
  private int finishedRows = 0;

  /**
   * @param parameters
   */
  public FormulaSortTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    weightIsotopeScore =
        parameters.getParameter(FormulaSortParameters.ISOTOPE_SCORE_WEIGHT).getValue().floatValue();
    ppmMaxWeight = parameters.getParameter(FormulaSortParameters.MAX_PPM_WEIGHT).getValue()
        .floatValue();
    weightMSMSscore = parameters.getParameter(FormulaSortParameters.MSMS_SCORE_WEIGHT).getValue()
        .floatValue();
    parameterSet = parameters;
  }

  public FormulaSortTask(ModularFeatureList featureList, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    this(parameters, moduleCallDate);
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
        FormulaSortModule.class, parameterSet, getModuleCallDate()));
    setStatus(TaskStatus.FINISHED);
  }

  public void sort(List<ResultFormula> list) {
    FormulaUtils.sortFormulaList(list, ppmMaxWeight, weightIsotopeScore, weightMSMSscore);
  }

  public void sort(List<ResultFormula> list, double neutralMass) {
    FormulaUtils
        .sortFormulaList(list, neutralMass, ppmMaxWeight, weightIsotopeScore, weightMSMSscore);
  }

}
