package net.sf.mzmine.data.impl;

import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.MzDataTable;
import net.sf.mzmine.util.Range;

public class SimpleMzDataTable implements MzDataTable {
	
	private DataPoint dataPoints[];
	
	public SimpleMzDataTable(DataPoint[] dataPoints){
		this.dataPoints = dataPoints;
	}

	public DataPoint[] getDataPoints() {
		return dataPoints;
	}

	public DataPoint[] getDataPointsByMass(Range mzRange) {
		int startIndex, endIndex;
		for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
			if (dataPoints[startIndex].getMZ() >= mzRange.getMin())
				break;
		}

		for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
			if (dataPoints[endIndex].getMZ() > mzRange.getMax())
				break;
		}

		DataPoint pointsWithinRange[] = new DataPoint[endIndex - startIndex];

		// Copy the relevant points
		System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0, endIndex
				- startIndex);

		return pointsWithinRange;
	}

	public DataPoint[] getDataPointsOverIntensity(double intensity) {
		int index;
		Vector<DataPoint> points = new Vector<DataPoint>();
		
		for (index = 0; index < dataPoints.length; index++) {
			if (dataPoints[index].getIntensity() >= intensity)
				points.add(dataPoints[index]);
		}

		DataPoint pointsOverIntensity[] = points.toArray(new DataPoint[0]);

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
	public void setDataPoints(DataPoint[] dataPoints) {
		this.dataPoints = dataPoints;
	}


}
