/*
 * Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datasets;

import java.util.ArrayList;
import java.util.List;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;

/**
 * Data set for MzPeaks, used in feature detection preview
 */
public class DataPointsDataSet extends AbstractXYDataset
        implements IntervalXYDataset {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected DataPoint mzPeaks[];
    private String label;

    public DataPointsDataSet(String label, DataPoint mzPeaks[]) {
        this.label = label;
        this.mzPeaks = mzPeaks;
        // if we have some data points, remove extra zeros
        if (mzPeaks.length > 0) {
            List<DataPoint> dp = new ArrayList<>();
            dp.add(mzPeaks[0]);
            for (int i = 1; i < mzPeaks.length - 1; i++) {
                // previous , this and next are zero --> do not add this data
                // point
                if (Double.compare(mzPeaks[i - 1].getIntensity(), 0d) != 0
                        || Double.compare(mzPeaks[i].getIntensity(), 0d) != 0
                        || Double.compare(mzPeaks[i + 1].getIntensity(),
                                0d) != 0) {
                    dp.add(mzPeaks[i]);
                }

                dp.add(mzPeaks[mzPeaks.length - 1]);
                this.mzPeaks = dp.toArray(new DataPoint[0]);
            }
        }

    }

    @Override
    public int getSeriesCount() {
        return 1;
    }

    @Override
    public Comparable<?> getSeriesKey(int series) {
        return label;
    }

    @Override
    public int getItemCount(int series) {
        return mzPeaks.length;
    }

    @Override
    public Number getX(int series, int item) {
        return mzPeaks[item].getMZ();
    }

    @Override
    public Number getY(int series, int item) {
        return mzPeaks[item].getIntensity();
    }

    @Override
    public Number getEndX(int series, int item) {
        return getX(series, item).doubleValue();
    }

    @Override
    public double getEndXValue(int series, int item) {
        return getX(series, item).doubleValue();
    }

    @Override
    public Number getEndY(int series, int item) {
        return getY(series, item);
    }

    @Override
    public double getEndYValue(int series, int item) {
        return getYValue(series, item);
    }

    @Override
    public Number getStartX(int series, int item) {
        return getX(series, item).doubleValue();
    }

    @Override
    public double getStartXValue(int series, int item) {
        return getX(series, item).doubleValue();
    }

    @Override
    public Number getStartY(int series, int item) {
        return getY(series, item);
    }

    @Override
    public double getStartYValue(int series, int item) {
        return getYValue(series, item);
    }

}
