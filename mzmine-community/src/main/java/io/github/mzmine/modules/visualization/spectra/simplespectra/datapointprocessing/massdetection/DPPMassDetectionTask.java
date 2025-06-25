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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.massdetection;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResultsDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import java.awt.Color;
import org.jmol.util.Logger;

public class DPPMassDetectionTask extends DataPointProcessingTask {

  private final MassDetector detector;
  int currentIndex;

  DPPMassDetectionTask(MassSpectrum dataPoints, SpectraPlot targetPlot, ParameterSet parameterSet,
      DataPointProcessingController controller, TaskStatusListener listener) {
    super(dataPoints, targetPlot, parameterSet, controller, listener);
    currentIndex = 0;
    var massDetectorStep = parameterSet.getParameter(DPPMassDetectionParameters.massDetector)
        .getValueWithParameters();
    detector = MassDetectors.createMassDetector(massDetectorStep);

    setDisplayResults(
        parameterSet.getParameter(DPPMassDetectionParameters.displayResults).getValue());

    Color c = FxColorUtil.fxColorToAWT(
        parameterSet.getParameter(DPPMassDetectionParameters.datasetColor).getValue());
    setColor(c);

  }

  @Override
  public double getFinishedPercentage() {
    if (getDataPoints().getNumberOfDataPoints() == 0) {
      return 0;
    }
    return currentIndex / (double) getDataPoints().getNumberOfDataPoints();
  }

  @Override
  public void run() {

    if (!checkParameterSet() || !checkValues()) {
      setStatus(TaskStatus.ERROR);
      return;
    }

    if (getStatus() == TaskStatus.CANCELED) {
      return;
    }

    setStatus(TaskStatus.PROCESSING);

    double[][] masses = detector.getMassValues(getDataPoints());

    if (masses == null || masses.length <= 0) {
      Logger.info(
          "Data point/Spectra processing: No masses were detected with the given parameters.");
      setStatus(TaskStatus.CANCELED);
      return;
    }

    ProcessedDataPoint[] dp = ProcessedDataPoint.convert(masses[0], masses[1]);

    currentIndex = dataPoints.getNumberOfDataPoints();

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
