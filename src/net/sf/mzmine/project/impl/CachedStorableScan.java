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

	/**
	 * Clone constructor
	 */
	public CachedStorableScan(Scan sc, RawDataFileImpl rawDataFile) {
		this(sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(), sc
				.getParentScanNumber(), sc.getPrecursorMZ(), sc.getPrecursorCharge(), sc
				.getFragmentScanNumbers(), sc.getDataPoints(), sc
				.isCentroided(), rawDataFile);
	}

	/**
	 * Constructor for creating scan with given data
	 */
	public CachedStorableScan(int scanNumber, int msLevel, double retentionTime,
			int parentScan, double precursorMZ, int precursorCharge, int fragmentScans[],
			DataPoint[] dataPoints, boolean centroided,
			RawDataFileImpl rawDataFile) {

		super(scanNumber, msLevel, retentionTime, parentScan, precursorMZ, precursorCharge,
				fragmentScans, dataPoints, centroided, rawDataFile);

		dataPointsCache = new SoftReference<DataPoint[]>(dataPoints);

	}

	/**
	 * We cache the data with help of GC
	 */
	@Override
	public DataPoint[] getDataPoints() {
		// the GC eats the data, reload from disk
		DataPoint[] p = dataPointsCache.get();
		if (p == null) {
			p = super.getDataPoints();
			dataPointsCache = new SoftReference<DataPoint[]>(p);
		}
		return p;
	}

}
