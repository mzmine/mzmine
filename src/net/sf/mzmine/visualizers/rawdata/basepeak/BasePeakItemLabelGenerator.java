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
package net.sf.mzmine.visualizers.rawdata.basepeak;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * 
 */
class BasePeakItemLabelGenerator implements XYItemLabelGenerator {

    private BasePeakPlot plot;
    
    // TODO: get this from parameter storage
    private static NumberFormat mzFormat = new DecimalFormat("0.00");

    BasePeakItemLabelGenerator(BasePeakPlot plot) {
        this.plot = plot;
    }

    /**
     * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
     *      int, int)
     */
    public String generateLabel(XYDataset dataset, int series, int item) {

        final double originalX = dataset.getXValue(series, item);

        final double pointX = plot.getPlot().getDomainAxis().getRange()
                .getLength()
                / plot.getWidth();

        for (int i = item - 1; i > 1; i--) {
            if (dataset.getXValue(series, i) < (originalX - 50 * pointX))
                break;
            if (dataset.getYValue(series, item) <= dataset.getYValue(series, i))
                return null;
        }
        for (int i = item + 1; i < dataset.getItemCount(series); i++) {
            if (dataset.getXValue(series, i) > (originalX + 50 * pointX))
                break;
            if (dataset.getYValue(series, item) <= dataset.getYValue(series, i))
                return null;
        }

        return mzFormat.format(((BasePeakDataSet) dataset).getMZValue(item));
    }

}
