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

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.savitzkygolay;

public final class SGDerivative {

    /**
     * This method returns the second smoothed derivative values of an array.
     * 
     * @param double[] values
     * @param boolean is first derivative
     * @param int level of filter (1 - 12)
     * @return double[] derivative of values
     */
    public static double[] calculateDerivative(double[] values,
	    boolean firstDerivative, int levelOfFilter) {

	double[] derivative = new double[values.length];
	int M = 0;

	for (int k = 0; k < derivative.length; k++) {

	    // Determine boundaries
	    if (k <= levelOfFilter)
		M = k;
	    if (k + M > derivative.length - 1)
		M = derivative.length - (k + 1);

	    // Perform derivative using Savitzky Golay coefficients
	    for (int i = -M; i <= M; i++) {
		derivative[k] += values[k + i]
			* getSGCoefficient(M, i, firstDerivative);
	    }
	    // if ((Math.abs(derivative[k])) > maxValueDerivative)
	    // maxValueDerivative = Math.abs(derivative[k]);

	}

	return derivative;
    }

    /**
     * This method return the Savitzky-Golay 2nd smoothed derivative coefficient
     * from an array
     * 
     * @param M
     * @param signedC
     * @return
     */
    private static Double getSGCoefficient(int M, int signedC,
	    boolean firstDerivate) {

	int C = Math.abs(signedC), sign = 1;
	if (firstDerivate) {
	    if (signedC < 0)
		sign = -1;
	    return sign
		    * SGCoefficients.SGCoefficientsFirstDerivativeQuartic[M][C];
	} else {
	    return SGCoefficients.SGCoefficientsSecondDerivative[M][C];
	}

    }

}
