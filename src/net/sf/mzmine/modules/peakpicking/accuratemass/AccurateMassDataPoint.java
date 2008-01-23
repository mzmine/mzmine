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

package net.sf.mzmine.modules.peakpicking.accuratemass;

import net.sf.mzmine.data.DataPoint;

/**
 * This class represents one m/z peak in a spectra. The m/z of this peak is
 * calculated as a weighted average of m/z values of all raw data points of the
 * continuous mode peak. Those source raw data points are saved and can be
 * obtained by calling getRawDataPoints()
 */
class AccurateMassDataPoint implements DataPoint {

    private float mz, intensity;
    private DataPoint rawDataPoints[];

    /**
     * Creates a new AccurateMassDataPoint from source raw data points
     */
    AccurateMassDataPoint(DataPoint rawDataPoints[]) {

        this.rawDataPoints = rawDataPoints;

        // Variables for calculating the weighted m/z average
        float totalSum = 0f;
        float intensitySum = 0f;

        // Iterate all raw data points
        for (DataPoint rawDataPoint : rawDataPoints) {

            totalSum += rawDataPoint.getMZ() * rawDataPoint.getIntensity();
            intensitySum += rawDataPoint.getIntensity();

            // Remember top intensity
            if (rawDataPoint.getIntensity() > intensity) {
                intensity = rawDataPoint.getIntensity();
            }

        }

        // Calculate m/z value
        this.mz = totalSum / intensitySum;

    }

    public float getIntensity() {
        return intensity;
    }

    public float getMZ() {
        return mz;
    }

    DataPoint[] getRawDataPoints() {
        return rawDataPoints;
    }

}
