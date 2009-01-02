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

package net.sf.mzmine.data.impl;

import java.util.Vector;

import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.MzDataTable;
import net.sf.mzmine.util.Range;

public class SimpleMzDataTable implements MzDataTable {
	
	private MzDataPoint dataPoints[];
	
	public SimpleMzDataTable(MzDataPoint[] dataPoints){
		this.dataPoints = dataPoints;
	}

	public MzDataPoint[] getDataPoints() {
		return dataPoints;
	}

	public MzDataPoint[] getDataPointsByMass(Range mzRange) {
		int startIndex, endIndex;
		for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
			if (dataPoints[startIndex].getMZ() >= mzRange.getMin())
				break;
		}

		for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
			if (dataPoints[endIndex].getMZ() > mzRange.getMax())
				break;
		}

		MzDataPoint pointsWithinRange[] = new MzDataPoint[endIndex - startIndex];

		// Copy the relevant points
		System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0, endIndex
				- startIndex);

		return pointsWithinRange;
	}

	public MzDataPoint[] getDataPointsOverIntensity(double intensity) {
		int index;
		Vector<MzDataPoint> points = new Vector<MzDataPoint>();
		
		for (index = 0; index < dataPoints.length; index++) {
			if (dataPoints[index].getIntensity() >= intensity)
				points.add(dataPoints[index]);
		}

		MzDataPoint pointsOverIntensity[] = points.toArray(new MzDataPoint[0]);

		return pointsOverIntensity;
	}

	public int getNumberOfDataPoints() {
		return dataPoints.length;
	}
	
	/**
	 * @param mzValues
	 *            m/z values to set
	 * @param intensityValues
	 *            Intensity values to set
	 */
	public void setDataPoints(MzDataPoint[] dataPoints) {
		this.dataPoints = dataPoints;
	}


}
