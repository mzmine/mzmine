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

package net.sf.mzmine.modules.datapointprocessing;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.datapointprocessing.datamodel.ProcessedDataPoint;
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
 * ProcessedDataPoint[] results. DataPoints passed to this task will be stored in dataPoints[] (an
 * array of DataPoint[]). If you method requires mass detection, it is recommended to chech if it's
 * an instance of ProcessedDataPoint[]. ParameterSet, plot and controller are also stored during the
 * constructor of this this abstract class.
 * 
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 * 
 */
public abstract class DataPointProcessingTask extends AbstractTask {

  private SpectraPlot targetPlot;
  protected DataPoint[] dataPoints;
  protected ParameterSet parameterSet;
  private DataPointProcessingController controller;

  // move the results into this array by setReults to be collected by the controller and passed on
  // to the next DPPTask by it
  private ProcessedDataPoint[] results;

  /**
   * Stores the dataPoints, plot, parameters, controller, and TaskStatusListener passed to this task
   * and sets the task status to WAITING. Make sure to call this super constructor in your extending class.
   * 
   * @param dataPoints
   * @param plot
   * @param parameterSet
   * @param controller
   * @param listener
   */
  public DataPointProcessingTask(DataPoint[] dataPoints, SpectraPlot plot,
      ParameterSet parameterSet, DataPointProcessingController controller,
      TaskStatusListener listener) {
    setDataPoints(dataPoints);
    setTargetPlot(plot);
    setParameterSet(parameterSet);
    setController(controller);
    addTaskStatusListener(listener);
    setStatus(TaskStatus.WAITING);
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

  /**
   * 
   * @return The parameter set passed to this task.
   */
  public ParameterSet getParameterSet() {
    return parameterSet;
  }

  private void setParameterSet(ParameterSet parameterSet) {
    this.parameterSet = parameterSet;
  }

  public DataPointProcessingController getController() {
    return controller;
  }

  private void setController(DataPointProcessingController controller) {
    this.controller = controller;
  }

}
