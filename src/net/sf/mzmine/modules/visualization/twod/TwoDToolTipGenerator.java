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
package net.sf.mzmine.modules.visualization.twod;

import java.text.NumberFormat;

import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

import org.jfree.chart.labels.XYZToolTipGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

/**
 * 
 */
class TwoDToolTipGenerator implements XYZToolTipGenerator {

    /**
     * @see org.jfree.chart.labels.XYZToolTipGenerator#generateToolTip(org.jfree.data.xy.XYZDataset,
     *      int, int)
     */
    public String generateToolTip(XYDataset dataset, int series, int item) {
        return generateToolTip((XYZDataset) dataset, series, item);
    }

    /**
     * @see org.jfree.chart.labels.XYToolTipGenerator#generateToolTip(org.jfree.data.xy.XYDataset,
     *      int, int)
     */
    public String generateToolTip(XYZDataset dataset, int series, int item) {
        
        Desktop desktop = MainWindow.getInstance();
        NumberFormat rtFormat = desktop.getRTFormat();
        NumberFormat mzFormat = desktop.getMZFormat();
        NumberFormat intensityFormat = desktop.getIntensityFormat();
        
        double rtValue = dataset.getXValue(series, item);
        double mzValue = dataset.getYValue(series, item);
        double intValue = dataset.getZValue(series, item);
        return "Retention time: " + rtFormat.format(rtValue) + ", m/z: "
                + mzFormat.format(mzValue) + ", intensity: "
                + intensityFormat.format(intValue);
    }

}
