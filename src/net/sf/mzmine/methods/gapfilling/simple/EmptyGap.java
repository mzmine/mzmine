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

package net.sf.mzmine.methods.gapfilling.simple;

import java.util.Vector;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.Peak.PeakStatus;
import net.sf.mzmine.data.impl.ConstructionPeak;

class EmptyGap {

    private double centroidMZ;
    private double centroidRT;

    private double rangeMinMZ;
    private double rangeMaxMZ;
    private double rangeMinRT;
    private double rangeMaxRT;

    // These store information about peak that is currently under construction
    Vector<Integer> peakScanNumbers;
    Vector<Double> peakInts;
    Vector<Double> peakMZs;
    Vector<Double> peakRTs;

    private Peak bestPeak;

    private double closestMZ;
    private double closestRT;
    private int closestScanNumber;

    private boolean allDone = false;

    private double intTolerance;
    private double mzTolerance;
    private boolean rtToleranceUseAbs;
    private double rtToleranceValueAbs;
    private double rtToleranceValuePercent;

    /**
     * Constructor: Initializes an empty gap
     * 
     * @param mz M/Z coordinate of this empty gap
     * @param rt RT coordinate of this empty gap
     */
    EmptyGap(double mz, double rt, ParameterSet parameters) {

        this.centroidMZ = mz;
        this.centroidRT = rt;
        intTolerance = (Double) parameters.getParameterValue(SimpleGapFiller.IntTolerance);
        mzTolerance = (Double) parameters.getParameterValue(SimpleGapFiller.MZTolerance);
        if (parameters.getParameterValue(SimpleGapFiller.RTToleranceType) == SimpleGapFiller.RTToleranceTypeAbsolute)
            rtToleranceUseAbs = true;
        rtToleranceValueAbs = (Double) parameters.getParameterValue(SimpleGapFiller.RTToleranceValueAbs);
        rtToleranceValuePercent = (Double) parameters.getParameterValue(SimpleGapFiller.RTToleranceValuePercent);

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

        // If this empty gap s already filled, there is no need to analyse
        // anymore scans
        if (allDone) {
            return false;
        }

        double[] massValues = s.getMZValues();
        double[] intensityValues = s.getIntensityValues();
        double scanRT = s.getRetentionTime();
        int scanNumber = s.getScanNumber();

        // Find local intensity maximum inside the M/Z range
        double currentIntensity = -1;
        double currentMZ = -1;
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
        // (Interpolation is not fair because data maybe centroided)
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
            peakInts = new Vector<Double>();
            peakMZs = new Vector<Double>();
            peakRTs = new Vector<Double>();
            peakScanNumbers.add(new Integer(scanNumber));
            peakInts.add(new Double(currentIntensity));
            peakMZs.add(new Double(currentMZ));
            peakRTs.add(new Double(scanRT));
            return true;
        }

        // Check if this continues previous peak?
        if (checkRTShape(scanRT, currentIntensity, rangeMinRT, rangeMaxRT)) {
            // Yes, it is. Just continue this peak.
            peakScanNumbers.add(new Integer(scanNumber));
            peakInts.add(new Double(currentIntensity));
            peakMZs.add(new Double(currentMZ));
            peakRTs.add(new Double(scanRT));
        } else {

            // No it is not

            // Check previous peak as a candidate for estimator

            checkPeak();

            // New peak starts
            if (scanRT > rangeMaxRT) {
                allDone = true;
                return false;
            }

            peakScanNumbers.clear();
            peakInts.clear();
            peakMZs.clear();
            peakRTs.clear();
            peakScanNumbers.add(new Integer(scanNumber));
            peakInts.add(new Double(currentIntensity));
            peakMZs.add(new Double(currentMZ));
            peakRTs.add(new Double(scanRT));

        }

        return true;

    }

    public void noMoreOffers() {

        // Check peak that was last constructed
        checkPeak();
    }

    public Peak getEstimatedPeak() {
        if (bestPeak == null) {
            ConstructionPeak zeroPeak = new ConstructionPeak();
            zeroPeak.addDatapoint(closestScanNumber, closestMZ, closestRT, 0.0);
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
    private boolean checkRTShape(double nextRT, double nextInt,
            double rangeMinRT, double rangeMaxRT) {

        if (nextRT < rangeMinRT) {
            double prevInt = peakInts.get(peakInts.size() - 1);
            if (nextInt > (prevInt * (1 - intTolerance))) {
                return true;
            }
        }

        if ((rangeMinRT <= nextRT) && (nextRT <= rangeMaxRT)) {
            return true;
        }

        if (nextRT > rangeMaxRT) {
            double prevInt = peakInts.get(peakInts.size() - 1);
            if (nextInt < (prevInt * (1 + intTolerance))) {
                return true;
            }
        }

        return false;

    }

    private void checkPeak() {

        // 1) Check if previous peak has a local maximum inside the search range
        int highestMaximumInd = -1;
        double highestMaximumHeight = 0;
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
            double currentInt = peakInts.get(startInd);
            while (true) {
                if (startInd == 0) {
                    break;
                }
                startInd--;
                double nextInt = peakInts.get(startInd);
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
                double nextInt = peakInts.get(stopInd);
                if (nextInt <= (currentInt * (1 + intTolerance))) {
                } else {
                    break;
                }
                currentInt = nextInt;
            }

            // 3) Generate a Peak
            ConstructionPeak candidatePeak = new ConstructionPeak();
            for (int ind = startInd; ind <= stopInd; ind++) {
                candidatePeak.addDatapoint(peakScanNumbers.get(ind),
                        peakMZs.get(ind), peakRTs.get(ind), peakInts.get(ind));
            }
            candidatePeak.finalizedAddingDatapoints();
            candidatePeak.setPeakStatus(PeakStatus.ESTIMATED);

            // 4) Check if this is the best candidate for estimator
            if (bestPeak != null) {
                if (bestPeak.getRawHeight() <= candidatePeak.getRawHeight()) {
                    bestPeak = candidatePeak;
                }
            } else {
                bestPeak = candidatePeak;
            }

        }

    }

}
