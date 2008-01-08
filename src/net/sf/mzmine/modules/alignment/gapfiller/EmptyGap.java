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

package net.sf.mzmine.modules.alignment.gapfiller;

import java.util.Vector;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.Peak.PeakStatus;
import net.sf.mzmine.data.impl.ConstructionPeak;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.RawDataFile;

class EmptyGap {

    private RawDataFile rawDataFile;
    
    private float centroidMZ;
    private float centroidRT;

    private float rangeMinMZ;
    private float rangeMaxMZ;
    private float rangeMinRT;
    private float rangeMaxRT;

    // These store information about peak that is currently under construction
    Vector<Integer> peakScanNumbers;
    Vector<Float> peakInts;
    Vector<Float> peakMZs;
    Vector<Float> peakRTs;

    private Peak bestPeak;

    private float closestMZ;
    private float closestRT;
    private int closestScanNumber;

    private boolean allDone = false;

    private float intTolerance;
    private float mzTolerance;
    private boolean rtToleranceUseAbs;
    private float rtToleranceValueAbs;
    private float rtToleranceValuePercent;

    /**
     * Constructor: Initializes an empty gap
     * 
     * @param mz M/Z coordinate of this empty gap
     * @param rt RT coordinate of this empty gap
     */
    EmptyGap(RawDataFile rawDataFile, float mz, float rt, SimpleParameterSet parameters) {

        this.rawDataFile = rawDataFile;
        this.centroidMZ = mz;
        this.centroidRT = rt;
        intTolerance = (Float) parameters.getParameterValue(SimpleGapFillerParameters.intTolerance);
        mzTolerance = (Float) parameters.getParameterValue(SimpleGapFillerParameters.MZTolerance);
        if (parameters.getParameterValue(SimpleGapFillerParameters.RTToleranceType) == SimpleGapFillerParameters.RTToleranceTypeAbsolute)
            rtToleranceUseAbs = true;
        rtToleranceValueAbs = (Float) parameters.getParameterValue(SimpleGapFillerParameters.RTToleranceValueAbs);
        rtToleranceValuePercent = (Float) parameters.getParameterValue(SimpleGapFillerParameters.RTToleranceValuePercent);

        rangeMinMZ = centroidMZ - mzTolerance;
        rangeMaxMZ = centroidMZ + mzTolerance;

        if (rtToleranceUseAbs) {
            rangeMinRT = centroidRT - rtToleranceValueAbs;
            rangeMaxRT = centroidRT + rtToleranceValueAbs;
        } else {
            rangeMinRT = (1 - rtToleranceValuePercent) * centroidRT;
            rangeMaxRT = (1 + rtToleranceValuePercent) * centroidRT;
        }

    }

    public boolean offerNextScan(Scan s) {

        // If this empty gap s already filled, then do not process any more scans
        if (allDone) {
            return false;
        }

        float scanRT = s.getRetentionTime();
        // If not yet inside range, then do not process this scan
        if (scanRT<rangeMinRT) return true;
        
        float[] massValues = s.getMZValues();
        float[] intensityValues = s.getIntensityValues();        
        int scanNumber = s.getScanNumber();

        // Find local intensity maximum inside the M/Z range
        float currentIntensity = -1;
        float currentMZ = -1;
        for (int i = 0; i < massValues.length; i++) {

            // Not yet in the mz range
            if (massValues[i] < rangeMinMZ) {
                continue;
            }

            // Already passed mz range
            if (massValues[i] > rangeMaxMZ) {
                break;
            }

            // Inside MZ range
            if (currentIntensity <= intensityValues[i]) {
                currentIntensity = intensityValues[i];
                currentMZ = massValues[i];
            }

        }

        // If there are no datapoints inside the range, then assume intensity is
        // zero.
        // (Interpolation does not work if data is centroided)
        if (currentIntensity < 0) {
            currentIntensity = 0;
            currentMZ = centroidMZ;
        }

        if (java.lang.Math.abs(scanRT - centroidRT) <= java.lang.Math.abs(closestRT
                - centroidRT)) {
            closestMZ = currentMZ;
            closestRT = scanRT;
            closestScanNumber = scanNumber;
        }

        // If this is the very first scan offering, then just initialize
        if (peakInts == null) {
            // New peak starts
            peakScanNumbers = new Vector<Integer>();
            peakInts = new Vector<Float>();
            peakMZs = new Vector<Float>();
            peakRTs = new Vector<Float>();
            peakScanNumbers.add(new Integer(scanNumber));
            peakInts.add(new Float(currentIntensity));
            peakMZs.add(new Float(currentMZ));
            peakRTs.add(new Float(scanRT));
            return true;
        }

        // Check if this continues previous peak?
        if (checkRTShape(scanRT, currentIntensity, rangeMinRT, rangeMaxRT)) {
            // Yes, continue this peak.
            peakScanNumbers.add(new Integer(scanNumber));
            peakInts.add(new Float(currentIntensity));
            peakMZs.add(new Float(currentMZ));
            peakRTs.add(new Float(scanRT));
        } else {

            // No, new peak is starting

            // Check peak formed so far
            checkPeak();

            // Check if already out of range
            if (scanRT > rangeMaxRT) {
                allDone = true;
                return false;
            }

            // Initialize new peak
            peakScanNumbers.clear();
            peakInts.clear();
            peakMZs.clear();
            peakRTs.clear();
            peakScanNumbers.add(new Integer(scanNumber));
            peakInts.add(new Float(currentIntensity));
            peakMZs.add(new Float(currentMZ));
            peakRTs.add(new Float(scanRT));

        }

        return true;

    }

    public void noMoreOffers() {

        // Check peak that was last constructed
        checkPeak();
    }

    public Peak getEstimatedPeak() {
        if (bestPeak == null) {
            ConstructionPeak zeroPeak = new ConstructionPeak(rawDataFile);
            zeroPeak.addDatapoint(closestScanNumber, closestMZ, closestRT, 0.0f);
            zeroPeak.finalizedAddingDatapoints();
            zeroPeak.setPeakStatus(PeakStatus.ESTIMATED);
            bestPeak = zeroPeak;
        }
        return bestPeak;
    }

    /**
     * This function check for the shape of the peak in RT direction, and
     * determines if it is possible to add given m/z peak at the end of the
     * peak.
     */
    private boolean checkRTShape(float nextRT, float nextInt,
            float rangeMinRT, float rangeMaxRT) {

        if (nextRT < rangeMinRT) {
            float prevInt = peakInts.get(peakInts.size() - 1);
            if (nextInt > (prevInt * (1 - intTolerance))) {
                return true;
            }
        }

        if ((rangeMinRT <= nextRT) && (nextRT <= rangeMaxRT)) {
            return true;
        }

        if (nextRT > rangeMaxRT) {
            float prevInt = peakInts.get(peakInts.size() - 1);
            if (nextInt < (prevInt * (1 + intTolerance))) {
                return true;
            }
        }

        return false;

    }

    private void checkPeak() {

        // 1) Check if previous peak has a local maximum inside the search range
        int highestMaximumInd = -1;
        float highestMaximumHeight = 0;
        for (int ind = 0; ind < peakRTs.size(); ind++) {

            if (peakRTs.get(ind) > rangeMaxRT) {
                break;
            }

            if ((rangeMinRT <= peakRTs.get(ind))
                    && (peakRTs.get(ind) <= rangeMaxRT)) {

                int prevind = ind - 1;
                if (prevind < 0) {
                    prevind = 0;
                }
                int nextind = ind + 1;
                if (nextind >= peakRTs.size()) {
                    nextind = peakRTs.size() - 1;
                }

                if ((peakInts.get(ind) >= peakInts.get(nextind))
                        && (peakInts.get(ind) >= peakInts.get(prevind))) {

                    if (peakInts.get(ind) >= highestMaximumHeight) {

                        highestMaximumHeight = peakInts.get(ind);
                        highestMaximumInd = ind;
                    }
                }
            }
        }

        if (highestMaximumInd > -1) {

            // 2) Find elution start and stop

            int startInd = highestMaximumInd;
            float currentInt = peakInts.get(startInd);
            while (true) {
                if (startInd == 0) {
                    break;
                }
                startInd--;
                float nextInt = peakInts.get(startInd);
                if (currentInt >= (nextInt * (1 - intTolerance))) {
                } else {
                    break;
                }
                currentInt = nextInt;
            }

            int stopInd = highestMaximumInd;
            currentInt = peakInts.get(stopInd);
            while (true) {
                if (stopInd == (peakInts.size() - 1)) {
                    break;
                }
                stopInd++;
                float nextInt = peakInts.get(stopInd);
                if (nextInt <= (currentInt * (1 + intTolerance))) {
                } else {
                    break;
                }
                currentInt = nextInt;
            }

            // 3) Generate a Peak
            ConstructionPeak candidatePeak = new ConstructionPeak(rawDataFile);
            for (int ind = startInd; ind <= stopInd; ind++) {
                candidatePeak.addDatapoint(peakScanNumbers.get(ind),
                        peakMZs.get(ind), peakRTs.get(ind), peakInts.get(ind));
            }
            candidatePeak.finalizedAddingDatapoints();
            candidatePeak.setPeakStatus(PeakStatus.ESTIMATED);

            // 4) Check if this is the best candidate for estimator
            if (bestPeak != null) {
                if (bestPeak.getHeight() <= candidatePeak.getHeight()) {
                    bestPeak = candidatePeak;
                }
            } else {
                bestPeak = candidatePeak;
            }

        }

    }

}
