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
 * This class represents a Gaussian model, using the formula:
 * 
 * f(x) = a * e ^ ((x-b)^2 / (-2 * c^2)) 
 * 
 * where
 * 
 * a... height of the model (intensityMain)
 * b... center of the model (mzMain)
 * c... FWHM / (2 * sqrt(2 . ln(2)))  
 * FWHM... Full Width at Half Maximum         
 *
 */
public class GaussPeak implements PeakModel {

    private float mzMain, intensityMain, FWHM, partC, part2C2;

    /**
     * @see net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.peakmodel.PeakModel#setParameters(float,
     *      float, float)
     */
    public void setParameters(float mzMain, float intensityMain,
            float resolution) {
        
        this.mzMain = mzMain;
        this.intensityMain = intensityMain;

        // FWFM (Full Width at Half Maximum)
        FWHM = (mzMain / resolution);
        partC = FWHM / 2.354820045f;
        part2C2 = 2f * (float) Math.pow(partC, 2);
    }

    /**
     * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getBasePeakWidth()
     */
    public Range getWidth(float partialIntensity) {

        // The height value must be bigger than zero.
        if (partialIntensity <= 0)
            return new Range(0, Float.MAX_VALUE);

        // Using the Gaussian function we calculate the peak width at intensity
        // given (partialIntensity)

        float portion = partialIntensity / intensityMain;
        float ln = -1 * (float) Math.log(portion);

        float sideRange = (float) (Math.sqrt(part2C2 * ln));

        // This range represents the width of our peak in m/z
        Range rangePeak = new Range(mzMain - sideRange, mzMain + sideRange);

        return rangePeak;
    }

    /**
     * @see net.sf.mzmine.modules.peakpicking.twostep.peakmodel.PeakModel#getIntensity(float)
     */
    public float getIntensity(float mz) {

        // Using the Gaussian function we calculate the intensity at given m/z
        float diff2 = (float) Math.pow(mz - mzMain, 2);
        float exponent = -1 * (diff2 / part2C2);
        float eX = (float) Math.exp(exponent);
        float intensity = intensityMain * eX;
        return intensity;
    }

}
