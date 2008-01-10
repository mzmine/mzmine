/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.spectra;

import net.sf.mzmine.data.Scan;

import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Spectra visualizer data set for scan data points
 */
class ScanDataSet extends AbstractXYDataset implements IntervalXYDataset {

    private Scan scan;

    /*
     * Save a local copy of m/z and intensity values, because accessing the scan
     * every time may cause reloading the data from HDD
     */
    private float mzValues[], intensityValues[];

    ScanDataSet(Scan scan) {
        this.scan = scan;
        mzValues = scan.getMZValues();
        intensityValues = scan.getIntensityValues();
    }

    @Override public int getSeriesCount() {
        return 1;
    }

    @Override public Comparable getSeriesKey(int series) {
        return "Scan #" + scan.getScanNumber();
    }

    public int getItemCount(int series) {
        /*
         * Add 2 extra data points with zero intensity at the beginning and end
         * of scan range, to keep the auto-range feature consistant with actual
         * scan range
         */
        return mzValues.length + 2;
    }

    public Number getX(int series, int item) {
        if (item == 0)
            return scan.getMZRangeMin();
        if (item == mzValues.length + 1)
            return scan.getMZRangeMax();
        return mzValues[item - 1];
    }

    public Number getY(int series, int item) {
        if ((item == 0) || (item == mzValues.length + 1))
            return 0f;
        return intensityValues[item - 1];
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

}
