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

package net.sf.mzmine.modules.peakpicking.threestep.massdetection.exactmass.peakmodels;

import net.sf.mzmine.modules.peakpicking.threestep.massdetection.exactmass.PeakModel;
import net.sf.mzmine.util.Range;

/**
 * 
 * This class represents a Gaussian model extended by a triangle with the height
 * of 5% intensity and the width of 1 m/z. The purpose is to remove any small
 * side peak fluctuations which occur for example in case of FTMS data
 * ("shoulder peaks").
 * 
 */
public class GaussPlusTrianglePeak implements PeakModel {

    /**
     * This constant defines at what percentage of the intensity we set the
     * height of our triangle. Default is 5%.
     */
    public static final double shoulderIntensityRatio = 0.05f;

    private GaussPeak gaussModel;
    private double mzMain, shoulderIntensity;

    public GaussPlusTrianglePeak() {
        gaussModel = new GaussPeak();
    }

    public void setParameters(double mzMain, double intensityMain,
            double resolution) {

        this.mzMain = mzMain;
        this.shoulderIntensity = intensityMain * shoulderIntensityRatio;

        // update the Gaussian model
        gaussModel.setParameters(mzMain, intensityMain, resolution);

    }

    /**
     * @see net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel#setParameters(double,
     *      double, double)
     */
    public double getIntensity(double mz) {

        double gaussIntensity = gaussModel.getIntensity(mz);

        double mzDiff = Math.abs(mzMain - mz);

        if (mzDiff >= 1)
            return gaussIntensity;

        double triangleIntensity = shoulderIntensity * (1 - mzDiff);

        return Math.max(gaussIntensity, triangleIntensity);

    }

    public Range getWidth(double partialIntensity) {

        // The height value must be bigger than zero
        if (partialIntensity <= 0)
            return new Range(0, Double.MAX_VALUE);

        if (partialIntensity < shoulderIntensity)
            return new Range(mzMain - 1, mzMain + 1);

        Range gaussWidth = gaussModel.getWidth(partialIntensity);

        return gaussWidth;

    }

}
