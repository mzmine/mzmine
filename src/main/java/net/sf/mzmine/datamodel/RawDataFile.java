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
 * Raw data file
 */
public interface RawDataFile {

    /**
     * Returns the name of this data file (can be a descriptive name, not
     * necessarily the original file name)
     */
    @Nonnull
    String getName();

    /**
     * Change the name of this data file
     */
    void setName(@Nonnull String name);

    /**
     * Returns mutable collection of the scans
     */
    @Nonnull
    Collection<MsScan> scans();

    /**
     * Returns sorted array of all MS levels in this file
     */
    @Nonnull
    int[] getMSLevels();


    /**
     * Returns sorted array of all scans in given MS level
     * 
     * @param msLevel
     *            MS level
     * @return Sorted array of scans, never returns null
     */
    @Nonnull
    MsScan[] getScans(int msLevel);

    /**
     * Returns sorted array of all scans in given MS level and retention time
     * range
     * 
     * @param msLevel
     *            MS level
     * @param rtRange
     *            Retention time range
     * @return Sorted array of scan numbers, never returns null
     */
    @Nonnull
    MsScan[] getScans(int msLevel, @Nonnull Range<Double> rtRange);

    /**
     * 
     * @param scan
     *            Desired scan number
     * @return Desired scan, or null if no scan exists with that number
     */
    @Nullable
    MsScan getScan(int scanNumber);

    @Nonnull
    MsScan getMostIntenseScan(int msLevel);

    @Nonnull
    Range<Double> getDataMZRange();

    @Nonnull
    Range<Double> getDataRTRange();

    @Nonnull
    Range<Double> getDataMZRange(int msLevel);

    @Nonnull
    Range<Double> getDataRTRange(int msLevel);

    /**
     * Remove all data associated to this file from the disk.
     */
    void dispose();
}
