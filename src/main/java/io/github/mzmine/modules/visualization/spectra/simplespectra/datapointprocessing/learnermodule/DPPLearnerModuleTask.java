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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.learnermodule;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResultsDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;

/**
 * This is the heart of every DataPointProcessingModule, the actual task being
 * executed. Every new implementation of DataPointProcessingTask should use this
 * basic structure to function accordingly.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPLearnerModuleTask extends DataPointProcessingTask {

    int currentIndex;

    DPPLearnerModuleTask(DataPoint[] dataPoints, SpectraPlot targetPlot,
            ParameterSet parameterSet, DataPointProcessingController controller,
            TaskStatusListener listener) {
        // call the super constructor, this is important to set up the
        // class-wide variables.
        super(dataPoints, targetPlot, parameterSet, controller, listener);
        currentIndex = 0;

        // since these parameters are acquired by the parameter set, they have
        // to be set here manually.
        setDisplayResults(parameterSet
                .getParameter(DPPLearnerModuleParameters.displayResults)
                .getValue());
        setColor(parameterSet
                .getParameter(DPPLearnerModuleParameters.datasetColor)
                .getValue());
    }

    @Override
    public double getFinishedPercentage() {
        if (getDataPoints().length == 0)
            return 0;
        return currentIndex / getDataPoints().length;
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
                    new DPPResultsDataSet("Mass detection results ("
                            + getResults().length + ")", getResults()),
                    getColor(), false);
        }
    }

}
