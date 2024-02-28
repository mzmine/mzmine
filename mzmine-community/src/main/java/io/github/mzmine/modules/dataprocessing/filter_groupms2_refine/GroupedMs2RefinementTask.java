/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_groupms2_refine;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Filters out feature list rows.
 */
public class GroupedMs2RefinementTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(GroupedMs2RefinementTask.class.getName());

  private final FeatureList featureList;
  private final ParameterSet parameters;
  private final GroupedMs2RefinementProcessor processor;

  /**
   * @param featureList  feature list to process.
   * @param parameterSet task parameters.
   */
  public GroupedMs2RefinementTask(final FeatureList featureList, final ParameterSet parameterSet,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureList = featureList;

    parameters = parameterSet;
    // RT has two options / tolerance is only provided for second option
    double minAbsFeatureHeight = parameters.getValue(
        GroupedMs2RefinementParameters.minimumAbsoluteFeatureHeight);
    double minRelFeatureHeight = parameters.getValue(
        GroupedMs2RefinementParameters.minimumRelativeFeatureHeight);

    processor = new GroupedMs2RefinementProcessor(this, featureList, minRelFeatureHeight,
        minAbsFeatureHeight);
  }


  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);

      processor.process();

      if (isCanceled()) {
        return;
      }

      featureList.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(GroupedMs2RefinementModule.class, parameters,
              getModuleCallDate()));
      setStatus(TaskStatus.FINISHED);
      logger.info("Finished refining fragment scans for features in " + featureList.getName());

    } catch (Exception t) {
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
      logger.log(Level.SEVERE,
          "Error while refining fragment scans for features in " + featureList.getName(), t);
    }
  }

  @Override
  public double getFinishedPercentage() {
    return processor.getFinishedPercentage();
  }

  @Override
  public String getTaskDescription() {
    return processor.getTaskDescription();
  }

}
