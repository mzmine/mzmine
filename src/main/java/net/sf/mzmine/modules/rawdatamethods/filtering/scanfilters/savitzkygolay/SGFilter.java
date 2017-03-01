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

package net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.savitzkygolay;

import java.util.Hashtable;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.ScanFilter;
import net.sf.mzmine.parameters.ParameterSet;

public class SGFilter implements ScanFilter {

    private static final Hashtable<Integer, Integer> Hvalues = new Hashtable<Integer, Integer>();
    private static final Hashtable<Integer, int[]> Avalues = new Hashtable<Integer, int[]>();

    static {
	int[] a5Ints = { 17, 12, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	Avalues.put(5, a5Ints);
	int[] a7Ints = { 7, 6, 3, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	Avalues.put(7, a7Ints);
	int[] a9Ints = { 59, 54, 39, 14, -21, 0, 0, 0, 0, 0, 0, 0, 0 };
	Avalues.put(9, a9Ints);
	int[] a11Ints = { 89, 84, 69, 44, 9, -36, 0, 0, 0, 0, 0, 0, 0 };
	Avalues.put(11, a11Ints);
	int[] a13Ints = { 25, 24, 21, 16, 9, 0, -11, 0, 0, 0, 0, 0, 0 };
	Avalues.put(13, a13Ints);
	int[] a15Ints = { 167, 162, 147, 122, 87, 42, -13, -78, 0, 0, 0, 0, 0 };
	Avalues.put(15, a15Ints);
	int[] a17Ints = { 43, 42, 39, 34, 27, 18, 7, -6, -21, 0, 0, 0, 0 };
	Avalues.put(17, a17Ints);
	int[] a19Ints = { 269, 264, 249, 224, 189, 144, 89, 24, -51, -136, 0,
		0, 0 };
	Avalues.put(19, a19Ints);
	int[] a21Ints = { 329, 324, 309, 284, 249, 204, 149, 84, 9, -76, -171,
		0, 0 };
	Avalues.put(21, a21Ints);
	int[] a23Ints = { 79, 78, 75, 70, 63, 54, 43, 30, 15, -2, -21, -42, 0 };
	Avalues.put(23, a23Ints);
	int[] a25Ints = { 467, 462, 447, 422, 387, 343, 287, 222, 147, 62, -33,
		-138, -253 };
	Avalues.put(25, a25Ints);

	Hvalues.put(5, 35);
	Hvalues.put(7, 21);
	Hvalues.put(9, 231);
	Hvalues.put(11, 429);
	Hvalues.put(13, 143);
	Hvalues.put(15, 1105);
	Hvalues.put(17, 323);
	Hvalues.put(19, 2261);
	Hvalues.put(21, 3059);
	Hvalues.put(23, 805);
	Hvalues.put(25, 5175);

    }

    public Scan filterScan(Scan scan, ParameterSet parameters) {

	int numOfDataPoints = parameters.getParameter(
		SGFilterParameters.datapoints).getValue();

	assert Avalues.containsKey(numOfDataPoints);
	assert Hvalues.containsKey(numOfDataPoints);

	int[] aVals = Avalues.get(numOfDataPoints);
	int h = Hvalues.get(numOfDataPoints).intValue();

	// only process MS level 1 scans
	if (scan.getMSLevel() != 1) {
	    return scan;
	}

	int marginSize = (numOfDataPoints + 1) / 2 - 1;
	double sumOfInts;

	DataPoint oldDataPoints[] = scan.getDataPoints();
	int newDataPointsLength = oldDataPoints.length - (marginSize * 2);

	// only process scans with datapoints
	if (newDataPointsLength < 1) {
	    return scan;
	}

	DataPoint newDataPoints[] = new DataPoint[newDataPointsLength];

	for (int spectrumInd = marginSize; spectrumInd < (oldDataPoints.length - marginSize); spectrumInd++) {

	    // zero intensity data points must be left unchanged
	    if (oldDataPoints[spectrumInd].getIntensity() == 0) {
		newDataPoints[spectrumInd - marginSize] = oldDataPoints[spectrumInd];
		continue;
	    }

	    sumOfInts = aVals[0] * oldDataPoints[spectrumInd].getIntensity();

	    for (int windowInd = 1; windowInd <= marginSize; windowInd++) {
		sumOfInts += aVals[windowInd]
			* (oldDataPoints[spectrumInd + windowInd]
				.getIntensity() + oldDataPoints[spectrumInd
				- windowInd].getIntensity());
	    }

	    sumOfInts = sumOfInts / h;

	    if (sumOfInts < 0) {
		sumOfInts = 0;
	    }
	    newDataPoints[spectrumInd - marginSize] = new SimpleDataPoint(
		    oldDataPoints[spectrumInd].getMZ(), sumOfInts);

	}

	SimpleScan newScan = new SimpleScan(scan);
	newScan.setDataPoints(newDataPoints);
	return newScan;

    }

    public @Nonnull String getName() {
	return "Savitzky-Golay filter";
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return SGFilterParameters.class;
    }

}
