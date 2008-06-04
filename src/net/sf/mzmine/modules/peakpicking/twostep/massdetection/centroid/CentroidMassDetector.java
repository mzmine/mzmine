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

package net.sf.mzmine.modules.peakpicking.twostep.massdetection.centroid;

import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;

public class CentroidMassDetector implements MassDetector {

	// parameter values
	private float noiseLevel;

	public CentroidMassDetector(CentroidMassDetectorParameters parameters) {
		this.noiseLevel = (Float) parameters
				.getParameterValue(CentroidMassDetectorParameters.noiseLevel);
	}

	public MzPeak[] getMassValues(Scan scan) {
		Vector<MzPeak> mzPeaks = new Vector<MzPeak>();
		DataPoint dataPoints[] = scan.getDataPoints();

		// Find possible mzPeaks
		for (int j = 0; j < dataPoints.length; j++) {

			// Is intensity above the noise level?
			if (dataPoints[j].getIntensity() >= noiseLevel) {
				// Yes, then mark this index as mzPeak
				mzPeaks.add(new MzPeak(scan.getScanNumber(), dataPoints[j]
						.getMZ(), dataPoints[j].getIntensity()));
			}
		}
		return mzPeaks.toArray(new MzPeak[0]);
	}

}
