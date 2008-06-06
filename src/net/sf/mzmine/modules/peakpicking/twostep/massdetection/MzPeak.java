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

package net.sf.mzmine.modules.peakpicking.twostep.massdetection;

import net.sf.mzmine.data.DataPoint;

/**
 * This class represent an m/z peak
 */
public class MzPeak implements DataPoint {

	private float mz, intensity;
	private DataPoint[] rawDataPoints;

	/**
	 * This constructor takes this DataPoint to represent the portion of a peak
	 * (m/z domain) on certain scan.The raw data points that conform these m/z
	 * peak correspond to all range (start & end) of the peak in m/z domain, in
	 * this case only correspond to one single DataPoint.
	 * 
	 * @param dataPoint
	 */
	public MzPeak(DataPoint dataPoint) {
		this.mz = dataPoint.getMZ();
		this.intensity = dataPoint.getIntensity();
		DataPoint[] fakeRawDataPoints = { dataPoint };
		this.rawDataPoints = fakeRawDataPoints;
	}

	/**
	 * This constructor take this DataPoint to represent the portion of a peak
	 * (m/z domain) on certain scan. The raw data points that conform these m/z
	 * peak correspond to all range (start & end) of the peak in m/z domain.
	 * 
	 * @param dataPoint
	 * @param rawDataPoints
	 */
	public MzPeak(DataPoint dataPoint, DataPoint[] rawDataPoints) {
		this.mz = dataPoint.getMZ();
		this.intensity = dataPoint.getIntensity();
		this.rawDataPoints = rawDataPoints;
	}

	/**
	 * Returns intensity value of the peak on this scan. The value depends of
	 * the used mass detector.
	 */
	public float getIntensity() {
		return intensity;
	}

	/**
	 * Returns m/z value of the peak on this scan. The value depends of the used
	 * mass detector.
	 */
	public float getMZ() {
		return mz;
	}

	/**
	 * This method returns an array of raw data points that form this peak
	 */
	public DataPoint[] getRawDataPoints() {
		return rawDataPoints;
	}

}