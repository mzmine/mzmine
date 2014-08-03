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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Range;

/**
 * A class representing raw data of a detected feature.
 */
public interface FeatureRawData {

    /**
     * @return Raw data file where this feature is present, or null if this peak
     *         is not connected to any raw data.
     */
    @Nonnull
    RawDataFile getDataFile();

    /**
     * Assigns a raw data file to this feature.
     */
    void setDataFile(@Nonnull RawDataFile dataFile);

    /**
     * @return Scans that contain this peak, or null if this peak is not
     *         connected to any raw data.
     */
    @Nonnull
    MsScan[] getScans();

    /**
     * This method returns m/z and intensity of this peak in a given scan. This
     * m/z and intensity does not need to match any actual raw data point (e.g.
     * in continuous spectra, the m/z value may be calculated from the data
     * points forming the m/z signal).
     */
    @Nonnull
    DataPoint[] getDataPoints();

    /**
     * @return The most representative scan of this feature (with highest signal
     *         intensity), or null if this peak is not connected to any raw
     *         data.
     */
    @Nonnull
    MsScan getRepresentativeScan();

    /**
     * Returns the retention time range of all raw data points used to detect
     * this peak
     */
    @Nonnull
    Range<Double> getRawDataPointsRTRange();

    /**
     * Returns the range of m/z values of all raw data points used to detect
     * this peak
     */
    @Nonnull
    Range<Double> getRawDataPointsMZRange();

    /**
     * Returns the range of intensity values of all raw data points used to
     * detect this peak
     */
    @Nonnull
    Range<Double> getRawDataPointsIntensityRange();

    /**
     * Returns the number of scan that represents the fragmentation of this peak
     * in MS2 level.
     */
    @Nullable
    MsScan getMostIntenseFragmentScan();

}
