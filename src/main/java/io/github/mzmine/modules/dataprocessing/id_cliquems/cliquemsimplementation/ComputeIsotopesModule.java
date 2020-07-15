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


package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import io.github.mzmine.taskcontrol.AbstractTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

/**
 * Main class for computing isotopes provided anClique object whose cliques have been already
 * computed, if not the algorithm considers each node as 1 clique and compute isotopes.
 *
 * See https://github.com/osenan/cliqueMS/blob/master/R/findIsotopes.R for R code corresponding to
 * this class
 */
public class ComputeIsotopesModule {

  private final Logger logger = Logger.getLogger(getClass().getName());

  private final AbstractTask driverTask;
  private final AnClique anClique;

  public ComputeIsotopesModule(AnClique anClique, AbstractTask task){
    this.anClique = anClique;
    this.driverTask = task;
  }


  public void computelistofIsoTable(int maxCharge, int maxGrade, double ppm, double isom){
    List<PeakData> pdList = anClique.getPeakList();
    HashMap<Integer,PeakData> pdHash = new HashMap<>();
    for(PeakData pd : pdList){
      pdHash.put(pd.getNodeID(),pd);
    }
    for(Integer cliqueID : this.anClique.cliques.keySet()){
      if(driverTask.isCanceled()){
        return;
      }
      List<Pair<Double, Pair<Double,Integer>>> inData = new ArrayList<>(); // contains following data -> intensity, mz value, nodeID
      for(Integer cliquenodeID : this.anClique.cliques.get(cliqueID)){
        PeakData pd = pdHash.get(cliquenodeID);
        Pair<Double,Integer> p = new Pair(pd.getMz(),pd.getNodeID());
        Pair<Double,Pair<Double,Integer>> isoInput = new Pair(pd.getIntensity(),p);
        inData.add(isoInput);
      }

      Collections.sort(inData, (o1, o2) -> Double.compare(o2.getKey(),o1.getKey()));

      IsotopeAnCliqueMS an = new IsotopeAnCliqueMS(inData);
      an.getIsotopes(maxCharge,ppm,isom);

    }

  }

  /**
   *
   * @param maxCharge
   * @param maxGrade
   * @param ppm
   * @param isom
   */
  public void getIsotopes(int maxCharge, int maxGrade, double ppm, double isom){
    if(!anClique.cliquesFound){
      logger.log(Level.WARNING,"Cliques have not been computed for this object. This could lead"
          + " to long computing times for isotope annotation.");
    }
    if(anClique.isoFound){
      logger.log(Level.WARNING,"Isotopes have been already computed for this object");
    }
    logger.log(Level.INFO,"Computing Isotopes");
    computelistofIsoTable(maxCharge, maxGrade, ppm, isom);

    if(driverTask.isCanceled()){
      return;
    }
  }
  public void getIsotopes(){
    getIsotopes(3,2,10,1.003355);
  }
}
