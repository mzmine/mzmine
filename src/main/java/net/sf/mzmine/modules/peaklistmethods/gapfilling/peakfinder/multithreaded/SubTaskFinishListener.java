/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder.multithreaded;

import java.util.function.Consumer;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;

public class SubTaskFinishListener implements Consumer<PeakList> {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private ParameterSet parameters;
  private PeakList peakList;
  private int tasks;
  private int finished = 0;
  private boolean removeOriginal;


  public SubTaskFinishListener(MZmineProject project, ParameterSet parameters, PeakList peakList,
      boolean removeOriginal, int tasks) {
    super();
    this.project = project;
    this.parameters = parameters;
    this.peakList = peakList;
    this.tasks = tasks;
    this.removeOriginal = removeOriginal;
  }

  @Override
  public synchronized void accept(PeakList processedPeakList) {
    finished++;
    if (finished == tasks) {
      logger.info("All sub tasks of multithreaded gap-filling have finished. Finalising results.");
      // add pkl to project
      // Append processed peak list to the project
      project.addPeakList(processedPeakList);

      // Add quality parameters to peaks
      QualityParameters.calculateQualityParameters(processedPeakList);

      // Add task description to peakList
      processedPeakList
          .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("Gap filling ", parameters));

      // Remove the original peaklist if requested
      if (removeOriginal)
        project.removePeakList(peakList);

      logger.info("Completed: Multithreaded gap-filling successfull");
    }
  }

}
