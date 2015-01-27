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

package net.sf.mzmine.modules.visualization.neutralloss;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;

import com.google.common.collect.Range;

class NeutralLossDataSet extends AbstractXYDataset implements Task,
	XYToolTipGenerator {

    private static final long serialVersionUID = 1L;

    private RawDataFile rawDataFile;

    private Range<Double> totalMZRange;
    private int numOfFragments;
    private Object xAxisType;
    private int scanNumbers[], totalScans, processedScans;

    private TaskStatus status = TaskStatus.WAITING;

    private HashMap<Integer, Vector<NeutralLossDataPoint>> dataSeries;

    private NeutralLossVisualizerWindow visualizer;

    private static int RAW_LEVEL = 0;
    private static int PRECURSOR_LEVEL = 1;
    private static int NEUTRALLOSS_LEVEL = 2;

    NeutralLossDataSet(RawDataFile rawDataFile, Object xAxisType,
	    Range<Double> rtRange, Range<Double> mzRange, int numOfFragments,
	    NeutralLossVisualizerWindow visualizer) {

	this.rawDataFile = rawDataFile;

	totalMZRange = mzRange;
	this.numOfFragments = numOfFragments;
	this.xAxisType = xAxisType;
	this.visualizer = visualizer;

	// get MS/MS scans
	scanNumbers = rawDataFile.getScanNumbers(2, rtRange);

	totalScans = scanNumbers.length;

	dataSeries = new HashMap<Integer, Vector<NeutralLossDataPoint>>();

	dataSeries.put(RAW_LEVEL, new Vector<NeutralLossDataPoint>(totalScans));
	dataSeries.put(PRECURSOR_LEVEL, new Vector<NeutralLossDataPoint>(
		totalScans));
	dataSeries.put(NEUTRALLOSS_LEVEL, new Vector<NeutralLossDataPoint>(
		totalScans));

    }

    public void run() {

	setStatus(TaskStatus.PROCESSING);
	processedScans = 0;

	for (int scanNumber : scanNumbers) {

	    // Cancel?
	    if (status == TaskStatus.CANCELED)
		return;

	    Scan scan = rawDataFile.getScan(scanNumber);

	    // check parent m/z
	    if (!totalMZRange.contains(scan.getPrecursorMZ())) {
		continue;
	    }

	    // get m/z and intensity values
	    DataPoint scanDataPoints[] = scan.getDataPoints();

	    // skip empty scans
	    if (scan.getHighestDataPoint() == null) {
		processedScans++;
		continue;
	    }

	    // topPeaks will contain indexes to mzValues peaks of top intensity
	    int topPeaks[] = new int[numOfFragments];
	    Arrays.fill(topPeaks, -1);

	    for (int i = 0; i < scanDataPoints.length; i++) {

		fragmentsCycle: for (int j = 0; j < numOfFragments; j++) {

		    // Cancel?
		    if (status == TaskStatus.CANCELED)
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

	    }

	    // add the data points
	    for (int i = 0; i < topPeaks.length; i++) {

		int peakIndex = topPeaks[i];

		// if we have a very few peaks, the array may not be full
		if (peakIndex < 0)
		    break;

		NeutralLossDataPoint newPoint = new NeutralLossDataPoint(
			scanDataPoints[peakIndex].getMZ(),
			scan.getScanNumber(), 
			scan.getPrecursorMZ(), scan.getPrecursorCharge(),
			scan.getRetentionTime());

		dataSeries.get(0).add(newPoint);

	    }

	    processedScans++;

	}

	fireDatasetChanged();
	setStatus(TaskStatus.FINISHED);

    }

    public void updateOnRangeDataPoints(String rangeType) {

	NeutralLossPlot plot = visualizer.getPlot();
	Range<Double> prRange = plot.getHighlightedPrecursorRange();
	Range<Double> nlRange = plot.getHighlightedNeutralLossRange();

	// Set type of search
	int level = NEUTRALLOSS_LEVEL;
	if (rangeType.equals("HIGHLIGHT_PRECURSOR"))
	    level = PRECURSOR_LEVEL;

	// Clean previous selection
	dataSeries.get(level).clear();

	NeutralLossDataPoint point;
	boolean b = false;
	for (int i = 0; i < dataSeries.get(RAW_LEVEL).size(); i++) {
	    point = dataSeries.get(RAW_LEVEL).get(i);
	    // Verify if the point is on range
	    if (level == PRECURSOR_LEVEL)
		b = prRange.contains(point.getPrecursorMass());
	    else
		b = nlRange.contains(point.getNeutralLoss());
	    if (b)
		dataSeries.get(level).add(point);
	}

	fireDatasetChanged();
    }

    /**
     * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesCount()
     */
    public int getSeriesCount() {
	return dataSeries.size();
    }

    /**
     * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesKey(int)
     */
    public Comparable<Integer> getSeriesKey(int series) {
	return series;
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getItemCount(int)
     */
    public int getItemCount(int series) {
	return dataSeries.get(series).size();
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getX(int, int)
     */
    public Number getX(int series, int item) {
	NeutralLossDataPoint point = dataSeries.get(series).get(item);
	if (xAxisType.equals(NeutralLossParameters.xAxisPrecursor)) {
	    double mz = point.getPrecursorMass();
	    return mz;
	} else
	    return point.getRetentionTime();

    }

    /**
     * @see org.jfree.data.xy.XYDataset#getY(int, int)
     */
    public Number getY(int series, int item) {
	NeutralLossDataPoint point = dataSeries.get(series).get(item);
	return point.getNeutralLoss();
    }

    public NeutralLossDataPoint getDataPoint(int item) {
	return dataSeries.get(RAW_LEVEL).get(item);
    }

    public NeutralLossDataPoint getDataPoint(double xValue, double yValue) {
	Vector<NeutralLossDataPoint> dataCopy = new Vector<NeutralLossDataPoint>(
		dataSeries.get(RAW_LEVEL));
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
	return dataSeries.get(series).get(item).getName();
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
	return "Updating neutral loss visualizer of " + rawDataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#setStatus()
     */
    public void setStatus(TaskStatus newStatus) {
	this.status = newStatus;
    }

    public boolean isCanceled() {
	return status == TaskStatus.CANCELED;
    }

}
