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

package net.sf.mzmine.util;

import java.util.Arrays;

/**
 * Mathematical calculation-related helper class
 */
public class MathUtils {

    /**
     * Calculates q-quantile value of values. q=0.5 => median
     * 
     */
    public static double calcQuantile(double[] values, double q) {

	if (values.length == 0)
	    return 0;

	if (values.length == 1)
	    return values[0];

	if (q > 1)
	    q = 1;

	if (q < 0)
	    q = 0;

	double[] vals = (double[]) values.clone();

	Arrays.sort(vals);

	int ind1 = (int) Math.floor((vals.length - 1) * q);
	int ind2 = (int) Math.ceil((vals.length - 1) * q);

	return (vals[ind1] + vals[ind2]) / (double) 2;

    }

    public static double[] calcQuantile(double[] values, double[] qs) {

	double[] retVals = new double[qs.length];

	if (values.length == 0) {
	    for (int qInd = 0; qInd < qs.length; qInd++) {
		retVals[qInd] = 0;
	    }
	    return retVals;
	}
	if (values.length == 1) {
	    for (int qInd = 0; qInd < qs.length; qInd++) {
		retVals[qInd] = values[0];
	    }
	    return retVals;
	}

	double[] vals = (double[]) values.clone();
	Arrays.sort(vals);

	double q;
	int ind1, ind2;
	for (int qInd = 0; qInd < qs.length; qInd++) {
	    q = qs[qInd];

	    if (q > 1) {
		q = 1;
	    }
	    if (q < 0) {
		q = 0;
	    }

	    ind1 = (int) Math.floor((vals.length - 1) * q);
	    ind2 = (int) Math.ceil((vals.length - 1) * q);

	    retVals[qInd] = (vals[ind1] + vals[ind2]) / (double) 2;
	}

	return retVals;
    }

    public static double calcStd(double[] values) {

	double avg, stdev;
	double sum = 0;
	for (double d : values) {
	    sum += d;
	}
	avg = sum / values.length;

	sum = 0;
	for (double d : values) {
	    sum += (d - avg) * (d - avg);
	}

	stdev = (double) Math.sqrt((double) sum / (double) (values.length - 1));
	return stdev;
    }

    public static double calcCV(double[] values) {

	double avg, stdev;
	double sum = 0;
	for (double d : values) {
	    sum += d;
	}
	avg = sum / values.length;

	if (avg == 0)
	    return Double.NaN;

	sum = 0;
	for (double d : values) {
	    sum += (d - avg) * (d - avg);
	}

	stdev = (double) Math.sqrt((double) sum / (double) (values.length - 1));

	return stdev / avg;
    }

    public static double calcAvg(double[] values) {
	if (values.length == 0)
	    return Double.NaN;

	double sum = 0;
	for (double d : values) {
	    sum += d;
	}
	return sum / values.length;

    }

}