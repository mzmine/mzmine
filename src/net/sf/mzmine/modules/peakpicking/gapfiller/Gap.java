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

package net.sf.mzmine.modules.peakpicking.gapfiller;

import java.util.List;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeak;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

class Gap {

    private PeakListRow peakListRow;
    private RawDataFile rawDataFile;

    private float searchMZ, searchRT;
    private Range mzRange, rtRange;
    private float intTolerance;

    // These store information about peak that is currently under construction
    private List<GapDataPoint> currentPeakDataPoints;
    private List<GapDataPoint> bestPeakDataPoints;
    private float bestPeakHeight;

    /**
     * Constructor: Initializes an empty gap
     * 
     * @param mz M/Z coordinate of this empty gap
     * @param rt RT coordinate of this empty gap
     */
    Gap(PeakListRow peakListRow, RawDataFile rawDataFile, float mz, float rt,
            float intTolerance, float mzTolerance, float rtTolerance) {

        this.peakListRow = peakListRow;
        this.rawDataFile = rawDataFile;

        this.searchMZ = mz;
        this.searchRT = rt;

        this.intTolerance = intTolerance;

        this.mzRange = new Range(searchMZ - mzTolerance, searchMZ + mzTolerance);

        this.rtRange = new Range(searchRT - rtTolerance, searchRT + rtTolerance);

    }

    void offerNextScan(Scan scan) {

        float scanRT = scan.getRetentionTime();

        // If not yet inside the RT range
        if (scanRT < rtRange.getMin())
            return;

        // If we have passed the RT range and finished processing last peak
        if ((scanRT > rtRange.getMax()) && (currentPeakDataPoints == null))
            return;

        // Find top m/z peak in our range
        DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);

        GapDataPoint currentDataPoint;
        if (basePeak != null) {
            currentDataPoint = new GapDataPoint(scan.getScanNumber(),
                    basePeak.getMZ(), scanRT, basePeak.getIntensity());
        } else {
            currentDataPoint = new GapDataPoint(scan.getScanNumber(), searchMZ,
                    scanRT, 0);
        }

        // If we have not yet started, just create a new peak
        if (currentPeakDataPoints == null) {
            currentPeakDataPoints = new Vector<GapDataPoint>();
            currentPeakDataPoints.add(currentDataPoint);
            return;
        }

        // Check if this continues previous peak?
        if (checkRTShape(currentDataPoint)) {
            // Yes, continue this peak.
            currentPeakDataPoints.add(currentDataPoint);
        } else {

            // No, new peak is starting

            // Check peak formed so far
            if (currentPeakDataPoints != null) {
                checkCurrentPeak();
                currentPeakDataPoints = null;
            }

        }

    }

    public void noMoreOffers() {

        // Check peak that was last constructed
        if (currentPeakDataPoints != null) {
            checkCurrentPeak();
            currentPeakDataPoints = null;
        }

        // If we have best peak candidate, construct a SimplePeak
        if (bestPeakDataPoints != null) {

            float area = 0f, height = 0f, mz = 0f, rt = 0f;
            int scanNumbers[] = new int[bestPeakDataPoints.size()];
            DataPoint peakDataPoints[] = new DataPoint[bestPeakDataPoints.size()];
            DataPoint peakRawDataPoints[][] = new DataPoint[bestPeakDataPoints.size()][1];

            // Process all datapoints
            for (int i = 0; i < bestPeakDataPoints.size(); i++) {

                scanNumbers[i] = bestPeakDataPoints.get(i).getScanNumber();
                peakDataPoints[i] = bestPeakDataPoints.get(i);
                peakRawDataPoints[i][0] = bestPeakDataPoints.get(i);
                mz += bestPeakDataPoints.get(i).getMZ();

                // Check height
                if (bestPeakDataPoints.get(i).getIntensity() > height) {
                    height = bestPeakDataPoints.get(i).getIntensity();
                    rt = bestPeakDataPoints.get(i).getRT();
                }

                // Skip last data point
                if (i == bestPeakDataPoints.size() - 1)
                    break;

                // X axis interval length
                float rtDifference = bestPeakDataPoints.get(i + 1).getRT()
                        - bestPeakDataPoints.get(i).getRT();

                // intensity at the beginning and end of the interval
                float intensityStart = bestPeakDataPoints.get(i).getIntensity();
                float intensityEnd = bestPeakDataPoints.get(i + 1).getIntensity();

                // calculate area of the interval
                area += (rtDifference * (intensityStart + intensityEnd) / 2);

            }

            // Calculate average m/z value
            mz /= bestPeakDataPoints.size();

            SimplePeak newPeak = new SimplePeak(rawDataFile, mz, rt, height,
                    area, scanNumbers, peakDataPoints, peakRawDataPoints,
                    PeakStatus.ESTIMATED);

            // Fill the gap
            peakListRow.addPeak(rawDataFile, newPeak, newPeak);
        }

    }

    /**
     * This function check for the shape of the peak in RT direction, and
     * determines if it is possible to add given m/z peak at the end of the
     * peak.
     */
    private boolean checkRTShape(GapDataPoint dp) {

        if (dp.getRT() < rtRange.getMin()) {
            float prevInt = currentPeakDataPoints.get(
                    currentPeakDataPoints.size() - 1).getIntensity();
            if (dp.getIntensity() > (prevInt * (1 - intTolerance))) {
                return true;
            }
        }

        if (rtRange.contains(dp.getRT())) {
            return true;
        }

        if (dp.getRT() > rtRange.getMax()) {
            float prevInt = currentPeakDataPoints.get(
                    currentPeakDataPoints.size() - 1).getIntensity();
            if (dp.getIntensity() < (prevInt * (1 + intTolerance))) {
                return true;
            }
        }

        return false;

    }

    private void checkCurrentPeak() {

        // 1) Check if currentpeak has a local maximum inside the search range
        int highestMaximumInd = -1;
        float currentMaxHeight = 0f;
        for (int i = 1; i < currentPeakDataPoints.size() - 1; i++) {

            if (rtRange.contains(currentPeakDataPoints.get(i).getRT())) {

                if ((currentPeakDataPoints.get(i).getIntensity() >= currentPeakDataPoints.get(
                        i + 1).getIntensity())
                        && (currentPeakDataPoints.get(i).getIntensity() >= currentPeakDataPoints.get(
                                i - 1).getIntensity())) {

                    if (currentPeakDataPoints.get(i).getIntensity() > currentMaxHeight) {

                        currentMaxHeight = currentPeakDataPoints.get(i).getIntensity();
                        highestMaximumInd = i;
                    }
                }
            }
        }

        // If no local maximum, return
        if (highestMaximumInd == -1)
            return;

        // 2) Find elution start and stop

        int startInd = highestMaximumInd;
        float currentInt = currentPeakDataPoints.get(startInd).getIntensity();
        while (startInd > 0) {

            float nextInt = currentPeakDataPoints.get(startInd - 1).getIntensity();
            if (currentInt < (nextInt * (1 - intTolerance)))
                break;
            startInd--;
            currentInt = nextInt;
        }

        int stopInd = highestMaximumInd;
        currentInt = currentPeakDataPoints.get(stopInd).getIntensity();
        while (stopInd < (currentPeakDataPoints.size() - 1)) {
            float nextInt = currentPeakDataPoints.get(stopInd + 1).getIntensity();
            if (nextInt > (currentInt * (1 + intTolerance)))
                break;
            stopInd++;
            currentInt = nextInt;
        }

        // 3) Check if this is the best candidate for a peak
        if ((bestPeakDataPoints == null) || (bestPeakHeight < currentMaxHeight)) {
            bestPeakDataPoints = currentPeakDataPoints.subList(startInd,
                    stopInd);
        }

    }

}
