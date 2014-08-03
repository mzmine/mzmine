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

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Range;

/**
 * Represent one MS spectrum in a raw data file.
 */
public interface MsScan extends MassSpectrum {

    /**
     * 
     * @return RawDataFile containing this Scan
     */
    @Nonnull
    RawDataFile getDataFile();

    /**
     * 
     * @return Scan number
     */
    int getScanNumber();

    /**
     * 
     * @return MS level
     */
    int getMSLevel();

    void setMSLevel(int msLevel);
    
    /**
     * Returns the sum of intensities of all data points.
     * 
     * @return Total ion current
     */
    double getTIC();

    /**
     * 
     * @return Retention time of this scan in minutes
     */
    double getRetentionTime();
    
    void setRetentionTime(double retentionTime);

    /**
     * @return the actual scanning range of the instrument
     */
    Range<Double> getScanRange();

    @Nonnull
    Polarity getPolarity();

    /**
     * 
     * @return array of fragment scans, or null if there are none
     */
    @Nonnull
    Collection<MsMsScan> fragmentScans();

    @Nonnull
    Collection<MassList> massLists();

    /**
     * Shortcut method
     * @param name
     * @return
     */
    @Nullable
    MassList getMassList(@Nonnull String name);

}
