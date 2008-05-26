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

package net.sf.mzmine.modules.peakpicking.recursivethreshold;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

/**
 * 
 */
class RecursivePickerTask implements Task {

    private RawDataFile dataFile;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // scan counter
    private int processedScans, totalScans;

    // parameter values
    private String suffix;
    private float binSize, chromatographicThresholdLevel, intTolerance;
    private float noiseLevel, minimumPeakHeight, mzTolerance;
    private float minimumMZPeakWidth, maximumMZPeakWidth, minimumPeakDuration;

    // peak id counter
    private int newPeakID = 1;

    /**
     * @param dataFile
     * @param parameters
     */
    RecursivePickerTask(RawDataFile dataFile,
            RecursivePickerParameters parameters) {

        this.dataFile = dataFile;
        suffix = (String) parameters.getParameterValue(RecursivePickerParameters.suffix);
        binSize = (Float) parameters.getParameterValue(RecursivePickerParameters.binSize);
        chromatographicThresholdLevel = (Float) parameters.getParameterValue(RecursivePickerParameters.chromatographicThresholdLevel);
        intTolerance = (Float) parameters.getParameterValue(RecursivePickerParameters.intTolerance);
        minimumPeakDuration = (Float) parameters.getParameterValue(RecursivePickerParameters.minimumPeakDuration);
        minimumPeakHeight = (Float) parameters.getParameterValue(RecursivePickerParameters.minimumPeakHeight);
        minimumMZPeakWidth = (Float) parameters.getParameterValue(RecursivePickerParameters.minimumMZPeakWidth);
        maximumMZPeakWidth = (Float) parameters.getParameterValue(RecursivePickerParameters.maximumMZPeakWidth);
        mzTolerance = (Float) parameters.getParameterValue(RecursivePickerParameters.mzTolerance);
        noiseLevel = (Float) parameters.getParameterValue(RecursivePickerParameters.noiseLevel);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Recursive threshold peak detection on " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        if (totalScans == 0)
            return 0.0f;
        return (float) processedScans / (float) (2 * totalScans);
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

    public RawDataFile getDataFile() {
        return dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        // Create new peak list
        SimplePeakList newPeakList = new SimplePeakList(
                dataFile + " " + suffix, dataFile);

        // Get all scans of MS level 1
        int[] scanNumbers = dataFile.getScanNumbers(1);
        totalScans = scanNumbers.length;

        /*
         * Calculate M/Z binning
         */

        // Get minimum and maximum m/z values
        Range mzRange = dataFile.getDataMZRange(1);
        

        int numOfBins = (int) (Math.ceil(mzRange.getSize() / binSize));
        float[] chromatographicThresholds = new float[numOfBins];

        if (chromatographicThresholdLevel > 0) {

            float[][] binInts = new float[numOfBins][totalScans];

            // Loop through scans and calculate binned maximum intensities
            for (int i = 0; i < totalScans; i++) {

                if (status == TaskStatus.CANCELED)
                    return;

                Scan sc = dataFile.getScan(scanNumbers[i]);

                DataPoint dataPoints[] = sc.getDataPoints();
                float[] mzValues = new float[dataPoints.length];
                float[] intensityValues = new float[dataPoints.length];
                for (int dp = 0; dp < dataPoints.length; dp++) {
                    mzValues[dp] = dataPoints[dp].getMZ();
                    intensityValues[dp] = dataPoints[dp].getIntensity();
                }
                
                float[] tmpInts = ScanUtils.binValues(mzValues,
                        intensityValues, mzRange, numOfBins, true,
                        ScanUtils.BinningType.MAX);
                for (int bini = 0; bini < numOfBins; bini++) {
                    binInts[bini][i] = tmpInts[bini];
                }

                processedScans++;

            }

            // Calculate filtering threshold from each RIC
            float initialThreshold = Float.MAX_VALUE;

            for (int bini = 0; bini < numOfBins; bini++) {

                if (status == TaskStatus.CANCELED)
                    return;

                chromatographicThresholds[bini] = MathUtils.calcQuantile(
                        binInts[bini], chromatographicThresholdLevel);
                if (chromatographicThresholds[bini] < initialThreshold) {
                    initialThreshold = chromatographicThresholds[bini];
                }
            }

            binInts = null;

        } else {
            processedScans += totalScans;
            for (int bini = 0; bini < numOfBins; bini++)
                chromatographicThresholds[bini] = 0;

        }

        Vector<RecursivePeak> underConstructionPeaks = new Vector<RecursivePeak>();
        Vector<OneDimPeak> oneDimPeaks = new Vector<OneDimPeak>();
        for (int i = 0; i < totalScans; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            // Get next scan
            Scan sc = dataFile.getScan(scanNumbers[i]);

            DataPoint dataPoints[] = sc.getDataPoints();
            float[] mzValues = new float[dataPoints.length];
            float[] intensityValues = new float[dataPoints.length];
            for (int dp = 0; dp < dataPoints.length; dp++) {
                mzValues[dp] = dataPoints[dp].getMZ();
                intensityValues[dp] = dataPoints[dp].getIntensity();
            }

            // Find 1D-peaks

            Vector<Integer> inds = new Vector<Integer>();
            recursiveThreshold(mzValues, intensityValues, 0, mzValues.length - 1,
                    noiseLevel, minimumMZPeakWidth, maximumMZPeakWidth, inds, 0);

            for (Integer j : inds) {
                // Is intensity above the noise level
                if (intensityValues[j] >= noiseLevel) {

                    // Determine correct bin
                    int bin = (int) Math.floor((mzValues[j] - mzRange.getMin()) / binSize);
                    if (bin < 0) {
                        bin = 0;
                    }
                    if (bin >= numOfBins) {
                        bin = numOfBins - 1;
                    }

                    // Is intensity above the chromatographic threshold level
                    // for this bin?
                    if (intensityValues[j] >= chromatographicThresholds[bin]) {
                        oneDimPeaks.add(new OneDimPeak(i, j, mzValues[j],
                                intensityValues[j]));
                    }

                }

            }

            
            // Calculate scores between under-construction scores and 1d-peaks

            TreeSet<MatchScore> scores = new TreeSet<MatchScore>();

            for (RecursivePeak ucPeak : underConstructionPeaks) {

                for (OneDimPeak oneDimPeak : oneDimPeaks) {
                    MatchScore score = new MatchScore(ucPeak, oneDimPeak,
                            mzTolerance, intTolerance);
                    if (score.getScore() < Float.MAX_VALUE) {
                        scores.add(score);
                    }
                }

            }


            // Connect the best scoring pairs of under-construction and 1d peaks

            Iterator<MatchScore> scoreIterator = scores.iterator();
            while (scoreIterator.hasNext()) {
                MatchScore score = scoreIterator.next();

                // If 1d peak is already connected, then move to next score
                OneDimPeak oneDimPeak = score.getOneDimPeak();
                if (oneDimPeak.isConnected()) {
                    continue;
                }

                // If uc peak is already connected, then move on to next score
                RecursivePeak ucPeak = score.getPeak();
                if (ucPeak.isGrowing()) {
                    continue;
                }

                // Connect 1d to uc
                ucPeak.addDatapoint(sc.getScanNumber(), oneDimPeak.mz,
                        sc.getRetentionTime(), oneDimPeak.intensity);
                oneDimPeak.setConnected();

            }

            // Check if there are any under-construction peaks that were not
            // connected
            for (RecursivePeak ucPeak : underConstructionPeaks) {

                // If nothing was added,
                if (!ucPeak.isGrowing()) {
                	
                	// Finalize peak
                	ucPeak.finalizedAddingDatapoints(PeakStatus.DETECTED);
                	
                    // Check length
                    float ucLength = ucPeak.getRawDataPointsRTRange().getSize();
                    float ucHeight = ucPeak.getHeight();
                    if ((ucLength >= minimumPeakDuration)
                            && (ucHeight >= minimumPeakHeight)) {

                        // Good peak, add it to the peak list
                        SimplePeakListRow newRow = new SimplePeakListRow(
                                newPeakID);
                        newPeakID++;
                        newRow.addPeak(dataFile, ucPeak, ucPeak);
                        newPeakList.addRow(newRow);
                    }

                    // Remove the peak from under construction peaks
                    int ucInd = underConstructionPeaks.indexOf(ucPeak);
                    underConstructionPeaks.set(ucInd, null);
                }

            }

            // Clean-up empty slots under-construction peaks collection and
            // reset growing statuses for remaining under construction peaks
            for (int ucInd = 0; ucInd < underConstructionPeaks.size(); ucInd++) {
                RecursivePeak ucPeak = underConstructionPeaks.get(ucInd);
                if (ucPeak == null) {
                    underConstructionPeaks.remove(ucInd);
                    ucInd--;
                } else {
                    ucPeak.resetGrowingState();
                }
            }

            // If there are some unconnected 1d-peaks, then start a new
            // under-construction peak for each of them
            for (OneDimPeak oneDimPeak : oneDimPeaks) {

                if (!oneDimPeak.isConnected()) {

                    RecursivePeak ucPeak = new RecursivePeak(dataFile);
                    ucPeak.addDatapoint(sc.getScanNumber(), oneDimPeak.mz,
                            sc.getRetentionTime(), oneDimPeak.intensity);
                    ucPeak.resetGrowingState();
                    underConstructionPeaks.add(ucPeak);
                }

            }

            oneDimPeaks.clear();

            processedScans++;
            
        } // End of scan loop

        // Finally process all remaining under-construction peaks

        for (RecursivePeak ucPeak : underConstructionPeaks) {

        	// Finalize peak
        	ucPeak.finalizedAddingDatapoints(PeakStatus.DETECTED);
        	
            // Check length & height
            float ucLength = ucPeak.getRawDataPointsRTRange().getSize();
            float ucHeight = ucPeak.getHeight();
            if ((ucLength >= minimumPeakDuration)
                    && (ucHeight >= minimumPeakHeight)) {

                // Good peak, add it to the peak list
                SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
                newPeakID++;
                newRow.addPeak(dataFile, ucPeak, ucPeak);
                newPeakList.addRow(newRow);

            }

        }

        // Add new peaklist to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(newPeakList);

        status = TaskStatus.FINISHED;

    }

    /**
     * This function searches for maximums from given part of a spectrum
     */
    private int recursiveThreshold(float[] masses, float intensities[],
            int startInd, int stopInd, float thresholdLevel,
            float minPeakWidthMZ, float maxPeakWidthMZ,
            Vector<Integer> CentroidInds, int recuLevel) {

        int peakStartInd;
        int peakStopInd;
        float peakWidthMZ;
        int peakMinInd;
        int peakMaxInd;

        for (int ind = startInd; ind <= stopInd; ind++) {
            // While below threshold
            while ((ind <= stopInd) && (intensities[ind] <= thresholdLevel)) {
                ind++;
            }

            if (ind >= stopInd) {
                break;
            }

            peakStartInd = ind;
            peakMinInd = peakStartInd;
            peakMaxInd = peakStartInd;

            // While peak is on
            while ((ind <= stopInd) && (intensities[ind] > thresholdLevel)) {
                // Check if this is the minimum point of the peak
                if (intensities[ind] < intensities[peakMinInd]) {
                    peakMinInd = ind;
                }

                // Check if this is the maximum poin of the peak
                if (intensities[ind] > intensities[peakMaxInd]) {
                    peakMaxInd = ind;
                }

                ind++;
            }

            if (ind == stopInd) {
                ind--;
            }
            // peakStopInd = ind - 1;
            peakStopInd = ind - 1;

            // Is this suitable peak?

            if (peakStopInd < 0) {
                peakWidthMZ = 0;
            } else {
                int tmpInd1 = peakStartInd - 1;
                if (tmpInd1 < startInd) {
                    tmpInd1 = startInd;
                }
                int tmpInd2 = peakStopInd + 1;
                if (tmpInd2 > stopInd) {
                    tmpInd2 = stopInd;
                }
                peakWidthMZ = masses[peakStopInd] - masses[peakStartInd];
            }

            if ((peakWidthMZ >= minPeakWidthMZ)
                    && (peakWidthMZ <= maxPeakWidthMZ)) {

                // Two options: define peak centroid index as maxintensity index
                // or mean index of all indices
                CentroidInds.add(new Integer(peakMaxInd));

                if (recuLevel > 0) {
                    return peakStopInd + 1;
                }
                // lastKnownGoodPeakStopInd = peakStopInd;
            }

            // Is there need for further investigation?
            if (peakWidthMZ > maxPeakWidthMZ) {
                ind = recursiveThreshold(masses, intensities, peakStartInd,
                        peakStopInd, intensities[peakMinInd], minPeakWidthMZ,
                        maxPeakWidthMZ, CentroidInds, recuLevel + 1);
            }

            if (ind == (stopInd - 1)) {
                break;
            }
        }

        // return lastKnownGoodPeakStopInd;
        return stopInd;

    }

}
