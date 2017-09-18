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

package net.sf.mzmine.modules.visualization.msms;

import java.awt.Color;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;

import org.jfree.data.xy.AbstractXYDataset;

import com.google.common.collect.Range;

/**
 * 
 */
class MsMsDataSet extends AbstractXYDataset implements Task {

    private static final long serialVersionUID = 1L;

    // For comparing small differences.
    private static final double EPSILON = 0.0000001;

    private RawDataFile rawDataFile;
    private Range<Double> totalRTRange, totalMZRange;
    private int allScanNumbers[], msmsScanNumbers[], totalScans,
	    totalmsmsScans, processedScans, allProcessedScans, processedColors,
	    totalEntries, lastMSIndex;
    private final double[] rtValues, mzValues, intensityValues;
    private IntensityType intensityType;
    private NormalizationType normalizationType;
    private final int[] scanNumbers;
    private double minPeakInt, maxIntensity;
    private Color[] colorValues;

    private TaskStatus status = TaskStatus.WAITING;

    MsMsDataSet(RawDataFile rawDataFile, Range<Double> rtRange,
	    Range<Double> mzRange, IntensityType intensityType,
	    NormalizationType normalizationType, Double minPeakInt,
	    MsMsVisualizerWindow visualizer) {

	this.rawDataFile = rawDataFile;

	totalRTRange = rtRange;
	totalMZRange = mzRange;
	this.intensityType = intensityType;
	this.normalizationType = normalizationType;
	this.minPeakInt = minPeakInt - EPSILON;

	allScanNumbers = rawDataFile.getScanNumbers();
	msmsScanNumbers = rawDataFile.getScanNumbers(2, rtRange);

	totalScans = allScanNumbers.length;
	totalmsmsScans = msmsScanNumbers.length;
	totalEntries = totalmsmsScans;

	scanNumbers = new int[totalScans];
	rtValues = new double[totalmsmsScans];
	mzValues = new double[totalmsmsScans];
	intensityValues = new double[totalmsmsScans];
	colorValues = new Color[totalmsmsScans];

	MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);

    }

    @Override
    public void run() {

	status = TaskStatus.PROCESSING;
	double totalScanIntensity, maxPeakIntensity;

	for (int index = 0; index < totalScans; index++) {

	    // Cancel?
	    if (status == TaskStatus.CANCELED)
		return;

	    Scan scan = rawDataFile.getScan(allScanNumbers[index]);

	    if (scan.getMSLevel() == 1) {
		// Store info about MS spectra for MS/MS to allow extraction of
		// intensity of precursor ion in MS scan.
		lastMSIndex = index;
	    } else {
		Double precursorMZ = scan.getPrecursorMZ(); // Precursor m/z
							    // value
		Double scanRT = scan.getRetentionTime(); // Scan RT

		// Calculate total intensity
		totalScanIntensity = 0;
		if (intensityType == IntensityType.MS) {
		    // Get intensity of precursor ion from MS scan
		    Scan msscan = rawDataFile
			    .getScan(allScanNumbers[lastMSIndex]);
		    Double mzTolerance = precursorMZ * 10 / 1000000;
		    Range<Double> precursorMZRange = Range.closed(precursorMZ
			    - mzTolerance, precursorMZ + mzTolerance);
		    DataPoint scanDataPoints[] = msscan
			    .getDataPointsByMass(precursorMZRange);
		    for (int x = 0; x < scanDataPoints.length; x++) {
			totalScanIntensity = totalScanIntensity
				+ scanDataPoints[x].getIntensity();
		    }
		} else if (intensityType == IntensityType.MSMS) {
		    // Get total intensity of all peaks in MS/MS scan
		    DataPoint scanDataPoints[] = scan.getDataPoints();
		    for (int x = 0; x < scanDataPoints.length; x++) {
			totalScanIntensity = totalScanIntensity
				+ scanDataPoints[x].getIntensity();
		    }
		}

		maxPeakIntensity = 0;
		DataPoint scanDataPoints[] = scan.getDataPoints();
		for (int x = 0; x < scanDataPoints.length; x++) {
		    if (maxPeakIntensity < scanDataPoints[x].getIntensity()) {
			maxPeakIntensity = scanDataPoints[x].getIntensity();
		    }
		}

		if (totalRTRange.contains(scanRT)
			&& totalMZRange.contains(precursorMZ)
			&& maxPeakIntensity > minPeakInt) {
		    // Add values to arrays
		    rtValues[processedScans] = scanRT;
		    mzValues[processedScans] = precursorMZ;
		    intensityValues[processedScans] = totalScanIntensity;
		    scanNumbers[processedScans] = index + 1; // +1 because loop
							     // runs from 0 not
							     // 1
		    processedScans++;
		}

	    }
	    allProcessedScans++;

	}

	// Update max Z values
	for (int row = 0; row < totalmsmsScans; row++) {
	    if (maxIntensity < intensityValues[row]) {
		maxIntensity = intensityValues[row];
	    }
	}

	// Update color table for all spots
	totalEntries = processedScans - 1;
	for (int index = 0; index < processedScans - 1; index++) {

	    // Cancel?
	    if (status == TaskStatus.CANCELED)
		return;

	    double maxIntensityVal = 1;

	    if (normalizationType == NormalizationType.all) {
		// Normalize based on all m/z values
		maxIntensityVal = maxIntensity;
	    } else if (normalizationType == NormalizationType.similar) {
		// Normalize based on similar m/z values
		double precursorMZ = mzValues[index];
		Double mzTolerance = precursorMZ * 10 / 1000000;
		Range<Double> precursorMZRange = Range.closed(precursorMZ
			- mzTolerance, precursorMZ + mzTolerance);
		maxIntensityVal = (double) getMaxZ(precursorMZRange);
	    }

	    // Calculate normalized intensity
	    double normIntensity = (double) intensityValues[index]
		    / maxIntensityVal;
	    if (normIntensity > 1) {
		normIntensity = 1;
	    }

	    // Convert normIntensity into gray color tone
	    // RGB tones go from 0 to 255 - we limit it to 220 to not include
	    // too light colors
	    int rgbVal = (int) Math.round(220 - normIntensity * 220);

	    // Update color table
	    colorValues[index] = new Color(rgbVal, rgbVal, rgbVal);

	    processedColors++;
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
	return maxIntensity;
    }

    public Number getMaxZ(Range<Double> mzRange) {
	double max = 1.0;
	for (int row = 0; row < totalmsmsScans; row++) {
	    if (mzRange.contains(mzValues[row])) {
		if (max < intensityValues[row]) {
		    max = intensityValues[row];
		}
	    }
	}
	return max;
    }

    public Color getColor(int series, int item) {
	return colorValues[item];
    }

    public void setColor(int series, int item, Color c) {
	colorValues[item] = c;
    }

    /**
     * Highlights all MS/MS spots for which a peak is found in the MS/MS
     * spectrum with the m/z value
     *
     * @param mz
     *            m/z.
     * @param ppm
     *            ppm value.
     * @param neutralLoss
     *            true or false.
     * @param c
     *            color.
     * 
     */
    public void highlightSpectra(double mz, MZTolerance searchMZTolerance, double minIntensity,
	    boolean neutralLoss, Color c) {
	// mzRange
	searchMZTolerance.getToleranceRange(mz);
	Range<Double> precursorMZRange = searchMZTolerance.getToleranceRange(mz);

	// Loop through all scans
	for (int row = 0; row < scanNumbers.length; row++) {
	    Scan msscan = rawDataFile.getScan(scanNumbers[row]);

	    // Get total intensity of all peaks in MS/MS scan
	    if (scanNumbers[row] > 0) {
		DataPoint scanDataPoints[] = msscan.getDataPoints();
		double selectedIons[] = new double[scanDataPoints.length];
		int ions = 0;
		boolean colorSpectra = false;

		// Search for neutral loss in ms/ms spectrum
		if (neutralLoss) {
		    for (int x = 0; x < scanDataPoints.length; x++) {
			if (scanDataPoints[x].getIntensity() > minIntensity) {
			    selectedIons[ions] = scanDataPoints[x].getMZ();
			    ions++;
			}
		    }

		    if (ions > 1) {
			double ionDiff[][] = new double[ions][ions];
			for (int x = 0; x < ions; x++) {
			    for (int y = 0; y < ions; y++) {
				ionDiff[x][y] = Math.abs(selectedIons[x]
					- selectedIons[y]);
			    }
			}

			for (int x = 0; x < ions; x++) {
			    for (int y = 0; y < ions; y++) {
				if (precursorMZRange.contains(ionDiff[x][y])) {
				    colorSpectra = true;
				    break;
				}
			    }
			}
		    }
		}

		// Search for specific ion in ms/ms spectrum
		else {
		    for (int x = 0; x < scanDataPoints.length; x++) {
			if (precursorMZRange
				.contains(scanDataPoints[x].getMZ())
				&& scanDataPoints[x].getIntensity() > minIntensity) {
			    colorSpectra = true;
			    break;
			}
		    }
		}

		if (colorSpectra) {
		    // If color is red green or blue then use toning from
		    // current color
		    int rgb = getColor(0, row).getRed();
		    if (c == Color.red) {
			setColor(0, row, new Color(255, rgb, rgb));
		    } else if (c == Color.green) {
			setColor(0, row, new Color(rgb, 255, rgb));
		    } else if (c == Color.blue) {
			setColor(0, row, new Color(rgb, rgb, 255));
		    } else {
			setColor(0, row, c);
		    }
		}

	    }

	}

	fireDatasetChanged();
    }

    public RawDataFile getDataFile() {
	return rawDataFile;
    }

    public int getScanNumber(final int item) {
	return scanNumbers[item];
    }

    /**
     * Returns index of data point which exactly matches given X and Y values
     *
     * @param retentionTime
     *            retention time.
     * @param mz
     *            m/z.
     * @return the nearest data point index.
     */
    public int getIndex(final double retentionTime, final double mz) {

	int index = -1;
	for (int i = 0; index < 0 && i < processedScans; i++) {

	    if (Math.abs(retentionTime - rtValues[i]) < EPSILON
		    && Math.abs(mz - mzValues[i]) < EPSILON) {

		index = i;
	    }
	}

	return index;
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
	if (totalScans == 0) {
	    return 0;
	}
	return (double) 0.5 * (allProcessedScans / totalScans) + 0.5
		* (100 * processedColors / totalEntries) / 100;
    }

    @Override
    public TaskStatus getStatus() {
	return status;
    }

    @Override
    public String getTaskDescription() {
	return "Updating MS/MS visualizer of " + rawDataFile;
    }

}