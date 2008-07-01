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

package net.sf.mzmine.modules.peakpicking.threestep.massdetection.exactmass.peakmodels;

import net.sf.mzmine.modules.peakpicking.threestep.massdetection.exactmass.PeakModel;
import net.sf.mzmine.util.Range;

public class LorentzianPeakWithShoulderExtended implements PeakModel {

	private float mzMain, intensityMain, HWHM, MWHM;
	private Range rangePeak, rangePeakModified;
	private float diffWidth, diffMass, widthStep;

	
	/**
	 * @see net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel#setParameters(float,
	 *      float, float)
	 */
	public void setParameters(float mzMain, float intensityMain,
			float resolution) {
		this.mzMain = mzMain;
		this.intensityMain = intensityMain;

		// HWFM (Half Width at Half Maximum)
		HWHM = (mzMain / resolution) / 2.0f ;

		// We set a width given by 5% of actual resolution called
		// MWFM (Modified Width at Half Maximum) and we use it to calculate the
		// width at the level defined in getWidth() method's parameter.
		MWHM = HWHM * 40.0f;

		// Using the Gaussian function we calculate the peak width at 5% of
		// intensity. This % of the intensity is chosen according with the
		// definition of lateral peaks. Lateral peaks is any under 5% of
		// intensity of the main peak.

		double partA = 2 * Math.pow(HWHM, 2);
		double ln = Math.abs(Math.log(0.05));
		float sideRange = (float) Math.sqrt(partA * ln) / 2.0f;

		// We use this range to recognize when to use a different value of width
		// for Lorentzian function
		rangePeak = new Range(mzMain - sideRange, mzMain + sideRange);
		

		// This value is the difference between the FWHM used to calculate the
		// width (Gaussian peak) at 5% of intensity and the MWHM used to
		// calculate the width (Lorentzian function) at 0.1% of intensity.
		diffWidth = MWHM - HWHM;

	}

	/**
	 * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getBasePeakWidth()
	 */
	public Range getWidth(float partialIntensity) {

		/*
		 * The height value must be bigger than zero.The zero value is not used
		 * because the Lorentzian function tends to infinite and in this
		 * function with zero intensity we get a NaN. If that is the case we
		 * have a too big range and could result in to make useless comparisons.
		 */

		if (partialIntensity < 0)
			return new Range(0, Float.MAX_VALUE);
		if (partialIntensity == 0)
			partialIntensity = 1;

		double partA = ((intensityMain / partialIntensity) - 1)
				* Math.pow(MWHM, 2);

		// Using the Lorentzian function we calculate the base peak width,
		float sideRange = (float) Math.sqrt(partA) / 2.0f;

		rangePeakModified = new Range(mzMain - sideRange, mzMain + sideRange);

		// These two values are used to calculate the appropiated HWHM to
		// determine the intensity of our modified lorentzian peak
		diffMass = rangePeakModified.getMax() - rangePeak.getMax();
		widthStep = diffWidth / diffMass;

		return rangePeakModified;
	}

	/**
	 * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getIntensity(float)
	 */
	public float getIntensity(float mz) {

		float width = MWHM;
		float height = intensityMain * 0.05f;

		// Depending of the value of mz, we use our increased width to get
		// larger shoulders and try to cover more lateral peaks.
		if ((mz > rangePeak.getMin()) && (mz < rangePeak.getMax())){
			height = intensityMain;
			width = HWHM; 
		}

		// We calculate the intensity using the Lorentzian function

		double partA = width * width;
		double partB = height * partA;
		double partC = Math.pow((mz - mzMain), 2) + partA;

		float intensity = (float) (partB / partC);
		
		return intensity;
	}

}
