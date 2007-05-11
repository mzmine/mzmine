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

/**
 * 
 */
package net.sf.mzmine.modules.visualization.rawdata.tic;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * 
 */
class BasePeakItemLabelGenerator implements XYItemLabelGenerator {

    private TICPlot plot;

    // TODO: get this from parameter storage
    private static NumberFormat mzFormat = new DecimalFormat("0.00");

    BasePeakItemLabelGenerator(TICPlot plot) {
        this.plot = plot;
    }

    /**
     * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
     *      int, int)
     */
    public String generateLabel(XYDataset dataset, int series, int item) {

        // TODO: write comments
        final double originalX = dataset.getXValue(series, item);
        final double originalY = dataset.getYValue(series, item);

        final double pointX = plot.getXYPlot().getDomainAxis().getRange().getLength()
                / plot.getWidth();

        final int itemCount = dataset.getItemCount(series);

        final double limitLeft = originalX - (40 * pointX);
        final double limitRight = originalX + (40 * pointX);

        for (int i = 1; (item - i > 0) || (item + i < itemCount); i++) {

            if ((item - i > 0)
                    && (dataset.getXValue(series, item - i) < limitLeft)
                    && ((item + i >= itemCount) || (dataset.getXValue(series,
                            item + i) > limitRight)))
                break;

            if ((item + i < itemCount)
                    && (dataset.getXValue(series, item + i) > limitRight)
                    && ((item - i <= 0) || (dataset.getXValue(series, item - i) < limitLeft)))
                break;

            if ((item - i > 0)
                    && (originalY <= dataset.getYValue(series, item - i)))
                return null;

            if ((item + i < itemCount)
                    && (originalY <= dataset.getYValue(series, item + i)))
                return null;

        }

        double mz = ((TICDataSet) dataset).getMZValue(item);
        return mzFormat.format(mz);

    }
}
