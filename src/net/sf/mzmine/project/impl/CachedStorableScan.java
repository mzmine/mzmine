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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.project.impl;

import java.lang.ref.SoftReference;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;

public class CachedStorableScan extends StorableScan {

	private SoftReference<DataPoint[]> dataPointsCache;

	public CachedStorableScan(Scan originalScan, RawDataFileImpl rawDataFile,
			long scanFileOffset, int numberOfDataPoints) {
		super(originalScan, rawDataFile, scanFileOffset, numberOfDataPoints);
	}

	public CachedStorableScan(RawDataFileImpl rawDataFile, long scanFileOffset,
			int numberOfDataPoints, int scanNumber, int msLevel,
			double retentionTime, int parentScan, double precursorMZ,
			int precursorCharge, int[] fragmentScans, boolean centroided) {
		super(rawDataFile, scanFileOffset, numberOfDataPoints, scanNumber,
				msLevel, retentionTime, parentScan, precursorMZ,
				precursorCharge, fragmentScans, centroided);
	}

	/**
	 * We cache the data with help of the garbage collector
	 */
	@Override
	public DataPoint[] getDataPoints() {

		DataPoint dataPoints[] = null;

		if (dataPointsCache != null)
			dataPoints = dataPointsCache.get();

		if (dataPoints == null) {
			dataPoints = super.getDataPoints();
			dataPointsCache = new SoftReference<DataPoint[]>(dataPoints);
		}

		return dataPoints;
	}

}
