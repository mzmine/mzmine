/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.tic;

import java.util.Date;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataAcceptor;

import org.jfree.data.xy.AbstractXYZDataset;

/**
 * TIC visualizer data set
 */
class TICDataSet extends AbstractXYZDataset implements RawDataAcceptor {

    // redraw the chart every 100 ms while updating
    private static final int REDRAW_INTERVAL = 100;

    private TICVisualizerWindow visualizer;
    private RawDataFile dataFile;
    private int scanNumbers[], loadedScans = 0;
    private float mzValues[], intensityValues[], rtValues[];
    private float mzMin, mzMax;

    private static Date lastRedrawTime = new Date();

    TICDataSet(RawDataFile dataFile, int scanNumbers[], float mzMin,
            float mzMax, TICVisualizerWindow visualizer) {

        this.visualizer = visualizer;
        this.mzMin = mzMin;
        this.mzMax = mzMax;
        this.dataFile = dataFile;
        this.scanNumbers = scanNumbers;

        mzValues = new float[scanNumbers.length];
        intensityValues = new float[scanNumbers.length];
        rtValues = new float[scanNumbers.length];

    }

    int getIndex(float retentionTime, float intensity) {
        for (int i = 0; i < intensityValues.length; i++) {
            if ((Math.abs(retentionTime - rtValues[i]) < 0.0000001f)
                    && (Math.abs(intensity - intensityValues[i]) < 0.0000001f))
                return i;
        }
        return -1;
    }

    int getScanNumber(int index) {
        return scanNumbers[index];
    }

    RawDataFile getDataFile() {
        return dataFile;
    }

    /**
     * @see net.sf.mzmine.io.RawDataAcceptor#addScan(net.sf.mzmine.data.Scan)
     */
    public void addScan(final Scan scan, int index, int total) {

        float totalIntensity = 0;

        switch (visualizer.getPlotType()) {

        case TIC:
            float intensityValues[] = scan.getIntensityValues();
            float mzValues[] = scan.getMZValues();
            for (int j = 0; j < intensityValues.length; j++) {
                if ((mzValues[j] >= mzMin) && (mzValues[j] <= mzMax))
                    totalIntensity += intensityValues[j];
            }
            break;

        case BASE_PEAK:
            totalIntensity = scan.getBasePeakIntensity();
            break;

        }

        mzValues[index] = scan.getBasePeakMZ();
        intensityValues[index] = totalIntensity;
        rtValues[index] = scan.getRetentionTime();

        loadedScans++;
        
        // redraw every REDRAW_INTERVAL ms
        boolean notify = false;
        Date currentTime = new Date();
        if (currentTime.getTime() - lastRedrawTime.getTime() > REDRAW_INTERVAL) {
            notify = true;
            lastRedrawTime = currentTime;
        }

        // always redraw when we add last value
        if (index == total - 1)
            notify = true;
        
        if (notify)
            this.fireDatasetChanged();

    }

    @Override public int getSeriesCount() {
        return 1;
    }

    @Override public Comparable getSeriesKey(int series) {
        return dataFile.toString();
    }

    public Number getZ(int series, int item) {
        return mzValues[item];
    }

    public int getItemCount(int series) {
        return loadedScans;
    }

    public Number getX(int series, int item) {
        return rtValues[item];
    }

    public Number getY(int series, int item) {
        return intensityValues[item];
    }
}
