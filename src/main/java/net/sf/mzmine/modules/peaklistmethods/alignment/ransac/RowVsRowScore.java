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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.alignment.ransac;

import net.sf.mzmine.datamodel.PeakListRow;

/**
 * This class represents a score between peak list row and aligned peak list row
 */
public class RowVsRowScore implements Comparable<RowVsRowScore> {

    private PeakListRow peakListRow, alignedRow;
    double score;
    private String errorMessage;

    public RowVsRowScore(PeakListRow peakListRow, PeakListRow alignedRow,
	    double mzMaxDiff, double rtMaxDiff, double correctedRT)
	    throws Exception {

	this.alignedRow = alignedRow;
	this.peakListRow = peakListRow;

	// Calculate differences between m/z and RT values
	double mzDiff = Math.abs(peakListRow.getAverageMZ()
		- alignedRow.getAverageMZ());
	double rtDiff = Math.abs(correctedRT - alignedRow.getAverageRT());

	score = ((1 - mzDiff / mzMaxDiff) + (1 - rtDiff / rtMaxDiff));
    }

    /**
     * This method returns the peak list row which is being aligned
     */
    public PeakListRow getPeakListRow() {
	return peakListRow;
    }

    /**
     * This method returns the row of aligned peak list
     */
    public PeakListRow getAlignedRow() {
	return alignedRow;
    }

    /**
     * This method returns score between the these two peaks (the lower score,
     * the better match)
     */
    public double getScore() {
	return score;
    }

    String getErrorMessage() {
	return errorMessage;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(RowVsRowScore object) {

	// We must never return 0, because the TreeSet in JoinAlignerTask would
	// treat such elements as equal
	if (score < object.getScore()) {
	    return 1;
	} else {
	    return -1;
	}

    }

}
