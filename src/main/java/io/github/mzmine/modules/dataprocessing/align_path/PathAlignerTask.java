/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package io.github.mzmine.modules.dataprocessing.align_path;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.align_path.functions.Aligner;
import io.github.mzmine.modules.dataprocessing.align_path.functions.ScoreAligner;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
class PathAlignerTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private ModularFeatureList peakLists[];
  private ModularFeatureList alignedPeakList;
  private String peakListName;
  private ParameterSet parameters;
  private Aligner aligner;

  PathAlignerTask(MZmineProject project, ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.parameters = parameters;
    peakLists = (ModularFeatureList[])
        parameters.getParameter(PathAlignerParameters.peakLists).getValue()
            .getMatchingFeatureLists();

    peakListName = parameters.getParameter(PathAlignerParameters.peakListName).getValue();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Path aligner, " + peakListName + " (" + peakLists.length + " feature lists)";
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (aligner == null) {
      return 0f;
    } else {
      return aligner.getProgress();
    }
  }

  /**
   * @see Runnable#run()
   */
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running Path aligner");

    aligner = (Aligner) new ScoreAligner(this.peakLists, parameters, getMemoryMapStorage());
    alignedPeakList = (ModularFeatureList) aligner.align();
    // Add new aligned feature list to the project
    project.addFeatureList(alignedPeakList);

    // Add task description to peakList
    alignedPeakList
        .addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod("Path aligner",
            PathAlignerModule.class, parameters, getModuleCallDate()));

    logger.info("Finished Path aligner");
    setStatus(TaskStatus.FINISHED);

  }
}
