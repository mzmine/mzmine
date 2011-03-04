/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.tic;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskEvent;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

import org.jfree.data.xy.AbstractXYZDataset;

/**
 * TIC visualizer data set, one data set is created per each file shown in this
 * visualizer. We need to create separate data set for each file, because user
 * may add/remove files later.
 */
public class TICDataSet extends AbstractXYZDataset implements Task {

	// redraw the chart every 100 ms while updating
	private static final int REDRAW_INTERVAL = 100;
	private static Date lastRedrawTime = new Date();

	private TICVisualizerWindow visualizer;
	private RawDataFile dataFile;

	private int scanNumbers[], totalScans, processedScans;

	private TaskStatus status = TaskStatus.WAITING;

	private double basePeakValues[], intensityValues[], rtValues[];
	private Range mzRange;
	private double intensityMin, intensityMax;
	private LinkedList<TaskListener> taskListeners = new LinkedList<TaskListener>();

	public TICDataSet(RawDataFile dataFile, int scanNumbers[], Range mzRange,
			TICVisualizerWindow visualizer) {

		this.visualizer = visualizer;
		this.mzRange = mzRange;
		this.dataFile = dataFile;
		this.scanNumbers = scanNumbers;

		totalScans = scanNumbers.length;

		basePeakValues = new double[scanNumbers.length];
		intensityValues = new double[scanNumbers.length];
		rtValues = new double[scanNumbers.length];

		// Start-up the refresh task
		MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);

	}

	/**
	 * Returns index of data point which exactly matches given X and Y values
	 * 
	 * @param retentionTime
	 * @param intensity
	 * @return
	 */
	public int getIndex(double retentionTime, double intensity) {
		for (int i = 0; i < processedScans; i++) {
			if ((Math.abs(retentionTime - rtValues[i]) < 0.0000001)
					&& (Math.abs(intensity - intensityValues[i]) < 0.0000001))
				return i;
		}
		return -1;
	}

	int getScanNumber(int series, int item) {
		return scanNumbers[item];
	}

	RawDataFile getDataFile() {
		return dataFile;
	}

	/**
     */
	public void run() {

		setStatus(TaskStatus.PROCESSING);

		for (int index = 0; index < scanNumbers.length; index++) {

			// Cancel?
			if (status == TaskStatus.CANCELED)
				return;

			Scan scan = dataFile.getScan(scanNumbers[index]);

			double totalIntensity = 0;
			DataPoint basePeak = null;

			if (scan.getMZRange().isWithin(mzRange)) {
				basePeak = scan.getBasePeak();
			} else {
				basePeak = ScanUtils.findBasePeak(scan, mzRange);
			}

			if (basePeak != null)
				basePeakValues[index] = basePeak.getMZ();

			PlotType plotType;

			if (visualizer != null)
				plotType = visualizer.getPlotType();
			else
				plotType = PlotType.BASEPEAK;

			if (plotType == PlotType.TIC) {
				if (scan.getMZRange().isWithin(mzRange)) {
					totalIntensity = scan.getTIC();
				} else {
					DataPoint dataPoints[] = scan.getDataPointsByMass(mzRange);
					for (int j = 0; j < dataPoints.length; j++) {
						totalIntensity += dataPoints[j].getIntensity();
					}
				}

			}

			if (plotType == PlotType.BASEPEAK) {
				if (basePeak != null)
					totalIntensity = basePeak.getIntensity();
			}

			if (index == 0) {
				intensityMin = totalIntensity;
				intensityMax = totalIntensity;
			} else {
				if (totalIntensity < intensityMin)
					intensityMin = totalIntensity;
				if (totalIntensity > intensityMax)
					intensityMax = totalIntensity;
			}

			intensityValues[index] = totalIntensity;
			rtValues[index] = scan.getRetentionTime();

			processedScans++;

			// redraw every REDRAW_INTERVAL ms

			Date currentTime = new Date();
			if (currentTime.getTime() - lastRedrawTime.getTime() > REDRAW_INTERVAL) {
				fireDatasetChanged();
				lastRedrawTime = currentTime;
			}

		}

		// always redraw when we add last value
		fireDatasetChanged();

		if (visualizer != null) {
			visualizer.actionPerformed(new ActionEvent(this,
					ActionEvent.ACTION_PERFORMED, "TICDataSet_upgraded"));
		}

		setStatus(TaskStatus.FINISHED);

	}

	@Override
	public int getSeriesCount() {
		return 1;
	}

	@Override
	public Comparable getSeriesKey(int series) {
		return dataFile.toString();
	}

	public Number getZ(int series, int item) {
		return basePeakValues[item];
	}

	public int getItemCount(int series) {
		return processedScans;
	}

	public Number getX(int series, int item) {
		return rtValues[item];
	}

	public Number getY(int series, int item) {
		return intensityValues[item];
	}

	/**
	 * Checks if given data point is local maximum
	 */
	public boolean isLocalMaximum(int item) {
		if ((item <= 0) || (item >= processedScans - 1))
			return false;
		if (intensityValues[item - 1] > intensityValues[item])
			return false;
		if (intensityValues[item + 1] > intensityValues[item])
			return false;
		return true;
	}

	/**
	 * Gets indexes of local maxima within given range
	 */
	public int[] findLocalMaxima(double xMin, double xMax, double yMin,
			double yMax) {

		// save data set size
		final int currentSize = processedScans;
		double rtCopy[];

		// if the RT values array is not filled yet, create a shrinked copy
		if (currentSize < rtValues.length) {
			rtCopy = new double[currentSize];
			System.arraycopy(rtValues, 0, rtCopy, 0, currentSize);
		} else {
			rtCopy = rtValues;
		}

		int startIndex = Arrays.binarySearch(rtCopy, xMin);
		if (startIndex < 0)
			startIndex = (startIndex * -1) - 1;

		Vector<Integer> indices = new Vector<Integer>();

		for (int index = startIndex; (index < rtCopy.length)
				&& (rtCopy[index] <= xMax); index++) {

			// check Y range
			if ((intensityValues[index] < yMin)
					|| (intensityValues[index] > yMax))
				continue;

			if (!isLocalMaximum(index))
				continue;

			indices.add(index);
		}

		int indexArray[] = CollectionUtils.toIntArray(indices);

		return indexArray;

	}

	public double getMinIntensity() {
		return intensityMin;
	}

	public double getMaxIntensity() {
		return intensityMax;
	}

	public void cancel() {
		setStatus(TaskStatus.CANCELED);
	}

	public String getErrorMessage() {
		return null;
	}

	public double getFinishedPercentage() {
		if (totalScans == 0)
			return 0;
		else
			return ((double) processedScans / totalScans);
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return "Updating TIC visualizer of " + dataFile;
	}

	public Object[] getCreatedObjects() {
		return null;
	}

	/**
	 * Adds a TaskListener to this Task
	 * 
	 * @param t
	 *            The TaskListener to add
	 */
	public void addTaskListener(TaskListener t) {
		this.taskListeners.add(t);
	}

	/**
	 * Returns all of the TaskListeners which are listening to this task.
	 * 
	 * @return An array containing the TaskListeners
	 */
	public TaskListener[] getTaskListeners() {
		return this.taskListeners.toArray(new TaskListener[this.taskListeners
				.size()]);
	}

	private void fireTaskEvent() {
		TaskEvent event = new TaskEvent(this);
		for (TaskListener t : this.taskListeners) {
			t.statusChanged(event);
		}
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#setStatus()
	 */
	public void setStatus(TaskStatus newStatus) {
		this.status = newStatus;
		this.fireTaskEvent();
	}

	public boolean isCanceled() {
		return status == TaskStatus.CANCELED;
	}

	public boolean isFinished() {
		return status == TaskStatus.FINISHED;
	}
}
