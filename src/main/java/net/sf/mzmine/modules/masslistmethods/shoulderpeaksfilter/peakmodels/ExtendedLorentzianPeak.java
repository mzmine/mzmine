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
 * This class is using two Lorentzian peak models. One is used to model the
 * actual peak, and one is used to model a peak with 5% of the intensity of the
 * main peak, and 5% of its resolution. This broader and smaller peak should
 * cover side regions, for example FTMS shoulder peaks.
 * 
 */
public class ExtendedLorentzianPeak implements PeakModel {

    /**
     * This constant defines at what percentage of the intensity we set as the
     * border between the main and the broad (shoulder) peak models. Default is
     * 5%.
     */
    public static final double shoulderIntensityRatio = 0.05;

    /**
     * This constant defines what percentage of the resolution shall we use to
     * build the broad (shoulder) peak model. Default is 5%.
     */
    public static final double shoulderResolutionRatio = 0.05;

    private LorentzianPeak mainPeak, shoulderPeak;
    private Range<Double> mainPeakRange;
    private double shoulderIntensity;

    public ExtendedLorentzianPeak() {
	mainPeak = new LorentzianPeak();
	shoulderPeak = new LorentzianPeak();
    }

    /**
     * @see net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel#setParameters(double,
     *      double, double)
     */
    public void setParameters(double mzMain, double intensityMain,
	    double resolution) {

	mainPeak.setParameters(mzMain, intensityMain, resolution);
	shoulderPeak.setParameters(mzMain, intensityMain
		* shoulderIntensityRatio, resolution * shoulderResolutionRatio);

	this.shoulderIntensity = intensityMain * shoulderIntensityRatio;
	this.mainPeakRange = mainPeak.getWidth(shoulderIntensity);

    }

    /**
     * @see net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.peakmodel.PeakModel#getBasePeakWidth()
     */
    public Range<Double> getWidth(double partialIntensity) {

	// The height value must be bigger than zero.
	if (partialIntensity <= 0)
	    return Range.atLeast(0.0);

	if (partialIntensity < shoulderIntensity)
	    return shoulderPeak.getWidth(partialIntensity);
	else
	    return mainPeak.getWidth(partialIntensity);

    }

    /**
     * @see net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter.peakpicking.twostep.peakmodel.PeakModel#getIntensity(double)
     */
    public double getIntensity(double mz) {

	if (mainPeakRange.contains(mz))
	    return mainPeak.getIntensity(mz);
	else
	    return shoulderPeak.getIntensity(mz);

    }

}
