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

public interface IntensityMergeMode {

    static IntensityMergeMode[] values() {
        return new IntensityMergeMode[]{SUM,MAXIMUM,AVERAGE};
    }

    public double merge(DataPoint[] sources);

    public static final IntensityMergeMode MAXIMUM = new IntensityMergeMode() {
        @Override
        public double merge(DataPoint[] sources) {
            double max = 0d;
            for (DataPoint p : sources)
                max = Math.max(p.getIntensity(), max);
            return max;
        }

        @Override
        public String toString() {
            return "maximum intensity";
        }
    };
    public static final IntensityMergeMode SUM = new IntensityMergeMode() {
        @Override
        public double merge(DataPoint[] sources) {
            double sum = 0d;
            for (DataPoint p : sources)
                sum += p.getIntensity();
            return sum;
        }
        @Override
        public String toString() {
            return "sum intensities";
        }
    };
    public static final IntensityMergeMode AVERAGE = new IntensityMergeMode() {
        @Override
        public double merge(DataPoint[] sources) {
            double avg = 0d;
            for (DataPoint p : sources)
                avg += p.getIntensity();
            return avg / sources.length;
        }
        @Override
        public String toString() {
            return "mean intensity";
        }
    };

}
