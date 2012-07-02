/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.LinkedList;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.TaskEvent;
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
	private SoftReference<DataPoint[]> dataPointMatrix[];

	private Range totalRTRange, totalMZRange;
	private int scanNumbers[], totalScans, processedScans;

	private TaskStatus status = TaskStatus.WAITING;
	private LinkedList <TaskListener> taskListeners = new LinkedList<TaskListener>( );

	@SuppressWarnings("unchecked")
	TwoDDataSet(RawDataFile rawDataFile, int msLevel, Range rtRange,
			Range mzRange, TwoDVisualizerWindow visualizer) {

		this.rawDataFile = rawDataFile;

		totalRTRange = rtRange;
		totalMZRange = mzRange;

		scanNumbers = rawDataFile.getScanNumbers(msLevel, rtRange);

		totalScans = scanNumbers.length;

		dataPointMatrix = new SoftReference[scanNumbers.length];
		retentionTimes = new double[scanNumbers.length];
		basePeaks = new double[scanNumbers.length];

		MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);

	}

	/**
	 */
	public void run() {

		setStatus( TaskStatus.PROCESSING );

		for (int index = 0; index < scanNumbers.length; index++) {

			// Cancel?
			if (status == TaskStatus.CANCELED)
				return;

			Scan scan = rawDataFile.getScan(scanNumbers[index]);
			DataPoint scanBasePeak = scan.getBasePeak();
			retentionTimes[index] = scan.getRetentionTime();
			basePeaks[index] = (scanBasePeak == null ? 0 : scanBasePeak
					.getIntensity());
			DataPoint scanDataPoints[] = scan.getDataPoints();
			dataPointMatrix[index] = new SoftReference<DataPoint[]>(
					scanDataPoints);
			processedScans++;
		}

		fireDatasetChanged();

		setStatus( TaskStatus.FINISHED );

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
	public Comparable<?> getSeriesKey(int series) {
		return rawDataFile.getName();
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
				return getMaxIntensity(startScanIndex - 1, mzRange, plotMode);

			// find which scan point is closer
			double diffNext = searchRetentionTimes[startScanIndex]
					- rtRange.getMax();
			double diffPrev = rtRange.getMin()
					- searchRetentionTimes[startScanIndex - 1];

			if (diffPrev < diffNext)
				return getMaxIntensity(startScanIndex - 1, mzRange, plotMode);
			else
				return getMaxIntensity(startScanIndex, mzRange, plotMode);
		}

		for (int scanIndex = startScanIndex; ((scanIndex < searchRetentionTimes.length) && (searchRetentionTimes[scanIndex] <= rtRange
				.getMax())); scanIndex++) {

			// ignore scans where all peaks are smaller than current max
			if (basePeaks[scanIndex] < maxIntensity)
				continue;

			double scanMax = getMaxIntensity(scanIndex, mzRange, plotMode);

			if (scanMax > maxIntensity)
				maxIntensity = scanMax;

		}

		return maxIntensity;

	}

	private double getMaxIntensity(int dataPointMatrixIndex, Range mzRange,
			PlotMode plotMode) {
		DataPoint dataPoints[] = dataPointMatrix[dataPointMatrixIndex].get();
		if (dataPoints == null) {
			Scan scan = rawDataFile.getScan(scanNumbers[dataPointMatrixIndex]);
			dataPoints = scan.getDataPoints();
			dataPointMatrix[dataPointMatrixIndex] = new SoftReference<DataPoint[]>(
					dataPoints);
		}
		return getMaxIntensity(dataPoints, mzRange, plotMode);
	}

	private double getMaxIntensity(DataPoint dataPoints[], Range mzRange,
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
		setStatus( TaskStatus.CANCELED );
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
		return status;
	}

	public String getTaskDescription() {
		return "Updating 2D visualizer of " + rawDataFile;
	}

	public Object[] getCreatedObjects() {
		return null;
	}

	/**
	 * Adds a TaskListener to this Task
	 * 
	 * @param t The TaskListener to add
	 */
	public void addTaskListener( TaskListener t ) {
		this.taskListeners.add( t );
	}

	/**
	 * Returns all of the TaskListeners which are listening to this task.
	 * 
	 * @return An array containing the TaskListeners
	 */
	public TaskListener[] getTaskListeners( ) {
		return this.taskListeners.toArray( new TaskListener[ this.taskListeners.size( )]);
	}

	private void fireTaskEvent( ) {
		TaskEvent event = new TaskEvent( this );
		for( TaskListener t : this.taskListeners ) {
			t.statusChanged( event );
		}
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#setStatus()
	 */
	public void setStatus( TaskStatus newStatus ) {
		this.status = newStatus;
		this.fireTaskEvent( );
	}

	public boolean isCanceled( ) {
		return status == TaskStatus.CANCELED;
	}

	public boolean isFinished( ) {
		return status == TaskStatus.FINISHED;
	}
}
