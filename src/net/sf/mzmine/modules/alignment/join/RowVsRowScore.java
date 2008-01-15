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

package net.sf.mzmine.modules.alignment.join;

import net.sf.mzmine.data.PeakListRow;

/**
 * This class represents a score between master peak list row and isotope
 * pattern
 */
class RowVsRowScore implements Comparable<RowVsRowScore> {

    private PeakListRow peakListRow, alignedRow;
    float score;

    RowVsRowScore(PeakListRow peakListRow, PeakListRow alignedRow,
            float MZvsRTBalance) {

        this.peakListRow = peakListRow;
        this.alignedRow = alignedRow;

        // Calculate differences between M/Z and RT values
        float diffMZ = Math.abs(peakListRow.getAverageMZ()
                - alignedRow.getAverageMZ());

        float diffRT = Math.abs(peakListRow.getAverageRT()
                - alignedRow.getAverageRT());

        score = MZvsRTBalance * diffMZ + diffRT;

    }

    /**
     * This method returns the peak list row which is being aligned
     */
    PeakListRow getPeakListRow() {
        return peakListRow;
    }

    /**
     * This method returns the row of aligned peak list
     */
    PeakListRow getAlignedRow() {
        return alignedRow;
    }

    /**
     * This method returns score between the these two peaks (the lower score,
     * the better match)
     */
    float getScore() {
        return score;
    }

    public int compareTo(RowVsRowScore object) {
        Float myScore = new Float(score);
        Float objectScore = new Float(object.getScore());
        return myScore.compareTo(objectScore);
    }

}
