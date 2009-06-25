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

import java.util.Vector;
import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;

public class AlignStructMol {

    public PeakListRow row1,  row2;
    public double RT,  RT2;
    public boolean Aligned= false;
    public boolean ransacMaybeInLiers;
    public boolean ransacAlsoInLiers; 
    public Vector<Boolean> isAligned;

    public AlignStructMol(PeakListRow row1, PeakListRow row2) {
        this.row1 = row1;
        this.row2 = row2;
        this.isAligned = new Vector<Boolean>();
		RT = row1.getAverageRT();
		 RT2 = row2.getAverageRT();
    }

    public boolean isMols(PeakListRow row1, PeakListRow row2) {
        if (this.row1 == row1 && this.row2 == row2) {
            return true;
        }
        return false;
    }

	public boolean isHere(PeakListRow row){
		if (this.row1 == row || this.row2 == row) {
            return true;
        }
        return false;
	}

    public void setRT(RawDataFile data) {  
       // ChromatographicPeak peak = row1.getPeak(data);
		/*System.out.println(row2.getNumberOfPeaks());
        if (peak != null) {
            RT = peak.getRT();
        } else {
            RT = row1.getPeaks()[0].getRT();
       // }

        ChromatographicPeak peak2 = row2.getPeak(data);
        if (peak2 != null) {
            RT2 = peak2.getRT();
        } else {
            RT2 = row2.getPeaks()[0].getRT();
        }*/

		 RT = row1.getAverageRT();
		 RT2 = row2.getAverageRT();
    }

    public void addResult(boolean result) {
        this.isAligned.addElement(new Boolean(result));
    }

    public boolean getResult() {
        int truer = 0;
        int falser = 0;
        for (Boolean result : isAligned) {
            if (result) {
                truer++;
            } else {
                falser++;
            }
        }
        if (truer > 1) {
            return true;
        } else {
            return false;
        }

    }

	public void resetResult(){
		 this.isAligned = new Vector<Boolean>();       
	}
}



