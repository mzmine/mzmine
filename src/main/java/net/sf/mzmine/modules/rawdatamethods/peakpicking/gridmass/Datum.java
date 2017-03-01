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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.peakpicking.gridmass;

import net.sf.mzmine.datamodel.DataPoint;

class Datum implements Comparable<Datum> {
    double mz = 0;
    double intensity = 0;
    int spotId = 0;
    int scan = 0;
    boolean available = true;
    boolean neighbours = false;
    boolean included = true;
    double mzOriginal = 0;
    double intensityOriginal = 0;

    Datum(DataPoint dp, int iScan, DataPoint dpOriginal) {
	mz = dp.getMZ();
	intensity = dp.getIntensity();
	scan = iScan;
	mzOriginal = dpOriginal.getMZ();
	intensityOriginal = dpOriginal.getIntensity();
    }

    public int compareTo(Datum other) {
	if (this.intensity > other.intensity)
	    return -1;
	if (this.intensity < other.intensity)
	    return 1;

	// equal intensities, then sort by lower mz
	if (this.mz < other.mz)
	    return -1;
	if (this.mz > other.mz)
	    return 1;

	// otherwise they are equal in intensity and mz
	return 0;
    }

    public String toString() {
	return "MZ=" + Math.round(mz * 10000) / 10000 + " | Int=" + intensity
		+ " | Scan=" + scan + " | spotId=" + spotId;
    }
}
