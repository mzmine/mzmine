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
package net.sf.mzmine.modules.alignment.ransac;

import java.util.Comparator;
import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;

public class AlignStructMol implements Comparator{

    public PeakListRow row1,  row2;
    public DataPoint dp1,  dp2;
    public double RT,  RT2;
    public boolean Aligned = false;
    public boolean ransacMaybeInLiers;
    public boolean ransacAlsoInLiers;

    public AlignStructMol(PeakListRow row1, PeakListRow row2) {
        this.row1 = row1;
        this.row2 = row2;
        RT = row1.getAverageRT();
        RT2 = row2.getAverageRT();
    }

    public AlignStructMol(DataPoint dp, DataPoint dp2) {
        this.dp1 = dp;
        this.dp2 = dp2;
        RT = dp1.getMZ();
        RT2 = dp2.getMZ();
    }

    public AlignStructMol(PeakListRow row1, PeakListRow row2, RawDataFile file, RawDataFile file2) {
        this.row1 = row1;
        this.row2 = row2;
        if (row1.getPeak(file) != null) {
            RT = row1.getPeak(file).getRT();
        } else {
            RT = row1.getAverageRT();
        }

        if (row2.getPeak(file2) != null) {
            RT2 = row2.getPeak(file2).getRT();
        } else {
            RT = row1.getAverageRT();
        }
    }

    AlignStructMol() {
        
    }

    public boolean isMols(PeakListRow row1, PeakListRow row2) {
        if (this.row1 == row1 && this.row2 == row2) {
            return true;
        }
        return false;
    }

    public AlignStructMol(ChromatographicPeak row1, ChromatographicPeak row2) {
        RT = row1.getRT();
        RT2 = row2.getRT();
    }

    public double getCorrectedRT(PeakListRow row) {
        if (this.Aligned) {
            if (row == row1) {
                return RT2;
            } else if (row == row2) {
                return RT;
            }
        }
        return -1;
    }

    public int compare(Object arg0, Object arg1) {
            if (((AlignStructMol) arg0).RT < ((AlignStructMol) arg1).RT) {
                return -1;
            } else {
                return 1;
            }
        }
}



