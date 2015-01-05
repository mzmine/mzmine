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

package net.sf.mzmine.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;

import org.apache.axis.encoding.Base64;
import org.openscience.cdk.annotations.TestClass;
import org.openscience.cdk.annotations.TestMethod;

import com.google.common.collect.Range;

/**
 * Scan related utilities
 */
@TestClass("net.sf.mzmine.util.ScanUtilsTest")
public class ScanUtils {

    /**
     * Common utility method to be used as Scan.toString() method in various
     * Scan implementations
     * 
     * @param scan
     *            Scan to be converted to String
     * @return String representation of the scan
     */
    public static @Nonnull String scanToString(@Nonnull Scan scan) {
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
	if (scan.getSpectrumType() == MassSpectrumType.CENTROIDED)
	    buf.append(" c");
	else
	    buf.append(" p");

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
    public static @Nonnull DataPoint findBasePeak(@Nonnull Scan scan,
	    @Nonnull Range<Double> mzRange) {

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
    public static double calculateTIC(Scan scan, Range<Double> mzRange) {

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
	    Range<Double> mzRange) {
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
    public static double[] binValues(double[] x, double[] y,
	    Range<Double> binRange, int numberOfBins, boolean interpolate,
	    BinningType binningType) {

	Double[] binValues = new Double[numberOfBins];
	double binWidth = (binRange.upperEndpoint() - binRange.lowerEndpoint())
		/ numberOfBins;

	double beforeX = Double.MIN_VALUE;
	double beforeY = 0.0f;
	double afterX = Double.MAX_VALUE;
	double afterY = 0.0f;

	double[] noOfEntries = null;

	// Binnings
	for (int valueIndex = 0; valueIndex < x.length; valueIndex++) {

	    // Before first bin?
	    if ((x[valueIndex] - binRange.lowerEndpoint()) < 0) {
		if (x[valueIndex] > beforeX) {
		    beforeX = x[valueIndex];
		    beforeY = y[valueIndex];
		}
		continue;
	    }

	    // After last bin?
	    if ((binRange.upperEndpoint() - x[valueIndex]) < 0) {
		if (x[valueIndex] < afterX) {
		    afterX = x[valueIndex];
		    afterY = y[valueIndex];
		}
		continue;
	    }

	    int binIndex = (int) ((x[valueIndex] - binRange.lowerEndpoint()) / binWidth);

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

	// calculate the AVG
	if (binningType.equals(BinningType.AVG)) {
	    assert noOfEntries != null;
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
			    .floor((beforeX - binRange.lowerEndpoint())
				    / binWidth);
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
			    + (int) Math.ceil((afterX - binRange
				    .upperEndpoint()) / binWidth);
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
     * centroided or continuous (profile). The MZmine algorithm is very simple:
     * if the spectrum contains at least one data point with zero intensity, it
     * should be continuous. If all data points are non-zero, the spectrum is
     * centroided.
     */
    @TestMethod("testDetectSpectrumType")
    public static MassSpectrumType detectSpectrumType(
	    @Nonnull DataPoint[] dataPoints) {

	// If the spectrum has less than 5 data points, it should be
	// centroided
	if (dataPoints.length < 5)
	    return MassSpectrumType.CENTROIDED;

	for (DataPoint dp : dataPoints) {
	    if (dp.getIntensity() == 0.0)
		return MassSpectrumType.PROFILE;
	}
	return MassSpectrumType.CENTROIDED;
    }

    /**
     * Finds the MS/MS scan with highest intensity, within given retention time
     * range and with precursor m/z within given m/z range
     */
    public static int findBestFragmentScan(RawDataFile dataFile,
	    Range<Double> rtRange, Range<Double> mzRange) {

	assert dataFile != null;
	assert rtRange != null;
	assert mzRange != null;

	int bestFragmentScan = -1;
	double topBasePeak = 0;

	int[] fragmentScanNumbers = dataFile.getScanNumbers(2, rtRange);

	for (int number : fragmentScanNumbers) {

	    Scan scan = dataFile.getScan(number);

	    if (mzRange.contains(scan.getPrecursorMZ())) {

		DataPoint basePeak = scan.getHighestDataPoint();

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
     * Find the highest data point in array
     * 
     */
    public static @Nonnull DataPoint findTopDataPoint(
	    @Nonnull DataPoint dataPoints[]) {

	DataPoint topDP = null;

	for (DataPoint dp : dataPoints) {
	    if ((topDP == null) || (dp.getIntensity() > topDP.getIntensity())) {
		topDP = dp;
	    }
	}

	return topDP;
    }

    /**
     * Find the m/z range of the data points in the array. We assume there is at
     * least one data point, and the data points are sorted by m/z.
     */
    public static @Nonnull Range<Double> findMzRange(
	    @Nonnull DataPoint dataPoints[]) {

	assert dataPoints.length > 0;

	double lowMz = dataPoints[0].getMZ();
	double highMz = dataPoints[0].getMZ();
	for (int i = 1; i < dataPoints.length; i++) {
	    if (dataPoints[i].getMZ() < lowMz) {
		lowMz = dataPoints[i].getMZ();
		continue;
	    }
	    if (dataPoints[i].getMZ() > highMz)
		highMz = dataPoints[i].getMZ();
	}

	return Range.closed(lowMz, highMz);
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
