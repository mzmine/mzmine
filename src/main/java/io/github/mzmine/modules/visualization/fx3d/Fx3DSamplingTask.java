/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.fx3d;

import java.util.Random;
import java.util.logging.Logger;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.ScanUtils.BinningType;
import javafx.application.Platform;
import javafx.scene.paint.Color;

/**
 * Sampling task which loads the raw data and feeds them to Fx3DDisplay
 */
class Fx3DSamplingTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private RawDataFile dataFile;
    private Scan scans[];
    private Range<Double> rtRange, mzRange;

    // Data resolution on m/z and retention time axis
    private int rtResolution, mzResolution;
    private Random random = new Random();
    private int retrievedScans = 0;

    // maximum value on Z axis
    private double maxBinnedIntensity;

    private Fx3DStageController controller;
    private static final Color[] PEAK_COLORS = { Color.BLUE, Color.GREEN,
            Color.RED, Color.YELLOW, Color.DARKORANGE, Color.CYAN,
            Color.FUCHSIA, Color.GOLD, Color.AQUA, Color.BROWN,
            Color.CHARTREUSE, Color.CRIMSON, Color.DARKBLUE,
            Color.DARKMAGENTA };

    /**
     * Task constructor
     * 
     * @param dataFile
     * @param msLevel
     * @param visualizer
     */
    Fx3DSamplingTask(RawDataFile dataFile, ScanSelection scanSel,
            Range<Double> mzRange, int rtResolution, int mzResolution,
            Fx3DStageController controller) {

        this.dataFile = dataFile;
        this.scans = scanSel.getMatchingScans(dataFile);
        this.rtRange = ScanUtils.findRtRange(scans);
        this.mzRange = mzRange;
        this.rtResolution = rtResolution;
        this.mzResolution = mzResolution;
        this.controller = controller;
    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Sampling 3D plot of " + dataFile;
    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        return (double) retrievedScans / scans.length;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);

        logger.info("Started sampling 3D plot of " + dataFile);

        try {

            final double rtStep = (rtRange.upperEndpoint()
                    - rtRange.lowerEndpoint()) / rtResolution;

            // create an array for all data points
            float[][] intensityValues = new float[1][mzResolution
                    * rtResolution];
            boolean rtDataSet[] = new boolean[rtResolution];

            // load scans
            for (int scanIndex = 0; scanIndex < scans.length; scanIndex++) {

                if (isCanceled())
                    return;

                Scan scan = scans[scanIndex];
                final Desktop desktop = MZmineCore.getDesktop();

                // Check scan numbers.
                if (scans.length == 0) {
                    desktop.displayErrorMessage(
                            MZmineCore.getDesktop().getMainWindow(),
                            "No scans found");
                    return;
                }

                DataPoint dataPoints[] = scan.getDataPoints();
                double[] scanMZValues = new double[dataPoints.length];
                double[] scanIntensityValues = new double[dataPoints.length];
                for (int dp = 0; dp < dataPoints.length; dp++) {
                    scanMZValues[dp] = dataPoints[dp].getMZ();
                    scanIntensityValues[dp] = dataPoints[dp].getIntensity();
                }

                double[] binnedIntensities = ScanUtils.binValues(scanMZValues,
                        scanIntensityValues, mzRange, mzResolution,
                        scan.getSpectrumType() != MassSpectrumType.CENTROIDED,
                        BinningType.MAX);

                int scanBinIndex;

                double rt = scan.getRetentionTime();
                scanBinIndex = (int) ((rt - rtRange.lowerEndpoint()) / rtStep);

                // last scan falls into last bin
                if (scanBinIndex == rtResolution) {
                    scanBinIndex--;
                }

                for (int mzIndex = 0; mzIndex < mzResolution; mzIndex++) {

                    int intensityValuesIndex = (rtResolution * mzIndex)
                            + scanBinIndex;
                    if (binnedIntensities[mzIndex] > intensityValues[0][intensityValuesIndex]) {
                        intensityValues[0][intensityValuesIndex] = (float) binnedIntensities[mzIndex];
                    }
                    if (intensityValues[0][intensityValuesIndex] > maxBinnedIntensity)
                        maxBinnedIntensity = (double) binnedIntensities[mzIndex];
                }

                rtDataSet[scanBinIndex] = true;

                retrievedScans++;

            }

            // Interpolate missing values on the RT-axis
            for (int rtIndex = 1; rtIndex < rtResolution - 1; rtIndex++) {

                // If the data was set, go to next RT line
                if (rtDataSet[rtIndex])
                    continue;
                int prevIndex, nextIndex;
                for (prevIndex = rtIndex - 1; prevIndex >= 0; prevIndex--) {
                    if (rtDataSet[prevIndex])
                        break;
                }
                for (nextIndex = rtIndex
                        + 1; nextIndex < rtResolution; nextIndex++) {
                    if (rtDataSet[nextIndex])
                        break;
                }

                // If no neighboring data was found, give up
                if ((prevIndex < 0) || (nextIndex >= rtResolution))
                    continue;

                for (int mzIndex = 0; mzIndex < mzResolution; mzIndex++) {

                    int valueIndex = (rtResolution * mzIndex) + rtIndex;
                    int nextValueIndex = (rtResolution * mzIndex) + nextIndex;
                    int prevValueIndex = (rtResolution * mzIndex) + prevIndex;

                    double prevValue = intensityValues[0][prevValueIndex];
                    double nextValue = intensityValues[0][nextValueIndex];

                    double slope = (nextValue - prevValue)
                            / (nextIndex - prevIndex);
                    intensityValues[0][valueIndex] = (float) (prevValue
                            + (slope * (rtIndex - prevIndex)));

                }
            }

            float[][] finalIntensityValues = new float[rtResolution][mzResolution];
            for (int rtIndex = 0; rtIndex < rtResolution; rtIndex++) {
                for (int mzIndex = 0; mzIndex < mzResolution; mzIndex++) {
                    int valueIndex = (rtResolution * mzIndex) + rtIndex;
                    finalIntensityValues[rtIndex][mzIndex] = (float) (intensityValues[0][valueIndex]
                            / maxBinnedIntensity);
                }
            }
            Fx3DRawDataFileDataset plotMesh = new Fx3DRawDataFileDataset(
                    dataFile, finalIntensityValues, rtResolution, mzResolution,
                    maxBinnedIntensity, dataFile.toString(),
                    PEAK_COLORS[random.nextInt(14)]);

            Platform.runLater(() -> {
                controller.addDataset(plotMesh);
            });

        } catch (Throwable e) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Error while sampling 3D data,"
                    + ExceptionUtils.exceptionToString(e));
            return;
        }

        logger.info("Finished sampling 3D plot of " + dataFile);

        setStatus(TaskStatus.FINISHED);

    }

}
