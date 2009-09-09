/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.rawdata.scanfilters.resample;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.modules.rawdata.scanfilters.RawDataFilter;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

public class ResampleFilter implements RawDataFilter {

	private Double binSize;

	public ResampleFilter(ResampleFilterParameters parameters) {
		binSize = (Double) parameters.getParameterValue(ResampleFilterParameters.binSize);
	}

	public Scan getNewScan(Scan scan) {

		Range mzRange = scan.getMZRange();
		int numberOfBins = (int) Math.round((mzRange.getMax() - mzRange.getMin()) / binSize);
		if (numberOfBins == 0) {
			numberOfBins++;
		}

		// ScanUtils.binValues needs arrays
		DataPoint dps[] = scan.getDataPoints();
		double[] x = new double[dps.length];
		double[] y = new double[dps.length];
		for (int i = 0; i < dps.length; i++) {
			x[i] = dps[i].getMZ();
			y[i] = dps[i].getIntensity();
		}
		// the new intensity values
		double[] newY = ScanUtils.binValues(x, y, mzRange,
				numberOfBins, !scan.isCentroided(),
				ScanUtils.BinningType.AVG);
		SimpleDataPoint[] newPoints = new SimpleDataPoint[newY.length];

		// set the new m/z value in the middle of the bin
		double newX = mzRange.getMin() + binSize / 2.0;
		// creates new DataPoints
		for (int i = 0; i < newY.length; i++) {
			newPoints[i] = new SimpleDataPoint(newX, newY[i]);
			newX += binSize;
		}

		// Create updated scan
		SimpleScan newScan = new SimpleScan(scan);
		newScan.setDataPoints(newPoints);


		return newScan;
	}
}
