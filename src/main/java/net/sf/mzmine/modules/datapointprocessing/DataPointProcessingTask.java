package net.sf.mzmine.modules.datapointprocessing;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

/**
 * 
 * This abstract class defines the methods for processing an array of DataPoints. When implementing
 * this, make sure to use setStatus and setResults at the end of the task. The next task will not be
 * launched, if the status has not been set to FINISHED. The next Task will be launched using
 * ProcessedDataPoint[] results.
 * 
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 * 
 */
public abstract class DataPointProcessingTask extends AbstractTask {

  SpectraPlot targetPlot;
  DataPoint[] dataPoints;
  ProcessedDataPoint[] results;
  ParameterSet parameterSet;

  DataPointProcessingTask(DataPoint[] dataPoints, SpectraPlot targetPlot,
      ParameterSet parameterSet) {
    setDataPoints(dataPoints);
    setTargetPlot(targetPlot);
    setParameterSet(parameterSet);
  }

  public DataPoint[] getDataPoints() {
    return dataPoints;
  }

  private void setDataPoints(DataPoint[] dataPoints) {
    this.dataPoints = dataPoints;
  }

  public SpectraPlot getTargetPlot() {
    return targetPlot;
  }

  private void setTargetPlot(SpectraPlot targetPlot) {
    this.targetPlot = targetPlot;
  }

  /**
   * 
   * @return Array of ProcessedDataPoints. Make sure the task has finished. If results are not set a
   *         new ProcessedDataPoint[0] will be returned.
   */
  public ProcessedDataPoint[] getResults() {
    if (results != null)
      return results;
    return new ProcessedDataPoint[0];
  }

  /**
   * Set the results when your task is done processing.
   * 
   * @param dp Array the results shall be set to.
   */
  public void setResults(ProcessedDataPoint[] dp) {
    this.results = dp;
  }

  public ParameterSet getParameterSet() {
    return parameterSet;
  }

  public void setParameterSet(ParameterSet parameterSet) {
    this.parameterSet = parameterSet;
  }
}
