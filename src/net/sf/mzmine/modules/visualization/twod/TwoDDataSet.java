/*
 * Copyright 2006-2007 The MZmine Development Team
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

import java.util.Date;
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

    private float intensityMatrix[][];
    private float maxValue = 0;

    private float totalRTMin, totalRTMax, totalMZMin, totalMZMax;
    private float totalRTStep, totalMZStep;

    private Date lastRedrawTime = new Date();

    TwoDDataSet(RawDataFile rawDataFile, int msLevel, float rtMin, float rtMax,
            float mzMin, float mzMax, int rtResolution, int mzResolution,
            TwoDVisualizerWindow visualizer) {

        this.rawDataFile = rawDataFile;
        this.visualizer = visualizer;

        totalRTMin = rtMin;
        totalRTMax = rtMax;
        totalMZMin = mzMin;
        totalMZMax = mzMax;
        totalRTStep = (rtMax - rtMin) / rtResolution;
        totalMZStep = (mzMax - mzMin) / mzResolution;
        intensityMatrix = new float[rtResolution][mzResolution];

        int scanNumbers[] = rawDataFile.getScanNumbers(msLevel, rtMin, rtMax);
        assert scanNumbers != null;

        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                "Updating 2D visualizer of " + rawDataFile, this);

        MZmineCore.getTaskController().addTask(updateTask, TaskPriority.HIGH,
                visualizer);

    }

    /**
     * @see net.sf.mzmine.io.RawDataAcceptor#addScan(net.sf.mzmine.data.Scan)
     */
    public void addScan(Scan scan, int index, int total) {

        int bitmapSizeX, bitmapSizeY;

        bitmapSizeX = intensityMatrix.length;
        bitmapSizeY = intensityMatrix[0].length;

        if ((scan.getRetentionTime() < totalRTMin)
                || (scan.getRetentionTime() > totalRTMax))
            return;

        int xIndex = (int) Math.floor((scan.getRetentionTime() - totalRTMin)
                / totalRTStep);
        if (xIndex >= bitmapSizeX) xIndex = bitmapSizeX - 1;

        DataPoint dataPoints[] = scan.getDataPoints();
        float[] mzValues = new float[dataPoints.length];
        float[] intensityValues = new float[dataPoints.length];
        for (int dp = 0; dp < dataPoints.length; dp++) {
            mzValues[dp] = dataPoints[dp].getMZ();
            intensityValues[dp] = dataPoints[dp].getIntensity();
        }

        float binnedIntensities[] = ScanUtils.binValues(mzValues,
                intensityValues, totalMZMin, totalMZMax, bitmapSizeY, false,
                BinningType.SUM);
        
        for (int i = 0; i < bitmapSizeY; i++) {
            intensityMatrix[xIndex][i] += binnedIntensities[i];
            if (intensityMatrix[xIndex][i] > maxValue) {
                maxValue = intensityMatrix[xIndex][i];
            }
        }

        
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
        return intensityMatrix.length * intensityMatrix[0].length;
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getX(int, int)
     */
    public Number getX(int series, int item) {
        int xIndex = item % intensityMatrix.length;
        float xValue = totalRTMin + (totalRTStep * xIndex);
        return xValue;
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getY(int, int)
     */
    public Number getY(int series, int item) {
        int yIndex = item / intensityMatrix.length;
        float yValue = totalMZMin + (totalMZStep * yIndex);
        return yValue;
    }

    /**
     * @see org.jfree.data.xy.XYZDataset#getZ(int, int)
     */
    public Number getZ(int series, int item) {
        int xIndex = item % intensityMatrix.length;
        int yIndex = item / intensityMatrix.length;
        return intensityMatrix[xIndex][yIndex];
    }


    
    float getSummedIntensity(float rtMin, float rtMax, float mzMin, float mzMax) {
        
        int bitmapSizeX, bitmapSizeY;

        bitmapSizeX = intensityMatrix.length;
        bitmapSizeY = intensityMatrix[0].length;
        
        int rtMinIndex = (int) Math.floor((rtMin - totalRTMin)
                / totalRTStep);
        if (rtMinIndex >= bitmapSizeX) rtMinIndex = bitmapSizeX - 1;
        if (rtMinIndex < 0) rtMinIndex = 0;
        
        int rtMaxIndex = (int) Math.floor((rtMax - totalRTMin)
                / totalRTStep);
        if (rtMaxIndex >= bitmapSizeX) rtMaxIndex = bitmapSizeX - 1;
        if (rtMaxIndex < 0) rtMaxIndex = 0;
        
        int mzMinIndex = (int) Math.floor((mzMin - totalMZMin)
                / totalMZStep);
        if (mzMinIndex >= bitmapSizeY) mzMinIndex = bitmapSizeY - 1;
        if (mzMinIndex < 0) mzMinIndex = 0;
        
        int mzMaxIndex = (int) Math.floor((mzMax - totalMZMin)
                / totalMZStep);
        if (mzMaxIndex >= bitmapSizeY) mzMaxIndex = bitmapSizeY - 1; 
        if (mzMaxIndex < 0) mzMaxIndex = 0;
        
        float summedIntensity = 0;
        
        for (int i = rtMinIndex; i <= rtMaxIndex; i++) 
            for (int j = mzMinIndex; j <= mzMaxIndex; j++)
                summedIntensity += intensityMatrix[i][j];
        
        return summedIntensity;
        
    }
    
}