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
    public static float calcQuantile(float[] values, float q) {

        if (values.length == 0)
            return 0;

        if (values.length == 1)
            return values[0];

        if (q > 1)
            q = 1;

        if (q < 0)
            q = 0;

        float[] vals = (float[]) values.clone();

        Arrays.sort(vals);

        int ind1 = (int) Math.floor((vals.length - 1) * q);
        int ind2 = (int) Math.ceil((vals.length - 1) * q);

        return (vals[ind1] + vals[ind2]) / (float) 2;

    }

    public static float[] calcQuantile(float[] values, float[] qs) {

        float[] retVals = new float[qs.length];

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

        float[] vals = (float[]) values.clone();
        Arrays.sort(vals);

        float q;
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

            retVals[qInd] = (vals[ind1] + vals[ind2]) / (float) 2;
        }

        return retVals;
    }

    public static float calcStd(float[] values) {

        float avg, stdev;
        float sum = 0;
        for (float d : values) {
            sum += d;
        }
        avg = sum / values.length;

        sum = 0;
        for (float d : values) {
            sum += (d - avg) * (d - avg);
        }

        stdev = (float) Math.sqrt((float) sum / (float) (values.length - 1));
        return stdev;
    }

    public static float calcCV(float[] values) {

        float avg, stdev;
        float sum = 0;
        for (float d : values) {
            sum += d;
        }
        avg = sum / values.length;

        if (avg == 0)
            return Float.NaN;

        sum = 0;
        for (float d : values) {
            sum += (d - avg) * (d - avg);
        }

        stdev = (float) Math.sqrt((float) sum / (float) (values.length - 1));

        return stdev / avg;
    }

    public static float calcAvg(float[] values) {
        if (values.length == 0)
            return Float.NaN;

        float sum = 0;
        for (float d : values) {
            sum += d;
        }
        return sum / values.length;

    }

}