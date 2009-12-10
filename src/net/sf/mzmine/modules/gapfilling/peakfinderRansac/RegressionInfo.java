/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.gapfilling.peakfinderRansac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.sf.mzmine.data.RawDataFile;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math.analysis.interpolation.UnivariateRealInterpolator;
import org.apache.commons.math.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math.stat.regression.SimpleRegression;

public class RegressionInfo {

    private RawDataFile file;
    private RawDataFile file2;
    private List<RTs> data;
    private Random rand;

    public RegressionInfo(RawDataFile rawDataFile1, RawDataFile rawDataFile2) {
        this.file = rawDataFile1;
        this.file2 = rawDataFile2;
        this.data = new ArrayList<RTs>();
        this.rand = new Random();
    }

    public SimpleRegression getSimpleRegression(int map) {
        SimpleRegression regression = new SimpleRegression();
        for (RTs rt : data) {
            if (map == 0) {
                regression.addData(rt.RT, rt.RT2);
            } else {
                regression.addData(rt.RT2, rt.RT);
            }
        }
        return regression;
    }

    public PolynomialSplineFunction getRegression(int map) throws MathException {
        UnivariateRealInterpolator loess = new LoessInterpolator();
        double[] xval = new double[data.size()];
        double[] yval = new double[data.size()];
        int i = 0;
        Collections.sort(data, new RTs(map));
        for (RTs rt : data) {
            if (map == 0) {
                xval[i] = rt.RT;
                yval[i++] = rt.RT2;
            } else {
                xval[i] = rt.RT2;
                yval[i++] = rt.RT;
            }
        }
        return (PolynomialSplineFunction) loess.interpolate(xval, yval);
    }

    public RawDataFile getRawDataFile1() {
        return file;
    }

    public RawDataFile getRawDataFile2() {
        return file2;
    }

    public void addData(double RT, double RT2) {
        this.data.add(new RTs(RT, RT2));
    }

    private class RTs implements Comparator {

        double RT;
        double RT2;
        int map;

        public RTs(int map) {
            this.map = map;
        }

        public RTs(double RT, double RT2) {
            this.RT = RT + 0.001 / rand.nextDouble();
            this.RT2 = RT2 + 0.001 / rand.nextDouble();
        }

        public int compare(Object arg0, Object arg1) {

            // We must never return 0, because the TreeSet in JoinAlignerTask would
            // treat such elements as equal
            if (map == 0) {
                if (((RTs) arg0).RT < ((RTs) arg1).RT) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                if (((RTs) arg0).RT2 < ((RTs) arg1).RT2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
    }
}