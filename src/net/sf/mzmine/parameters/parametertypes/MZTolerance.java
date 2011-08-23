/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
 * 
 */
public class MZTolerance {

	// Tolerance can be either absolute (in m/z) or relative (in ppm)
	private boolean isAbsolute;
	private double tolerance;

	public MZTolerance(boolean isAbsolute, double tolerance) {
		this.isAbsolute = isAbsolute;
		this.tolerance = tolerance;
	}

	public boolean isAbsolute() {
		return isAbsolute;
	}

	public double getTolerance() {
		return tolerance;
	}

	public Range getToleranceRange(double mzValue) {
		double absoluteTolerance = isAbsolute ? tolerance
				: (mzValue / 1000000 * tolerance);
		return new Range(mzValue - absoluteTolerance, mzValue
				+ absoluteTolerance);
	}

	public Range getToleranceRange(Range mzRange) {
		double absoluteMinTolerance = isAbsolute ? tolerance : (mzRange
				.getMin() / 1000000 * tolerance);
		double absoluteMaxTolerance = isAbsolute ? tolerance : (mzRange
				.getMax() / 1000000 * tolerance);
		return new Range(mzRange.getMin() - absoluteMinTolerance,
				mzRange.getMax() + absoluteMaxTolerance);
	}

	public boolean checkWithinTolerance(double mz1, double mz2) {
		Range toleranceRange = getToleranceRange(mz1);
		return toleranceRange.contains(mz2);
	}

}
