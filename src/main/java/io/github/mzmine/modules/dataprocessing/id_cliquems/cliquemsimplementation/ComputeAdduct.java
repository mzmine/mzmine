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

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_cliquems.CliqueMSTask;
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

  public void getAnnotation( int topmasstotal, int topmassf, int sizeanG, double ppm, double filter,
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



  }


  //default values
  public void getAnnotation(){
    getAnnotation(10, 1, 20, 10, 1e-4,-6, true);
  }

}
