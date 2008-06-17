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

package net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.peakmodel.impl;

import net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel;
import net.sf.mzmine.util.Range;

public class GaussPeak implements PeakModel {
	
	private float mzMain, intensityMain, FWHM;
	private double partA;

	/* (non-Javadoc)
	 * @see net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel#setParameters(float, float, float)
	 */
	public void setParameters(float mzMain, float intensityMain,
			float resolution) {
			this.mzMain = mzMain;
			this.intensityMain = intensityMain;
			
			// FWFM (Full Width at Half Maximum)
			FWHM = mzMain
					/ ((float) resolution );
			partA = 2 * Math.pow(FWHM, 2);		
	}

	/* (non-Javadoc)
	 * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getBasePeakWidth()
	 */
	public Range getWidth(float	partialIntensity) {

		/*
		 * Calculates the 0.0001% of peak's height and applies natural
		 * logarithm. This height value is chosen because is the closest to
		 * zero. The zero value is not used because the Gaussian function
		 * tends to infinite at this height
		 */
		
		if (partialIntensity <= 0)
			partialIntensity = 1;
		
		double ln = Math.abs(Math.log(partialIntensity));

		// Using the Gaussian function we calculate the base peak width,
		float sideRange = (float) Math.sqrt(partA * ln) / 2.0f;

		Range rangePeak = new Range(mzMain - sideRange,
				mzMain + sideRange);
		
		return rangePeak;
	}

	/* (non-Javadoc)
	 * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getIntensity(float)
	 */
	public float getIntensity(float mz) {
		double partB = -1 * Math.pow((mzMain - mz), 2) / partA;
		float intensity =  (float) (intensityMain * Math.exp(partB));
		return intensity;
	}


}
