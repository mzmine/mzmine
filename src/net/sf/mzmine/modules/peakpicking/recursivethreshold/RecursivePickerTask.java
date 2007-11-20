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

package net.sf.mzmine.modules.peakpicking.recursivethreshold;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.Peak.PeakStatus;
import net.sf.mzmine.data.impl.ConstructionPeak;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.ScanUtils;

/**
 * 
 */
class RecursivePickerTask implements Task {

    private RawDataFile dataFile;
    private RawDataFile rawDataFile;

    private TaskStatus status;
    private String errorMessage;

    private int processedScans;
    private int totalScans;

    private SimplePeakList readyPeakList;

    private ParameterSet parameters;
    private float binSize;
    private float chromatographicThresholdLevel;
    private float intTolerance;
    private float minimumPeakDuration;
    private float minimumPeakHeight;
    private float minimumMZPeakWidth;
    private float maximumMZPeakWidth;
    private float mzTolerance;
    private float noiseLevel;

    /**
     * @param rawDataFile
     * @param parameters
     */
    RecursivePickerTask(RawDataFile dataFile, SimpleParameterSet parameters) {
        status = TaskStatus.WAITING;
        this.dataFile = dataFile;
        this.rawDataFile = dataFile;

        this.parameters = parameters;
        binSize = (Float) parameters.getParameterValue(RecursivePicker.binSize);
        chromatographicThresholdLevel = (Float) parameters.getParameterValue(RecursivePicker.chromatographicThresholdLevel);
        intTolerance = (Float) parameters.getParameterValue(RecursivePicker.intTolerance);
        minimumPeakDuration = (Float) parameters.getParameterValue(RecursivePicker.minimumPeakDuration);
        minimumPeakHeight = (Float) parameters.getParameterValue(RecursivePicker.minimumPeakHeight);
        minimumMZPeakWidth = (Float) parameters.getParameterValue(RecursivePicker.minimumMZPeakWidth);
        maximumMZPeakWidth = (Float) parameters.getParameterValue(RecursivePicker.maximumMZPeakWidth);
        mzTolerance = (Float) parameters.getParameterValue(RecursivePicker.mzTolerance);
        noiseLevel = (Float) parameters.getParameterValue(RecursivePicker.noiseLevel);

        readyPeakList = new SimplePeakList(dataFile.toString() + " peak list");
        readyPeakList.addRawDataFile(dataFile);
        
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
        return (float) processedScans / (2.0f * totalScans);
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
     * @see net.sf.mzmine.taskcontrol.Task#getResult()
     */
    public Object getResult() {
        Object[] results = new Object[3];
        results[0] = dataFile;
        results[1] = readyPeakList;
        results[2] = parameters;
        return results;
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

        int[] scanNumbers = rawDataFile.getScanNumbers(1);

        totalScans = scanNumbers.length;

        int newPeakID = 1;

        /*
         * Calculate M/Z binning
         */

        float startMZ = rawDataFile.getDataMinMZ(1); // minimum m/z value in
        // the raw data file
        float endMZ = rawDataFile.getDataMaxMZ(1); // maximum m/z value in the
        // raw data file
        int numOfBins = (int) (Math.ceil((endMZ - startMZ) / binSize));
        float[] chromatographicThresholds = new float[numOfBins];

        if (chromatographicThresholdLevel > 0) {

            float[][] binInts = new float[numOfBins][totalScans];

            // Loop through scans and calculate binned maximum intensities
            for (int i = 0; i < totalScans; i++) {

                if (status == TaskStatus.CANCELED)
                    return;

                Scan sc = rawDataFile.getScan(scanNumbers[i]);

                float[] mzValues = sc.getMZValues();
                float[] intensityValues = sc.getIntensityValues();
                float[] tmpInts = ScanUtils.binValues(mzValues,
                        intensityValues, startMZ, endMZ, numOfBins, true,
                        ScanUtils.BinningType.MAX);
                for (int bini = 0; bini < numOfBins; bini++) {
                    binInts[bini][i] = tmpInts[bini];
                }

                processedScans++;

            }

            // Calculate filtering threshold from each RIC
            float initialThreshold = Float.MAX_VALUE;

            for (int bini = 0; bini < numOfBins; bini++) {

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

        Vector<ConstructionPeak> underConstructionPeaks = new Vector<ConstructionPeak>();
        Vector<OneDimPeak> oneDimPeaks = new Vector<OneDimPeak>();
        for (int i = 0; i < totalScans; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            // Get next scan

            Scan sc = rawDataFile.getScan(scanNumbers[i]);

            float[] masses = sc.getMZValues();
            float[] intensities = sc.getIntensityValues();

            // Find 1D-peaks

            Vector<Integer> inds = new Vector<Integer>();
            recursiveThreshold(masses, intensities, 0, masses.length - 1,
                    noiseLevel, minimumMZPeakWidth, maximumMZPeakWidth, inds, 0);

            for (Integer j : inds) {
                // Is intensity above the noise level
                if (intensities[j] >= noiseLevel) {

                    // Determine correct bin
                    int bin = (int) Math.floor((masses[j] - startMZ) / binSize);
                    if (bin < 0) {
                        bin = 0;
                    }
                    if (bin >= numOfBins) {
                        bin = numOfBins - 1;
                    }

                    // Is intensity above the chromatographic threshold level
                    // for this bin?
                    if (intensities[j] >= chromatographicThresholds[bin]) {
                        oneDimPeaks.add(new OneDimPeak(i, j, masses[j],
                                intensities[j]));
                    }

                }

            }
            
            // Clean-up empty slots under-construction peaks collection and
            // reset growing statuses for remaining under construction peaks
            for (int ucInd = 0; ucInd < underConstructionPeaks.size(); ucInd++) {
                ConstructionPeak ucPeak = underConstructionPeaks.get(ucInd);
                if (ucPeak == null) {
                    underConstructionPeaks.remove(ucInd);
                    ucInd--;
                } else {
                    ucPeak.resetGrowingState();
                }
            }
            
            // Calculate scores between under-construction scores and 1d-peaks

            TreeSet<MatchScore> scores = new TreeSet<MatchScore>();

            for (ConstructionPeak ucPeak : underConstructionPeaks) {

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

                /* doesn't make sense?
                // If score is too high for connecting, then stop the loop
                if (score.getScore() >= Float.MAX_VALUE) {
                    break;
                }*/

                // If 1d peak is already connected, then move to next score
                OneDimPeak oneDimPeak = score.getOneDimPeak();
                if (oneDimPeak.isConnected()) {
                    continue;
                }

                // If uc peak is already connected, then move on to next score
                ConstructionPeak ucPeak = score.getPeak();
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
            for (ConstructionPeak ucPeak : underConstructionPeaks) {

                // If nothing was added,
                if (!ucPeak.isGrowing()) {

                    // Check length
                    float ucLength = ucPeak.getDataPointMaxRT()
                            - ucPeak.getDataPointMinRT();
                    float ucHeight = ucPeak.getHeight();
                    if ((ucLength >= minimumPeakDuration)
                            && (ucHeight >= minimumPeakHeight)) {

                        // Good peak, finalized adding datapoints
                        ucPeak.finalizedAddingDatapoints();

                        // Define peak's status
                        ucPeak.setPeakStatus(PeakStatus.DETECTED);

                        // add it to the peak list
                        SimplePeakListRow newRow = new SimplePeakListRow(
                                newPeakID);
                        newPeakID++;
                        newRow.addPeak(dataFile, ucPeak, ucPeak);
                        readyPeakList.addRow(newRow);
                    }

                    // Remove the peak from under construction peaks
                    int ucInd = underConstructionPeaks.indexOf(ucPeak);
                    underConstructionPeaks.set(ucInd, null);
                }

            }

            // If there are some unconnected 1d-peaks, then start a new
            // under-construction peak for each of them
            for (OneDimPeak oneDimPeak : oneDimPeaks) {

                if (!oneDimPeak.isConnected()) {

                    ConstructionPeak ucPeak = new ConstructionPeak(dataFile);
                    ucPeak.addDatapoint(sc.getScanNumber(), oneDimPeak.mz,
                            sc.getRetentionTime(), oneDimPeak.intensity);
                    underConstructionPeaks.add(ucPeak);

                }

            }

            oneDimPeaks.clear();

            processedScans++;

        } // End of scan loop

        // Finally process all remaining under-construction peaks

        for (ConstructionPeak ucPeak : underConstructionPeaks) {

            // Check length & height
            float ucLength = ucPeak.getDataPointMaxRT()
                    - ucPeak.getDataPointMinRT();
            float ucHeight = ucPeak.getHeight();
            if ((ucLength >= minimumPeakDuration)
                    && (ucHeight >= minimumPeakHeight)) {

                // Good peak. finalized adding datapoints
                ucPeak.finalizedAddingDatapoints();

                // Define peak's status
                ucPeak.setPeakStatus(PeakStatus.DETECTED);

                // add it to the peak list
                SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
                newPeakID++;
                newRow.addPeak(dataFile, ucPeak, ucPeak);
                readyPeakList.addRow(newRow);

            }

        }

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
