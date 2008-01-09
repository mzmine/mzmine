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

package net.sf.mzmine.modules.visualization.tic;

import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.userinterface.Desktop;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * 
 */
class TICItemLabelGenerator implements XYItemLabelGenerator {

    private TICPlot plot;
    private TICVisualizerWindow ticWindow;

    TICItemLabelGenerator(TICPlot plot, TICVisualizerWindow ticWindow) {
        this.plot = plot;
        this.ticWindow = ticWindow;
    }

    /**
     * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
     *      int, int)
     */
    public String generateLabel(XYDataset dataset, int series, int item) {

        final double originalX = dataset.getXValue(series, item);
        final double originalY = dataset.getYValue(series, item);

        final double xLength = plot.getXYPlot().getDomainAxis().getRange().getLength();
        final double pointX = xLength / plot.getWidth();

        final int itemCount = dataset.getItemCount(series);

        final double limitLeft = originalX - (40 * pointX);
        final double limitRight = originalX + (40 * pointX);

        for (int i = 1; (item - i > 0) || (item + i < itemCount); i++) {

            if (((item - i < 0) || (dataset.getXValue(series, item - i) < limitLeft))
                    && ((item + i >= itemCount) || (dataset.getXValue(series,
                            item + i) > limitRight)))
                break;

            if ((item - i >= 0)
                    && (dataset.getXValue(series, item - i) >= limitLeft)
                    && (originalY <= dataset.getYValue(series, item - i)))
                return null;

            if ((item + i < itemCount)
                    && (dataset.getXValue(series, item + i) <= limitRight)
                    && (originalY <= dataset.getYValue(series, item + i)))
                return null;

        }

        Desktop desktop = MZmineCore.getDesktop();
        Object plotType = ticWindow.getPlotType();
        
        String label = "";
        
        if (plotType == TICVisualizerParameters.plotTypeBP) {
            double mz = ((TICDataSet) dataset).getZValue(series, item);
            NumberFormat mzFormat = desktop.getMZFormat();
            label = mzFormat.format(mz);
        }
        
        if (plotType == TICVisualizerParameters.plotTypeTIC) {
            double tic = dataset.getYValue(series, item);
            NumberFormat intensityFormat = desktop.getIntensityFormat();
            label = intensityFormat.format(tic);        }
        
        return label;

    }

}
