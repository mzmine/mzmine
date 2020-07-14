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

package io.github.mzmine.modules.dataprocessing.id_cliquems;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.modules.dataprocessing.id_camera.CameraSearchTask;
import io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation.AnClique;
import io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation.ComputeCliqueModule;
import io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation.ComputeIsotopesModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.logging.Logger;

public class CliqueMSTask extends AbstractTask {
  // Logger.
  private static final Logger logger = Logger.getLogger(CameraSearchTask.class.getName());

  private final double PPM_CONV = 0.000001;

  // Feature list to process.
  private final PeakList peakList;

  // Task progress
  private Double progress;

  // Project
  private final MZmineProject project;

  // Parameters.
  private final ParameterSet parameters;

  public CliqueMSTask(final MZmineProject project, final ParameterSet parameters,
      final PeakList list){
    this.project = project;
    this.parameters = parameters;
    peakList = list;
  }

  @Override
  public String getTaskDescription() {

    return "Identification of pseudo-spectra in " + peakList;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void cancel(){
    super.cancel();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    this.progress = 0.0;

    try {
      //TODO multiple rawDataFile support
      ComputeCliqueModule cm = new ComputeCliqueModule(peakList,peakList.getRawDataFile(0));
      this.progress = 0.2;
      // Check if not canceled
      if (isCanceled())
        return;
      AnClique anClique =  cm.getClique(parameters.getParameter(CliqueMSParameters.FILTER).getValue(),
          parameters.getParameter(CliqueMSParameters.MZ_DIFF).getValue().getPpmTolerance()* PPM_CONV,
          parameters.getParameter(CliqueMSParameters.RT_DIFF).getValue().getTolerance(),
          parameters.getParameter(CliqueMSParameters.IN_DIFF).getValue(),
          parameters.getParameter(CliqueMSParameters.TOL).getValue());
      this.progress = 0.5;
      // Check if not canceled
      if (isCanceled())
        return;
      ComputeIsotopesModule cim = new ComputeIsotopesModule(anClique);
      cim.getIsotopes();
      // Check if not canceled
      if (isCanceled())
        return;

      // Finished.
      this.progress = 1.0;
      setStatus(TaskStatus.FINISHED);
    }
    catch(Exception e){
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not calculate cliques for features "+ peakList.getName()+" \n" +
          e.getMessage());
    }
  }
}
