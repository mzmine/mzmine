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

public class GaussPeak implements PeakModel {

	
	private float mzMain, intensityMain, FWHM;
	private double partA;
	

	/**
	 * @see net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel#setParameters(float,
	 *      float, float)
	 */
	public void setParameters(float mzMain, float intensityMain,
			float resolution) {
		this.mzMain = mzMain;
		this.intensityMain = intensityMain;

		// FWFM (Full Width at Half Maximum)
		FWHM = mzMain / resolution;
		partA = 2 * Math.pow(FWHM, 2);
	}

	/**
	 * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getBasePeakWidth()
	 */
	public Range getWidth(float partialIntensity) {

		/*
		 * The height value must be bigger than zero.The zero value is not used
		 * because the Gaussian function tends to infinite and in this function
		 * with zero intensity we get a NaN. If that is the case we have a too
		 * big range and could result in to make useless comparisons.
		 */

		if (partialIntensity < 0)
			return new Range(0, Float.MAX_VALUE);
		if (partialIntensity == 0)
			partialIntensity = 1;
		
		double portion = partialIntensity/intensityMain;
		double ln = Math.abs(Math.log(portion));

		// Using the Gaussian function we calculate the peak width at intensity given (partialIntensity),
		float sideRange = (float) (Math.sqrt(partA * ln) / 2.0f );

		// This range represents the width of our peak in m/z terms
		Range rangePeak = new Range(mzMain - sideRange, mzMain + sideRange);

		return rangePeak;
	}

	/**
	 * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getIntensity(float)
	 */
	public float getIntensity(float mz) {

		// Using the Gaussian function we calculate the intensity m/z given (mz)
		double diff = (mz - mzMain) * 2;
		double diff2 = diff * diff;
		double partB = -1 * (diff2 / partA);
		double eX = Math.exp(partB);
		float intensity = (float) (intensityMain * eX);
		
		//double partB = -1 * (Math.pow((mz - mzMain), 2) / partA );
		//float intensity = (float) (intensityMain * Math.exp(partB));
		return intensity;
	}

}
