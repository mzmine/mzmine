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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.massdetection;

import org.jmol.util.Logger;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResultsDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;

public class DPPMassDetectionTask extends DataPointProcessingTask {

    int currentIndex;
    // private MZmineProcessingStep<MassDetector> pMassDetector;
    private MZmineProcessingStep<MassDetector> massDetector;

    DPPMassDetectionTask(DataPoint[] dataPoints, SpectraPlot targetPlot,
            ParameterSet parameterSet, DataPointProcessingController controller,
            TaskStatusListener listener) {
        super(dataPoints, targetPlot, parameterSet, controller, listener);
        currentIndex = 0;
        massDetector = parameterSet
                .getParameter(DPPMassDetectionParameters.massDetector)
                .getValue();
        // massDetector = step.getModule();
        setDisplayResults(parameterSet
                .getParameter(DPPMassDetectionParameters.displayResults)
                .getValue());
        setColor(parameterSet
                .getParameter(DPPMassDetectionParameters.datasetColor)
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

        if (!checkParameterSet() || !checkValues()) {
            setStatus(TaskStatus.ERROR);
            return;
        }

        if (getStatus() == TaskStatus.CANCELED) {
            return;
        }

        setStatus(TaskStatus.PROCESSING);

        MassDetector detector = massDetector.getModule();
        DataPoint[] masses = detector.getMassValues(getDataPoints(),
                massDetector.getParameterSet());

        if (masses == null || masses.length <= 0) {
            Logger.info(
                    "Data point/Spectra processing: No masses were detected with the given parameters.");
            setStatus(TaskStatus.CANCELED);
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
        if (getController().isLastTaskRunning() || isDisplayResults()) {
            getTargetPlot().addDataSet(
                    new DPPResultsDataSet("Mass detection results ("
                            + getResults().length + ")", getResults()),
                    getColor(), false);
        }
    }

}
