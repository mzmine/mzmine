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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass;

import java.text.Format;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.main.MZmineCore;

/**
 * This class represent an m/z peak within a spectrum
 */
public class ExactMzDataPoint implements DataPoint {

    private double mz, intensity;
    // dulab Edit
    private DataPoint[] rawDataPoints;

    /**
     * This constructor takes the given raw data point to represent this m/z
     * peak.
     * 
     * @param dataPoint
     */
    public ExactMzDataPoint(DataPoint dataPoint) {
	this(dataPoint.getMZ(), dataPoint.getIntensity(),
		new DataPoint[] { dataPoint });
    }

    /**
     * This constructor takes the given m/z and intensity (provided as
     * DataPoint) to represent this m/z peak and sets the raw data points
     * accordingly.
     * 
     * @param dataPoint
     * @param rawDataPoints
     */
    public ExactMzDataPoint(DataPoint dp, DataPoint[] rawDataPoints) {
	this(dp.getMZ(), dp.getIntensity(), rawDataPoints);
    }

    /**
     * This constructor takes the given m/z and intensity to represent this m/z
     * peak and sets the raw data points accordingly.
     * 
     * @param dataPoint
     * @param rawDataPoints
     */
    public ExactMzDataPoint(double mz, double intensity,
	    DataPoint[] rawDataPoints) {
	this.mz = mz;
	this.intensity = intensity;
	this.rawDataPoints = rawDataPoints;
    }

    /**
     * Returns intensity value of this m/z peak
     */
    public double getIntensity() {
	return intensity;
    }

    /**
     * Returns m/z value of the peak on this scan. The value depends of the used
     * mass detector.
     */
    public double getMZ() {
	return mz;
    }

    /**
     * Sets the m/z value of this m/z peak
     */
    public void setMZ(double mz) {
	this.mz = mz;
    }

    /**
     * This method returns an array of raw data points that form this m/z peak
     */
    public DataPoint[] getRawDataPoints() {
	return rawDataPoints;
    }

    public boolean equals(Object obj) {
	if (!(obj instanceof DataPoint))
	    return false;
	DataPoint dp = (DataPoint) obj;
	return (this.mz == dp.getMZ()) && (this.intensity == dp.getIntensity());
    }

    public int hashCode() {
	return (int) (this.mz + this.intensity);
    }

    public String getName() {
	Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
	Format intensityFormat = MZmineCore.getConfiguration()
		.getIntensityFormat();
	String str = "m/z: " + mzFormat.format(mz) + ", intensity: "
		+ intensityFormat.format(intensity);
	return str;
    }
}
