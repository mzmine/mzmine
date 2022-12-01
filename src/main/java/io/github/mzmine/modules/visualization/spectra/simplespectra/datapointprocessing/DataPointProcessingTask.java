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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

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

  private static final Logger logger = Logger.getLogger(DataPointProcessingTask.class.getName());

  private SpectraPlot targetPlot;
  protected MassSpectrum dataPoints;
  protected ParameterSet parameterSet;
  private DataPointProcessingController controller;
  protected String taskDescription;
  protected Color color;
  protected boolean displayResults;

  // move the results into this array by setReults to be collected by the
  // controller and passed on
  // to the next DPPTask by it
  private ProcessedDataPoint[] results;

  /**
   * Stores the dataPoints, plot, parameters, controller, and TaskStatusListener passed to this task
   * and sets the task status to WAITING. Make sure to call this super constructor in your extending
   * class.
   *
   * @param dataPoints
   * @param plot
   * @param parameterSet
   * @param controller
   * @param listener
   */
  public DataPointProcessingTask(@NotNull MassSpectrum dataPoints, @NotNull SpectraPlot plot,
      @NotNull ParameterSet parameterSet, @NotNull DataPointProcessingController controller,
      @NotNull TaskStatusListener listener) {
    super(null, Instant.now()); // no new data stored -> null, date irrelevant (not executed in batch)
    logger.warning("Rethink storage creation when re-implementing data point processing");

    setDataPoints(dataPoints);
    setTargetPlot(plot);
    setParameterSet(parameterSet);
    setController(controller);
    String name = this.getClass().getName();
    name = name.substring(name.lastIndexOf(".") + 1);
    setTaskDescription(name + " of scan #" + plot.getMainScanDataSet().getScan().getScanNumber());
    addTaskStatusListener(listener);
    setStatus(TaskStatus.WAITING);
  }

  public abstract void displayResults();

  public @NotNull MassSpectrum getDataPoints() {
    return dataPoints;
  }

  private void setDataPoints(@NotNull MassSpectrum dataPoints) {
    this.dataPoints = dataPoints;
  }

  public @NotNull SpectraPlot getTargetPlot() {
    return targetPlot;
  }

  private void setTargetPlot(@NotNull SpectraPlot targetPlot) {
    this.targetPlot = targetPlot;
  }

  /**
   *
   * @return Array of ProcessedDataPoints. Make sure the task has finished. If results are not set a
   *         new ProcessedDataPoint[0] will be returned.
   */
  public @NotNull ProcessedDataPoint[] getResults() {
    if (results != null)
      return results;
    return new ProcessedDataPoint[0];
  }

  /**
   * Set the results when your task is done processing.
   *
   * @param dp Array the results shall be set to.
   */
  public void setResults(@NotNull ProcessedDataPoint[] dp) {
    this.results = dp;
  }

  /**
   *
   * @return The parameter set passed to this task.
   */
  public @NotNull ParameterSet getParameterSet() {
    return parameterSet;
  }

  private void setParameterSet(@NotNull ParameterSet parameterSet) {
    this.parameterSet = parameterSet;
  }

  public @NotNull DataPointProcessingController getController() {
    return controller;
  }

  private void setController(@NotNull DataPointProcessingController controller) {
    this.controller = controller;
  }

  @Override
  public String getTaskDescription() {
    return taskDescription;
  }

  private void setTaskDescription(String taskDescription) {
    this.taskDescription = taskDescription;
  }

  /**
   * Convenience method to execute the {@link ParameterSet#checkParameterValues} method and set an
   * error message using setErrorMessage method.
   *
   * @return true if all values are valid, false otherwise.
   */
  protected boolean checkParameterSet() {
    List<String> error = new ArrayList<String>();
    if (!parameterSet.checkParameterValues(error)) {
      setErrorMessage(
          "Data point/Spectra processing: Parameter check failed. \n" + error.toString());
      return false;
    }
    return true;
  }

  /**
   * Checks if any invalid arguments were passed through the constructor of this class and sets an
   * error message using setErrorMessage. Only checks for errors that would cause a
   * NullPointerException, the length of the passed DataPoint array is not checked.
   *
   * @return true if all arguments are valid, false otherwise.
   */
  protected boolean checkValues() {
    if (getDataPoints() == null || getTargetPlot() == null || getParameterSet() == null
        || getController() == null) {
      setErrorMessage("Data point/Spectra processing: Invalid constructor arguments passed to "
          + getTaskDescription());
      return false;
    }
    return true;
  }

  /**
   *
   * @return Returns the color the results of this task should be displayed with.
   */
  public Color getColor() {
    return color;
  }

  /**
   *
   * @return true if the results should be displayed, false otherwise.
   */
  public boolean isDisplayResults() {
    return displayResults;
  }

  /**
   * Sets the color of the results of this task.
   *
   * @param color
   */
  protected void setColor(Color color) {
    this.color = color;
  }

  /**
   * Sets if the results of this task should be displayed.
   *
   * @param displayResults
   */
  protected void setDisplayResults(boolean displayResults) {
    this.displayResults = displayResults;
  }
}
