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

package net.sf.mzmine.modules.peakpicking.twostep.massdetection.wavelet;

import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;

public class WaveletMassDetector implements MassDetector {

	//private Logger logger = Logger.getLogger(this.getClass().getName());

	// parameter values
	private float scaleLevel;

	public WaveletMassDetector(WaveletMassDetectorParameters parameters) {
		scaleLevel = (Float) parameters
				.getParameterValue(WaveletMassDetectorParameters.scaleLevel);
	}

	public MzPeak[] getMassValues(Scan scan) {
		Scan sc = scan;
		DataPoint dataPoints[] = sc.getDataPoints();
		float[] mzValues = new float[dataPoints.length];
		float[] intensityValues = new float[dataPoints.length];
		for (int dp = 0; dp < dataPoints.length; dp++) {
			mzValues[dp] = dataPoints[dp].getMZ();
			intensityValues[dp] = dataPoints[dp].getIntensity();
		}

		Vector<MzPeak> mzPeaks = new Vector<MzPeak>();

		// Find MzPeaks

		Vector<Integer> mzPeakInds = new Vector<Integer>();
		//recursiveThreshold(mzValues, intensityValues, 0, mzValues.length - 1,
			//	scaleLevel, mzPeakInds, 0);

		for (Integer j : mzPeakInds) {
			// Is intensity above the noise level
			if (intensityValues[j] >= scaleLevel) {
				mzPeaks.add(new MzPeak(scan.getScanNumber(), j, mzValues[j],
						intensityValues[j]));
			}
		}
		return mzPeaks.toArray(new MzPeak[0]);
	}

	private DataPoint[] insertEdge(DataPoint[] originalDataPoints) {
		Vector<DataPoint> edgeDataPoint = new Vector<DataPoint>();
		for (int dp = 1; dp < originalDataPoints.length; dp++) {
			if ((originalDataPoints[dp].getIntensity() == 0)
					&& (originalDataPoints[dp - 1].getIntensity() > 0)
					&& (originalDataPoints[dp + 1].getIntensity() == 0)) {
				int i;
				for (i=0; i<5; i++){
					SimpleDataPoint newDp = new SimpleDataPoint(((float)originalDataPoints[dp].getMZ()+(0.0001f*i)),0.0f);
					edgeDataPoint.add(newDp);
				}
			}
		}
		DataPoint[] peaks = edgeDataPoint.toArray(new DataPoint[0]);
		return peaks;
	}

}
