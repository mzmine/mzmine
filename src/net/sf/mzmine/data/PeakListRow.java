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

package net.sf.mzmine.data;


/**
 * 
 */
public interface PeakListRow {

    /**
     * Return raw datas with peaks on this row
     */
    public RawDataFile[] getRawDataFiles();

    /**
     * Returns ID of this row
     */
    public int getID();

    /**
     * Returns number of peaks assigned to this row
     */
    public int getNumberOfPeaks();

    /**
     * Return peaks assigned to this row
     */
    public Peak[] getPeaks();

    /**
     * Returns peak for given raw data file
     */
    public Peak getPeak(RawDataFile rawData);

    /**
     * Add a peak
     */
    public void addPeak(RawDataFile rawData, Peak original, Peak current);
    
    /**
     * Has a peak?
     */
    public boolean hasPeak(Peak peak);
        
    /**
     * Returns peak for given raw data file
     */
    public Peak getOriginalPeakListEntry(RawDataFile rawData);

    /**
     * Returns average M/Z for peaks on this row
     */
    public float getAverageMZ();

    /**
     * Returns average RT for peaks on this row
     */
    public float getAverageRT();

    /**
     * Returns comment for this row
     */
    public String getComment();

    /**
     * Sets comment for this row
     */
    public void setComment(String comment);

    /**
     * Add a new identity candidate (result of identification method)
     * 
     * @param identity New compound identity
     */
    public void addCompoundIdentity(CompoundIdentity identity);

    /**
     * Returns all candidates for this compound's identity
     * 
     * @return Identity candidates
     */
    public CompoundIdentity[] getCompoundIdentities();

    /**
     * Returns preferred compound identity among candidates
     * 
     * @return Preferred identity
     */
    public CompoundIdentity getPreferredCompoundIdentity();

    /**
     * Sets a preferred compound identity among candidates
     * 
     * @param identity Preferred identity
     */
    public void setPreferredCompoundIdentity(CompoundIdentity identity);

    /**
     * Returns maximum raw data point intensity among all peaks in this row
     * 
     * @return Maximum intensity
     */
    public float getDataPointMaxIntensity();

}
