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

package io.github.mzmine.modules.example;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureUtils;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

class StreamFeatureListRowLearnerTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private FeatureList featureList;
  private FeatureList resultFeatureList;

  // features counter
  private int processedFeatures;
  private int totalRows;

  // parameter values
  private String suffix;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private boolean removeOriginal;
  private ParameterSet parameters;

  /**
   * Constructor to set all parameters and the project
   *
   * @param rawDataFile
   * @param parameters
   */
  public StreamFeatureListRowLearnerTask(MZmineProject project, FeatureList featureList,
      ParameterSet parameters) {
    this.project = project;
    this.featureList = featureList;
    this.parameters = parameters;
    // Get parameter values for easier use
    suffix = parameters.getParameter(LearnerParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(LearnerParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(LearnerParameters.rtTolerance).getValue();
    removeOriginal = parameters.getParameter(LearnerParameters.autoRemove).getValue();
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
    if (totalRows == 0)
      return 0;
    return (double) processedFeatures / (double) totalRows;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running learner task on " + featureList);

    // Create a new results feature list which is added at the end
    resultFeatureList = new ModularFeatureList(featureList + " " + suffix, featureList.getRawDataFiles());

    /**
     * - A FeatureList is a list of Features (feature in retention time dimension with accurate m/z)<br>
     * ---- contains one or multiple RawDataFiles <br>
     * ---- access mean retention time, mean m/z, maximum intensity, ...<br>
     */

    // use streams to filter, sort and create list
    List<FeatureListRow> rowList = featureList.getRows().stream().filter(r -> r.getAverageHeight() > 5000)
        .sorted(new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending))
        .collect(Collectors.toList());
    totalRows = rowList.size();

    // ###########################################################
    // OPTION 1: Streams
    // either use stream to process all rows
    rowList.stream().forEachOrdered(row -> {
      // check for cancelled state and stop
      if (isCanceled())
        return;

      // access details
      double mz = row.getAverageMZ();
      double intensity = row.getAverageHeight();
      double rt = row.getAverageRT();
      Feature feature = row.getBestFeature();
      // do stuff
      // ...

      // add row to feature list result
      FeatureListRow copy = copyFeatureRow(row);
      resultFeatureList.addRow(copy);

      // Update completion rate
      processedFeatures++;
    });

    // ###########################################################
    // OPTION 2: For loop
    for (FeatureListRow row : rowList) {
      // check for cancelled state and stop
      if (isCanceled())
        return;

      // access details
      double mz = row.getAverageMZ();
      double intensity = row.getAverageHeight();
      double rt = row.getAverageRT();
      Feature feature = row.getBestFeature();
      // do stuff
      // ...

      // add row to feature list result
      FeatureListRow copy = copyFeatureRow(row);
      resultFeatureList.addRow(copy);

      // Update completion rate
      processedFeatures++;
    }

    // add to project
    addResultToProject();

    logger.info("Finished on " + featureList);
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Create a copy of a feature list row.
   *
   * @param row the row to copy.
   * @return the newly created copy.
   */
  private static FeatureListRow copyFeatureRow(final FeatureListRow row) {
    // Copy the feature list row.
    final FeatureListRow newRow = new ModularFeatureListRow(
        (ModularFeatureList) row.getFeatureList(), row.getID());
    FeatureUtils.copyFeatureListRowProperties(row, newRow);

    // Copy the features.
    for (final Feature feature : row.getFeatures()) {
      final Feature newFeature = new ModularFeature(feature);
      FeatureUtils.copyFeatureProperties(feature, newFeature);
      newRow.addFeature(feature.getRawDataFile(), newFeature);
    }

    return newRow;
  }

  /**
   * Add feature list to project, delete old if requested, add description to result
   */
  public void addResultToProject() {
    // Add new feature list to the project
    project.addFeatureList(resultFeatureList);

    // Load previous applied methods
    for (FeatureListAppliedMethod proc : featureList.getAppliedMethods()) {
      resultFeatureList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to feature list
    resultFeatureList
        .addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod("Learner task", parameters));

    // Remove the original feature list if requested
    if (removeOriginal)
      project.removeFeatureList(featureList);
  }

}
