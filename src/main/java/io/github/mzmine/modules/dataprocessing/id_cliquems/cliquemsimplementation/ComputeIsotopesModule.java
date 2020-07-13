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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

/**
 * Main class for computing isotopes provided anClique object whose cliques have been already
 * computed, if not the algorithm considers each node as 1 clique and compute isotopes.
 */
public class ComputeIsotopesModule {

  private Logger logger = Logger.getLogger(getClass().getName());

  private AnClique anClique;

  public ComputeIsotopesModule(AnClique anClique){
    this.anClique = anClique;
  }


  public void computelistofIsoTable(int maxCharge, int maxGrade, double ppm, double isom){
    List<PeakData> pdList = anClique.getPeakList();
    HashMap<Integer,PeakData> pdHash = new HashMap<>();
    for(PeakData pd : pdList){
      pdHash.put(pd.getNodeID(),pd);
    }
    for(Integer cliqueID : this.anClique.cliques.keySet()){
      List<Pair<Double, Pair<Double,Integer>>> inData = new ArrayList<>(); // contains following data -> intensity, mz value, nodeID
      for(Integer cliquenodeID : this.anClique.cliques.get(cliqueID)){
        PeakData pd = pdHash.get(cliquenodeID);
        Pair<Double,Integer> p = new Pair(pd.getMz(),pd.getNodeID());
        Pair<Double,Pair<Double,Integer>> isoInput = new Pair(pd.getIntensity(),p);
        inData.add(isoInput);
      }
      Collections.sort(inData, new Comparator<>() {
        @Override
        public int compare(Pair<Double, Pair<Double, Integer>> o1,
            Pair<Double, Pair<Double, Integer>> o2) {
          if(o1.getKey()<o2.getKey()){
            return 1;
          }
          else if(o1.getKey().equals(o2.getKey())){
            return 0;
          }
          else{
            return -1;
          }
        }
      });

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
  }
  public void getIsotopes(){
    getIsotopes(3,2,10,1.003355);
  }
}
