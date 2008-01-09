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

package net.sf.mzmine.modules.visualization.tic;

import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataAcceptor;
import net.sf.mzmine.io.util.RawDataRetrievalTask;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.ScanUtils;

import org.jfree.data.xy.AbstractXYZDataset;

/**
 * TIC visualizer data set
 */
class TICDataSet extends AbstractXYZDataset implements RawDataAcceptor {

    // redraw the chart every 100 ms while updating
    private static final int REDRAW_INTERVAL = 100;
    private static Date lastRedrawTime = new Date();

    private TICVisualizerWindow visualizer;
    private RawDataFile dataFile;

    private int scanNumbers[], loadedScans = 0;
    private float basePeakValues[], intensityValues[], rtValues[];
    private float mzMin, mzMax;
    private float intensityMin, intensityMax;

    TICDataSet(RawDataFile dataFile, int scanNumbers[], float mzMin,
            float mzMax, TICVisualizerWindow visualizer) {

        this.visualizer = visualizer;
        this.mzMin = mzMin;
        this.mzMax = mzMax;
        this.dataFile = dataFile;
        this.scanNumbers = scanNumbers;

        basePeakValues = new float[scanNumbers.length];
        intensityValues = new float[scanNumbers.length];
        rtValues = new float[scanNumbers.length];

        // Start-up the refresh task
        Task updateTask = new RawDataRetrievalTask(dataFile, scanNumbers,
                "Updating TIC visualizer of " + dataFile, this);
        MZmineCore.getTaskController().addTask(updateTask, TaskPriority.HIGH,
                visualizer);

    }

    /**
     * Returns index of data point which exactly matches given X and Y values
     * 
     * @param retentionTime
     * @param intensity
     * @return
     */
    int getIndex(float retentionTime, float intensity) {
        for (int i = 0; i < loadedScans; i++) {
            if ((Math.abs(retentionTime - rtValues[i]) < 0.0000001f)
                    && (Math.abs(intensity - intensityValues[i]) < 0.0000001f))
                return i;
        }
        return -1;
    }

    int getScanNumber(int index) {
        return scanNumbers[index];
    }

    RawDataFile getDataFile() {
        return dataFile;
    }

    /**
     * @see net.sf.mzmine.io.RawDataAcceptor#addScan(net.sf.mzmine.data.Scan)
     */
    public void addScan(final Scan scan, int index, int total) {

        float totalIntensity = 0;

        if (visualizer.getPlotType() == TICVisualizerParameters.plotTypeTIC) {
            if ((mzMin <= scan.getMZRangeMin())
                    && (mzMax >= scan.getMZRangeMax())) {
                totalIntensity = scan.getTIC();
            } else {
                float intensityValues[] = scan.getIntensityValues();
                float scanMzValues[] = scan.getMZValues();
                for (int j = 0; j < intensityValues.length; j++) {
                    if ((scanMzValues[j] >= mzMin)
                            && (scanMzValues[j] <= mzMax))
                        totalIntensity += intensityValues[j];
                }
            }
            basePeakValues[index] = scan.getBasePeakMZ();
        }

        if (visualizer.getPlotType() == TICVisualizerParameters.plotTypeBP) {
            if ((mzMin <= scan.getMZRangeMin())
                    && (mzMax >= scan.getMZRangeMax())) {
                basePeakValues[index] = scan.getBasePeakMZ();
                totalIntensity = scan.getBasePeakIntensity();
            } else {
                float basePeak[] = ScanUtils.findBasePeak(scan, mzMin, mzMax);
                if (basePeak != null) {
                    basePeakValues[index] = basePeak[0];
                    totalIntensity = basePeak[1];
                } else {
                    totalIntensity = 0;
                }
            }

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

        loadedScans++;

        // redraw every REDRAW_INTERVAL ms
        boolean notify = false;
        Date currentTime = new Date();
        if (currentTime.getTime() - lastRedrawTime.getTime() > REDRAW_INTERVAL) {
            notify = true;
            lastRedrawTime = currentTime;
        }

        // always redraw when we add last value
        if (index == total - 1)
            notify = true;

        if (notify)
            this.fireDatasetChanged();

    }

    @Override public int getSeriesCount() {
        return 1;
    }

    @Override public Comparable getSeriesKey(int series) {
        return dataFile.toString();
    }

    public Number getZ(int series, int item) {
        return basePeakValues[item];
    }

    public int getItemCount(int series) {
        return loadedScans;
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
        if ((item <= 0) || (item >= loadedScans - 1))
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
    public int[] findLocalMaxima(float xMin, float xMax, float yMin, float yMax) {

        // save data set size
        final int currentSize = loadedScans;
        float rtCopy[];

        // if the RT values array is not filled yet, create a shrinked copy
        if (currentSize < rtValues.length) {
            rtCopy = new float[currentSize];
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

    public float getMinIntensity() {
        return intensityMin;
    }

    public float getMaxIntensity() {
        return intensityMax;
    }

}
