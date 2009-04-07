/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.twod;

import java.util.Arrays;
import java.util.HashMap;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import org.jfree.data.xy.AbstractXYDataset;

/**
 * 
 */
class TwoDDataSet extends AbstractXYDataset implements Task {

	private RawDataFile rawDataFile;

	private double retentionTimes[];
	private double basePeaks[];
	private HashMap<Integer, Scan> dataPointMatrix;

	private Range totalRTRange, totalMZRange;
	private int scanNumbers[], totalScans, processedScans;

	private TaskStatus taskStatus = TaskStatus.WAITING;

	TwoDDataSet(RawDataFile rawDataFile, int msLevel, Range rtRange,
			Range mzRange, TwoDVisualizerWindow visualizer) {

		this.rawDataFile = rawDataFile;

		totalRTRange = rtRange;
		totalMZRange = mzRange;

		scanNumbers = rawDataFile.getScanNumbers(msLevel, rtRange);

		totalScans = scanNumbers.length;

		dataPointMatrix = new HashMap<Integer, Scan>(scanNumbers.length);
		retentionTimes = new double[scanNumbers.length];
		basePeaks = new double[scanNumbers.length];

		MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);

	}

	/**
	 */
	public void run() {

		taskStatus = TaskStatus.PROCESSING;

		for (int index = 0; index < scanNumbers.length; index++) {

			// Cancel?
			if (taskStatus == TaskStatus.CANCELED)
				return;

			Scan scan = rawDataFile.getScan(scanNumbers[index]);
			DataPoint scanBasePeak = scan.getBasePeak();
			retentionTimes[index] = scan.getRetentionTime();
			basePeaks[index] = (scanBasePeak == null ? 0 : scanBasePeak
					.getIntensity());
			dataPointMatrix.put(index, scan);
			processedScans++;
		}

		fireDatasetChanged();

		taskStatus = TaskStatus.FINISHED;

	}

	/**
	 * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesCount()
	 */
	public int getSeriesCount() {
		return 2;
	}

	/**
	 * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesKey(int)
	 */
	public Comparable getSeriesKey(int series) {
		return rawDataFile.toString();
	}

	/**
	 * @see org.jfree.data.xy.XYDataset#getItemCount(int)
	 */
	public int getItemCount(int series) {
		return 2;
	}

	/**
	 * @see org.jfree.data.xy.XYDataset#getX(int, int)
	 */
	public Number getX(int series, int item) {
		if (series == 0)
			return totalRTRange.getMin();
		else
			return totalRTRange.getMax();
	}

	/**
	 * @see org.jfree.data.xy.XYDataset#getY(int, int)
	 */
	public Number getY(int series, int item) {
		if (item == 0)
			return totalMZRange.getMin();
		else
			return totalMZRange.getMax();
	}

	double getMaxIntensity(Range rtRange, Range mzRange, PlotMode plotMode) {

		double maxIntensity = 0;

		double searchRetentionTimes[] = retentionTimes;
		if (processedScans < totalScans) {
			searchRetentionTimes = new double[processedScans];
			System.arraycopy(retentionTimes, 0, searchRetentionTimes, 0,
					searchRetentionTimes.length);
		}

		int startScanIndex = Arrays.binarySearch(searchRetentionTimes, rtRange
				.getMin());

		if (startScanIndex < 0)
			startScanIndex = (startScanIndex * -1) - 1;

		if (startScanIndex >= searchRetentionTimes.length) {
			return 0;
		}

		if (searchRetentionTimes[startScanIndex] > rtRange.getMax()) {
			if (startScanIndex == 0)
				return 0;

			if (startScanIndex == searchRetentionTimes.length - 1)
				return getMaxIntensity(dataPointMatrix.get(startScanIndex - 1)
						.getDataPoints(), mzRange, plotMode);

			// find which scan point is closer
			double diffNext = searchRetentionTimes[startScanIndex]
					- rtRange.getMax();
			double diffPrev = rtRange.getMin()
					- searchRetentionTimes[startScanIndex - 1];

			if (diffPrev < diffNext)
				return getMaxIntensity(dataPointMatrix.get(startScanIndex - 1)
						.getDataPoints(), mzRange, plotMode);
			else
				return getMaxIntensity(dataPointMatrix.get(startScanIndex)
						.getDataPoints(), mzRange, plotMode);
		}

		for (int scanIndex = startScanIndex; ((scanIndex < searchRetentionTimes.length) && (searchRetentionTimes[scanIndex] <= rtRange
				.getMax())); scanIndex++) {

			// ignore scans where all peaks are smaller than current max
			if (basePeaks[scanIndex] < maxIntensity)
				continue;

			double scanMax = getMaxIntensity(dataPointMatrix.get(scanIndex)
					.getDataPoints(), mzRange, plotMode);
			;
			if (scanMax > maxIntensity)
				maxIntensity = scanMax;

		}

		return maxIntensity;

	}

	double getMaxIntensity(DataPoint dataPoints[], Range mzRange,
			PlotMode plotMode) {

		double maxIntensity = 0;

		DataPoint searchMZ = new SimpleDataPoint(mzRange.getMin(), 0);
		int startMZIndex = Arrays.binarySearch(dataPoints, searchMZ,
				new DataPointSorter(SortingProperty.MZ,
						SortingDirection.Ascending));
		if (startMZIndex < 0)
			startMZIndex = (startMZIndex * -1) - 1;

		if (startMZIndex >= dataPoints.length)
			return 0;

		if (dataPoints[startMZIndex].getMZ() > mzRange.getMax()) {
			if (plotMode != PlotMode.CENTROID) {
				if (startMZIndex == 0)
					return 0;
				if (startMZIndex == dataPoints.length - 1)
					return dataPoints[startMZIndex - 1].getIntensity();

				// find which data point is closer
				double diffNext = dataPoints[startMZIndex].getMZ()
						- mzRange.getMax();
				double diffPrev = mzRange.getMin()
						- dataPoints[startMZIndex - 1].getMZ();

				if (diffPrev < diffNext)
					return dataPoints[startMZIndex - 1].getIntensity();
				else
					return dataPoints[startMZIndex].getIntensity();
			} else {
				return 0;
			}

		}

		for (int mzIndex = startMZIndex; ((mzIndex < dataPoints.length) && (dataPoints[mzIndex]
				.getMZ() <= mzRange.getMax())); mzIndex++) {
			if (dataPoints[mzIndex].getIntensity() > maxIntensity)
				maxIntensity = dataPoints[mzIndex].getIntensity();
		}

		return maxIntensity;

	}

	public void cancel() {
		taskStatus = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return null;
	}

	public double getFinishedPercentage() {
		if (totalScans == 0)
			return 0;
		return (double) processedScans / totalScans;
	}

	public TaskStatus getStatus() {
		return taskStatus;
	}

	public String getTaskDescription() {
		return "Updating 2D visualizer of " + rawDataFile;
	}

	public Object[] getCreatedObjects() {
		return null;
	}

}