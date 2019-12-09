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

package io.github.mzmine.modules.dataprocessing.featdet_shapemodeler;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimplePeakListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

class ShapeModelerTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final MZmineProject project;
    private final PeakList originalPeakList;

    // scan counter
    private int processedRows = 0, totalRows;
    private int newPeakID = 1;

    // User parameters
    private String suffix;
    private boolean removeOriginal;

    private ShapeModel shapeModelerType;
    private double resolution;

    private SimplePeakList newPeakList;

    private ParameterSet parameters;

    public ShapeModelerTask(MZmineProject project, PeakList peakList,
            ParameterSet parameters) {

        this.project = project;
        this.originalPeakList = peakList;
        this.parameters = parameters;

        shapeModelerType = parameters
                .getParameter(ShapeModelerParameters.shapeModelerType)
                .getValue();
        suffix = parameters.getParameter(ShapeModelerParameters.suffix)
                .getValue();
        removeOriginal = parameters
                .getParameter(ShapeModelerParameters.autoRemove).getValue();
        resolution = parameters
                .getParameter(ShapeModelerParameters.massResolution).getValue();

    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Shape modeling peaks from " + originalPeakList;
    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0;
        else
            return (double) processedRows / totalRows;
    }

    public void run() {

        setStatus(TaskStatus.PROCESSING);

        Class<?> shapeModelClass = shapeModelerType.getModelClass();
        Constructor<?> shapeModelConstruct;
        shapeModelConstruct = shapeModelClass.getConstructors()[0];

        // Get data file information
        RawDataFile dataFile = originalPeakList.getRawDataFile(0);

        // Create new feature list
        newPeakList = new SimplePeakList(originalPeakList + " " + suffix,
                dataFile);

        totalRows = originalPeakList.getNumberOfRows();
        int[] scanNumbers;
        double[] retentionTimes, intensities;
        SimplePeakListRow newRow;

        for (PeakListRow row : originalPeakList.getRows()) {

            if (isCanceled())
                return;

            newRow = new SimplePeakListRow(newPeakID);

            try {
                for (Feature peak : row.getPeaks()) {

                    // Load the intensities into array
                    dataFile = peak.getDataFile();
                    scanNumbers = peak.getScanNumbers();
                    retentionTimes = new double[scanNumbers.length];
                    for (int i = 0; i < scanNumbers.length; i++)
                        retentionTimes[i] = dataFile.getScan(scanNumbers[i])
                                .getRetentionTime();

                    intensities = new double[scanNumbers.length];
                    for (int i = 0; i < scanNumbers.length; i++) {
                        DataPoint dp = peak.getDataPoint(scanNumbers[i]);
                        if (dp != null)
                            intensities[i] = dp.getIntensity();
                        else
                            intensities[i] = 0;
                    }

                    Feature shapePeak = (Feature) shapeModelConstruct
                            .newInstance(peak, scanNumbers, intensities,
                                    retentionTimes, resolution);

                    newRow.addPeak(shapePeak.getDataFile(), shapePeak);
                }

            } catch (Exception e) {
                String message = "Error trying to make an instance of shape model class "
                        + shapeModelClass;
                MZmineCore.getDesktop().displayErrorMessage(
                        MZmineCore.getDesktop().getMainWindow(), message);
                logger.severe(message);
                return;
            }

            newPeakList.addRow(newRow);
            newPeakID++;
            processedRows++;
        }

        // Add new peaklist to the project
        project.addPeakList(newPeakList);

        // Add quality parameters to peaks
        QualityParameters.calculateQualityParameters(newPeakList);

        // Remove the original peaklist if requested
        if (removeOriginal)
            project.removePeakList(originalPeakList);

        // Load previous applied methods
        for (PeakListAppliedMethod proc : originalPeakList
                .getAppliedMethods()) {
            newPeakList.addDescriptionOfAppliedTask(proc);
        }

        // Add task description to peakList
        newPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                "Peaks shaped by " + shapeModelerType + " function",
                parameters));

        logger.finest("Finished peak shape modeler " + processedRows
                + " rows processed");

        setStatus(TaskStatus.FINISHED);

    }

}
