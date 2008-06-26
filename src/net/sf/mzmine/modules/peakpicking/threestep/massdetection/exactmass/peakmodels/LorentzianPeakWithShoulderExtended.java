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

	private float mzMain, intensityMain, FWHM, MWHM;
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

		// FWFM (Full Width at Half Maximum)
		FWHM = mzMain / ((float) resolution);

		// We set a width given by 5% of actual resolution called
		// MWFM (Modified Width at Half Maximum) and we use it to calculate the
		// width at the level defined in getWidth() method's parameter.
		MWHM = mzMain / ((float) resolution * 0.05f);

		// Using the Gaussian function we calculate the peak width at 5% of
		// intensity. This % of the intensity is chosen according with the
		// definition of lateral peaks. Lateral peaks is any under 5% of
		// intensity of the main peak.

		double partA = 2 * Math.pow(FWHM, 2);
		double ln = Math.abs(Math.log(intensityMain * 0.05));
		float sideRange = (float) Math.sqrt(partA * ln) / 2.0f;

		// We use this range to recognize when to use a different value of width
		// for Lorentzian function
		rangePeak = new Range(mzMain - sideRange, mzMain + sideRange);

		// This value is the difference between the FWHM used to calculate the
		// width (Gaussian peak) at 5% of intensity and the MWHM used to
		// calculate the width (Lorentzian function) at 0.1% of intensity.
		diffWidth = MWHM - FWHM;

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

		if (partialIntensity <= 0)
			return new Range(0, Float.MAX_VALUE);

		double partA = ((intensityMain / partialIntensity) - 1)
				* Math.pow(MWHM, 2);

		// Using the Lorentzian function we calculate the base peak width,
		float sideRange = (float) Math.sqrt(partA) / 2.0f;

		rangePeakModified = new Range(mzMain - sideRange, mzMain + sideRange);

		// These two values are used to calculate the appropiated FWHM to
		// determine the intensity of our modified lorentzian peak
		diffMass = rangePeakModified.getMax() - rangePeak.getMax();
		widthStep = diffWidth / diffMass;

		return rangePeakModified;
	}

	/**
	 * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getIntensity(float)
	 */
	public float getIntensity(float mz) {

		float width = FWHM;

		// Depending of the value of mz, we use our increased width to get
		// larger shoulders and try to cover more lateral peaks.
		if (mz < rangePeak.getMin())
			width = FWHM + (widthStep * Math.abs(rangePeak.getMin() - mz));
		if (mz > rangePeak.getMin())
			width = FWHM + (widthStep * Math.abs(mz - rangePeak.getMax()));

		// We calculate the intensity using the Lorentzian function

		double partA = Math.pow(width, 2);
		double partB = intensityMain * partA;
		double partC = Math.pow((mz - mzMain), 2) + partA;

		float intensity = (float) (partB / partC);

		return intensity;
	}

}
