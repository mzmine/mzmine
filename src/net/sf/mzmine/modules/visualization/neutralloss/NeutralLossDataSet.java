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

package net.sf.mzmine.modules.visualization.neutralloss;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.Range;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * 
 */
class NeutralLossDataSet extends AbstractXYDataset implements Task,
		XYToolTipGenerator {

	private RawDataFile rawDataFile;

	private Range totalMZRange;
	private int numOfFragments;
	private Object xAxisType;
	private int scanNumbers[], totalScans, processedScans;

	private TaskStatus taskStatus = TaskStatus.WAITING;

	private Vector<NeutralLossDataPoint> dataPoints;

	NeutralLossDataSet(RawDataFile rawDataFile, Object xAxisType,
			Range rtRange, Range mzRange, int numOfFragments,
			NeutralLossVisualizerWindow visualizer) {

		this.rawDataFile = rawDataFile;

		totalMZRange = mzRange;
		this.numOfFragments = numOfFragments;
		this.xAxisType = xAxisType;

		// get MS/MS scans
		scanNumbers = rawDataFile.getScanNumbers(2, rtRange);
		
		totalScans = scanNumbers.length;

		dataPoints = new Vector<NeutralLossDataPoint>(totalScans);

		MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);

	}

	/**
     */
	public void run() {

		taskStatus = TaskStatus.PROCESSING;
		
		for (int scanNumber : scanNumbers) {
			
			// Cancel?
			if (taskStatus == TaskStatus.CANCELED)
				return;
			
			Scan scan = rawDataFile.getScan(scanNumber);

			// check parent m/z
			if (!totalMZRange.contains(scan.getPrecursorMZ()))
				return;

			// get m/z and intensity values
			DataPoint scanDataPoints[] = scan.getDataPoints();

			// skip empty scans
			if (scan.getBasePeak() == null)
				return;

			// topPeaks will contain indexes to mzValues peaks of top intensity
			int topPeaks[] = new int[numOfFragments];
			Arrays.fill(topPeaks, -1);

			for (int i = 0; i < scanDataPoints.length; i++) {

				fragmentsCycle: for (int j = 0; j < numOfFragments; j++) {
					
					// Cancel?
					if (taskStatus == TaskStatus.CANCELED)
						return;

					if ((topPeaks[j] < 0)
							|| (scanDataPoints[i].getIntensity()) > scanDataPoints[topPeaks[j]]
									.getIntensity()) {

						// shift the top peaks array
						for (int k = numOfFragments - 1; k > j; k--)
							topPeaks[k] = topPeaks[k - 1];

						// add the peak to the appropriate place
						topPeaks[j] = i;

						break fragmentsCycle;
					}
				}

			processedScans++;
			
			}

			// add the data points
			for (int i = 0; i < topPeaks.length; i++) {

				int peakIndex = topPeaks[i];

				// if we have a very few peaks, the array may not be full
				if (peakIndex < 0)
					break;

				NeutralLossDataPoint newPoint = new NeutralLossDataPoint(
						scanDataPoints[peakIndex].getMZ(),
						scan.getScanNumber(), scan.getParentScanNumber(), scan
								.getPrecursorMZ(), scan.getPrecursorCharge(),
						scan.getRetentionTime());

				dataPoints.add(newPoint);

			}
		}

		fireDatasetChanged();
		
		taskStatus = TaskStatus.FINISHED;

	}

	/**
	 * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesCount()
	 */
	public int getSeriesCount() {
		return 1;
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
		return dataPoints.size();
	}

	/**
	 * @see org.jfree.data.xy.XYDataset#getX(int, int)
	 */
	public Number getX(int series, int item) {
		if (xAxisType == NeutralLossParameters.xAxisPrecursor)
			return dataPoints.get(item).getPrecursorMass();
		else
			return dataPoints.get(item).getRetentionTime();

	}

	/**
	 * @see org.jfree.data.xy.XYDataset#getY(int, int)
	 */
	public Number getY(int series, int item) {
		return dataPoints.get(item).getNeutralLoss();
	}

	public NeutralLossDataPoint getDataPoint(int item) {
		return dataPoints.get(item);
	}

	public NeutralLossDataPoint getDataPoint(double xValue, double yValue) {
		Vector<NeutralLossDataPoint> dataCopy = new Vector<NeutralLossDataPoint>(
				dataPoints);
		Iterator<NeutralLossDataPoint> it = dataCopy.iterator();
		double currentX, currentY;
		while (it.hasNext()) {
			NeutralLossDataPoint point = it.next();
			if (xAxisType == NeutralLossParameters.xAxisPrecursor)
				currentX = point.getPrecursorMass();
			else
				currentX = point.getRetentionTime();
			currentY = point.getNeutralLoss();
			// check for equality
			if ((Math.abs(currentX - xValue) < 0.00000001)
					&& (Math.abs(currentY - yValue) < 0.00000001))
				return point;
		}
		return null;
	}

	/**
	 * @see org.jfree.chart.labels.XYToolTipGenerator#generateToolTip(org.jfree.data.xy.XYDataset,
	 *      int, int)
	 */
	public String generateToolTip(XYDataset dataset, int series, int item) {
		return dataPoints.get(item).toString();
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
		else
			return ((double) processedScans / totalScans);
	}

	public TaskStatus getStatus() {
		return taskStatus;
	}

	public String getTaskDescription() {
		return "Updating neutral loss visualizer of " + rawDataFile;
	}

}