/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;

import org.apache.axis.encoding.Base64;

/**
 * Scan related utilities
 */
public class ScanUtils {

    /**
     * Common utility method to be used as Scan.toString() method in various
     * Scan implementations
     * 
     * @param scan
     *            Scan to be converted to String
     * @return String representation of the scan
     */
    public static String scanToString(Scan scan) {
        StringBuffer buf = new StringBuffer();
        Format rtFormat = MZmineCore.getConfiguration().getRTFormat();
        Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
        buf.append("#");
        buf.append(scan.getScanNumber());
        buf.append(" @");
        buf.append(rtFormat.format(scan.getRetentionTime()));
        buf.append(" MS");
        buf.append(scan.getMSLevel());
        if (scan.getMSLevel() > 1)
            buf.append(" (" + mzFormat.format(scan.getPrecursorMZ()) + ")");

        return buf.toString();
    }

    /**
     * Find a base peak of a given scan in a given m/z range
     * 
     * @param scan
     *            Scan to search
     * @param mzMin
     *            m/z range minimum
     * @param mzMax
     *            m/z range maximum
     * @return double[2] containing base peak m/z and intensity
     */
    public static DataPoint findBasePeak(Scan scan, Range mzRange) {

        DataPoint dataPoints[] = scan.getDataPointsByMass(mzRange);
        DataPoint basePeak = null;

        for (DataPoint dp : dataPoints) {
            if ((basePeak == null)
                    || (dp.getIntensity() > basePeak.getIntensity()))
                basePeak = dp;
        }

        return basePeak;
    }

    /**
     * Calculate the total ion count of a scan within a given mass range.
     * 
     * @param scan
     *            the scan.
     * @param mzRange
     *            mass range.
     * @return the total ion count of the scan within the mass range.
     */
    public static double calculateTIC(Scan scan, Range mzRange) {

        double tic = 0.0;
        for (final DataPoint dataPoint : scan.getDataPointsByMass(mzRange)) {

            tic += dataPoint.getIntensity();
        }
        return tic;
    }

    /**
     * Selects data points within given m/z range
     * 
     */
    public static DataPoint[] selectDataPointsByMass(DataPoint dataPoints[],
            Range mzRange) {
        ArrayList<DataPoint> goodPoints = new ArrayList<DataPoint>();
        for (DataPoint dp : dataPoints) {
            if (mzRange.contains(dp.getMZ()))
                goodPoints.add(dp);
        }
        return goodPoints.toArray(new DataPoint[0]);
    }

    /**
     * Selects data points with intensity >= given intensity
     * 
     */
    public static DataPoint[] selectDataPointsOverIntensity(
            DataPoint dataPoints[], double minIntensity) {
        ArrayList<DataPoint> goodPoints = new ArrayList<DataPoint>();
        for (DataPoint dp : dataPoints) {
            if (dp.getIntensity() >= minIntensity)
                goodPoints.add(dp);
        }
        return goodPoints.toArray(new DataPoint[0]);
    }

    /**
     * Binning modes
     */
    public static enum BinningType {
        SUM, MAX, MIN, AVG
    }

    /**
     * This method bins values on x-axis. Each bin is assigned biggest y-value
     * of all values in the same bin.
     * 
     * @param x
     *            X-coordinates of the data
     * @param y
     *            Y-coordinates of the data
     * @param firstBinStart
     *            Value at the "left"-edge of the first bin
     * @param lastBinStop
     *            Value at the "right"-edge of the last bin
     * @param numberOfBins
     *            Number of bins
     * @param interpolate
     *            If true, then empty bins will be filled with interpolation
     *            using other bins
     * @param binningType
     *            Type of binning (sum of all 'y' within a bin, max of 'y', min
     *            of 'y', avg of 'y')
     * @return Values for each bin
     */
    public static double[] binValues(double[] x, double[] y, Range binRange,
            int numberOfBins, boolean interpolate, BinningType binningType) {

        Double[] binValues = new Double[numberOfBins];
        double binWidth = binRange.getSize() / numberOfBins;

        double beforeX = Double.MIN_VALUE;
        double beforeY = 0.0f;
        double afterX = Double.MAX_VALUE;
        double afterY = 0.0f;

        double[] noOfEntries = null;

        // Binnings
        for (int valueIndex = 0; valueIndex < x.length; valueIndex++) {

            // Before first bin?
            if ((x[valueIndex] - binRange.getMin()) < 0) {
                if (x[valueIndex] > beforeX) {
                    beforeX = x[valueIndex];
                    beforeY = y[valueIndex];
                }
                continue;
            }

            // After last bin?
            if ((binRange.getMax() - x[valueIndex]) < 0) {
                if (x[valueIndex] < afterX) {
                    afterX = x[valueIndex];
                    afterY = y[valueIndex];
                }
                continue;
            }

            int binIndex = (int) ((x[valueIndex] - binRange.getMin()) / binWidth);

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
            case AVG:
                if (noOfEntries == null) {
                    noOfEntries = new double[binValues.length];
                }
                if (binValues[binIndex] == null) {
                    noOfEntries[binIndex] = 1;
                    binValues[binIndex] = y[valueIndex];
                } else {
                    noOfEntries[binIndex]++;
                    binValues[binIndex] += y[valueIndex];
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

        assert noOfEntries != null;

        // calculate the AVG
        if (binningType.equals(BinningType.AVG)) {
            for (int binIndex = 0; binIndex < binValues.length; binIndex++) {
                if (binValues[binIndex] != null) {
                    binValues[binIndex] /= noOfEntries[binIndex];
                }
            }
        }

        // Interpolation
        if (interpolate) {

            for (int binIndex = 0; binIndex < binValues.length; binIndex++) {
                if (binValues[binIndex] == null) {

                    // Find exisiting left neighbour
                    double leftNeighbourValue = beforeY;
                    int leftNeighbourBinIndex = (int) Math
                            .floor((beforeX - binRange.getMin()) / binWidth);
                    for (int anotherBinIndex = binIndex - 1; anotherBinIndex >= 0; anotherBinIndex--) {
                        if (binValues[anotherBinIndex] != null) {
                            leftNeighbourValue = binValues[anotherBinIndex];
                            leftNeighbourBinIndex = anotherBinIndex;
                            break;
                        }
                    }

                    // Find existing right neighbour
                    double rightNeighbourValue = afterY;
                    int rightNeighbourBinIndex = (binValues.length - 1)
                            + (int) Math.ceil((afterX - binRange.getMax())
                                    / binWidth);
                    for (int anotherBinIndex = binIndex + 1; anotherBinIndex < binValues.length; anotherBinIndex++) {
                        if (binValues[anotherBinIndex] != null) {
                            rightNeighbourValue = binValues[anotherBinIndex];
                            rightNeighbourBinIndex = anotherBinIndex;
                            break;
                        }
                    }

                    double slope = (rightNeighbourValue - leftNeighbourValue)
                            / (rightNeighbourBinIndex - leftNeighbourBinIndex);
                    binValues[binIndex] = new Double(leftNeighbourValue + slope
                            * (binIndex - leftNeighbourBinIndex));

                }

            }

        }

        double[] res = new double[binValues.length];
        for (int binIndex = 0; binIndex < binValues.length; binIndex++) {
            res[binIndex] = binValues[binIndex] == null ? 0
                    : binValues[binIndex];
        }
        return res;

    }

    /**
     * Returns index of m/z value in a given array, which is closest to given
     * value, limited by given m/z tolerance. We assume the m/z array is sorted.
     * 
     * @return index of best match, or -1 if no datapoint was found
     */
    public static int findClosestDatapoint(double key, double mzValues[],
            double mzTolerance) {

        int index = Arrays.binarySearch(mzValues, key);

        if (index >= 0)
            return index;

        // Get "insertion point"
        index = (index * -1) - 1;

        // If key value is bigger than biggest m/z value in array
        if (index == mzValues.length)
            index--;
        else if (index > 0) {
            // Check insertion point value and previous one, see which one
            // is closer
            if (Math.abs(mzValues[index - 1] - key) < Math.abs(mzValues[index]
                    - key))
                index--;
        }

        // Check m/z tolerancee
        if (Math.abs(mzValues[index] - key) <= mzTolerance)
            return index;

        // Nothing was found
        return -1;

    }

    /**
     * Determines if the spectrum represented by given array of data points is
     */
    public static boolean isCentroided(DataPoint[] dataPoints) {

        // If the spectrum has less than 10 data points, it should be centroid
        if (dataPoints.length <= 10)
            return true;

        boolean centroid = false;
        Range mzRange = null;
        boolean hasZeroDP = false;

        mzRange = new Range(dataPoints[0].getMZ());
        for (DataPoint dp : dataPoints) {
            mzRange.extendRange(dp.getMZ());
            if (dp.getIntensity() == 0)
                hasZeroDP = true;
        }

        // If the spectrum has no zero data points, it should be centroid
        if (!hasZeroDP)
            return true;

        double massStep = mzRange.getSize() / dataPoints.length;
        double tempdiff, diff = 0, previousMass = dataPoints[0].getMZ();
        for (DataPoint dp : dataPoints) {
            tempdiff = Math.abs(dp.getMZ() - previousMass);
            previousMass = dp.getMZ();
            if (dp.getIntensity() == 0)
                continue;
            if (tempdiff > (massStep * 1.5d)) {
                centroid = true;
                if (tempdiff > diff)
                    diff = tempdiff;
            }
        }

        return centroid;

    }

    /**
     * Finds the MS/MS scan with highest intensity, within given retention time
     * range and with precursor m/z within given m/z range
     */
    public static int findBestFragmentScan(RawDataFile dataFile, Range rtRange,
            Range mzRange) {

        assert dataFile != null;
        assert rtRange != null;
        assert mzRange != null;

        int bestFragmentScan = -1;
        double topBasePeak = 0;

        int[] fragmentScanNumbers = dataFile.getScanNumbers(2, rtRange);

        for (int number : fragmentScanNumbers) {

            Scan scan = dataFile.getScan(number);

            if (mzRange.contains(scan.getPrecursorMZ())) {

                DataPoint basePeak = scan.getBasePeak();

                // If there is no peak in the scan, basePeak can be null
                if (basePeak == null)
                    continue;

                if (basePeak.getIntensity() > topBasePeak) {
                    bestFragmentScan = scan.getScanNumber();
                    topBasePeak = basePeak.getIntensity();
                }
            }

        }

        return bestFragmentScan;

    }

    /**
     * Removes zero-intensity data points from the given array. This function
     * doesn't remove ALL zero data points. In case the spectrum is continuous,
     * one zero data point is required to form a correct border of the peak.
     * This function may return the original array (same instance) in case
     * nothing was removed. Otherwise, it returns a new array.
     * 
     */
    public static DataPoint[] removeZeroDataPoints(DataPoint dataPoints[],
            boolean centroided) {

        // First, check if we actually have any zero data point
        boolean haveZeroDP = false;
        for (DataPoint dp : dataPoints) {
            if (dp.getIntensity() == 0)
                haveZeroDP = true;
        }

        // If no zero data point was found, return the original array
        if (!haveZeroDP)
            return dataPoints;

        // Prepare a list of good data points
        ArrayList<DataPoint> newDataPoints = new ArrayList<DataPoint>(
                dataPoints.length);

        for (int i = 0; i < dataPoints.length; i++) {

            // If the data point is > 0, add it
            if (dataPoints[i].getIntensity() > 0) {
                newDataPoints.add(dataPoints[i]);
                continue;
            }

            // Check the neighbouring data points, but only if the scan is not
            // centroided
            if (!centroided) {
                if ((i > 0) && (dataPoints[i - 1].getIntensity() > 0)) {
                    newDataPoints.add(dataPoints[i]);
                    continue;
                }
                if ((i < dataPoints.length - 1)
                        && (dataPoints[i + 1].getIntensity() > 0)) {
                    newDataPoints.add(dataPoints[i]);
                    continue;
                }
            }
        }

        // If no data point was removed, return the original array
        if (newDataPoints.size() == dataPoints.length)
            return dataPoints;

        DataPoint[] newDataPointsArray = newDataPoints
                .toArray(new DataPoint[0]);

        return newDataPointsArray;

    }

    /**
     * Find the highest data point in array
     * 
     */
    public static DataPoint findTopDataPoint(DataPoint dataPoints[]) {

        DataPoint topDP = null;

        for (DataPoint dp : dataPoints) {
            if ((topDP == null) || (dp.getIntensity() > topDP.getIntensity())) {
                topDP = dp;
            }
        }

        return topDP;
    }

    public static byte[] encodeDataPointsToBytes(DataPoint dataPoints[]) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream peakStream = new DataOutputStream(byteStream);
        for (int i = 0; i < dataPoints.length; i++) {

            try {
                peakStream.writeDouble(dataPoints[i].getMZ());
                peakStream.writeDouble(dataPoints[i].getIntensity());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        byte peakBytes[] = byteStream.toByteArray();
        return peakBytes;
    }

    public static char[] encodeDataPointsBase64(DataPoint dataPoints[]) {
        byte peakBytes[] = encodeDataPointsToBytes(dataPoints);
        char encodedData[] = Base64.encode(peakBytes).toCharArray();
        return encodedData;
    }

    public static DataPoint[] decodeDataPointsFromBytes(byte bytes[]) {
        // each double is 8 bytes and we need one for m/z and one for intensity
        int dpCount = bytes.length / 2 / 8;

        // make a data input stream
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        DataInputStream peakStream = new DataInputStream(byteStream);

        DataPoint dataPoints[] = new DataPoint[dpCount];

        for (int i = 0; i < dataPoints.length; i++) {
            try {
                double mz = peakStream.readDouble();
                double intensity = peakStream.readDouble();
                dataPoints[i] = new SimpleDataPoint(mz, intensity);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return dataPoints;
    }

    public static DataPoint[] decodeDataPointsBase64(char encodedData[]) {
        byte[] bytes = Base64.decode(new String(encodedData));
        DataPoint dataPoints[] = decodeDataPointsFromBytes(bytes);
        return dataPoints;
    }

}
