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

package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import java.util.function.Consumer;
import java.util.logging.Logger;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.parameters.ParameterSet;

public class SubTaskFinishListener implements Consumer<FeatureList> {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private ParameterSet parameters;
  private ModularFeatureList originalFeatureList;
  private int tasks;
  private int finished = 0;
  private boolean removeOriginal;

  public SubTaskFinishListener(MZmineProject project, ParameterSet parameters, ModularFeatureList originalFeatureList,
      boolean removeOriginal, int tasks) {
    super();
    this.project = project;
    this.parameters = parameters;
    this.originalFeatureList = originalFeatureList;
    this.tasks = tasks;
    this.removeOriginal = removeOriginal;
  }

  @Override
  public synchronized void accept(FeatureList processedPeakList) {
    finished++;
    if (finished == tasks) {
      logger.info("All sub tasks of multithreaded gap-filling have finished. Finalising results.");
      // add pkl to project
      // Append processed feature list to the project
      project.addFeatureList(processedPeakList);

      // Add quality parameters to peaks
      //QualityParameters.calculateQualityParameters(processedPeakList);

      // Add task description to peakList
      processedPeakList
          .addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod("Gap filling ",
              MultiThreadPeakFinderModule.class, parameters));

      // Remove the original peaklist if requested
      if (removeOriginal)
        project.removeFeatureList(originalFeatureList);

      logger.info("Completed: Multithreaded gap-filling successfull");
    }
  }

}
