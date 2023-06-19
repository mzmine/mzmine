/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.example;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
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

class FeatureListRowLearnerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(FeatureListRowLearnerTask.class.getName());

  private final MZmineProject project;
  private final OriginalFeatureListOption handleOriginal;
  private FeatureList featureList;
  private ModularFeatureList resultFeatureList;

  // features counter
  private int processedFeatures;
  private int totalRows;

  // parameter values
  private String suffix;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private ParameterSet parameters;

  /**
   * Constructor to set all parameters and the project
   *
   */
  public FeatureListRowLearnerTask(MZmineProject project, FeatureList featureList, ParameterSet parameters, @Nullable
      MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
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

  @Override
  public String getTaskDescription() {
    return "Learner task on " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0;
    return (double) processedFeatures / (double) totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running learner task on " + featureList);

    // Create a new results feature list which is added at the end
    resultFeatureList = new ModularFeatureList(featureList + " " + suffix, getMemoryMapStorage(), featureList.getRawDataFiles());

    /**
     * - A FeatureList is a list of Features (feature in retention time dimension with accurate m/z)<br>
     * ---- contains one or multiple RawDataFiles <br>
     * ---- access mean retention time, mean m/z, maximum intensity, ...<br>
     */
    // get all rows and sort by m/z
    ModularFeatureListRow[] rows = featureList.getRows().toArray(ModularFeatureListRow[]::new);
    Arrays.sort(rows, new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

    totalRows = rows.length;
    for (int i = 0; i < totalRows; i++) {
      // check for cancelled state and stop
      if (isCanceled())
        return;

      ModularFeatureListRow row = rows[i];
      // access details
      double mz = row.getAverageMZ();
      double intensity = row.getAverageHeight();
      double rt = row.getAverageRT();
      Feature feature = row.getBestFeature();
      // do stuff
      // ...

      // add row to feature list result
      ModularFeatureListRow copy = new ModularFeatureListRow(resultFeatureList, row, true);
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
        .addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(LearnerModule.class, parameters, getModuleCallDate()));

  }

}
