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

package net.sf.mzmine.modules.visualization.twod;

import java.util.Arrays;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.util.DataPointSorterByMZ;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.RawDataAcceptor;
import net.sf.mzmine.util.RawDataRetrievalTask;

import org.jfree.data.xy.AbstractXYDataset;

/**
 * 
 */
class TwoDDataSet extends AbstractXYDataset implements RawDataAcceptor {

    private RawDataFile rawDataFile;

    private float retentionTimes[];
    private float basePeaks[];
    private DataPoint dataPointMatrix[][];

    private Range totalRTRange, totalMZRange;
    private int loadedScans = 0;

    TwoDDataSet(RawDataFile rawDataFile, int msLevel, Range rtRange,
            Range mzRange, TwoDVisualizerWindow visualizer) {

        this.rawDataFile = rawDataFile;

        totalRTRange = rtRange;
        totalMZRange = mzRange;

        int scanNumbers[] = rawDataFile.getScanNumbers(msLevel, rtRange);
        assert scanNumbers != null;

        dataPointMatrix = new DataPoint[scanNumbers.length][];
        retentionTimes = new float[scanNumbers.length];
        basePeaks = new float[scanNumbers.length];

        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                "Updating 2D visualizer of " + rawDataFile, this);

        MZmineCore.getTaskController().addTask(updateTask, TaskPriority.HIGH,
                visualizer);

    }

    /**
     * @see net.sf.mzmine.io.RawDataAcceptor#addScan(net.sf.mzmine.data.Scan)
     */
    public void addScan(Scan scan, int index, int total) {

        DataPoint scanBasePeak = scan.getBasePeak();
        retentionTimes[index] = scan.getRetentionTime();
        basePeaks[index] = (scanBasePeak == null ? 0 : scanBasePeak.getIntensity());
        dataPointMatrix[index] = scan.getDataPoints(totalMZRange);
        loadedScans++;

        // redraw when we add last value
        if (index == total - 1) {
            fireDatasetChanged();
        }

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

    float getMaxIntensity(Range rtRange, Range mzRange, PlotMode plotMode) {

        float maxIntensity = 0;

        float searchRetentionTimes[] = retentionTimes;
        if (loadedScans < retentionTimes.length) {
            searchRetentionTimes = new float[loadedScans];
            System.arraycopy(retentionTimes, 0, searchRetentionTimes, 0,
                    searchRetentionTimes.length);
        }

        int startScanIndex = Arrays.binarySearch(searchRetentionTimes,
                rtRange.getMin());

        if (startScanIndex < 0)
            startScanIndex = (startScanIndex * -1) - 1;

        if (startScanIndex >= searchRetentionTimes.length) {
            return 0;
        }

        if (searchRetentionTimes[startScanIndex] > rtRange.getMax()) {
            if (startScanIndex == 0)
                return 0;

            if (startScanIndex == searchRetentionTimes.length - 1)
                return getMaxIntensity(dataPointMatrix[startScanIndex - 1],
                        mzRange, plotMode);

            // find which scan point is closer
            float diffNext = searchRetentionTimes[startScanIndex]
                    - rtRange.getMax();
            float diffPrev = rtRange.getMin()
                    - searchRetentionTimes[startScanIndex - 1];

            if (diffPrev < diffNext)
                return getMaxIntensity(dataPointMatrix[startScanIndex - 1],
                        mzRange, plotMode);
            else
                return getMaxIntensity(dataPointMatrix[startScanIndex], mzRange, plotMode);
        }

        for (int scanIndex = startScanIndex; ((scanIndex < searchRetentionTimes.length) && (searchRetentionTimes[scanIndex] <= rtRange.getMax())); scanIndex++) {

            // ignore scans where all peaks are smaller than current max
            if (basePeaks[scanIndex] < maxIntensity)
                continue;

            float scanMax = getMaxIntensity(dataPointMatrix[scanIndex], mzRange, plotMode);
            if (scanMax > maxIntensity)
                maxIntensity = scanMax;

        }

        return maxIntensity;

    }

    float getMaxIntensity(DataPoint dataPoints[], Range mzRange, PlotMode plotMode) {

        float maxIntensity = 0;

        DataPoint searchMZ = new SimpleDataPoint(mzRange.getMin(), 0);
        int startMZIndex = Arrays.binarySearch(dataPoints, searchMZ,
                new DataPointSorterByMZ());
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
	            float diffNext = dataPoints[startMZIndex].getMZ()
	                    - mzRange.getMax();
	            float diffPrev = mzRange.getMin()
	                    - dataPoints[startMZIndex - 1].getMZ();
	
	            if (diffPrev < diffNext)
	                return dataPoints[startMZIndex - 1].getIntensity();
	            else
	                return dataPoints[startMZIndex].getIntensity();
        	} else {
        		return 0;
        	}

        }

        for (int mzIndex = startMZIndex; ((mzIndex < dataPoints.length) && (dataPoints[mzIndex].getMZ() <= mzRange.getMax())); mzIndex++) {
            if (dataPoints[mzIndex].getIntensity() > maxIntensity)
                maxIntensity = dataPoints[mzIndex].getIntensity();
        }

        return maxIntensity;

    }
}