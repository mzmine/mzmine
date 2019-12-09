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

package io.github.mzmine.modules.example;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleFeature;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimplePeakListRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.PeakListRowSorter;
import io.github.mzmine.util.PeakUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

class StreamPeakListRowLearnerTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final MZmineProject project;
    private PeakList peakList;
    private PeakList resultPeakList;

    // peaks counter
    private int processedPeaks;
    private int totalRows;

    // parameter values
    private String suffix;
    private MZTolerance mzTolerance;
    private RTTolerance rtTolerance;
    private boolean removeOriginal;
    private ParameterSet parameters;

    /**
     * Constructor to set all parameters and the project
     * 
     * @param rawDataFile
     * @param parameters
     */
    public StreamPeakListRowLearnerTask(MZmineProject project,
            PeakList peakList, ParameterSet parameters) {
        this.project = project;
        this.peakList = peakList;
        this.parameters = parameters;
        // Get parameter values for easier use
        suffix = parameters.getParameter(LearnerParameters.suffix).getValue();
        mzTolerance = parameters.getParameter(LearnerParameters.mzTolerance)
                .getValue();
        rtTolerance = parameters.getParameter(LearnerParameters.rtTolerance)
                .getValue();
        removeOriginal = parameters.getParameter(LearnerParameters.autoRemove)
                .getValue();
    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
     */
    @Override
    public String getTaskDescription() {
        return "Learner task on " + peakList;
    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    @Override
    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0;
        return (double) processedPeaks / (double) totalRows;
    }

    /**
     * @see Runnable#run()
     */
    @Override
    public void run() {
        setStatus(TaskStatus.PROCESSING);
        logger.info("Running learner task on " + peakList);

        // Create a new results peakList which is added at the end
        resultPeakList = new SimplePeakList(peakList + " " + suffix,
                peakList.getRawDataFiles());

        /**
         * - A PeakList is a list of Features (peak in retention time dimension
         * with accurate m/z)<br>
         * ---- contains one or multiple RawDataFiles <br>
         * ---- access mean retention time, mean m/z, maximum intensity, ...<br>
         */

        // use streams to filter, sort and create list
        List<PeakListRow> rowList = Arrays.stream(peakList.getRows())
                .filter(r -> r.getAverageHeight() > 5000)
                .sorted(new PeakListRowSorter(SortingProperty.MZ,
                        SortingDirection.Ascending))
                .collect(Collectors.toList());
        totalRows = rowList.size();

        // ###########################################################
        // OPTION 1: Streams
        // either use stream to process all rows
        rowList.stream().forEachOrdered(row -> {
            // check for cancelled state and stop
            if (isCanceled())
                return;

            // access details
            double mz = row.getAverageMZ();
            double intensity = row.getAverageHeight();
            double rt = row.getAverageRT();
            Feature peak = row.getBestPeak();
            // do stuff
            // ...

            // add row to peaklist result
            PeakListRow copy = copyPeakRow(row);
            resultPeakList.addRow(copy);

            // Update completion rate
            processedPeaks++;
        });

        // ###########################################################
        // OPTION 2: For loop
        for (PeakListRow row : rowList) {
            // check for cancelled state and stop
            if (isCanceled())
                return;

            // access details
            double mz = row.getAverageMZ();
            double intensity = row.getAverageHeight();
            double rt = row.getAverageRT();
            Feature peak = row.getBestPeak();
            // do stuff
            // ...

            // add row to peaklist result
            PeakListRow copy = copyPeakRow(row);
            resultPeakList.addRow(copy);

            // Update completion rate
            processedPeaks++;
        }

        // add to project
        addResultToProject();

        logger.info("Finished on " + peakList);
        setStatus(TaskStatus.FINISHED);
    }

    /**
     * Create a copy of a feature list row.
     *
     * @param row
     *            the row to copy.
     * @return the newly created copy.
     */
    private static PeakListRow copyPeakRow(final PeakListRow row) {
        // Copy the feature list row.
        final PeakListRow newRow = new SimplePeakListRow(row.getID());
        PeakUtils.copyPeakListRowProperties(row, newRow);

        // Copy the peaks.
        for (final Feature peak : row.getPeaks()) {
            final Feature newPeak = new SimpleFeature(peak);
            PeakUtils.copyPeakProperties(peak, newPeak);
            newRow.addPeak(peak.getDataFile(), newPeak);
        }

        return newRow;
    }

    /**
     * Add peaklist to project, delete old if requested, add description to
     * result
     */
    public void addResultToProject() {
        // Add new peakList to the project
        project.addPeakList(resultPeakList);

        // Load previous applied methods
        for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
            resultPeakList.addDescriptionOfAppliedTask(proc);
        }

        // Add task description to peakList
        resultPeakList.addDescriptionOfAppliedTask(
                new SimplePeakListAppliedMethod("Learner task", parameters));

        // Remove the original peakList if requested
        if (removeOriginal)
            project.removePeakList(peakList);
    }

}
