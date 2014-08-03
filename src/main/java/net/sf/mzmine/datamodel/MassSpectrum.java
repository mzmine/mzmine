/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.datamodel;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Range;

/**
 * Represent one mass spectrum. For example, a scan in a raw data file, a
 * predicted isotope pattern, etc.
 */
public interface MassSpectrum {

    /**
     * Returns the m/z range of this Scan. Never returns null.
     * 
     * @return m/z range of this Scan
     */
    @Nonnull
    Range<Double> getMZRange();

    /**
     * Returns the top intensity data point. May return null if there are no
     * data points in this spectrum.
     * 
     * @return Base peak
     */
    @Nullable
    DataPoint getHighestDataPoint();

    /**
     * 
     * @return True if the spectrum is centroided
     */
    boolean isCentroided();

    /**
     * @return Number of m/z and intensity data points. 
     */
    int getNumberOfDataPoints();

    /**
     * Returns data points of this spectrum, always sorted in m/z order.
     * 
     * This method may need to read data from disk, therefore it may be quite
     * slow. Modules should be aware of that and cache the data points if
     * necessary.
     * 
     * @return Data points (m/z and intensity pairs) of this scan
     */
    @Nonnull
    DataPoint[] getDataPoints() throws IOException;

    void setDataPoints(@Nonnull DataPoint newDataPoints[]) throws IOException;

    /**
     * Returns data points in given m/z range, sorted in m/z order.
     * 
     * This method may need to read data from disk, therefore it may be quite
     * slow. Modules should be aware of that and cache the data points if
     * necessary.
     * 
     * @return Data points (m/z and intensity pairs) of this spectrum
     */
    @Nonnull
    DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange) throws IOException;

    /**
     * Returns data points over given intensity, sorted in m/z order.
     * 
     * This method may need to read data from disk, therefore it may be quite
     * slow. Modules should be aware of that and cache the data points if
     * necessary.
     * 
     * @return Data points (m/z and intensity pairs) of this Spectrum
     */
    @Nonnull
    DataPoint[] getDataPointsOverIntensity(double intensity) throws IOException;

}
