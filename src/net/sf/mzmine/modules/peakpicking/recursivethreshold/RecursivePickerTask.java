/*
 * Copyright 2006 The MZmine Development Team
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

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.Peak.PeakStatus;
import net.sf.mzmine.data.impl.ConstructionPeak;
import net.sf.mzmine.data.impl.SimpleIsotopePattern;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.ScanUtils;

/**
 * 
 */
class RecursivePickerTask implements Task {

    private OpenedRawDataFile dataFile;
    private RawDataFile rawDataFile;

    private TaskStatus status;
    private String errorMessage;

    private int processedScans;
    private int totalScans;

    private SimplePeakList readyPeakList;

    private ParameterSet parameters;
    private double binSize;
    private double chromatographicThresholdLevel;
    private double intTolerance;
    private double minimumPeakDuration;
    private double minimumPeakHeight;
    private double minimumMZPeakWidth;
    private double maximumMZPeakWidth;
    private double mzTolerance;
    private double noiseLevel;

    /**
     * @param rawDataFile
     * @param parameters
     */
    RecursivePickerTask(OpenedRawDataFile dataFile,
            ParameterSet parameters) {
        status = TaskStatus.WAITING;
        this.dataFile = dataFile;
        this.rawDataFile = dataFile.getCurrentFile();

        this.parameters = parameters;
        binSize = (Double) parameters.getParameterValue(RecursivePicker.binSize);
        chromatographicThresholdLevel = (Double) parameters.getParameterValue(RecursivePicker.chromatographicThresholdLevel);
        intTolerance = (Double) parameters.getParameterValue(RecursivePicker.intTolerance);
        minimumPeakDuration = (Double) parameters.getParameterValue(RecursivePicker.minimumPeakDuration);
        minimumPeakHeight = (Double) parameters.getParameterValue(RecursivePicker.minimumPeakHeight);
        minimumMZPeakWidth = (Double) parameters.getParameterValue(RecursivePicker.minimumMZPeakWidth);
        maximumMZPeakWidth = (Double) parameters.getParameterValue(RecursivePicker.maximumMZPeakWidth);
        mzTolerance = (Double) parameters.getParameterValue(RecursivePicker.mzTolerance);
        noiseLevel = (Double) parameters.getParameterValue(RecursivePicker.noiseLevel);

        readyPeakList = new SimplePeakList();
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

    public OpenedRawDataFile getDataFile() {
        return dataFile;
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

        int[] scanNumbers = rawDataFile.getScanNumbers(1);

        totalScans = scanNumbers.length;

        /*
         * Calculate M/Z binning
         */

        double startMZ = rawDataFile.getDataMinMZ(1); // minimum m/z value in
        // the raw data file
        double endMZ = rawDataFile.getDataMaxMZ(1); // maximum m/z value in the
        // raw data file
        int numOfBins = (int) (java.lang.Math.ceil((endMZ - startMZ) / binSize));
        double[] chromatographicThresholds = new double[numOfBins];

        if (chromatographicThresholdLevel > 0) {

            double[][] binInts = new double[numOfBins][totalScans];

            // Loop through scans and calculate binned maximum intensities
            for (int i = 0; i < totalScans; i++) {

                if (status == TaskStatus.CANCELED)
                    return;

                try {
                    Scan sc = rawDataFile.getScan(scanNumbers[i]);

                    double[] mzValues = sc.getMZValues();
                    double[] intensityValues = sc.getIntensityValues();
                    double[] tmpInts = ScanUtils.binValues(mzValues,
                            intensityValues, startMZ, endMZ, numOfBins, true,
                            ScanUtils.BinningType.MAX);
                    for (int bini = 0; bini < numOfBins; bini++) {
                        binInts[bini][i] = tmpInts[bini];
                    }

                } catch (IOException e) {
                    status = TaskStatus.ERROR;
                    errorMessage = e.toString();
                }

                processedScans++;

            }

            // Calculate filtering threshold from each RIC
            double initialThreshold = Double.MAX_VALUE;

            for (int bini = 0; bini < numOfBins; bini++) {

                chromatographicThresholds[bini] = MathUtils.calcQuantile(
                        binInts[bini], chromatographicThresholdLevel);
                if (chromatographicThresholds[bini] < initialThreshold) {
                    initialThreshold = chromatographicThresholds[bini];
                }
            }

            binInts = null;
            System.gc();

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

            Scan sc = null;
            try {
                sc = rawDataFile.getScan(scanNumbers[i]);
            } catch (IOException e) {
                status = TaskStatus.ERROR;
                errorMessage = e.toString();
                return;
            }

            double[] masses = sc.getMZValues();
            double[] intensities = sc.getIntensityValues();

            // Find 1D-peaks

            // System.out.print("Find 1D-peaks: ");

            Vector<Integer> inds = new Vector<Integer>();
            recursiveThreshold(masses, intensities, 0, masses.length - 1,
                    noiseLevel, minimumMZPeakWidth, maximumMZPeakWidth, inds, 0);

            for (Integer j : inds) {
                // Is intensity above the noise level
                if (intensities[j] >= noiseLevel) {

                    // Determine correct bin
                    int bin = (int) java.lang.Math.floor((masses[j] - startMZ)
                            / binSize);
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

            // System.out.println("Found " + oneDimPeaks.size() + " 1D-peaks.");

            // Calculate scores between under-construction scores and 1d-peaks

            TreeSet<MatchScore> scores = new TreeSet<MatchScore>();

            for (ConstructionPeak ucPeak : underConstructionPeaks) {

                for (OneDimPeak oneDimPeak : oneDimPeaks) {
                    MatchScore score = new MatchScore(ucPeak, oneDimPeak, mzTolerance, intTolerance);
                    if (score.getScore() < Double.MAX_VALUE) {
                        scores.add(score);
                    }
                }

            }

            // Connect the best scoring pairs of under-construction and 1d peaks

            Iterator<MatchScore> scoreIterator = scores.iterator();
            while (scoreIterator.hasNext()) {
                MatchScore score = scoreIterator.next();

                // If score is too high for connecting, then stop the loop
                if (score.getScore() >= Double.MAX_VALUE) {
                    break;
                }

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
                    double ucLength = ucPeak.getMaxRT() - ucPeak.getMinRT();
                    double ucHeight = ucPeak.getRawHeight();
                    if ((ucLength >= minimumPeakDuration)
                            && (ucHeight >= minimumPeakHeight)) {

                        // Good peak, finalized adding datapoints
                        ucPeak.finalizedAddingDatapoints();

                        // Since this peak picker doesn't detect isotope
                        // patterns, assign this peak to a dummy pattern
                        ucPeak.addData(IsotopePattern.class,
                                new SimpleIsotopePattern(1));

                        // Define peak's status
                        ucPeak.setPeakStatus(PeakStatus.DETECTED);

                        // add it to the peak list
                        readyPeakList.addPeak(ucPeak);
                    }

                    // Remove the peak from under construction peaks
                    int ucInd = underConstructionPeaks.indexOf(ucPeak);
                    underConstructionPeaks.set(ucInd, null);
                }

            }

            // System.out.println("" + readyPeakList.getNumberOfPeaks() + "
            // ready peaks.");

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

            // If there are some unconnected 1d-peaks, then start a new
            // under-construction peak for each of them
            for (OneDimPeak oneDimPeak : oneDimPeaks) {

                if (!oneDimPeak.isConnected()) {

                    ConstructionPeak ucPeak = new ConstructionPeak();
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
            double ucLength = ucPeak.getMaxRT() - ucPeak.getMinRT();
            double ucHeight = ucPeak.getRawHeight();
            if ((ucLength >= minimumPeakDuration)
                    && (ucHeight >= minimumPeakHeight)) {

                // Good peak. finalized adding datapoints
                ucPeak.finalizedAddingDatapoints();

                // Since this peak picker doesn't detect isotope patterns,
                // assign this peak to a dummy pattern
                ucPeak.addData(IsotopePattern.class,
                        new SimpleIsotopePattern(1));

                // Define peak's status
                ucPeak.setPeakStatus(PeakStatus.DETECTED);

                // add it to the peak list
                readyPeakList.addPeak(ucPeak);

            }

        }

        status = TaskStatus.FINISHED;

    }

    /**
     * This function searches for maximums from given part of a spectrum
     */
    private int recursiveThreshold(double[] masses, double intensities[],
            int startInd, int stopInd, double thresholdLevel,
            double minPeakWidthMZ, double maxPeakWidthMZ,
            Vector<Integer> CentroidInds, int recuLevel) {

        int peakStartInd;
        int peakStopInd;
        double peakWidthMZ;
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
