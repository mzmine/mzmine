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

package io.github.mzmine.modules.dataprocessing.featdet_peakextender;

import java.util.Arrays;
import java.util.logging.Logger;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimplePeakListRow;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.PeakSorter;
import io.github.mzmine.util.PeakUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.ScanUtils;

public class PeakExtenderTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final MZmineProject project;
    private PeakList peakList, extendedPeakList;

    // peaks counter
    private int processedPeaks, totalPeaks;

    // Parameters
    private MZTolerance mzTolerance;
    private double minimumHeight;
    private String suffix;
    private boolean removeOriginal;

    private ParameterSet parameters;

    public PeakExtenderTask(MZmineProject project, PeakList peakList,
            ParameterSet parameters) {

        this.project = project;
        this.peakList = peakList;
        this.parameters = parameters;

        suffix = parameters.getParameter(PeakExtenderParameters.suffix)
                .getValue();
        mzTolerance = parameters
                .getParameter(PeakExtenderParameters.mzTolerance).getValue();
        minimumHeight = parameters
                .getParameter(PeakExtenderParameters.minimumHeight).getValue();
        removeOriginal = parameters
                .getParameter(PeakExtenderParameters.autoRemove).getValue();
    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Extending peaks on " + peakList;
    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalPeaks == 0)
            return 0.0;
        return (double) processedPeaks / (double) totalPeaks;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);
        logger.info("Running peak extender on " + peakList);

        // We assume source peakList contains one datafile
        RawDataFile dataFile = peakList.getRawDataFile(0);

        // Create a new deisotoped peakList
        extendedPeakList = new SimplePeakList(peakList + " " + suffix,
                peakList.getRawDataFiles());

        // Sort peaks by descending height
        Feature[] sortedPeaks = peakList.getPeaks(dataFile);
        Arrays.sort(sortedPeaks, new PeakSorter(SortingProperty.Height,
                SortingDirection.Descending));

        // Loop through all peaks
        totalPeaks = sortedPeaks.length;
        Feature oldPeak;

        for (int ind = 0; ind < totalPeaks; ind++) {

            if (isCanceled())
                return;

            oldPeak = sortedPeaks[ind];

            if (oldPeak.getHeight() >= minimumHeight) {
                Feature newPeak = this.getExtendedPeak(oldPeak);
                // Get previous pekaListRow
                PeakListRow oldRow = peakList.getPeakRow(oldPeak);

                // keep old ID
                int oldID = oldRow.getID();
                SimplePeakListRow newRow = new SimplePeakListRow(oldID);
                PeakUtils.copyPeakListRowProperties(oldRow, newRow);
                newRow.addPeak(dataFile, newPeak);
                extendedPeakList.addRow(newRow);
            }

            // Update completion rate
            processedPeaks++;

        }

        // Add new peakList to the project
        project.addPeakList(extendedPeakList);

        // Add quality parameters to peaks
        QualityParameters.calculateQualityParameters(extendedPeakList);

        // Load previous applied methods
        for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
            extendedPeakList.addDescriptionOfAppliedTask(proc);
        }

        // Add task description to peakList
        extendedPeakList.addDescriptionOfAppliedTask(
                new SimplePeakListAppliedMethod("Peak extender", parameters));

        // Remove the original peakList if requested
        if (removeOriginal)
            project.removePeakList(peakList);

        logger.info("Finished peak extender on " + peakList);
        setStatus(TaskStatus.FINISHED);

    }

    private Feature getExtendedPeak(Feature oldPeak) {

        double maxHeight = oldPeak.getHeight();
        int originScanNumber = oldPeak.getRepresentativeScanNumber();
        RawDataFile rawFile = oldPeak.getDataFile();
        ExtendedPeak newPeak = new ExtendedPeak(rawFile);
        int totalScanNumber = rawFile.getNumOfScans();
        Range<Double> mzRange = mzTolerance.getToleranceRange(oldPeak.getMZ());
        Scan scan;
        DataPoint dataPoint;

        // Look for dataPoint related to this peak to the left
        int scanNumber = originScanNumber;
        scanNumber--;
        while (scanNumber > 0) {

            scan = rawFile.getScan(scanNumber);

            if (scan == null) {
                scanNumber--;
                continue;
            }

            if (scan.getMSLevel() != 1) {
                scanNumber--;
                continue;
            }

            dataPoint = ScanUtils.findBasePeak(scan, mzRange);

            if (dataPoint == null)
                break;
            if (dataPoint.getIntensity() < minimumHeight)
                break;

            newPeak.addMzPeak(scanNumber, dataPoint);
            if (dataPoint.getIntensity() > maxHeight)
                maxHeight = dataPoint.getIntensity();

            scanNumber--;

        }

        // Add original dataPoint
        newPeak.addMzPeak(originScanNumber,
                oldPeak.getDataPoint(originScanNumber));

        // Look to the right
        scanNumber = originScanNumber;
        scanNumber++;
        while (scanNumber <= totalScanNumber) {

            scan = rawFile.getScan(scanNumber);

            if (scan == null) {
                scanNumber++;
                continue;
            }

            if (scan.getMSLevel() != 1) {
                scanNumber++;
                continue;
            }

            dataPoint = ScanUtils.findBasePeak(scan, mzRange);

            if (dataPoint == null)
                break;
            if (dataPoint.getIntensity() < minimumHeight)
                break;

            newPeak.addMzPeak(scanNumber, dataPoint);
            if (dataPoint.getIntensity() > maxHeight)
                maxHeight = dataPoint.getIntensity();

            scanNumber++;

        }

        newPeak.finishExtendedPeak();

        newPeak.setMostIntenseFragmentScanNumber(
                oldPeak.getMostIntenseFragmentScanNumber());

        int[] scanNumbers = newPeak.getScanNumbers();
        logger.finest("Extended peak original " + originScanNumber + " from "
                + scanNumbers[0] + " - " + scanNumbers[scanNumbers.length - 1]
                + " height " + maxHeight);

        return newPeak;

    }

}
