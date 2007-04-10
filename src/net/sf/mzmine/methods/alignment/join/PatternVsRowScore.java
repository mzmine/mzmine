/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.methods.alignment.join;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.util.IsotopePatternUtils;

/**
 * This class represents a score between master peak list row and isotope
 * pattern
 */
class PatternVsRowScore {

    MasterIsotopeListRow masterIsotopeListRow;
    IsotopePatternWrapper wrappedIsotopePattern;
    double score = Double.MAX_VALUE;
    boolean goodEnough = false;

    public PatternVsRowScore(MasterIsotopeListRow masterIsotopeListRow,
            IsotopePatternWrapper wrappedIsotopePattern,
            IsotopePatternUtils isotopePatternUtil, double MZTolerance, boolean RTToleranceUseAbs, double RTToleranceValueAbs, double RTToleranceValuePercent, double MZvsRTBalance) {

        this.masterIsotopeListRow = masterIsotopeListRow;
        this.wrappedIsotopePattern = wrappedIsotopePattern;

        // Check that charge is same
        if (masterIsotopeListRow.getChargeState() != wrappedIsotopePattern.getIsotopePattern().getChargeState()) {
            return;
        }

        // Get monoisotopic peak
        Peak monoPeak = isotopePatternUtil.getMonoisotopicPeak(wrappedIsotopePattern.getIsotopePattern());

        // Calculate differences between M/Z and RT values of isotope pattern
        // and median of the row
        double diffMZ = java.lang.Math.abs(masterIsotopeListRow.getMonoisotopicMZ()
                - monoPeak.getNormalizedMZ());
        score = Double.MAX_VALUE;
        goodEnough = false;
        if (diffMZ < MZTolerance) {

            double diffRT = java.lang.Math.abs(masterIsotopeListRow.getMonoisotopicRT()
                    - monoPeak.getNormalizedRT());

            // What type of RT tolerance is used?
            double rtTolerance = 0;
            if (RTToleranceUseAbs) {
                rtTolerance = RTToleranceValueAbs;
            } else {
                rtTolerance = RTToleranceValuePercent
                        * 0.5
                        * (masterIsotopeListRow.getMonoisotopicRT() + monoPeak.getNormalizedRT());
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
    public MasterIsotopeListRow getMasterIsotopeListRow() {
        return masterIsotopeListRow;
    }

    /**
     * This method return the isotope pattern that is compared in this score
     */
    public IsotopePatternWrapper getWrappedIsotopePattern() {
        return wrappedIsotopePattern;
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
