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

    private float mzMain, intensityMain, squareHWHM;

    /**
     * @see net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel#setParameters(float,
     *      float, float)
     */
    public void setParameters(float mzMain, float intensityMain,
            float resolution) {

        this.mzMain = mzMain;
        this.intensityMain = intensityMain;

        // HWFM (Half Width at Half Maximum) ^ 2
        squareHWHM = (float) Math.pow((mzMain / resolution) / 2, 2);
    }

    /**
     * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getBasePeakWidth()
     */
    public Range getWidth(float partialIntensity) {

        // The height value must be bigger than zero.
        if (partialIntensity <= 0)
            return new Range(0, Float.MAX_VALUE);

        // Using the Lorentzian function we calculate the peak width
        float squareX = ((intensityMain / partialIntensity) - 1) * squareHWHM;

        float sideRange = (float) Math.sqrt(squareX);

        // This range represents the width of our peak in m/z terms
        Range rangePeak = new Range(mzMain - sideRange, mzMain + sideRange);

        return rangePeak;
    }

    /**
     * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getIntensity(float)
     */
    public float getIntensity(float mz) {

        // Using the Lorentzian function we calculate the intensity at given
        // m/z
        float squareX = (float) Math.pow((mz - mzMain), 2);
        float ratio = squareX / squareHWHM;
        float intensity = intensityMain / (1 + ratio);
        return intensity;
    }

}
