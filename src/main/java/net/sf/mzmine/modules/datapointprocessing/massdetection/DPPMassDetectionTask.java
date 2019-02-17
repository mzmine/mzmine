package net.sf.mzmine.modules.datapointprocessing.massdetection;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.datapointprocessing.ProcessedDataPoint;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetectorParameters;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetectorParameters;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

public class DPPMassDetectionTask extends DataPointProcessingTask {

  int currentIndex;
//  private MZmineProcessingStep<MassDetector> pMassDetector;
  private MassDetector massDetector;
  double noiseLevel;
  
  
  DPPMassDetectionTask(DataPoint[] dataPoints, SpectraPlot targetPlot, ParameterSet parameterSet, DataPointProcessingController controller, TaskStatusListener listener) {
    super(dataPoints, targetPlot, parameterSet, controller, listener);
    currentIndex = 0;
    massDetector = parameterSet.getParameter(DPPMassDetectionParameters.massDetector).getValue().getModule();
    noiseLevel =  parameterSet.getParameter(DPPMassDetectionParameters.noiseLevel).getValue();
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
    
    if(getDataPoints() == null) {
      setStatus(TaskStatus.ERROR);
      return;
    }
    
    if(massDetector instanceof ExactMassDetector) {
      ExactMassDetectorParameters.noiseLevel.setValue(noiseLevel);
    } else {
      CentroidMassDetectorParameters.noiseLevel.setValue(noiseLevel);
    }
    
    ProcessedDataPoint[] dp = ProcessedDataPoint.convert(massDetector.getMassValues(getDataPoints(), getParameterSet()));
    
    setResults(dp);
    setStatus(TaskStatus.FINISHED);
  }

}
