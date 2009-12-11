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
import java.util.List;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.util.Range;
import org.apache.commons.math.stat.regression.SimpleRegression;

public class RegressionInfo {

    private RawDataFile file;
    private RawDataFile file2;
    private List<RTs> data;
    double[] values;

    public RegressionInfo(RawDataFile rawDataFile1, RawDataFile rawDataFile2) {
        this.file = rawDataFile1;
        this.file2 = rawDataFile2;
        this.data = new ArrayList<RTs>();
    }

    public SimpleRegression getSimpleRegression(Range rtRange, double step) {
        SimpleRegression regression = new SimpleRegression();
        while (regression.getN() < 15) {
            double min = rtRange.getMin();
            double max = rtRange.getMax();
            min-=step;
            if(min < 0) min = 0;
            max+=step;
            rtRange = new Range(min, max);
            for (RTs rt : data) {
                if (rtRange.contains(rt.RT)) {
                    regression.addData(rt.RT2, rt.RT);
                }

            }
        }
        return regression;
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

    private class RTs {

        double RT;
        double RT2;

        public RTs() {
        }

        public RTs(double RT, double RT2) {
            this.RT = RT;
            this.RT2 = RT2;
        }
    }
}