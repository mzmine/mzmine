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
