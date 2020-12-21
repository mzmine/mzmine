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


import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.modules.dataprocessing.id_formula_sort.FormulaSortParameters;
import io.github.mzmine.modules.dataprocessing.id_formula_sort.FormulaSortTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class CreateAvgNetworkFormulasTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private FeatureList featureList;
  private String message;
  private int totalRows;
  private AtomicInteger finishedNets = new AtomicInteger(0);
  private boolean sortResults;
  private FormulaSortTask sorter;

  /**
   *
   * @param parameters
   */
  public CreateAvgNetworkFormulasTask() {
    sortResults = false;
    this.sorter = null;
    message = "Creation of average molecular formulas for MS annotation networks";
  }

  public CreateAvgNetworkFormulasTask(FormulaSortTask sorter) {
    sortResults = sorter != null;
    this.sorter = sorter;
    message = "Creation of average molecular formulas for MS annotation networks";
  }

  public CreateAvgNetworkFormulasTask(FormulaSortParameters parameters) {
    sortResults = true;
    FormulaSortParameters sortingParam =
        parameters.getParameter(CreateAvgNetworkFormulasParameters.sorting).getEmbeddedParameters();
    sorter = new FormulaSortTask(sortingParam);
    message = "Creation of average molecular formulas for MS annotation networks";
  }

  /**
   *
   * @param parameters
   * @param peakList
   * @param peakListRow
   * @param peak
   */
  public CreateAvgNetworkFormulasTask(FeatureList featureList, ParameterSet parameters) {
    this.featureList = featureList;

    sortResults = parameters.getParameter(CreateAvgNetworkFormulasParameters.sorting).getValue();
    if (sortResults) {
      FormulaSortParameters sortingParam = parameters
          .getParameter(CreateAvgNetworkFormulasParameters.sorting).getEmbeddedParameters();
      sorter = new FormulaSortTask(sortingParam);
    }
    message = "Creation of average molecular formulas for MS annotation networks";
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0.0;
    return finishedNets.get() / (double) totalRows;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return message;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // get all networks to run in parallel
    IonNetwork[] nets = IonNetworkLogic.getAllNetworks(featureList, false);
    totalRows = nets.length;
    if (totalRows == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No annotation networks found in this list. Run MS annotation");
      cancel();
      return;
    }

    // parallel
    Arrays.stream(nets).forEach(net -> {
      message = "Average formula creation on " + net.getID();
      if (!isCanceled()) {
        combineFormulasOfNetwork(net);
      }
      finishedNets.incrementAndGet();
    });

    logger.info("Finished formula search for all networks");
    setStatus(TaskStatus.FINISHED);
  }

  public List<MolecularFormulaIdentity> combineFormulasOfNetwork(IonNetwork net) {
    // find all formula lists of ions in network
    List<List<MolecularFormulaIdentity>> allLists = new ArrayList<>();
    for (Map.Entry<FeatureListRow, IonIdentity> e : net.entrySet()) {
      FeatureListRow r = e.getKey();
      IonIdentity ion = e.getValue();
      if (!ion.getIonType().isUndefinedAdduct()) {
        List<MolecularFormulaIdentity> list = ion.getMolFormulas();
        if (list != null && !list.isEmpty()) {
          // copy to not change original
          allLists.add(new ArrayList<>(list));
        }
      }
    }

    List<MolecularFormulaIdentity> results = new ArrayList<>();

    // find equals
    createAllAvgFormulas(allLists, results);

    if (!results.isEmpty()) {
      // find best formula for neutral mol of network
      // add all that have the same mol formula in at least 2 different ions (rows)
      if (sortResults && sorter != null) {
        double neutralMass = net.getNeutralMass();
        sorter.sort(results, neutralMass);
      }
      // add to net
      net.setMolFormulas(results);
    }
    return results;
  }

  /**
   * Create an average formula for all present formulas in all lists
   * 
   * @param allLists
   * @param results
   */
  private void createAllAvgFormulas(List<List<MolecularFormulaIdentity>> allLists,
      List<MolecularFormulaIdentity> results) {
    removeEmptyLists(allLists);
    // need to have more than one list
    if (allLists.size() <= 1)
      return;
    // create average formula
    List<MolecularFormulaIdentity> list = allLists.get(0);
    MolecularFormulaIdentity f = list.remove(0);
    removeEmptyLists(allLists);

    AverageResultFormula avg = new AverageResultFormula(f);
    // add all matches
    addAllMatchingFormula(allLists, avg);

    if (avg.getFormulas().size() > 1)
      results.add(avg);

    createAllAvgFormulas(allLists, results);
  }

  private void removeEmptyLists(List<List<MolecularFormulaIdentity>> allLists) {
    for (int i = 0; i < allLists.size();) {
      if (allLists.get(i).isEmpty())
        allLists.remove(i);
      else
        i++;
    }
  }

  /**
   * Searches all matching molecular formulas, adds them to the avgFormula and removes from the
   * lists
   * 
   * @param allLists
   * @param avg
   */
  private void addAllMatchingFormula(List<List<MolecularFormulaIdentity>> allLists,
      AverageResultFormula avg) {
    for (int listIndex = 0; listIndex < allLists.size(); listIndex++) {
      List<MolecularFormulaIdentity> list = allLists.get(listIndex);
      for (int i = 0; i < list.size();) {
        MolecularFormulaIdentity f = list.get(i);
        // compare and add (remove fitting)
        if (avg.isMatching(f)) {
          avg.addFormula(f);
          // remove from list
          list.remove(i);
        } else {
          i++;
        }
      }
    }
  }

}
