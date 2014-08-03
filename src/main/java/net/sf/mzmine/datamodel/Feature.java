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

/**
 * A detected feature, characterized mainly by its m/z value, retention time,
 * height and area. A feature can also be called a chromatographic peak, or an
 * isotope trace. A single compound analyzed by MS can produce many features in
 * the data (isotopes, adducts, fragments etc.). The feature can be bound to raw
 * data file, if the raw data is available.
 */
public interface Feature {

    /**
     * @return The status of this feature.
     */
    @Nonnull
    FeatureStatus getFeatureStatus();

    /**
     * Sets a new status of this feature.
     */
    void setFeatureStatus(@Nonnull FeatureStatus newStatus);

    /**
     * @return m/z value of this feature. The m/z value might be different from
     *         the raw m/z data points.
     */
    double getMZ();

    /**
     * Sets new m/z value of this feature.
     */
    void setMZ(double newMZ);

    /**
     * @return The retention time of this feature.
     */
    double getRT();

    /**
     * Sets new retention time to this feature.
     */
    void setRT(double newRT);

    /**
     * @return The height of this feature. The height might be different from
     *         the raw data point intensities (e.g. normalized).
     */
    double getHeight();

    /**
     * Sets new height to this feature.
     */
    void setHeight(double newHeight);

    /**
     * @return The area of this feature. The area might be different from the
     *         area of the raw data points (e.g. normalized).
     */
    double getArea();

    /**
     * Sets new area to this feature.
     */
    void setArea(double newArea);

    /**
     * @return Raw data file where this peak is present, or null if this peak is
     *         not connected to any raw data.
     */
    @Nullable
    FeatureRawData getRawData();

    /**
     * Assigns a raw data file to this feature.
     */
    void setRawData(@Nullable FeatureRawData rawData);

    /**
     * Returns the isotope pattern of this peak or null if no pattern is
     * attached
     */
    @Nullable
    IsotopePattern getIsotopePattern();

    /**
     * Sets the isotope pattern of this peak
     */
    void setIsotopePattern(@Nonnull IsotopePattern isotopePattern);

    /**
     * Returns the charge of this feature. If the charge is unknown, returns 0.
     */
    int getCharge();

    /**
     * Sets the charge of this feature. Unknown charge is represented by 0.
     */
    void setCharge(int charge);

}
