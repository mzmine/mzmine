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
 */

package net.sf.mzmine.modules.visualization.new3d;

import java.util.logging.Logger;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;

public class ThreeDSamplingTaskData {
	public Logger logger;
	public RawDataFile dataFile;
	public Scan[] scans;
	public Range<Double> rtRange;
	public Range<Double> mzRange;
	public int rtResolution;
	public int mzResolution;
	public int retrievedScans;
	public ThreeDDisplay display;
	public ThreeDBottomPanel bottomPanel;
	public double maxBinnedIntensity;

	public ThreeDSamplingTaskData(Logger logger, int retrievedScans) {
		this.logger = logger;
		this.retrievedScans = retrievedScans;
	}
}