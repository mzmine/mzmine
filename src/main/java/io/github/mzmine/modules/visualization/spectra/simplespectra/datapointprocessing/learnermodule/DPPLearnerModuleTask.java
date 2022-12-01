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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.learnermodule;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResultsDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;

/**
 * This is the heart of every DataPointProcessingModule, the actual task being executed. Every new
 * implementation of DataPointProcessingTask should use this basic structure to function
 * accordingly.
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPLearnerModuleTask extends DataPointProcessingTask {

  int currentIndex;

  DPPLearnerModuleTask(MassSpectrum spectrum, SpectraPlot targetPlot, ParameterSet parameterSet,
      DataPointProcessingController controller, TaskStatusListener listener) {
    // call the super constructor, this is important to set up the
    // class-wide variables.
    super(spectrum, targetPlot, parameterSet, controller, listener);
    currentIndex = 0;

    // since these parameters are acquired by the parameter set, they have
    // to be set here manually.
    setDisplayResults(
        parameterSet.getParameter(DPPLearnerModuleParameters.displayResults).getValue());
    Color c = FxColorUtil.fxColorToAWT(
        parameterSet.getParameter(DPPLearnerModuleParameters.datasetColor).getValue());
    setColor(c);
  }

  @Override
  public double getFinishedPercentage() {
    if (getDataPoints().getNumberOfDataPoints() == 0)
      return 0;
    return currentIndex / getDataPoints().getNumberOfDataPoints();
  }

  @Override
  public void run() {

    // check the parameter set and constructor values first, and back out,
    // if they are invalid.
    // error messages are set within these convenience methods.
    if (!checkParameterSet() || !checkValues()) {
      setStatus(TaskStatus.ERROR);
      return;
    }

    // check if this task has been cancelled by now
    if (getStatus() == TaskStatus.CANCELED) {
      return;
    }

    // set status to processing and start
    setStatus(TaskStatus.PROCESSING);

    // do your processing now

    ProcessedDataPoint[] dp = new ProcessedDataPoint[0];

    // it is CRUCIAL the results are being set in general, and it is crucial
    // they are set BEFORE the
    // status of this task is set to FINISHED, because the status listener
    // will start the next task.
    setResults(dp);
    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public void displayResults() {
    // if this is the last task, display even if not checked.
    if (getController().isLastTaskRunning() || isDisplayResults()) {
      getTargetPlot().addDataSet(
          new DPPResultsDataSet("Mass detection results (" + getResults().length + ")",
              getResults()), getColor(), false, true);
    }
  }

}
