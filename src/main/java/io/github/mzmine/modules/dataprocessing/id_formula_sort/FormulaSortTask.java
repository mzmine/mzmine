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
package io.github.mzmine.modules.dataprocessing.id_formula_sort;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;

public class FormulaSortTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private PeakList peakList;
  private String message;
  private int totalRows;
  private int finishedRows = 0;
  private Double weightIsotopeScore;
  private Double ppmMaxWeight;
  private Double weightMSMSscore;

  /**
   *
   * @param parameters
   * @param peakList
   * @param peakListRow
   * @param peak
   */
  public FormulaSortTask(ParameterSet parameters) {
    weightIsotopeScore =
        parameters.getParameter(FormulaSortParameters.ISOTOPE_SCORE_WEIGHT).getValue();
    ppmMaxWeight = parameters.getParameter(FormulaSortParameters.MAX_PPM_WEIGHT).getValue();
    weightMSMSscore = parameters.getParameter(FormulaSortParameters.MSMS_SCORE_WEIGHT).getValue();
  }

  public FormulaSortTask(PeakList peakList, ParameterSet parameters) {
    this(parameters);
    this.peakList = peakList;
    message = "Sorting formula lists of peak list " + peakList.getName();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0.0;
    return finishedRows / (double) totalRows;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return message;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    for (PeakListRow row : peakList.getRows()) {
      // all formulas
      List<MolecularFormulaIdentity> formulas = Arrays.stream(row.getPeakIdentities())
          .filter(pi -> pi instanceof MolecularFormulaIdentity)
          .map(pi -> (MolecularFormulaIdentity) pi).collect(Collectors.toList());
      if (formulas.isEmpty())
        continue;

      sort(formulas);
      // replace
      formulas.forEach(f -> {
        row.removePeakIdentity(f);
      });
      formulas.forEach(f -> {
        row.addPeakIdentity(f, false);
      });
      row.setPreferredPeakIdentity(formulas.get(0));

      finishedRows++;
    }

    logger.finest("Finished formula search for all networks");
    setStatus(TaskStatus.FINISHED);
  }

  public void sort(List<MolecularFormulaIdentity> list) {
    FormulaUtils.sortFormulaList(list, ppmMaxWeight, weightIsotopeScore, weightMSMSscore);
  }

}
