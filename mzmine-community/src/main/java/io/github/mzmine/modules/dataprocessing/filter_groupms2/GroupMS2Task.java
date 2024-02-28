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

package io.github.mzmine.modules.dataprocessing.filter_groupms2;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Groups fragmentation scans with features in range
 */
public class GroupMS2Task extends AbstractTask {

  private static final Logger logger = Logger.getLogger(GroupMS2Task.class.getName());

  private final ParameterSet parameters;
  private final @NotNull GroupMS2Processor groupMS2Processor;
  private final FeatureList list;

  /**
   * Create the task.
   *
   * @param list         feature list to process.
   * @param parameterSet task parameters.
   */
  public GroupMS2Task(final FeatureList list, final ParameterSet parameterSet,
      @NotNull Instant moduleCallDate) {
    super(((ModularFeatureList) list).getMemoryMapStorage(),
        moduleCallDate); // use storage from feature list to store merged ms2 spectra.
    this.list = list;

    groupMS2Processor = new GroupMS2Processor(this, list, parameterSet, getMemoryMapStorage());

    parameters = parameterSet;
  }

  @Override
  public double getFinishedPercentage() {
    return groupMS2Processor.getFinishedPercentage();
  }

  @Override
  public String getTaskDescription() {
    return groupMS2Processor.getTaskDescription();
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);

      groupMS2Processor.process();
      if (isCanceled()) {
        return;
      }

      list.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(GroupMS2Module.class, parameters,
              getModuleCallDate()));
      setStatus(TaskStatus.FINISHED);
      logger.info("Finished adding all MS2 scans to their features in " + list.getName());

    } catch (Exception t) {
      logger.log(Level.WARNING, "Error while grouping MS2 with features " + t.getMessage(), t);
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
      logger.log(Level.SEVERE, "Error while adding all MS2 scans to their feautres", t);
    }
  }

}
