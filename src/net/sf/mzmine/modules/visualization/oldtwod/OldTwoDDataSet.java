/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.oldtwod;

import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataAcceptor;
import net.sf.mzmine.io.util.RawDataRetrievalTask;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.ScanUtils.BinningType;

/**
 * 
 */
public class OldTwoDDataSet implements RawDataAcceptor {

	public static final int NO_DATA = 0;
	public static final int LOADING_DATA = 1;
	public static final int DATA_READY = 2;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private RawDataFile rawDataFile;

	private OldTwoDVisualizerWindow visualizer;

	private float intensityMatrix[][];
	private int mzResolution, rtResolution;

	private ArrayList<Vector<Integer>> scanNumbers;

	private boolean interpolate;

	// bounds for rendered data range
	private double rtMin, rtMax, rtStep;
	private double mzMin, mzMax, mzStep;
	private int msLevel;

	// max intensity in current image
	private float maxIntensity;

	private int previousXIndex;

	private Task currentTask;

	public OldTwoDDataSet(RawDataFile rawDataFile,
			OldTwoDVisualizerWindow visualizer) {

		this.rawDataFile = rawDataFile;
		this.visualizer = visualizer;

	}

	public void resampleIntensityMatrix(boolean interpolate) {
		resampleIntensityMatrix(this.msLevel, this.rtMin, this.rtMax,
				this.mzMin, this.mzMax, this.rtResolution, this.mzResolution,
				interpolate);
	}

	public void resampleIntensityMatrix(int msLevel, double desiredRTMin,
			double desiredRTMax, double mzMin, double mzMax,
			int desiredRTResolution, int mzResolution, boolean interpolate) {

		this.msLevel = msLevel;
		this.mzMin = mzMin;
		this.mzMax = mzMax;
		this.mzResolution = mzResolution;
		this.interpolate = interpolate;

		// Pickup scan number within given rt range
		int[] scanNumbersForRetrieval = rawDataFile.getScanNumbers(msLevel,
				(float) desiredRTMin, (float) desiredRTMax);

		// Adjust rt resolution if there are less scans that desired resolution
		rtResolution = desiredRTResolution;
		if (scanNumbersForRetrieval.length < rtResolution)
			rtResolution = scanNumbersForRetrieval.length;

		// Find minimum and maximum RT of scans within range
		rtMin = Double.MAX_VALUE;
		rtMax = Double.MIN_VALUE;
		for (int scanNumber : scanNumbersForRetrieval) {
			double rt = rawDataFile.getScan(scanNumber).getRetentionTime();
			if (rtMin > rt)
				rtMin = rt;
			if (rtMax < rt)
				rtMax = rt;
		}

		// Initialize intensity matrix
		intensityMatrix = new float[rtResolution][mzResolution];

		scanNumbers = new ArrayList<Vector<Integer>>();
		for (int xIndex = 0; xIndex < rtResolution; xIndex++)
			scanNumbers.add(new Vector<Integer>());

		// Bin sizes
		this.rtStep = (rtMax - rtMin) / (double) rtResolution;
		this.mzStep = (mzMax - mzMin) / (double) mzResolution;

		currentTask = new RawDataRetrievalTask(rawDataFile,
				scanNumbersForRetrieval, "Updating 2D visualizer of "
						+ rawDataFile, this);

		maxIntensity = 0.0f;
		
		previousXIndex = 0;

		MZmineCore.getTaskController().addTask(currentTask, TaskPriority.HIGH,
				visualizer);

	}

	/**
	 * @see net.sf.mzmine.io.RawDataAcceptor#addScan(net.sf.mzmine.data.Scan)
	 */

	public synchronized void addScan(Scan scan, int index, int total) {

		int bitmapSizeX, bitmapSizeY;

		bitmapSizeX = intensityMatrix.length;
		bitmapSizeY = intensityMatrix[0].length;

		if ((scan.getRetentionTime() < rtMin)
				|| (scan.getRetentionTime() > rtMax))
			return;

		int xIndex = (int) Math
				.floor(((double) scan.getRetentionTime() - (double) rtMin)
						/ (double) rtStep);
		assert (xIndex <= scanNumbers.size());
		if (xIndex == scanNumbers.size())
			xIndex--;

		// If there is an empty gap in the bitmap between two scans, then fill
		// it with previous data
		if (xIndex > (previousXIndex + 1)) {
			
			for (int tmpXIndex = (previousXIndex + 1); tmpXIndex < xIndex; tmpXIndex++) {
				for (int i = 0; i < bitmapSizeY; i++)
					intensityMatrix[tmpXIndex][i] = intensityMatrix[previousXIndex][i];

			}
		}
		previousXIndex = xIndex;

		Vector<Integer> scanNumbersForXIndex = scanNumbers.get(xIndex);
		scanNumbersForXIndex.add(scan.getScanNumber());

		DataPoint dataPoints[] = scan.getDataPoints();
		float[] mzValues = new float[dataPoints.length];
		float[] intensityValues = new float[dataPoints.length];
		for (int dp = 0; dp < dataPoints.length; dp++) {
			mzValues[dp] = dataPoints[dp].getMZ();
			intensityValues[dp] = dataPoints[dp].getIntensity();
		}

		float binnedIntensities[] = ScanUtils.binValues(mzValues,
				intensityValues, (float) mzMin, (float) mzMax, bitmapSizeY,
				interpolate, BinningType.SUM);

		for (int i = 0; i < bitmapSizeY; i++) {

			if (intensityMatrix[xIndex][bitmapSizeY - i - 1]<binnedIntensities[i]) 
				intensityMatrix[xIndex][bitmapSizeY - i - 1] = binnedIntensities[i];

			if (intensityMatrix[xIndex][bitmapSizeY - i - 1] > maxIntensity)
				maxIntensity = intensityMatrix[xIndex][bitmapSizeY - i - 1];

		}

		
		if (index >= (total - 1))
			visualizer.datasetUpdateReady();
		else
			visualizer.datasetUpdating();

	}

	public float[][] getIntensityMatrix() {
		return intensityMatrix;
	}

	/**
	 * Return the smallest scan number mapped to xIndex
	 */
	public Integer getScanNumber(int xIndex) {
		Vector<Integer> scanNumbersForXIndex = scanNumbers.get(xIndex);
		if (scanNumbersForXIndex == null)
			return null;
		if (scanNumbersForXIndex.size() == 0)
			return null;
		Integer minValue = scanNumbersForXIndex.get(0);
		for (Integer value : scanNumbersForXIndex)
			if (value < minValue)
				minValue = value;
		return minValue;
	}

	/**
	 * Returns all scan numbers mapped to xIndex
	 */
	public Integer[] getScanNumbers(int xIndex) {
		Vector<Integer> scanNumbersForXIndex = scanNumbers.get(xIndex);
		if (scanNumbersForXIndex == null)
			return null;
		return scanNumbersForXIndex.toArray(new Integer[0]);
	}

	public boolean isInterpolated() {
		return interpolate;
	}

	public int getMSLevel() {
		return msLevel;
	}

	public float getMinRT() {
		return (float) rtMin;
	}

	public float getMaxRT() {
		return (float) rtMax;
	}

	public float getMinMZ() {
		return (float) mzMin;
	}

	public float getMaxMZ() {
		return (float) mzMax;
	}

	public float getMaxIntensity() {
		return maxIntensity;
	}

	public int getStatus() {
		if (currentTask == null)
			return NO_DATA;
		if ((currentTask.getStatus() == Task.TaskStatus.FINISHED)
				|| (currentTask.getStatus() == Task.TaskStatus.CANCELED)
				|| (currentTask.getStatus() == Task.TaskStatus.ERROR))
			return DATA_READY;

		if ((currentTask.getStatus() == Task.TaskStatus.PROCESSING)
				|| (currentTask.getStatus() == Task.TaskStatus.WAITING))
			return LOADING_DATA;

		return LOADING_DATA;
	}

	public RawDataFile getRawDataFile() {
		return rawDataFile;
	}
}