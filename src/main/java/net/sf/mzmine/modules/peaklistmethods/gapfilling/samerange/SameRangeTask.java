/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.gapfilling.samerange;

import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.RangeUtils;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;

class SameRangeTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final MZmineProject project;
    private PeakList peakList, processedPeakList;

    private String suffix;
    private MZTolerance mzTolerance;
    private boolean removeOriginal;

    private int processedRows, totalRows;

    private ParameterSet parameters;

    SameRangeTask(MZmineProject project, PeakList peakList,
	    ParameterSet parameters) {

	this.project = project;
	this.peakList = peakList;
	this.parameters = parameters;

	suffix = parameters.getParameter(SameRangeGapFillerParameters.suffix)
		.getValue();
	mzTolerance = parameters.getParameter(
		SameRangeGapFillerParameters.mzTolerance).getValue();
	removeOriginal = parameters.getParameter(
		SameRangeGapFillerParameters.autoRemove).getValue();

    }

    public void run() {

	logger.info("Started gap-filling " + peakList);

	setStatus(TaskStatus.PROCESSING);

	// Get total number of rows
	totalRows = peakList.getNumberOfRows();

	// Get peak list columns
	RawDataFile columns[] = peakList.getRawDataFiles();

	// Create new peak list
	processedPeakList = new SimplePeakList(peakList + " " + suffix, columns);

	// Fill gaps in given column
	for (int row = 0; row < totalRows; row++) {

	    // Canceled?
	    if (isCanceled())
		return;

	    PeakListRow sourceRow = peakList.getRow(row);
	    PeakListRow newRow = new SimplePeakListRow(sourceRow.getID());

	    // Copy comment
	    newRow.setComment(sourceRow.getComment());

	    // Copy identities
	    for (PeakIdentity ident : sourceRow.getPeakIdentities())
		newRow.addPeakIdentity(ident, false);
	    if (sourceRow.getPreferredPeakIdentity() != null)
		newRow.setPreferredPeakIdentity(sourceRow
			.getPreferredPeakIdentity());

	    // Copy each peaks and fill gaps
	    for (RawDataFile column : columns) {

		// Canceled?
		if (isCanceled())
		    return;

		// Get current peak
		Feature currentPeak = sourceRow.getPeak(column);

		// If there is a gap, try to fill it
		if (currentPeak == null)
		    currentPeak = fillGap(sourceRow, column);

		// If a peak was found or created, add it
		if (currentPeak != null)
		    newRow.addPeak(column, currentPeak);

	    }

	    processedPeakList.addRow(newRow);

	    processedRows++;

	}

	// Append processed peak list to the project
	project.addPeakList(processedPeakList);

        // Add quality parameters to peaks
	QualityParameters.calculateQualityParameters(processedPeakList);

	// Add task description to peakList
	processedPeakList
		.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
			"Gap filling using RT and m/z range", parameters));

	// Remove the original peaklist if requested
	if (removeOriginal)
	    project.removePeakList(peakList);

	setStatus(TaskStatus.FINISHED);

	logger.info("Finished gap-filling " + peakList);

    }

    private Feature fillGap(PeakListRow row, RawDataFile column) {

	SameRangePeak newPeak = new SameRangePeak(column);

	Range<Double> mzRange = null, rtRange = null;

	// Check the peaks for selected data files
	for (RawDataFile dataFile : row.getRawDataFiles()) {
	    Feature peak = row.getPeak(dataFile);
	    if (peak == null)
		continue;
	    if ((mzRange == null) || (rtRange == null)) {
		mzRange = peak.getRawDataPointsMZRange();
		rtRange = peak.getRawDataPointsRTRange();
	    } else {
		mzRange = mzRange.span(peak.getRawDataPointsMZRange());
		rtRange = rtRange.span(peak.getRawDataPointsRTRange());
	    }
	}

	assert mzRange != null;
	assert rtRange != null;

	Range<Double> mzRangeWithTol = mzTolerance.getToleranceRange(mzRange);

	// Get scan numbers
	int[] scanNumbers = column.getScanNumbers(1, rtRange);

	boolean dataPointFound = false;

	for (int scanNumber : scanNumbers) {

	    if (isCanceled())
		return null;

	    // Get next scan
	    Scan scan = column.getScan(scanNumber);

	    // Find most intense m/z peak
	    DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRangeWithTol);

	    if (basePeak != null) {
		if (basePeak.getIntensity() > 0)
		    dataPointFound = true;
		newPeak.addDatapoint(scan.getScanNumber(), basePeak);
	    } else {
		DataPoint fakeDataPoint = new SimpleDataPoint(
			RangeUtils.rangeCenter(mzRangeWithTol), 0);
		newPeak.addDatapoint(scan.getScanNumber(), fakeDataPoint);
	    }

	}

	if (dataPointFound) {
	    newPeak.finalizePeak();
	    if (newPeak.getArea() == 0)
		return null;
	    return newPeak;
	}

	return null;
    }

    public double getFinishedPercentage() {
	if (totalRows == 0)
	    return 0;
	return (double) processedRows / (double) totalRows;

    }

    public String getTaskDescription() {
	return "Gap filling " + peakList + " using RT and m/z range";
    }

}
