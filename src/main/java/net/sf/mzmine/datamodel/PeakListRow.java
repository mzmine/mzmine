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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.datamodel;

/**
 * 
 * 
 *
 */
public interface PeakListRow {

    /**
     * Return raw data with peaks on this row
     */
    RawDataFile[] getRawDataFiles();

    /**
     * Returns ID of this row
     */
    int getID();

    /**
     * Returns number of peaks assigned to this row
     */
    int getNumberOfPeaks();

    /**
     * Return peaks assigned to this row
     */
    Feature[] getPeaks();

    /**
     * Returns peak for given raw data file
     */
    Feature getPeak(RawDataFile rawData);

    /**
     * Add a peak
     */
    void addPeak(RawDataFile rawData, Feature peak);

    /**
     * Has a peak?
     */
    boolean hasPeak(Feature peak);

    /**
     * Has a peak?
     */
    boolean hasPeak(RawDataFile rawData);

    /**
     * Returns average M/Z for peaks on this row
     */
    double getAverageMZ();

    /**
     * Returns average RT for peaks on this row
     */
    double getAverageRT();

    /**
     * Returns average height for peaks on this row
     */
    double getAverageHeight();

    /**
     * Returns average area for peaks on this row
     */
    double getAverageArea();

    /**
     * Returns comment for this row
     */
    String getComment();

    /**
     * Sets comment for this row
     */
    void setComment(String comment);

    /**
     * Add a new identity candidate (result of identification method)
     * 
     * @param identity
     *            New peak identity
     * @param preffered
     *            boolean value to define this identity as preferred identity
     */
    void addPeakIdentity(PeakListRowAnnotation identity, boolean preffered);

    /**
     * Remove identity candidate
     * 
     * @param identity
     *            Peak identity
     */
    void removePeakIdentity(PeakListRowAnnotation identity);

    /**
     * Returns all candidates for this peak's identity
     * 
     * @return Identity candidates
     */
    PeakListRowAnnotation[] getPeakIdentities();

    /**
     * Returns preferred peak identity among candidates
     * 
     * @return Preferred identity
     */
    PeakListRowAnnotation getPreferredPeakIdentity();

    /**
     * Sets a preferred peak identity among candidates
     * 
     * @param identity
     *            Preferred identity
     */
    void setPreferredPeakIdentity(PeakListRowAnnotation identity);

    /**
     * Returns maximum raw data point intensity among all peaks in this row
     * 
     * @return Maximum intensity
     */
    double getDataPointMaxIntensity();

    /**
     * Returns the most intense peak in this row
     */
    Feature getBestPeak();

    /**
     * Returns the most intense isotope pattern in this row. If there are no
     * isotope patterns present in the row, returns null.
     */
    IsotopePattern getBestIsotopePattern();

}
