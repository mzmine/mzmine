package net.sf.mzmine.modules.datapointprocessing;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

/**
 * 
 * This abstract class defines the methods for processing an array of DataPoints.
 * 
 */
public abstract class DataPointProcessingTask extends AbstractTask {
  
  SpectraPlot targetPlot;
  DataPoint[] dataPoints;
  DataPointProcessingListener dppManager;
  ProcessedDataPoint[] results;
  ParameterSet parameterSet;

  DataPointProcessingTask(DataPoint[] dataPoints, SpectraPlot targetPlot, ParameterSet parameterSet) {
    setDataPoints(dataPoints);
    setTargetPlot(targetPlot);
    setParameterSet(parameterSet);
  }

  public DataPoint[] getDataPoints() {
    return dataPoints;
  }

  public void setDataPoints(DataPoint[] dataPoints) {
    this.dataPoints = dataPoints;
  }
  
  public SpectraPlot getTargetPlot() {
    return targetPlot;
  }

  public void setTargetPlot(SpectraPlot targetPlot) {
    this.targetPlot = targetPlot;
  }
  
  public void taskFinished() {
    if(this.getStatus() != TaskStatus.FINISHED) // make sure this is set
      this.setStatus(TaskStatus.FINISHED);
    dppManager.handle(new DataPointProcessingEvent(this));
  }
  
  public ProcessedDataPoint[] getResults() {
    if(results != null)
      return results;
    return new ProcessedDataPoint[0];
  }

  public ParameterSet getParameterSet() {
    return parameterSet;
  }

  public void setParameterSet(ParameterSet parameterSet) {
    this.parameterSet = parameterSet;
  }
}
