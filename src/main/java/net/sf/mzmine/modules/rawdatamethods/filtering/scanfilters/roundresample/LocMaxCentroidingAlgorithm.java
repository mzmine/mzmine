/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.roundresample;

import java.util.ArrayList;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleScan;

/*
 * Adapted from MSDK:
 * https://github.com/msdk/msdk/blob/master/msdk-rawdata/
 * msdk-rawdata-centroiding/src/main/java/io/github/
 * msdk/rawdata/centroiding/LocalMaximaCentroidingAlgorithm.java
 */
public class LocMaxCentroidingAlgorithm {

    private final DataPoint[] dataPoints;
    private Scan inputScan;
    private SimpleScan newScan;

    // Data structures
    private double mzBuffer[] = null;
    private double intensityBuffer[] = null;

    public LocMaxCentroidingAlgorithm(Scan inputScan) {
        this.inputScan = inputScan;
        this.dataPoints = inputScan.getDataPoints();
        mzBuffer = new double[this.dataPoints.length];
        intensityBuffer = new double[this.dataPoints.length];
    }

    public Scan centroidScan() {

        // Copy all scan properties
        this.newScan = new SimpleScan(inputScan);

        // Load data points
        for (int i = 0; i < dataPoints.length; ++i) {
            mzBuffer[i] = dataPoints[i].getMZ();
            intensityBuffer[i] = dataPoints[i].getIntensity();
        }

        final int numOfDataPoints = inputScan.getNumberOfDataPoints();
        int newNumOfDataPoints = 0;

        // If there are no data points, just return the scan
        ArrayList<DataPoint> newDataPoints = new ArrayList<DataPoint>();
        if (numOfDataPoints == 0) {
            for (int i = 0; i < numOfDataPoints; ++i) {
                newDataPoints.add(new SimpleDataPoint(mzBuffer[i],
                        intensityBuffer[i]));
            }
            newScan.setDataPoints(newDataPoints
                    .toArray(new SimpleDataPoint[newDataPoints.size()]));
            newScan.setSpectrumType(MassSpectrumType.CENTROIDED);
            return newScan;
        }

        int localMaximumIndex = 0;
        int rangeBeginning = 0, rangeEnd;
        boolean ascending = true;

        // Iterate through all data points
        for (int i = 0; i < numOfDataPoints - 1; i++) {

            final boolean nextIsBigger = intensityBuffer[i + 1] > intensityBuffer[i];
            final boolean nextIsZero = intensityBuffer[i + 1] == 0f;
            final boolean currentIsZero = intensityBuffer[i] == 0f;

            // Ignore zero intensity regions
            if (currentIsZero) {
                continue;
            }

            // Add current (non-zero) data point to the current m/z peak
            rangeEnd = i;

            // Check for local maximum
            if (ascending && (!nextIsBigger)) {
                localMaximumIndex = i;
                ascending = false;
                continue;
            }

            // Check for the end of the peak
            if ((!ascending) && (nextIsBigger || nextIsZero)) {

                final int numOfPeakDataPoints = rangeEnd - rangeBeginning;

                // Add the m/z peak if it has at least 4 data points
                if (numOfPeakDataPoints >= 4) {

                    // Add the new data point
                    mzBuffer[newNumOfDataPoints] = mzBuffer[localMaximumIndex];
                    intensityBuffer[newNumOfDataPoints] = intensityBuffer[localMaximumIndex];
                    newNumOfDataPoints++;

                }

                // Reset and start with new peak
                ascending = true;
                rangeBeginning = i;
            }

        }

        // Store the new data points
        for (int i = 0; i < newNumOfDataPoints; ++i) {
            newDataPoints.add(new SimpleDataPoint(mzBuffer[i],
                    intensityBuffer[i]));
        }
        newScan.setDataPoints(newDataPoints
                .toArray(new SimpleDataPoint[newDataPoints.size()]));
        newScan.setSpectrumType(MassSpectrumType.CENTROIDED);

        return newScan;

    }

}
