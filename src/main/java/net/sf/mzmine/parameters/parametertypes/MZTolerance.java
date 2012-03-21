/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.parameters.parametertypes;

import net.sf.mzmine.util.Range;

/**
 * This class represents m/z tolerance. Tolerance is set using absolute (m/z)
 * and relative (ppm) values. The tolerance range is calculated as the maximum
 * of the absolute and relative values.
 */
public class MZTolerance {

    // Tolerance has absolute (in m/z) and relative (in ppm) values
    private final double mzTolerance, ppmTolerance;

    public MZTolerance(double mzTolerance, double ppmTolerance) {
        this.mzTolerance = mzTolerance;
        this.ppmTolerance = ppmTolerance;
    }

    public double getMzTolerance() {
        return mzTolerance;
    }

    public double getPpmTolerance() {
        return ppmTolerance;
    }

    public double getMzToleranceForMass(double mzValue) {
        double calculatedMzTolerance = Math.max(mzTolerance,
                (mzValue / 1000000d * ppmTolerance));
        return calculatedMzTolerance;
    }

    public double getPpmToleranceForMass(double mzValue) {
        double calculatedPpmTolerance = Math.max(mzTolerance
                / (mzValue / 1000000d), ppmTolerance);
        return calculatedPpmTolerance;
    }

    public Range getToleranceRange(double mzValue) {
        double absoluteTolerance = Math.max(mzTolerance,
                (mzValue / 1000000 * ppmTolerance));
        return new Range(mzValue - absoluteTolerance, mzValue
                + absoluteTolerance);
    }

    public Range getToleranceRange(Range mzRange) {
        double absoluteMinTolerance = Math.max(mzTolerance,
                (mzRange.getMin() / 1000000 * ppmTolerance));
        double absoluteMaxTolerance = Math.max(mzTolerance,
                (mzRange.getMax() / 1000000 * ppmTolerance));
        return new Range(mzRange.getMin() - absoluteMinTolerance,
                mzRange.getMax() + absoluteMaxTolerance);
    }

    public boolean checkWithinTolerance(double mz1, double mz2) {
        Range toleranceRange = getToleranceRange(mz1);
        return toleranceRange.contains(mz2);
    }

}
