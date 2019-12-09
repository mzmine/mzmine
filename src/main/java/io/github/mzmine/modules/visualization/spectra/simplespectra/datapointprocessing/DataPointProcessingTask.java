/*
 * Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;

/**
 * 
 * This abstract class defines the methods for processing an array of
 * DataPoints. When implementing this, make sure to use setStatus and setResults
 * at the end of the task. The next task will not be launched, if the status has
 * not been set to FINISHED. The next Task will be launched using
 * ProcessedDataPoint[] results. DataPoints passed to this task will be stored
 * in dataPoints[] (an array of DataPoint[]). If you method requires mass
 * detection, it is recommended to chech if it's an instance of
 * ProcessedDataPoint[]. ParameterSet, plot and controller are also stored
 * during the constructor of this this abstract class.
 * 
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de /
 *         s_heuc03@uni-muenster.de
 * 
 */
public abstract class DataPointProcessingTask extends AbstractTask {

    private SpectraPlot targetPlot;
    protected DataPoint[] dataPoints;
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
     * Stores the dataPoints, plot, parameters, controller, and
     * TaskStatusListener passed to this task and sets the task status to
     * WAITING. Make sure to call this super constructor in your extending
     * class.
     * 
     * @param dataPoints
     * @param plot
     * @param parameterSet
     * @param controller
     * @param listener
     */
    public DataPointProcessingTask(@Nonnull DataPoint[] dataPoints,
            @Nonnull SpectraPlot plot, @Nonnull ParameterSet parameterSet,
            @Nonnull DataPointProcessingController controller,
            @Nonnull TaskStatusListener listener) {
        setDataPoints(dataPoints);
        setTargetPlot(plot);
        setParameterSet(parameterSet);
        setController(controller);
        String name = this.getClass().getName();
        name = name.substring(name.lastIndexOf(".") + 1);
        setTaskDescription(name + " of scan #"
                + plot.getMainScanDataSet().getScan().getScanNumber());
        addTaskStatusListener(listener);
        setStatus(TaskStatus.WAITING);
    }

    public abstract void displayResults();

    public @Nonnull DataPoint[] getDataPoints() {
        return dataPoints;
    }

    private void setDataPoints(@Nonnull DataPoint[] dataPoints) {
        this.dataPoints = dataPoints;
    }

    public @Nonnull SpectraPlot getTargetPlot() {
        return targetPlot;
    }

    private void setTargetPlot(@Nonnull SpectraPlot targetPlot) {
        this.targetPlot = targetPlot;
    }

    /**
     * 
     * @return Array of ProcessedDataPoints. Make sure the task has finished. If
     *         results are not set a new ProcessedDataPoint[0] will be returned.
     */
    public @Nonnull ProcessedDataPoint[] getResults() {
        if (results != null)
            return results;
        return new ProcessedDataPoint[0];
    }

    /**
     * Set the results when your task is done processing.
     * 
     * @param dp
     *            Array the results shall be set to.
     */
    public void setResults(@Nonnull ProcessedDataPoint[] dp) {
        this.results = dp;
    }

    /**
     * 
     * @return The parameter set passed to this task.
     */
    public @Nonnull ParameterSet getParameterSet() {
        return parameterSet;
    }

    private void setParameterSet(@Nonnull ParameterSet parameterSet) {
        this.parameterSet = parameterSet;
    }

    public @Nonnull DataPointProcessingController getController() {
        return controller;
    }

    private void setController(
            @Nonnull DataPointProcessingController controller) {
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
     * Convenience method to execute the
     * {@link ParameterSet#checkParameterValues} method and set an error message
     * using setErrorMessage method.
     * 
     * @return true if all values are valid, false otherwise.
     */
    protected boolean checkParameterSet() {
        List<String> error = new ArrayList<String>();
        if (!parameterSet.checkParameterValues(error)) {
            setErrorMessage(
                    "Data point/Spectra processing: Parameter check failed. \n"
                            + error.toString());
            return false;
        }
        return true;
    }

    /**
     * Checks if any invalid arguments were passed through the constructor of
     * this class and sets an error message using setErrorMessage. Only checks
     * for errors that would cause a NullPointerException, the length of the
     * passed DataPoint array is not checked.
     * 
     * @return true if all arguments are valid, false otherwise.
     */
    protected boolean checkValues() {
        if (getDataPoints() == null || getTargetPlot() == null
                || getParameterSet() == null || getController() == null) {
            setErrorMessage(
                    "Data point/Spectra processing: Invalid constructor arguments passed to "
                            + getTaskDescription());
            return false;
        }
        return true;
    }

    /**
     * 
     * @return Returns the color the results of this task should be displayed
     *         with.
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
