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

package io.github.mzmine.modules.example;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class MultiRawDataLearnerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MultiRawDataLearnerTask.class.getName());

  private final MZmineProject project;
  private final OriginalFeatureListOption handleOriginal;
  private FeatureList featureList;
  private FeatureList resultFeatureList;

  // features counter
  private int processedRows;
  private int totalRows;

  // parameter values
  private String suffix;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private ParameterSet parameters;

  /**
   * Constructor to set all parameters and the project
   */
  public MultiRawDataLearnerTask(MZmineProject project, FeatureList featureList,
      ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.project = project;
    this.featureList = featureList;
    this.parameters = parameters;
    // Get parameter values for easier use
    suffix = parameters.getParameter(LearnerParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(LearnerParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(LearnerParameters.rtTolerance).getValue();
    handleOriginal = parameters.getValue(LearnerParameters.handleOriginal);
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Learner task on " + featureList;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedRows / (double) totalRows;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running learner task on " + featureList);

    // Create a new results feature list which is added at the end
    resultFeatureList = new ModularFeatureList(featureList + " " + suffix, getMemoryMapStorage(),
        featureList.getRawDataFiles());

    /*
     * - A FeatureList is a list of Features (feature in retention time dimension with accurate m/z)<br>
     * ---- contains one or multiple RawDataFiles <br>
     * ---- access mean retention time, mean m/z, maximum intensity, ...<br>
     * - A RawDataFile holds a full chromatographic run with all ms scans<br>
     * ---- Each Scan and the underlying raw data can be accessed <br>
     * ---- Scans can be filtered by MS level, polarity, ...<br>
     */
    // get all rows and sort by m/z
    FeatureListRow[] rows = featureList.getRows().toArray(FeatureListRow[]::new);
    Arrays.sort(rows, new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

    // number of rawFiles is 1 prior to feature list alignment
    RawDataFile[] rawFiles = featureList.getRawDataFiles().toArray(RawDataFile[]::new);
    boolean isAlignedFeatureList = rawFiles.length > 1;

    totalRows = rows.length;
    // loop through all rows
    for (FeatureListRow row : rows) {
      // loop through all raw data files
      for (RawDataFile raw : rawFiles) {
        // check for cancelled state and stop
        if (isCanceled()) {
          return;
        }

        // current feature
        Feature feature = row.getFeature(raw);
        // check for feature in row for specific raw file
        if (feature != null) {
          double mz = feature.getMZ();
          double intensity = feature.getHeight();
          double rt = feature.getRT();

          // do stuff
          // ...

        }
      }
      // Update completion rate
      processedRows++;
    }

    // add to project
    addResultToProject();

    logger.info("Finished on " + featureList);
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Add feature list to project, delete old if requested, add description to result
   */
  public void addResultToProject() {
    // Add new feature list to the project
    handleOriginal.reflectNewFeatureListToProject(suffix, project, resultFeatureList, featureList);

    // Load previous applied methods
    for (FeatureListAppliedMethod proc : featureList.getAppliedMethods()) {
      resultFeatureList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to feature list
    resultFeatureList
        .addDescriptionOfAppliedTask(
            new SimpleFeatureListAppliedMethod(LearnerModule.class, parameters, getModuleCallDate()));

  }

}
