/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.gapfilling.peakfinderRTcorrection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.sf.mzmine.util.Range;
import org.apache.commons.math.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math.stat.regression.SimpleRegression;

public class RegressionInfo {

    private List<RTs> data;
    private PolynomialSplineFunction function;
    private Range RTrange;

    public RegressionInfo(Range RTrange) {
        this.data = new ArrayList<RTs>();
        this.RTrange = RTrange;

    }

    public void setFuction() {
        function = getPolynomialFunction();
    }

    public double predict(double RT) {
        try {
            return function.value(RT);
        } catch (Exception ex) {
            return -1;
        }
    }

    public void addData(double RT, double RT2) {
        this.data.add(new RTs(RT, RT2));
    }

    private PolynomialSplineFunction getPolynomialFunction() {
        data = this.smooth(data);
        Collections.sort(data, new RTs());

        double[] xval = new double[data.size()];
        double[] yval = new double[data.size()];
        int i = 0;

        for (RTs rt : data) {
            xval[i] = rt.RT;
            yval[i++] = rt.RT2;
        }

        try {
            LoessInterpolator loess = new LoessInterpolator();
            return loess.interpolate(xval, yval);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<RTs> smooth(List<RTs> list) {

        // Add one point at the begining and another at the end of the list to
        // ampliate the RT limits to cover the RT range completly
        try {
            Collections.sort(list, new RTs());

            RTs firstPoint = list.get(0);
            RTs lastPoint = list.get(list.size() - 1);

            double min = Math.abs(firstPoint.RT - RTrange.getMin());

            double RTx = firstPoint.RT - min;
            double RTy = firstPoint.RT2 - min;

            RTs newPoint = new RTs(RTx, RTy);
            list.add(newPoint);

            double max = Math.abs(RTrange.getMin() - lastPoint.RT);
            RTx = lastPoint.RT + max;
            RTy = lastPoint.RT2 + max;

            newPoint = new RTs(RTx, RTy);
            list.add(newPoint);
        } catch (Exception exception) {
        }

        // Add points to the model in between of the real points to smooth the regression model
        Collections.sort(list, new RTs());

        for (int i = 0; i < list.size() - 1; i++) {
            RTs point1 = list.get(i);
            RTs point2 = list.get(i + 1);
            if (point1.RT < point2.RT - 2) {
                SimpleRegression regression = new SimpleRegression();
                regression.addData(point1.RT, point1.RT2);
                regression.addData(point2.RT, point2.RT2);
                double rt = point1.RT + 1;
                while (rt < point2.RT) {
                    RTs newPoint = new RTs(rt, regression.predict(rt));
                    list.add(newPoint);
                    rt++;
                }

            }
        }

        return list;
    }

    private class RTs implements Comparator {

        double RT;
        double RT2;
        int map;

        public RTs() {
        }

        public RTs(double RT, double RT2) {
            this.RT = RT + 0.001 / Math.random();
            this.RT2 = RT2 + 0.001 / Math.random();
        }

        public int compare(Object arg0, Object arg1) {
            if (((RTs) arg0).RT < ((RTs) arg1).RT) {
                return -1;
            } else {
                return 1;
            }

        }
    }
}