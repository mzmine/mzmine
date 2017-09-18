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
 * WARRANTY; without even the im plied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.datamodel;

import javax.annotation.Nonnull;

import com.google.common.collect.Range;

/**
 * 
 */
public interface PeakList {

    public interface PeakListAppliedMethod {

	@Nonnull
	public String getDescription();

	@Nonnull
	public String getParameters();

    }

    /**
     * @return Short descriptive name for the peak list
     */
    public String getName();

    /**
     * Change the name of this peak list
     */
    public void setName(String name);

    /**
     * Returns number of raw data files participating in the peak list
     */
    public int getNumberOfRawDataFiles();

    /**
     * Returns all raw data files participating in the peak list
     */
    public RawDataFile[] getRawDataFiles();

    /**
     * Returns true if this peak list contains given file
     */
    public boolean hasRawDataFile(RawDataFile file);

    /**
     * Returns a raw data file
     * 
     * @param position
     *            Position of the raw data file in the matrix (running numbering
     *            from left 0,1,2,...)
     */
    public RawDataFile getRawDataFile(int position);

    /**
     * Returns number of rows in the alignment result
     */
    public int getNumberOfRows();

    /**
     * Returns the peak of a given raw data file on a give row of the peak list
     * 
     * @param row
     *            Row of the peak list
     * @param rawDataFile
     *            Raw data file where the peak is detected/estimated
     */
    public Feature getPeak(int row, RawDataFile rawDataFile);

    /**
     * Returns all peaks for a raw data file
     */
    public Feature[] getPeaks(RawDataFile rawDataFile);

    /**
     * Returns all peaks on one row
     */
    public PeakListRow getRow(int row);

    /**
     * Returns all peak list rows
     */
    public PeakListRow[] getRows();

    /**
     * Returns all rows with average retention time within given range
     * 
     * @param startRT
     *            Start of the retention time range
     * @param endRT
     *            End of the retention time range
     */
    public PeakListRow[] getRowsInsideScanRange(Range<Double> rtRange);

    /**
     * Returns all rows with average m/z within given range
     * 
     * @param startMZ
     *            Start of the m/z range
     * @param endMZ
     *            End of the m/z range
     */
    public PeakListRow[] getRowsInsideMZRange(Range<Double> mzRange);

    /**
     * Returns all rows with average m/z and retention time within given range
     * 
     * @param startRT
     *            Start of the retention time range
     * @param endRT
     *            End of the retention time range
     * @param startMZ
     *            Start of the m/z range
     * @param endMZ
     *            End of the m/z range
     */
    public PeakListRow[] getRowsInsideScanAndMZRange(Range<Double> rtRange,
	    Range<Double> mzRange);

    /**
     * Returns all peaks overlapping with a retention time range
     * 
     * @param startRT
     *            Start of the retention time range
     * @param endRT
     *            End of the retention time range
     */
    public Feature[] getPeaksInsideScanRange(RawDataFile file,
	    Range<Double> rtRange);

    /**
     * Returns all peaks in a given m/z range
     * 
     * @param startMZ
     *            Start of the m/z range
     * @param endMZ
     *            End of the m/z range
     */
    public Feature[] getPeaksInsideMZRange(RawDataFile file,
	    Range<Double> mzRange);

    /**
     * Returns all peaks in a given m/z & retention time ranges
     * 
     * @param startRT
     *            Start of the retention time range
     * @param endRT
     *            End of the retention time range
     * @param startMZ
     *            Start of the m/z range
     * @param endMZ
     *            End of the m/z range
     */
    public Feature[] getPeaksInsideScanAndMZRange(RawDataFile file,
	    Range<Double> rtRange, Range<Double> mzRange);

    /**
     * Returns maximum raw data point intensity among all peaks in this peak
     * list
     * 
     * @return Maximum intensity
     */
    public double getDataPointMaxIntensity();

    /**
     * Add a new row to the peak list
     */
    public void addRow(PeakListRow row);

    /**
     * Removes a row from this peak list
     * 
     */
    public void removeRow(int row);

    /**
     * Removes a row from this peak list
     * 
     */
    public void removeRow(PeakListRow row);

    /**
     * Returns a row number of given peak
     */
    public int getPeakRowNum(Feature peak);

    /**
     * Returns a row containing given peak
     */
    public PeakListRow getPeakRow(Feature peak);

    public void addDescriptionOfAppliedTask(PeakListAppliedMethod appliedMethod);

    /**
     * Returns all tasks (descriptions) applied to this peak list
     */
    public PeakListAppliedMethod[] getAppliedMethods();

    /**
     * Returns the whole m/z range of the peak list
     */
    public Range<Double> getRowsMZRange();

    /**
     * Returns the whole retention time range of the peak list
     */
    public Range<Double> getRowsRTRange();

}
