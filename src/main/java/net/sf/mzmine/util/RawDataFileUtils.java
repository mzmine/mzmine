/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;

/**
 * Raw data file related utilities
 */
public class RawDataFileUtils {

	public static Range findTotalRTRange(RawDataFile dataFiles[], int msLevel) {
		Range rtRange = null;
		for (RawDataFile file : dataFiles) {
			Range dfRange = file.getDataRTRange(msLevel);
			if (dfRange == null)
				continue;
			if (rtRange == null)
				rtRange = dfRange;
			else
				rtRange.extendRange(dfRange);
		}
		if (rtRange == null)
			rtRange = new Range(0);
		return rtRange;
	}

	public static Range findTotalMZRange(RawDataFile dataFiles[], int msLevel) {
		Range mzRange = null;
		for (RawDataFile file : dataFiles) {
			Range dfRange = file.getDataMZRange(msLevel);
			if (dfRange == null)
				continue;
			if (mzRange == null)
				mzRange = dfRange;
			else
				mzRange.extendRange(dfRange);
		}
		if (mzRange == null)
			mzRange = new Range(0);
		return mzRange;
	}

	/**
	 * Returns true if the given data file has mass lists for all MS1 scans
	 * 
	 */
	public static boolean hasMassLists(RawDataFile dataFile) {
		for (int scanNum : dataFile.getScanNumbers(1)) {
			Scan scan = dataFile.getScan(scanNum);
			if (scan.getMassLists().length == 0)
				return false;
		}
		return true;
	}

}
