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

import net.sf.mzmine.data.RawDataFile;

public class RegressionInfo {

	private double slope;
	private double intercept;
	private RawDataFile file;
	private RawDataFile file2;

	public RegressionInfo(double slope, double intercept, RawDataFile rawDataFile1, RawDataFile rawDataFile2) {
		this.slope = slope;
		this.intercept = intercept;
		this.file = rawDataFile1;
		this.file2 = rawDataFile2;
	}

	public double getSlope(){
		return slope;
	}

	public double getIntercept(){
		return intercept;
	}

	public RawDataFile getRawDataFile1(){
		return file;
	}

	public RawDataFile getRawDataFile2(){
		return file2;
	}
}
