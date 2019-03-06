/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.massdetection;

import java.awt.Color;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetectorParameters;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetectorParameters;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResultsDataSet;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

public class DPPMassDetectionTask extends DataPointProcessingTask {

  int currentIndex;
  // private MZmineProcessingStep<MassDetector> pMassDetector;
  private MZmineProcessingStep<MassDetector> massDetector;
  private boolean displayResults;
  private Color color;


  DPPMassDetectionTask(DataPoint[] dataPoints, SpectraPlot targetPlot, ParameterSet parameterSet,
      DataPointProcessingController controller, TaskStatusListener listener) {
    super(dataPoints, targetPlot, parameterSet, controller, listener);
    currentIndex = 0;
    massDetector = parameterSet.getParameter(DPPMassDetectionParameters.massDetector).getValue();
    // massDetector = step.getModule();
    displayResults =
        parameterSet.getParameter(DPPMassDetectionParameters.displayResults).getValue();
    color = parameterSet.getParameter(DPPMassDetectionParameters.datasetColor).getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Mass detection for Scan #"
        + getTargetPlot().getMainScanDataSet().getScan().getScanNumber();
  }

  @Override
  public double getFinishedPercentage() {
    if (getDataPoints().length == 0)
      return 0;
    return currentIndex / getDataPoints().length;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (getDataPoints() == null || massDetector == null || massDetector.getModule() == null
        || massDetector.getParameterSet() == null) {
      setStatus(TaskStatus.ERROR);
      return;
    }

    if(getStatus() == TaskStatus.CANCELED) {
      return;
    }
    
    MassDetector detector = massDetector.getModule();
    DataPoint[] masses = detector.getMassValues(getDataPoints(), massDetector.getParameterSet());
    
    if(masses == null || masses.length <= 0) {
      setStatus(TaskStatus.ERROR);
      return;
    }
    
    ProcessedDataPoint[] dp = ProcessedDataPoint.convert(masses);

    currentIndex = dataPoints.length;

    setResults(dp);
    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public void displayResults() {
    // if this is the last task, display even if not checked.
    if (getController().isLastTaskRunning() || displayResults) {
      getTargetPlot().addDataSet(new DPPResultsDataSet("Mass detection results", getResults()),
          color, false);
    }
  }

}
