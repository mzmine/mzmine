/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 */

package net.sf.mzmine.modules.tools.msmsspectramerge;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.util.scans.ScanUtils;

public interface MzMergeMode {

    public static MzMergeMode[] values() {
        return new MzMergeMode[]{MOST_INTENSE, WEIGHTED_AVERAGE_CUTOFF_OUTLIERS, WEIGHTED_AVERAGE};
    }

    public double merge(DataPoint[] sources);



    public static MzMergeMode WEIGHTED_AVERAGE = new MzMergeMode() {
        @Override
        public double merge(DataPoint[] sources) {
            double mz=0d, intens=0d;
            for (DataPoint d : sources) {
                mz += d.getMZ()*d.getIntensity();
                intens += d.getIntensity();
            }
            return mz/intens;
        }
        @Override
        public String toString() {
            return "weighted average";
        }
    };

    public static MzMergeMode MOST_INTENSE = new MzMergeMode() {
        @Override
        public double merge(DataPoint[] sources) {
            double mz=Double.NEGATIVE_INFINITY, intens=Double.NEGATIVE_INFINITY;
            for (DataPoint d : sources) {
                if (d.getIntensity()>intens) {
                    mz = d.getMZ();
                    intens = d.getIntensity();
                }
            }
            return mz;
        }
        @Override
        public String toString() {
            return "most intense";
        }
    };

    public static MzMergeMode WEIGHTED_AVERAGE_CUTOFF_OUTLIERS = new MzMergeMode() {
        @Override
        public double merge(DataPoint[] sources) {
            if (sources.length >= 4) {
                sources = sources.clone();
                ScanUtils.sortDataPointsByMz(sources);
                int i = (int)(sources.length*0.25);
                double mz=0d, intens=0d;
                for (int k=i; k < sources.length-i; ++k) {
                    mz += sources[k].getMZ()*sources[k].getIntensity();
                    intens += sources[k].getIntensity();
                }
                return mz/intens;
            } else return WEIGHTED_AVERAGE.merge(sources);
        }
        @Override
        public String toString() {
            return "weighted average (remove outliers)";
        }
    };

}
