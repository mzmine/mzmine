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

import java.util.Vector;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;

import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Picked peaks data set;
 */
class PeakListDataSet extends AbstractXYDataset implements IntervalXYDataset {

    private int scanNumber;
    private PeakList peakList;

    private Peak displayedPeaks[];

    PeakListDataSet(RawDataFile dataFile, int scanNumber, PeakList peakList) {

        this.scanNumber = scanNumber;
        this.peakList = peakList;

        Peak peaks[] = peakList.getPeaks(dataFile);

        Vector<Peak> candidates = new Vector<Peak>();
        for (Peak peak : peaks) {
            float peakDataPoint[] = peak.getRawDatapoint(scanNumber);
            if (peakDataPoint != null)
                candidates.add(peak);
        }
        displayedPeaks = candidates.toArray(new Peak[0]);

    }

    @Override public int getSeriesCount() {
        return 1;
    }

    @Override public Comparable getSeriesKey(int series) {
        return null;
    }

    public PeakList getPeakList() {
        return peakList;
    }

    public Peak getPeak(int series, int item) {
        return displayedPeaks[item];
    }

    public int getItemCount(int series) {
        return displayedPeaks.length;
    }

    public Number getX(int series, int item) {
        float dataPoint[] = displayedPeaks[item].getRawDatapoint(scanNumber);
        return dataPoint[0];
    }

    public Number getY(int series, int item) {
        float dataPoint[] = displayedPeaks[item].getRawDatapoint(scanNumber);
        return dataPoint[1];
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
