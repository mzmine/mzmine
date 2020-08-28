/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.ims.imsvisualizer;

import io.github.mzmine.datamodel.Scan;
import org.jfree.data.xy.AbstractXYDataset;

public class RetentionTimeIntensityXYDataset extends AbstractXYDataset {

    private final Scan[] scans;
    private final double[] xValues;
    private final double[] yValues;

    public RetentionTimeIntensityXYDataset(DataFactory dataFactory) {
        xValues = dataFactory.getRetentionTimeretentionTimeIntensity();
        yValues = dataFactory.getIntensityretentionTimeIntensity();
        scans = dataFactory.getScans();
    }

    @Override
    public int getSeriesCount() {
        return 1;
    }

    @Override
    public Comparable getSeriesKey(int series) {
        return getRowKey(series);
    }

    public Comparable<?> getRowKey(int item) {
        return scans[item].toString();
    }

    @Override
    public int getItemCount(int series) {
        return xValues.length;
    }

    @Override
    public Number getX(int series, int item) {
        return xValues[item];
    }

    @Override
    public Number getY(int series, int item) {
        return yValues[item];
    }
}
