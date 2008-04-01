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

package net.sf.mzmine.modules.visualization.threed;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.ScanUtils.BinningType;
import visad.Gridded2DSet;
import visad.Linear2DSet;
import visad.Set;

/**
 * Sampling task which loads the raw data and feeds them to ThreeDDisplay
 */
class ThreeDSamplingTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private RawDataFile dataFile;
    private int scanNumbers[];
    private Range rtRange, mzRange;

    // Data resolution on m/z and retention time axis
    private int rtResolution, mzResolution;

    private int retrievedScans = 0;
    private TaskStatus status;
    private String errorMessage;

    // The 3D display
    private ThreeDDisplay display;

    // maximum value on Z axis
    private float maxBinnedIntensity;

    /**
     * Task constructor
     * 
     * @param dataFile
     * @param msLevel
     * @param visualizer
     */
    ThreeDSamplingTask(RawDataFile dataFile, int scanNumbers[], Range rtRange, Range mzRange, int rtResolution,
            int mzResolution, ThreeDDisplay display) {

        status = TaskStatus.WAITING;

        this.dataFile = dataFile;
        this.scanNumbers = scanNumbers;

        this.rtRange = rtRange;
        this.mzRange = mzRange;
        this.rtResolution = rtResolution;
        this.mzResolution = mzResolution;

        this.display = display;

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Sampling 3D plot of " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return (float) retrievedScans / scanNumbers.length;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        try {

            // domain values set
            Set domainSet;

            // set the resolution (number of data points) on m/z axis
            final float mzStep = mzRange.getSize() / mzResolution;

            // set the resolution (number of data points) on retention time axis
            if (scanNumbers.length > rtResolution) {

                domainSet = new Linear2DSet(display.getDomainTuple(), rtRange.getMin(),
                        rtRange.getMax(), rtResolution, mzRange.getMin(), mzRange.getMax(), mzResolution);

            } else {

                // number of scans is lower then max. resolution, so we can
                // create a grid column for each scan
                rtResolution = scanNumbers.length;

                // domain points in 2D grid
                float domainPoints[][] = new float[2][mzResolution
                        * rtResolution];

                for (int j = 0; j < mzResolution; j++) {
                    for (int i = 0; i < rtResolution; i++) {

                        int point = (rtResolution * j) + i;
                        // set the point's X coordinate
                        domainPoints[0][point] = dataFile.getScan(
                                scanNumbers[i]).getRetentionTime();

                        // set the point's Y coordinate
                        domainPoints[1][point] = mzRange.getMin() + (j * mzStep);
                    }
                }

                domainSet = new Gridded2DSet(display.getDomainTuple(),
                        domainPoints, rtResolution, mzResolution);

            }

            final float rtStep = rtRange.getSize() / rtResolution;

            // create an array for all data points
            float[][] intensityValues = new float[1][mzResolution
                    * rtResolution];

            // load scans
            for (int scanIndex = 0; scanIndex < scanNumbers.length; scanIndex++) {

                if (status == TaskStatus.CANCELED)
                    return;

                Scan scan = dataFile.getScan(scanNumbers[scanIndex]);

                DataPoint dataPoints[] = scan.getDataPoints();
                float[] scanMZValues = new float[dataPoints.length];
                float[] scanIntensityValues = new float[dataPoints.length];
                for (int dp = 0; dp < dataPoints.length; dp++) {
                    scanMZValues[dp] = dataPoints[dp].getMZ();
                    scanIntensityValues[dp] = dataPoints[dp].getIntensity();
                }

                float[] binnedIntensities = ScanUtils.binValues(scanMZValues,
                        scanIntensityValues, mzRange, mzResolution, false,
                        BinningType.MAX);

                int scanBinIndex;

                if (domainSet instanceof Linear2DSet) {
                    double rt = scan.getRetentionTime();
                    scanBinIndex = (int) ((rt - rtRange.getMin()) / rtStep);

                    // last scan falls into last bin
                    if (scanBinIndex == rtResolution)
                        scanBinIndex--;

                } else {
                    // 1 scan per 1 grid column
                    scanBinIndex = scanIndex;
                }

                for (int mzIndex = 0; mzIndex < mzResolution; mzIndex++) {

                    int intensityValuesIndex = (rtResolution * mzIndex)
                            + scanBinIndex;

                    if (binnedIntensities[mzIndex] > intensityValues[0][intensityValuesIndex])
                        intensityValues[0][intensityValuesIndex] = (float) binnedIntensities[mzIndex];

                    if (intensityValues[0][intensityValuesIndex] > maxBinnedIntensity)
                        maxBinnedIntensity = (float) binnedIntensities[mzIndex];
                }

                retrievedScans++;

            }

            display.setData(intensityValues, domainSet, rtRange.getMin(), rtRange.getMax(), mzRange.getMin(),
                    mzRange.getMax(), maxBinnedIntensity);

        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Error while sampling 3D data", e);
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

        status = TaskStatus.FINISHED;

    }

}
