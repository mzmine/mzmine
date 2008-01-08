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

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.impl.SimplePeakListRow;

/**
 * This class represents a score between master peak list row and isotope
 * pattern
 */
class PeakVsRowScore {

    SimplePeakListRow masterListRow;
    PeakWrapper peak;
    float score = Float.MAX_VALUE;
    boolean goodEnough = false;

    public PeakVsRowScore(SimplePeakListRow masterListRow, PeakWrapper peak,
            float MZTolerance, boolean RTToleranceUseAbs,
            float RTToleranceValueAbs, float RTToleranceValuePercent,
            float MZvsRTBalance) {

        this.masterListRow = masterListRow;
        this.peak = peak;

        // Check that charge is same
        if ((peak instanceof IsotopePattern)
                && (masterListRow.getPeaks()[0] instanceof IsotopePattern)) {
            IsotopePattern myIsotope = (IsotopePattern) peak;
            IsotopePattern rowIsotope = (IsotopePattern) masterListRow.getPeaks()[0];
            if (myIsotope.getCharge() != rowIsotope.getCharge())
                return;
        }

        // Calculate differences between M/Z and RT values of isotope pattern
        // and median of the row
        float diffMZ = Math.abs(masterListRow.getAverageMZ()
                - peak.getPeak().getMZ());
        score = Float.MAX_VALUE;
        goodEnough = false;
        if (diffMZ < MZTolerance) {

            float diffRT = Math.abs(masterListRow.getAverageRT()
                    - peak.getPeak().getRT());

            // What type of RT tolerance is used?
            float rtTolerance = 0;
            if (RTToleranceUseAbs) {
                rtTolerance = RTToleranceValueAbs;
            } else {
                rtTolerance = RTToleranceValuePercent
                        * 0.5f
                        * (masterListRow.getAverageRT() + peak.getPeak().getRT());
            }

            if (diffRT < rtTolerance) {
                score = MZvsRTBalance * diffMZ + diffRT;
                goodEnough = true;
            }
        }
    }

    /**
     * This method return the master peak list that is compared in this score
     */
    public SimplePeakListRow getRow() {
        return masterListRow;
    }

    /**
     * This method return the isotope pattern that is compared in this score
     */
    public PeakWrapper getPeakWrapper() {
        return peak;
    }

    /**
     * This method returns score between the isotope pattern and the row (the
     * lower score, the better match)
     */
    public double getScore() {
        return score;
    }

    /**
     * This method returns true only if difference between isotope pattern and
     * row is within tolerance
     */
    public boolean isGoodEnough() {
        return goodEnough;
    }

}
