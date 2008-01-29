package net.sf.mzmine.modules.peakpicking.anothercentroid;

import java.util.Vector;

import net.sf.mzmine.data.DataPoint;

class OneDimIsotopePattern {

	Vector<DataPoint> addedDataPoints;
	
	OneDimIsotopePattern(DataPoint highestDatapoint) {
		addedDataPoints = new Vector<DataPoint>();
		addedDataPoints.add(highestDatapoint);
	}
	
	protected void addDataPoint(DataPoint dataPoint) {
		addedDataPoints.add(dataPoint);
	}
	
	protected DataPoint[] getDataPoints() {
		return addedDataPoints.toArray(new DataPoint[0]);
	}
	
	protected int getNumberOfDataPoints() {
		return addedDataPoints.size();
	}
	
}
