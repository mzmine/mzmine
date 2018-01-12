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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.datamodel;

public interface PeakListRow {

    /**
     * Return raw data with peaks on this row
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
    public Feature[] getPeaks();

    /**
     * Returns peak for given raw data file
     */
    public Feature getPeak(RawDataFile rawData);

    /**
     * Add a peak
     */
    public void addPeak(RawDataFile rawData, Feature peak);

    /**D
     * Remove a peak
     */
    public void removePeak(RawDataFile file);

    /**
     * Has a peak?
     */
    public boolean hasPeak(Feature peak);

    /**
     * Has a peak?
     */
    public boolean hasPeak(RawDataFile rawData);

    /**
     * Returns average M/Z for peaks on this row
     */
    public double getAverageMZ();

    /**
     * Returns average RT for peaks on this row
     */
    public double getAverageRT();

    /**
     * Returns average height for peaks on this row
     */
    public double getAverageHeight();

    /**
     * Returns the charge for peak on this row. If more charges are found 0 is
     * returned
     */
    public int getRowCharge();

    /**
     * Returns average area for peaks on this row
     */
    public double getAverageArea();

    /**
     * Returns comment for this row
     */
    public String getComment();

    /**
     * Sets comment for this row
     */
    public void setComment(String comment);

    /**
     * Sets average mz for this row
     */
    public void setAverageMZ(double mz);

    /**
     * Sets average rt for this row
     */
    public void setAverageRT(double rt);

    /**
     * Add a new identity candidate (result of identification method)
     * 
     * @param identity
     *            New peak identity
     * @param preffered
     *            boolean value to define this identity as preferred identity
     */
    public void addPeakIdentity(PeakIdentity identity, boolean preffered);

    /**
     * Remove identity candidate
     * 
     * @param identity
     *            Peak identity
     */
    public void removePeakIdentity(PeakIdentity identity);

    /**
     * Returns all candidates for this peak's identity
     * 
     * @return Identity candidates
     */
    public PeakIdentity[] getPeakIdentities();

    /**
     * Returns preferred peak identity among candidates
     * 
     * @return Preferred identity
     */
    public PeakIdentity getPreferredPeakIdentity();

    /**
     * Sets a preferred peak identity among candidates
     * 
     * @param identity
     *            Preferred identity
     */
    public void setPreferredPeakIdentity(PeakIdentity identity);

    
    /**
     * Adds a new PeakInformation object. 
     * 
     * PeakInformation is used to keep extra information about peaks in the 
     * form of a map <propertyName, propertyValue>
     * 
     * @param information object
     */
    
    public void setPeakInformation(PeakInformation information);
    
    
    /**
     * Returns PeakInformation
     * @return 
     */
    
    public PeakInformation getPeakInformation();
    
    /**
     * Returns maximum raw data point intensity among all peaks in this row
     * 
     * @return Maximum intensity
     */
    public double getDataPointMaxIntensity();

    /**
     * Returns the most intense peak in this row
     */
    public Feature getBestPeak();
    
    /**
     * Returns the most intense fragmentation scan in this row
     */
    public Scan getBestFragmentation();

    /**
     * Returns the most intense isotope pattern in this row. If there are no
     * isotope patterns present in the row, returns null.
     */
    public IsotopePattern getBestIsotopePattern();
    
    // DorresteinLaB edit
    /**
     * reset the rowID
     */
    public void setID(int id);
    
    // End DorresteinLab edit

}
