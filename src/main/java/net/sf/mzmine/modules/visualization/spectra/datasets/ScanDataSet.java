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

package net.sf.mzmine.modules.visualization.spectra.datasets;

import java.util.Map;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;

import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

import com.google.common.collect.Range;

/**
 * Spectra visualizer data set for scan data points
 */
public class ScanDataSet extends AbstractXYDataset implements IntervalXYDataset {

    private static final long serialVersionUID = 1L;

    private String label;
    private Scan scan;
    private Map<DataPoint, String> annotation;

    /*
     * Save a local copy of m/z and intensity values, because accessing the scan
     * every time may cause reloading the data from HDD
     */
    private DataPoint dataPoints[];

    public ScanDataSet(Scan scan) {
	this("Scan #" + scan.getScanNumber(), scan);
    }

    public ScanDataSet(String label, Scan scan) {
	this.dataPoints = scan.getDataPoints();
	this.scan = scan;
	this.label = label;
    }

    @Override
    public int getSeriesCount() {
	return 1;
    }

    @Override
    public Comparable<?> getSeriesKey(int series) {
	return label;
    }

    public int getItemCount(int series) {
	return dataPoints.length;
    }

    public Number getX(int series, int item) {
	return dataPoints[item].getMZ();
    }

    public Number getY(int series, int item) {
	return dataPoints[item].getIntensity();
    }

    public Number getEndX(int series, int item) {
	return getX(series, item);
    }

    public double getEndXValue(int series, int item) {
	return getXValue(series, item);
    }

    public Number getEndY(int series, int item) {
	return getY(series, item);
    }

    public double getEndYValue(int series, int item) {
	return getYValue(series, item);
    }

    public Number getStartX(int series, int item) {
	return getX(series, item);
    }

    public double getStartXValue(int series, int item) {
	return getXValue(series, item);
    }

    public Number getStartY(int series, int item) {
	return getY(series, item);
    }

    public double getStartYValue(int series, int item) {
	return getYValue(series, item);
    }

    /**
     * This function finds highest data point intensity in given m/z range. It
     * is important for normalizing isotope patterns.
     */
    public double getHighestIntensity(Range<Double> mzRange) {

	double maxIntensity = 0;
	for (DataPoint dp : dataPoints) {
	    if ((mzRange.contains(dp.getMZ()))
		    && (dp.getIntensity() > maxIntensity))
		maxIntensity = dp.getIntensity();
	}

	return maxIntensity;
    }

    public Scan getScan() {
	return scan;
    }

    public void addAnnotation(Map<DataPoint, String> annotation) {
	this.annotation = annotation;
    }

    public String getAnnotation(int item) {
	if (annotation == null)
	    return null;
	DataPoint itemDataPoint = dataPoints[item];
	for (DataPoint key : annotation.keySet()) {
	    if (Math.abs(key.getMZ() - itemDataPoint.getMZ()) < 0.001)
		return annotation.get(key);
	}
	return null;
    }

}
