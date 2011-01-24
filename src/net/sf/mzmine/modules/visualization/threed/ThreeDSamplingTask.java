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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.threed;

import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.ScanUtils.BinningType;
import visad.Gridded2DSet;
import visad.Linear2DSet;
import visad.Set;

/**
 * Sampling task which loads the raw data and feeds them to ThreeDDisplay
 */
class ThreeDSamplingTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private RawDataFile dataFile;
    private int scanNumbers[];
    private Range rtRange, mzRange;

    // Data resolution on m/z and retention time axis
    private int rtResolution, mzResolution;

    private int retrievedScans = 0;

    // The 3D display
    private ThreeDDisplay display;
    private ThreeDBottomPanel bottomPanel;

    // maximum value on Z axis
    private double maxBinnedIntensity;

    /**
     * Task constructor
     * 
     * @param dataFile
     * @param msLevel
     * @param visualizer
     */
    ThreeDSamplingTask(RawDataFile dataFile, int scanNumbers[], Range rtRange,
            Range mzRange, int rtResolution, int mzResolution,
            ThreeDDisplay display, ThreeDBottomPanel bottomPanel) {

        this.dataFile = dataFile;
        this.scanNumbers = scanNumbers;

        this.rtRange = rtRange;
        this.mzRange = mzRange;
        this.rtResolution = rtResolution;
        this.mzResolution = mzResolution;

        this.display = display;
        this.bottomPanel = bottomPanel;

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
    public double getFinishedPercentage() {
        return (double) retrievedScans / scanNumbers.length;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        setStatus( TaskStatus.PROCESSING );

        logger.info("Started sampling 3D plot of " + dataFile);
        
        try {

            // domain values set
            Set domainSet;

            // set the resolution (number of data points) on m/z axis
            final double mzStep = mzRange.getSize() / mzResolution;

            // set the resolution (number of data points) on retention time axis
            if (scanNumbers.length > rtResolution) {

                domainSet = new Linear2DSet(display.getDomainTuple(),
                        rtRange.getMin(), rtRange.getMax(), rtResolution,
                        mzRange.getMin(), mzRange.getMax(), mzResolution);

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
                        domainPoints[0][point] = (float) dataFile.getScan(
                                scanNumbers[i]).getRetentionTime();

                        // set the point's Y coordinate
                        domainPoints[1][point] = (float) (mzRange.getMin() + (j * mzStep));
                    }
                }

                domainSet = new Gridded2DSet(display.getDomainTuple(),
                        domainPoints, rtResolution, mzResolution);

            }

            final double rtStep = rtRange.getSize() / rtResolution;

            // create an array for all data points
            double[][] intensityValues = new double[1][mzResolution
                    * rtResolution];

            // load scans
            for (int scanIndex = 0; scanIndex < scanNumbers.length; scanIndex++) {

                if ( isCanceled( ))
                    return;

                Scan scan = dataFile.getScan(scanNumbers[scanIndex]);

                DataPoint dataPoints[] = scan.getDataPoints();
                double[] scanMZValues = new double[dataPoints.length];
                double[] scanIntensityValues = new double[dataPoints.length];
                for (int dp = 0; dp < dataPoints.length; dp++) {
                    scanMZValues[dp] = dataPoints[dp].getMZ();
                    scanIntensityValues[dp] = dataPoints[dp].getIntensity();
                }

                double[] binnedIntensities = ScanUtils.binValues(scanMZValues,
                        scanIntensityValues, mzRange, mzResolution,
                        !scan.isCentroided(), BinningType.MAX);

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
                        intensityValues[0][intensityValuesIndex] = (double) binnedIntensities[mzIndex];

                    if (intensityValues[0][intensityValuesIndex] > maxBinnedIntensity)
                        maxBinnedIntensity = (double) binnedIntensities[mzIndex];
                }

                retrievedScans++;

            }

            display.setData(intensityValues, domainSet, rtRange.getMin(),
                    rtRange.getMax(), mzRange.getMin(), mzRange.getMax(),
                    maxBinnedIntensity);
            
            // After we have constructed everything, load the peak lists into the
            // bottom panel
            bottomPanel.rebuildPeakListSelector();

        } catch (Throwable e) {
            setStatus( TaskStatus.ERROR );
            errorMessage = "Error while sampling 3D data, "
                    + ExceptionUtils.exceptionToString(e);
            return;
        }

        logger.info("Finished sampling 3D plot of " + dataFile);
        
        setStatus( TaskStatus.FINISHED );

    }

	public Object[] getCreatedObjects() {
		return null;
	}

}
