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
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class EmptyFeatureListTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(EmptyFeatureListTask.class.getName());

  private final ParameterSet parameters;
  private final MZmineProject project;
  private final ModularFeatureList featureList;

  // features counter
  private int finished;
  private int totalRows;

  /**
   * Constructor used to extract all parameters
   *
   * @param project        the current MZmineProject
   * @param featureList    runs this taks on this featureList
   * @param parameters     user parameters
   * @param storage        memory mapping is only used for memory intensive data that should be
   *                       stored for later processing - like spectra, feature data, ... so storage
   *                       is likely null here to process all in memory
   * @param moduleCallDate used internally to track the order of applied methods
   */
  public EmptyFeatureListTask(MZmineProject project, FeatureList featureList,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.project = project;
    this.featureList = (ModularFeatureList) featureList;
    this.parameters = parameters;
    // Get parameter values for easier use
  }

  @Override
  public String getTaskDescription() {
    return "Runs task on " + featureList;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running task on " + featureList);

    totalRows = featureList.getNumberOfRows();

    for (FeatureListRow row : featureList.getRows()) {
      // check for cancelled state and stop
      if (isCanceled()) {
        return;
      }

      // Update progress
      finished++;
    }

    // add to project
    addAppliedMethodsAndResultToProject();

    logger.info("Finished on " + featureList);
    setStatus(TaskStatus.FINISHED);
  }


  public void addAppliedMethodsAndResultToProject() {
    // Add task description to feature list
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(EmptyFeatureListModule.class, parameters,
            getModuleCallDate()));
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : finished / (double) totalRows;
  }
}
