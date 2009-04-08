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

package net.sf.mzmine.data.impl;

import java.text.Format;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.main.MZmineCore;

/**
 * This class represents one datapoint of a spectra (m/z and intensity)
 */
public class SimpleDataPoint implements DataPoint {

	private double mz, intensity;

	/**
	 * Constructor which copies the data from another DataPoint
	 */
	public SimpleDataPoint(DataPoint dp) {
		this.mz = dp.getMZ();
		this.intensity = dp.getIntensity();
	}
	
	/**
	 * @param mz
	 * @param intensity
	 */
	public SimpleDataPoint(double mz, double intensity) {
		this.mz = mz;
		this.intensity = intensity;
	}

	public double getIntensity() {
		return intensity;
	}

	public double getMZ() {
		return mz;
	}

	public void setIntensity(double intensity) {
		this.intensity = intensity;
	}

	public void setMZ(double mz) {
		this.mz = mz;
	}

	public String toString() {
		Format mzFormat = MZmineCore.getMZFormat();
		Format intensityFormat = MZmineCore.getIntensityFormat();
		String str = "m/z: " + mzFormat.format(mz) + ", intensity: "
				+ intensityFormat.format(intensity);
		return str;
	}

}
