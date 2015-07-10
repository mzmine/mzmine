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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.ida;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;

import org.jfree.data.xy.AbstractXYZDataset;

import com.google.common.collect.Range;

/**
 * 
 */
class IDADataSet extends AbstractXYZDataset implements Task {

    private static final long serialVersionUID = 1L;

    private RawDataFile rawDataFile;

    private Range<Double> totalRTRange, totalMZRange;
    private int allScanNumbers[], msmsScanNumbers[], totalScans, totalmsmsScans, processedScans;
    private final double[] rtValues, mzValues, intensityValues;

    private TaskStatus status = TaskStatus.WAITING;

    IDADataSet(RawDataFile rawDataFile, Range<Double> rtRange,
	    Range<Double> mzRange, IDAVisualizerWindow visualizer) {

	this.rawDataFile = rawDataFile;

	totalRTRange = rtRange;
	totalMZRange = mzRange;

	allScanNumbers = rawDataFile.getScanNumbers();
	msmsScanNumbers = rawDataFile.getScanNumbers(2, rtRange);

	totalScans = allScanNumbers.length;
	totalmsmsScans = msmsScanNumbers.length;
	
	rtValues = new double[totalmsmsScans];
	mzValues = new double[totalmsmsScans];
	intensityValues = new double[totalmsmsScans];

	MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);

    }

    @Override
    public void run() {

	status = TaskStatus.PROCESSING;
	double totalScanIntensity;

	for (int index = 0; index < totalScans; index++) {

	    // Cancel?
	    if (status == TaskStatus.CANCELED)
		return;

	    Scan scan = rawDataFile.getScan(allScanNumbers[index]);
	    
	    if (scan.getMSLevel() == 1) {
		// Store info about MS spectra for MS/MS to allow extraction of intensity of parent ion in MS scan. 
		//int msIndex = index;
	    }
	    else {
		Double precursorMZ = scan.getPrecursorMZ();	// Precursor m/z value
		Double scanRT = scan.getRetentionTime();	// Scan RT

		// Get total intensity of all peaks in MS/MS scan
		DataPoint scanDataPoints[] = scan.getDataPoints();
		totalScanIntensity = 0;
		for (int x = 0; x < scanDataPoints.length; x++) {
		    totalScanIntensity = totalScanIntensity + scanDataPoints[x].getIntensity();
		}

		if (totalRTRange.contains(scanRT) && totalMZRange.contains(precursorMZ)) {
		    // Add values to arrays
		    rtValues[processedScans] = scanRT;
		    mzValues[processedScans] = precursorMZ;
		    intensityValues[processedScans] = totalScanIntensity;
		    processedScans++;
		}

	    }

	}

	fireDatasetChanged();
	status = TaskStatus.FINISHED;

    }

    public int getSeriesCount() {
	return 1;
    }

    public Comparable<?> getSeriesKey(int series) {
	return rawDataFile.getName();
    }

    public int getItemCount(int series) {
	return totalmsmsScans;
    }

    public Number getX(int series, int item) {
	return rtValues[item];
    }

    public Number getY(int series, int item) {
	return mzValues[item];
    }

    public Number getZ(int series, int item) {
	return intensityValues[item];
    }

    public Number getMaxZ() {
	double max = intensityValues[0];
        for (int row = 0; row < totalmsmsScans; row++) {
            if (max < intensityValues[row]) {
                max = intensityValues[row];
            }
        }
        return max;
    }

    @Override
    public void cancel() {
	status = TaskStatus.CANCELED;
    }

    @Override
    public String getErrorMessage() {
	return null;
    }

    @Override
    public double getFinishedPercentage() {
	if (totalScans == 0)
	    return 0;
	return (double) processedScans / totalScans;
    }

    @Override
    public TaskStatus getStatus() {
	return status;
    }

    @Override
    public String getTaskDescription() {
	return "Updating IDA visualizer of " + rawDataFile;
    }

}