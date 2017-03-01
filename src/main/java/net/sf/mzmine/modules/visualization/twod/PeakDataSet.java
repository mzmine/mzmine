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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.twod;

import java.util.Vector;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;

import org.jfree.data.xy.AbstractXYDataset;

import com.google.common.collect.Range;

/**
 * Picked peaks data set
 */
class PeakDataSet extends AbstractXYDataset {

    private static final long serialVersionUID = 1L;

    private PeakList peakList;

    private Feature peaks[];

    private PeakDataPoint dataPoints[][];

    PeakDataSet(RawDataFile dataFile, PeakList peakList, Range<Double> rtRange,
	    Range<Double> mzRange) {

	this.peakList = peakList;

	Vector<Feature> processedPeaks = new Vector<Feature>(1024, 1024);
	Vector<PeakDataPoint[]> processedPeakDataPoints = new Vector<PeakDataPoint[]>(
		1024, 1024);
	Vector<PeakDataPoint> thisPeakDataPoints = new Vector<PeakDataPoint>();

	Feature allPeaks[] = peakList.getPeaks(dataFile);

	for (Feature peak : allPeaks) {

	    int scanNumbers[] = peak.getScanNumbers();

	    for (int scan : scanNumbers) {

		double rt = dataFile.getScan(scan).getRetentionTime();
		DataPoint dp = peak.getDataPoint(scan);
		if (dp != null) {
		    if (rtRange.contains(rt) && mzRange.contains(dp.getMZ())) {
			PeakDataPoint newDP = new PeakDataPoint(scan, rt, dp);
			thisPeakDataPoints.add(newDP);
		    }
		}

	    }

	    if (thisPeakDataPoints.size() > 0) {
		PeakDataPoint dpArray[] = thisPeakDataPoints
			.toArray(new PeakDataPoint[0]);
		processedPeaks.add(peak);
		processedPeakDataPoints.add(dpArray);
		thisPeakDataPoints.clear();
	    }

	}

	peaks = processedPeaks.toArray(new Feature[0]);
	dataPoints = processedPeakDataPoints.toArray(new PeakDataPoint[0][]);

    }

    @Override
    public int getSeriesCount() {
	return dataPoints.length;
    }

    @Override
    public Comparable<?> getSeriesKey(int series) {
	return new Integer(series);
    }

    public int getItemCount(int series) {
	return dataPoints[series].length;
    }

    public PeakList getPeakList() {
	return peakList;
    }

    public Feature getPeak(int series) {
	return peaks[series];
    }

    public PeakDataPoint getDataPoint(int series, int item) {
	return dataPoints[series][item];
    }

    public Number getX(int series, int item) {
	return dataPoints[series][item].getRT();
    }

    public Number getY(int series, int item) {
	return dataPoints[series][item].getMZ();
    }

}
