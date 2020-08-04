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

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_cliquems.CliqueMSTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ComputeAdduct {

  private final AnClique anClique;
  private final PolarityType polarityType;
  private final CliqueMSTask driverTask;
  private final Logger logger = Logger.getLogger(getClass().getName());

  public ComputeAdduct(AnClique anClique, CliqueMSTask driverTask, PolarityType polarityType){
    this.anClique = anClique;
    this.polarityType = polarityType;
    this.driverTask = driverTask;
  }

  private List<IonizationType> checkadinfo(String polarity){

    List<IonizationType> returnAdinfo = new ArrayList<>();
    PolarityType polarityType = (polarity.equals("positive"))?PolarityType.POSITIVE : PolarityType.NEGATIVE;

    if(polarity.equals("positive")){
      // Separate them in adducts with charge >1,
      // adducts with nmol >1 && charge ==1,
      // and adducts with charge and nmol = 1
      List<IonizationType> chargead = new ArrayList<>();
      EnumSet.allOf(IonizationType.class)
          .forEach(ion -> {
            if(ion.getCharge() > 1 && ion.getPolarity().equals(polarityType)){
              chargead.add(ion);
            }});
      Collections.sort(chargead, Comparator.comparingDouble(IonizationType::getAddedMass));

      List<IonizationType> nummolad = new ArrayList<>();
      EnumSet.allOf(IonizationType.class)
          .forEach( ion ->{
              if(ion.getNumMol() > 1 && ion.getPolarity().equals(polarityType)){
                nummolad.add(ion);
              }});
      Collections.sort(nummolad, Comparator.comparingDouble(IonizationType::getAddedMass));

      List<IonizationType> normalad = new ArrayList<>();
      EnumSet.allOf(IonizationType.class)
          .forEach( ion ->{
                if(ion.getCharge() == 1 && ion.getNumMol() == 1 && ion.getPolarity().equals(polarityType)){
                  normalad.add(ion);
                }});

      // Sort them from adducts with charge >1,
      // normal and adducts with nummol > 1,
      // trying to virtually sort all adducts from smaller massDiff to bigger
      returnAdinfo.addAll(chargead);
      returnAdinfo.addAll(normalad);
      returnAdinfo.addAll(nummolad);
    } else{
      // Separate them in adducts with charge < -1,
      // adducts with nmol >1 && charge == -1
      // and adducts with charge and nmol = 1


      List<IonizationType> chargead = new ArrayList<>();
      EnumSet.allOf(IonizationType.class)
          .forEach(ion -> {
            if(ion.getCharge() < -1 && ion.getPolarity().equals(polarityType)){
              chargead.add(ion);
            }});
      Collections.sort(chargead, Comparator.comparingDouble(IonizationType::getAddedMass));

      List<IonizationType> nummolad = new ArrayList<>();
      EnumSet.allOf(IonizationType.class)
          .forEach( ion ->{
            if(ion.getNumMol() > 1 && ion.getPolarity().equals(polarityType)){
              nummolad.add(ion);
            }});
      Collections.sort(nummolad, Comparator.comparingDouble(IonizationType::getAddedMass));

      List<IonizationType> normalad = new ArrayList<>();
      EnumSet.allOf(IonizationType.class)
          .forEach( ion ->{
            if(ion.getCharge() == -1 && ion.getNumMol() == 1 && ion.getPolarity().equals(polarityType)){
              normalad.add(ion);
            }});

      // Sort them from adducts with charge >1
      // normal and adducts with nummol > 1
      // trying to virtually sort all adducts from smaller massDiff to bigger

      returnAdinfo.addAll(chargead);
      returnAdinfo.addAll(normalad);
      returnAdinfo.addAll(nummolad);
    }
    return returnAdinfo;
  }

  public Set<OutputAn> getAnnotation( int topmasstotal, int topmassf, int sizeanG, double ppm, double filter,
      double emptyS , boolean normalizeScore ){
    if(anClique.anFound){
      logger.log(Level.WARNING,"Annotation has already been computed for this object.");
    }
    if(!anClique.isoFound){
      logger.log(Level.WARNING,"Isotopes have not been annotated. This could lead to some errors in adduct annotation");
    }
    if(!anClique.cliquesFound){
      logger.log(Level.WARNING,"Cliques have not been computed. This could lead to long computing times for adduct annotation");
    }
    logger.log(Level.FINEST,"Computing annotation.");

    ppm = ppm * 0.000001;

    List<IonizationType> orderadinfo = checkadinfo("positive");

    HashMap<Integer,PeakData> nodeIDtoPeakMap = new HashMap<>();

    Set<OutputAn> outAnSet = new HashSet<>();
    for(PeakData pd : this.anClique.getPeakDataList()){
      nodeIDtoPeakMap.put(pd.getNodeID(),pd);
    }
    for(Integer cliqueID : this.anClique.cliques.keySet()){
      List<PeakData> dfClique  = new ArrayList<>();
      for(Integer nodeID: this.anClique.cliques.get(cliqueID)){
        dfClique.add(nodeIDtoPeakMap.get(nodeID));
      }
      dfClique.removeIf(pd -> !pd.getIsotopeAnnotation().startsWith("M0"));
      Collections.sort(dfClique, Comparator.comparingDouble(PeakData::getMz));//sorting in order of mz values, in decreasing order
//      for(PeakData pd : dfClique){
//       logger.log(Level.WARNING,pd.getMz()+" "+pd.getCharge()+" "+pd.getNodeID()+" "+pd.getCliqueID());
//      }

      AdductAnnotationCliqueMS ad = new AdductAnnotationCliqueMS();
      OutputAn outAn = ad.returnAdductAnnotation(dfClique, orderadinfo, topmassf, topmasstotal, sizeanG, ppm, filter, emptyS, normalizeScore);
      outAnSet.add(outAn);
//      for(Integer itv : outAn.features){
//        String s = "";
//        for(int x=0; x<5; x++){
//          s+=" "+x+" ";
//          s+=outAn.masses.get(x).get(itv);
//          s+=" ";
//          s+=outAn.scores.get(x).get(itv);
//          s+=" ";
//          s+=outAn.ans.get(x).get(itv);
//        }
//          System.out.println(s);
//      }
    }
    return outAnSet;

  }


  //default values
  public Set<OutputAn> getAnnotation(){
    return getAnnotation(10, 1, 20, 10, 1e-4,-6, true);
  }

}
