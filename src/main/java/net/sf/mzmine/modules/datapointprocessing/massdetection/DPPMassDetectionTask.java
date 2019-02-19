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

package net.sf.mzmine.modules.datapointprocessing.massdetection;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetectorParameters;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetectorParameters;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

public class DPPMassDetectionTask extends DataPointProcessingTask {

  int currentIndex;
  // private MZmineProcessingStep<MassDetector> pMassDetector;
  private MassDetector massDetector;
  double noiseLevel;


  DPPMassDetectionTask(DataPoint[] dataPoints, SpectraPlot targetPlot, ParameterSet parameterSet,
      DataPointProcessingController controller, TaskStatusListener listener) {
    super(dataPoints, targetPlot, parameterSet, controller, listener);
    currentIndex = 0;
    MZmineProcessingStep<MassDetector> step =
        parameterSet.getParameter(DPPMassDetectionParameters.massDetector).getValue();
    massDetector = step.getModule();
    noiseLevel = parameterSet.getParameter(DPPMassDetectionParameters.noiseLevel).getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Mass detection for a single array of DataPoints.";
  }

  @Override
  public double getFinishedPercentage() {
    return currentIndex / getDataPoints().length;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (getDataPoints() == null) {
      setStatus(TaskStatus.ERROR);
      return;
    }

    ProcessedDataPoint[] dp;
    if (massDetector instanceof ExactMassDetector) {
      ExactMassDetectorParameters parameters = new ExactMassDetectorParameters();
      ExactMassDetectorParameters.noiseLevel.setValue(noiseLevel);
      dp = ProcessedDataPoint.convert(massDetector.getMassValues(getDataPoints(), parameters));
    } else {
      CentroidMassDetectorParameters parameters = new CentroidMassDetectorParameters();
      CentroidMassDetectorParameters.noiseLevel.setValue(noiseLevel);
      dp = ProcessedDataPoint.convert(massDetector.getMassValues(getDataPoints(), parameters));
    }
    currentIndex = dataPoints.length;

    setResults(dp);
    setStatus(TaskStatus.FINISHED);
  }
}
