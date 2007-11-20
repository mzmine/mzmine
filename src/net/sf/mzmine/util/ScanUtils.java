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

package net.sf.mzmine.util;

import java.util.Arrays;

import net.sf.mzmine.data.Scan;

/**
 * Scan related utilities
 */
public class ScanUtils {

    /**
     * Find a base peak of a given scan in a given m/z range
     * 
     * @param scan Scan to search
     * @param mzMin m/z range minimum
     * @param mzMax m/z range maximum
     * @return float[2] containing base peak m/z and intensity
     */
    public static float[] findBasePeak(Scan scan, float mzMin, float mzMax) {

        float mzValues[] = scan.getMZValues();
        float intensityValues[] = scan.getIntensityValues();
        float basePeak[] = new float[2];
        boolean foundPoint = false;

        int searchStart = Arrays.binarySearch(mzValues, mzMin);
        if (searchStart < 0) {
            searchStart++;
            searchStart *= -1;
        }
        for (int i = searchStart; i < mzValues.length; i++) {

            if (mzValues[i] > mzMax) break;
            
            if (intensityValues[i] > basePeak[1]) {
                foundPoint = true;
                basePeak[0] = mzValues[i];
                basePeak[1] = intensityValues[i];
            }

        }

        if (foundPoint == false) return null;
        return basePeak;
    }

    /**
     * Binning modes
     */
    public static enum BinningType {
        SUM, MAX, MIN
    };

    /**
     * This method bins values on x-axis. Each bin is assigned biggest y-value
     * of all values in the same bin.
     * 
     * @param x X-coordinates of the data
     * @param y Y-coordinates of the data
     * @param firstBinStart Value at the "left"-edge of the first bin
     * @param lastBinStop Value at the "right"-edge of the last bin
     * @param numberOfBins Number of bins
     * @param interpolate If true, then empty bins will be filled with
     *            interpolation using other bins
     * @param binningType Type of binning (sum of all 'y' within a bin, max of
     *            'y', min of 'y')
     * @return Values for each bin
     */
    public static float[] binValues(float[] x, float[] y,
            float firstBinStart, float lastBinStop, int numberOfBins,
            boolean interpolate, BinningType binningType) {

        Float[] binValues = new Float[numberOfBins];
        float binWidth = (lastBinStop - firstBinStart) / numberOfBins;

        float beforeX = Float.MIN_VALUE;
        float beforeY = 0.0f;
        float afterX = Float.MAX_VALUE;
        float afterY = 0.0f;

        // Binnings
        for (int valueIndex = 0; valueIndex < x.length; valueIndex++) {

            // Before first bin?
            if ((x[valueIndex] - firstBinStart) < 0) {
                if (x[valueIndex] > beforeX) {
                    beforeX = x[valueIndex];
                    beforeY = y[valueIndex];
                }
                continue;
            }

            // After last bin?
            if ((lastBinStop - x[valueIndex]) < 0) {
                if (x[valueIndex] < afterX) {
                    afterX = x[valueIndex];
                    afterY = y[valueIndex];
                }
                continue;
            }

            int binIndex = (int) ((x[valueIndex] - firstBinStart) / binWidth);

            // in case x[valueIndex] is exactly lastBinStop, we would overflow
            // the array
            if (binIndex == binValues.length)
                binIndex--;

            switch (binningType) {
            case MAX:
                if (binValues[binIndex] == null) {
                    binValues[binIndex] = y[valueIndex];
                } else {
                    if (binValues[binIndex] < y[valueIndex]) {
                        binValues[binIndex] = y[valueIndex];
                    }
                }
                break;
            case MIN:
                if (binValues[binIndex] == null) {
                    binValues[binIndex] = y[valueIndex];
                } else {
                    if (binValues[binIndex] > y[valueIndex]) {
                        binValues[binIndex] = y[valueIndex];
                    }
                }
                break;
            case SUM:
            default:
                if (binValues[binIndex] == null) {
                    binValues[binIndex] = y[valueIndex];
                } else {
                    binValues[binIndex] += y[valueIndex];
                }
                break;

            }

        }

        // Interpolation
        if (interpolate) {

            for (int binIndex = 0; binIndex < binValues.length; binIndex++) {
                if (binValues[binIndex] == null) {

                    // Find exisiting left neighbour
                    float leftNeighbourValue = beforeY;
                    int leftNeighbourBinIndex = (int) java.lang.Math.floor((beforeX - firstBinStart)
                            / binWidth);
                    for (int anotherBinIndex = binIndex - 1; anotherBinIndex >= 0; anotherBinIndex--) {
                        if (binValues[anotherBinIndex] != null) {
                            leftNeighbourValue = binValues[anotherBinIndex];
                            leftNeighbourBinIndex = anotherBinIndex;
                            break;
                        }
                    }

                    // Find existing right neighbour
                    float rightNeighbourValue = afterY;
                    int rightNeighbourBinIndex = (binValues.length - 1)
                            + (int) java.lang.Math.ceil((afterX - lastBinStop)
                                    / binWidth);
                    for (int anotherBinIndex = binIndex + 1; anotherBinIndex < binValues.length; anotherBinIndex++) {
                        if (binValues[anotherBinIndex] != null) {
                            rightNeighbourValue = binValues[anotherBinIndex];
                            rightNeighbourBinIndex = anotherBinIndex;
                            break;
                        }
                    }

                    float slope = (rightNeighbourValue - leftNeighbourValue)
                            / (rightNeighbourBinIndex - leftNeighbourBinIndex);
                    binValues[binIndex] = new Float(leftNeighbourValue + slope
                            * (binIndex - leftNeighbourBinIndex));

                }

            }

        }

        float[] res = new float[binValues.length];
        for (int binIndex = 0; binIndex < binValues.length; binIndex++) {
            res[binIndex] = binValues[binIndex] == null ? 0 : binValues[binIndex];
        }
        return res;

    }

}
