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

package net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.peakmodel;

import net.sf.mzmine.util.Range;

public interface PeakModel {
	
	/**
	 * This function calculates the width of the peak at the base
	 * 
	 * @return Range base width
	 * 
	 */
	public Range getWidth(float	partialIntensity);
	
	/**
	 * This function returns the intensity of modeled peak at certain m/z
	 * 
	 * @return float intensity
	 */
	public float getIntensity (float mz);

	/**
	 * This function set all required parameters to construct a peak model
	 * 
	 * @param mzMain
	 * @param intensityMain
	 * @param resolution
	 */
	public void setParameters(float mzMain, float intensityMain,
			float resolution);

}
