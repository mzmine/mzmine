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
import java.util.Date;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataAcceptor;
import net.sf.mzmine.io.util.RawDataRetrievalTask;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.util.DataPointSorterByMZ;

import org.jfree.data.xy.AbstractXYDataset;

/**
 * 
 */
class TwoDDataSet extends AbstractXYDataset implements RawDataAcceptor {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    // redraw the chart every 500 ms while updating
    private static final int REDRAW_INTERVAL = 500;

    private RawDataFile rawDataFile;
    private TwoDVisualizerWindow visualizer;

    private float retentionTimes[];
    private DataPoint dataPointMatrix[][];

    private float totalRTMin, totalRTMax, totalMZMin, totalMZMax;
    private int loadedScans = 0;

    private Date lastRedrawTime = new Date();

    TwoDDataSet(RawDataFile rawDataFile, int msLevel, float rtMin, float rtMax,
            float mzMin, float mzMax, TwoDVisualizerWindow visualizer) {

        this.rawDataFile = rawDataFile;
        this.visualizer = visualizer;

        totalRTMin = rtMin;
        totalRTMax = rtMax;
        totalMZMin = mzMin;
        totalMZMax = mzMax;

        int scanNumbers[] = rawDataFile.getScanNumbers(msLevel, rtMin, rtMax);
        assert scanNumbers != null;

        dataPointMatrix = new DataPoint[scanNumbers.length][];
        retentionTimes = new float[scanNumbers.length];

        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                "Updating 2D visualizer of " + rawDataFile, this);

        MZmineCore.getTaskController().addTask(updateTask, TaskPriority.HIGH,
                visualizer);

    }

    /**
     * @see net.sf.mzmine.io.RawDataAcceptor#addScan(net.sf.mzmine.data.Scan)
     */
    public void addScan(Scan scan, int index, int total) {

        retentionTimes[index] = scan.getRetentionTime();
        dataPointMatrix[index] = scan.getDataPoints(totalMZMin, totalMZMax);
        loadedScans++;

        // redraw every REDRAW_INTERVAL ms
        boolean notify = false;
        Date currentTime = new Date();
        if (currentTime.getTime() - lastRedrawTime.getTime() > REDRAW_INTERVAL) {
            notify = true;
            lastRedrawTime = currentTime;
        }

        // always redraw when we add last value
        if (index == total - 1) {
            notify = true;
        }

        if (notify) {
            fireDatasetChanged();
        }

    }

    /**
     * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesCount()
     */
    public int getSeriesCount() {
        if (loadedScans > 2)
            return 2;
        else
            return loadedScans;
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
            return retentionTimes[0];
        else
            return retentionTimes[loadedScans - 1];
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getY(int, int)
     */
    public Number getY(int series, int item) {
        if (item == 0)
            return dataPointMatrix[series][0].getMZ();
        else
            return dataPointMatrix[series][dataPointMatrix[series].length - 1].getMZ();
    }

    float getMaxIntensity(float rtMin, float rtMax, float mzMin, float mzMax) {

        float maxIntensity = 0;

        float searchRetentionTimes[] = retentionTimes;
        if (loadedScans < retentionTimes.length) {
            searchRetentionTimes = new float[loadedScans];
            System.arraycopy(retentionTimes, 0, searchRetentionTimes, 0,
                    searchRetentionTimes.length);
        }

        int startScanIndex = Arrays.binarySearch(searchRetentionTimes, rtMin);

        if (startScanIndex < 0)
            startScanIndex = (startScanIndex * -1) - 1;

        if (startScanIndex >= searchRetentionTimes.length) {
            return 0;
        }

        if (searchRetentionTimes[startScanIndex] > rtMax) {
            return getMaxIntensity(searchRetentionTimes[startScanIndex - 1],
                    searchRetentionTimes[startScanIndex - 1], mzMin, mzMax);
        }

        for (int scanIndex = startScanIndex; ((scanIndex < searchRetentionTimes.length) && (searchRetentionTimes[scanIndex] <= rtMax)); scanIndex++) {

            DataPoint dataPoints[] = dataPointMatrix[scanIndex];
            DataPoint searchMZ = new SimpleDataPoint(mzMin, 0);
            int startMZIndex = Arrays.binarySearch(dataPoints, searchMZ,
                    new DataPointSorterByMZ());
            if (startMZIndex < 0)
                startMZIndex = (startMZIndex * -1) - 1;

            for (int mzIndex = startMZIndex; ((mzIndex < dataPoints.length) && (dataPoints[mzIndex].getMZ() <= mzMax)); mzIndex++) {
                if (dataPoints[mzIndex].getIntensity() > maxIntensity)
                    maxIntensity = dataPoints[mzIndex].getIntensity();
            }

        }

        return maxIntensity;

    }

}