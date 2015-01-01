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

package net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakmodels;

import net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter.PeakModel;

import com.google.common.collect.Range;

/**
 * 
 * This class represents a Lorentzian function model, using the formula:
 * 
 * f(x) = a / (1 + ((x-b)^2 / (HWHM^2)))
 * 
 * where
 * 
 * a... height of the model (intensityMain) b... center of the model (mzMain)
 * HWHM... Half Width at Half Maximum
 * 
 */

public class LorentzianPeak implements PeakModel {

    private double mzMain, intensityMain, squareHWHM;

    /**
     * @see net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel#setParameters(double,
     *      double, double)
     */
    public void setParameters(double mzMain, double intensityMain,
	    double resolution) {

	this.mzMain = mzMain;
	this.intensityMain = intensityMain;

	// HWFM (Half Width at Half Maximum) ^ 2
	squareHWHM = (double) Math.pow((mzMain / resolution) / 2, 2);
    }

    /**
     * @see net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.peakmodel.PeakModel#getBasePeakWidth()
     */
    public Range<Double> getWidth(double partialIntensity) {

	// The height value must be bigger than zero.
	if (partialIntensity <= 0)
	    return Range.atLeast(0.0);

	// Using the Lorentzian function we calculate the peak width
	double squareX = ((intensityMain / partialIntensity) - 1) * squareHWHM;

	double sideRange = (double) Math.sqrt(squareX);

	// This range represents the width of our peak in m/z terms
	Range<Double> rangePeak = Range.closed(mzMain - sideRange, mzMain
		+ sideRange);

	return rangePeak;
    }

    /**
     * @see net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.peakmodel.PeakModel#getIntensity(double)
     */
    public double getIntensity(double mz) {

	// Using the Lorentzian function we calculate the intensity at given
	// m/z
	double squareX = (double) Math.pow((mz - mzMain), 2);
	double ratio = squareX / squareHWHM;
	double intensity = intensityMain / (1 + ratio);
	return intensity;
    }

}
