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

package net.sf.mzmine.modules.masslistmethods.shoulderpeaksfilter;

import com.google.common.collect.Range;

public interface PeakModel {

    /**
     * This function calculates the width of the peak at the base
     * 
     * @return Range base width
     * 
     */
    public Range<Double> getWidth(double partialIntensity);

    /**
     * This function returns the intensity of modeled peak at certain m/z
     * 
     * @return double intensity
     */
    public double getIntensity(double mz);

    /**
     * This function set all required parameters to construct a peak model
     * 
     * @param mzMain
     * @param intensityMain
     * @param resolution
     */
    public void setParameters(double mzMain, double intensityMain,
	    double resolution);

}
