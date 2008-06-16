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

public class LorentzianPeak implements PeakModel {

	private float mzMain, intensityMain, HWHM;

	public LorentzianPeak(float mzMain, float intensityMain, float resolution) {
		this.mzMain = mzMain;
		this.intensityMain = intensityMain;
		// HWFM (Half Width at Half Maximum)
		HWHM = mzMain / ((float) resolution * 2);
	}

	/* (non-Javadoc)
	 * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getBasePeakWidth()
	 */
	public Range getBasePeakWidth() {

		/*
		 * Calculates the 0.001% of peak's height. This height value is chosen
		 * because is close to zero. A lower value of intensity in this function
		 * is giving a range too big. If we have a wider range is possible to
		 * make useless comparisons.
		 * 
		 */
		double baseIntensity = intensityMain * 0.001;
		double partA = ((intensityMain / baseIntensity) - 1)
				* Math.pow(HWHM, 2);

		// Using the Lorentzian function we calculate the base peak width,
		float sideRange = (float) Math.sqrt(partA) / 2.0f;

		Range rangePeak = new Range(mzMain - sideRange, mzMain + sideRange);

		return rangePeak;
	}

	/* (non-Javadoc)
	 * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getIntensity(float)
	 */
	public float getIntensity(float mz) {

		double partA = Math.pow(HWHM, 2);
		double partB = intensityMain * partA;
		double partC = Math.pow((mz - mzMain), 2) + partA;
		
		float intensity = (float) (partB / partC);
		
		return intensity;
	}

}
